/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2020 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.pscan;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Source;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.parosproxy.paros.network.HttpHeader;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.ZAP;
import org.zaproxy.zap.extension.pscan.scanner.StatsPassiveScanner;
import org.zaproxy.zap.utils.Stats;
import org.zaproxy.zap.utils.StatsListener;

public class StatsPassiveScannerUnitTest {

    static {
        Config.LoggerProvider = ZAP.JERICHO_LOGGER_PROVIDER;
    }

    private StatsListener listener;
    private PluginPassiveScanner scanner;

    @BeforeEach
    public void setUp() {
        scanner = new StatsPassiveScanner();
        listener = spy(StatsListener.class);
        Stats.addListener(listener);
    }

    @AfterEach
    public void cleanup() {
        Stats.removeListener(listener);
    }

    @Test
    public void shouldCountOnlyCodeAndTimingGivenNoContentType()
            throws URIException, HttpMalformedHeaderException {
        // Given
        HttpMessage msg = new HttpMessage(new URI("http://example.com/", true));
        // When
        scanner.scanHttpResponseReceive(msg, -1, new Source(""));
        // Then
        verify(listener)
                .counterInc("http://example.com", StatsPassiveScanner.CODE_STATS_PREFIX + "0");
        verify(listener)
                .counterInc(
                        "http://example.com", StatsPassiveScanner.RESPONSE_TIME_STATS_PREFIX + "0");
        Mockito.verifyNoMoreInteractions(listener);
    }

    @Test
    public void shouldCountMessageWithContentTypeOnce()
            throws URIException, HttpMalformedHeaderException {
        // Given
        HttpMessage msg = new HttpMessage(new URI("http://example.com/", true));
        msg.getResponseHeader().addHeader(HttpHeader.CONTENT_TYPE, "text/html");
        // When
        scanner.scanHttpResponseReceive(msg, -1, new Source(""));
        // Then
        verify(listener)
                .counterInc("http://example.com", StatsPassiveScanner.CODE_STATS_PREFIX + "0");
        verify(listener)
                .counterInc(
                        "http://example.com",
                        StatsPassiveScanner.CONTENT_TYPE_STATS_PREFIX + "text/html");
        verify(listener)
                .counterInc(
                        "http://example.com", StatsPassiveScanner.RESPONSE_TIME_STATS_PREFIX + "0");
    }

    @Test
    public void shouldProperlyParseAndCountSegmentedContentType()
            throws URIException, HttpMalformedHeaderException {
        // Given
        String contentType = "multipart/byteranges; boundary=00000000000000000018";
        HttpMessage msg = new HttpMessage(new URI("http://example.com/", true));
        msg.getResponseHeader().addHeader(HttpHeader.CONTENT_TYPE, contentType);
        // When
        scanner.scanHttpResponseReceive(msg, -1, new Source(""));
        // Then
        verify(listener)
                .counterInc(
                        "http://example.com",
                        StatsPassiveScanner.CONTENT_TYPE_STATS_PREFIX + "multipart/byteranges");
    }

    @Test
    public void shouldProperlyParseAndCountSegmentedContentTypeIncludingCharset()
            throws URIException, HttpMalformedHeaderException {
        // Given
        String contentType = "multipart/byteranges; charset=UTF-8; boundary=00000000000000000018";
        HttpMessage msg = new HttpMessage(new URI("http://example.com/", true));
        msg.getResponseHeader().addHeader(HttpHeader.CONTENT_TYPE, contentType);
        // When
        scanner.scanHttpResponseReceive(msg, -1, new Source(""));
        // Then
        verify(listener)
                .counterInc(
                        "http://example.com",
                        StatsPassiveScanner.CONTENT_TYPE_STATS_PREFIX
                                + "multipart/byteranges; charset=UTF-8");
    }

    @Test
    public void shouldProperlyParseAndCountSegmentedContentTypeIncludingCharsetAsLastSegment()
            throws URIException, HttpMalformedHeaderException {
        // Given
        String contentType = "multipart/byteranges; boundary=00000000000000000018; charset=UTF-8";
        HttpMessage msg = new HttpMessage(new URI("http://example.com/", true));
        msg.getResponseHeader().addHeader(HttpHeader.CONTENT_TYPE, contentType);
        // When
        scanner.scanHttpResponseReceive(msg, -1, new Source(""));
        // Then
        verify(listener)
                .counterInc(
                        "http://example.com",
                        StatsPassiveScanner.CONTENT_TYPE_STATS_PREFIX
                                + "multipart/byteranges; charset=UTF-8");
    }

    @Test
    public void
            shouldProperlyParseAndCountSegmentedContentTypeIncludingCharsetAndSpuriousTrailingSeperator()
                    throws URIException, HttpMalformedHeaderException {
        // Given
        String contentType = "multipart/byteranges; charset=UTF-8; boundary=00000000000000000018; ";
        HttpMessage msg = new HttpMessage(new URI("http://example.com/", true));
        msg.getResponseHeader().addHeader(HttpHeader.CONTENT_TYPE, contentType);
        // When
        scanner.scanHttpResponseReceive(msg, -1, new Source(""));
        // Then
        verify(listener)
                .counterInc(
                        "http://example.com",
                        StatsPassiveScanner.CONTENT_TYPE_STATS_PREFIX
                                + "multipart/byteranges; charset=UTF-8");
    }
}
