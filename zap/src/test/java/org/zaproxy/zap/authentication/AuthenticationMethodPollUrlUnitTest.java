/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2013 The ZAP Development Team
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
package org.zaproxy.zap.authentication;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.httpclient.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;
import org.zaproxy.zap.authentication.AuthenticationMethod.AuthCheckingStrategy;
import org.zaproxy.zap.authentication.AuthenticationMethod.AuthPollFrequencyUnits;
import org.zaproxy.zap.testutils.NanoServerHandler;
import org.zaproxy.zap.testutils.TestUtils;

public class AuthenticationMethodPollUrlUnitTest extends TestUtils {

    private static final String LOGGED_IN_INDICATOR = "logged in";
    private static final String LOGGED_IN_BODY =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                    + "Pellentesque auctor nulla id turpis placerat vulputate."
                    + LOGGED_IN_INDICATOR
                    + " Proin tempor bibendum eros rutrum. ";

    private HttpMessage loginMessage;
    private AuthenticationMethod method;

    @BeforeEach
    public void setUp() throws Exception {
        loginMessage = new HttpMessage();
        HttpRequestHeader header = new HttpRequestHeader();
        header.setURI(new URI("http://www.example.com", true));
        loginMessage.setRequestHeader(header);
        method = Mockito.mock(AuthenticationMethod.class, Mockito.CALLS_REAL_METHODS);
        method.setAuthCheckingStrategy(AuthCheckingStrategy.EACH_RESP);

        this.startServer();
    }

    @AfterEach
    public void shutDownServer() throws Exception {
        stopServer();
    }

    @Test
    public void shouldPollOnFirstRequest() throws NullPointerException, IOException {
        // Given
        String test = "/shouldPollOnFirstRequest/test";
        String pollUrl = "/shouldPollOnFirstRequest/pollUrl";
        final List<String> orderedReqs = new ArrayList<>();

        this.nano.addHandler(
                new NanoServerHandler(pollUrl) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        orderedReqs.add(session.getUri());
                        return newFixedLengthResponse(LOGGED_IN_BODY);
                    }
                });
        HttpMessage testMsg = this.getHttpMessage(test);
        HttpMessage pollMsg = this.getHttpMessage(pollUrl);

        method.setAuthCheckingStrategy(AuthCheckingStrategy.POLL_URL);
        method.setPollUrl(pollMsg.getRequestHeader().getURI().toString());
        method.setPollFrequencyUnits(AuthPollFrequencyUnits.REQUESTS);
        method.setPollFrequency(5);
        method.setLoggedInIndicatorPattern(LOGGED_IN_INDICATOR);

        // When/Then
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(orderedReqs.size(), is(1));
        assertThat(orderedReqs.get(0), is(pollUrl));
    }

    @Test
    public void shouldPollOnSpecifiedNumberOfRequests() throws NullPointerException, IOException {
        // Given
        String test = "/shouldPollOnFirstRequest/test";
        String pollUrl = "/shouldPollOnFirstRequest/pollUrl";
        final List<String> orderedReqs = new ArrayList<>();

        this.nano.addHandler(
                new NanoServerHandler(pollUrl) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        orderedReqs.add(session.getUri());
                        return newFixedLengthResponse(LOGGED_IN_BODY);
                    }
                });
        HttpMessage testMsg = this.getHttpMessage(test);
        HttpMessage pollMsg = this.getHttpMessage(pollUrl);

        method.setAuthCheckingStrategy(AuthCheckingStrategy.POLL_URL);
        method.setPollUrl(pollMsg.getRequestHeader().getURI().toString());
        method.setPollFrequencyUnits(AuthPollFrequencyUnits.REQUESTS);
        method.setPollFrequency(5);
        method.setLoggedInIndicatorPattern(LOGGED_IN_INDICATOR);

        // When/Then
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(orderedReqs.size(), is(1));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(orderedReqs.size(), is(1));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(orderedReqs.size(), is(2));
        assertThat(orderedReqs.get(0), is(pollUrl));
        assertThat(orderedReqs.get(1), is(pollUrl));
    }

    @Test
    public void shouldPollEveryFailingRequest() throws NullPointerException, IOException {
        // Given
        String test = "/shouldPollEveryFailingRequest/test";
        String pollUrl = "/shouldPollEveryFailingRequest/pollUrl";
        final List<String> orderedReqs = new ArrayList<>();

        this.nano.addHandler(
                new NanoServerHandler(pollUrl) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        orderedReqs.add(session.getUri());
                        return newFixedLengthResponse("");
                    }
                });
        HttpMessage testMsg = this.getHttpMessage(test);
        HttpMessage pollMsg = this.getHttpMessage(pollUrl);

        method.setAuthCheckingStrategy(AuthCheckingStrategy.POLL_URL);
        method.setPollUrl(pollMsg.getRequestHeader().getURI().toString());
        method.setPollFrequencyUnits(AuthPollFrequencyUnits.REQUESTS);
        method.setPollFrequency(5);
        method.setLoggedInIndicatorPattern(LOGGED_IN_INDICATOR);

        // When/Then
        assertThat(method.isAuthenticated(testMsg, null), is(false));
        assertThat(orderedReqs.size(), is(1));
        assertThat(method.isAuthenticated(testMsg, null), is(false));
        assertThat(orderedReqs.size(), is(2));
        assertThat(method.isAuthenticated(testMsg, null), is(false));
        assertThat(orderedReqs.size(), is(3));
        assertThat(method.isAuthenticated(testMsg, null), is(false));
        assertThat(orderedReqs.size(), is(4));
    }

    @Test
    public void shouldPollWhenForced() throws NullPointerException, IOException {
        // Given
        String test = "/shouldPollWhenForced/test";
        String pollUrl = "/shouldPollWhenForced/pollUrl";
        final List<String> orderedReqs = new ArrayList<>();

        this.nano.addHandler(
                new NanoServerHandler(pollUrl) {
                    @Override
                    protected Response serve(IHTTPSession session) {
                        orderedReqs.add(session.getUri());
                        return newFixedLengthResponse(LOGGED_IN_BODY);
                    }
                });
        HttpMessage testMsg = this.getHttpMessage(test);
        HttpMessage pollMsg = this.getHttpMessage(pollUrl);

        method.setAuthCheckingStrategy(AuthCheckingStrategy.POLL_URL);
        method.setPollUrl(pollMsg.getRequestHeader().getURI().toString());
        method.setPollFrequencyUnits(AuthPollFrequencyUnits.REQUESTS);
        method.setPollFrequency(500);
        method.setLoggedInIndicatorPattern(LOGGED_IN_INDICATOR);

        // When/Then
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(orderedReqs.size(), is(1));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(orderedReqs.size(), is(1));
        method.setLastPollResult(false);
        assertThat(method.isAuthenticated(testMsg, null), is(true));
        assertThat(orderedReqs.size(), is(2));
        assertThat(orderedReqs.get(0), is(pollUrl));
        assertThat(orderedReqs.get(1), is(pollUrl));
    }
}
