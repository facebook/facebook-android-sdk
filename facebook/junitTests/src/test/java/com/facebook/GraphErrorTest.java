/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook;

import com.facebook.internal.FacebookRequestErrorClassification;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

@PrepareForTest( {
        AccessToken.class,
        AccessTokenCache.class,
        FacebookSdk.class,
        GraphRequest.class
})
public final class GraphErrorTest extends FacebookPowerMockTestCase {

    @Before
    public void before() throws Exception {
        mockStatic(FacebookSdk.class);
        when(FacebookSdk.isInitialized()).thenReturn(true);
        when(FacebookSdk.getApplicationContext()).thenReturn(Robolectric.application);
        stub(method(AccessTokenCache.class, "save")).toReturn(null);
    }

    @Test
    public void testAccessTokenResetOnTokenError() throws JSONException, IOException {
        AccessToken accessToken = mock(AccessToken.class);
        AccessToken.setCurrentAccessToken(accessToken);

        JSONObject errorBody = new JSONObject();
        errorBody.put("message", "Invalid OAuth access token.");
        errorBody.put("type", "OAuthException");
        errorBody.put("code", FacebookRequestErrorClassification.EC_INVALID_TOKEN);
        JSONObject error = new JSONObject();
        error.put("error", errorBody);
        String errorString = error.toString();

        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(400);

        GraphRequest request = mock(GraphRequest.class);
        when(request.getAccessToken()).thenReturn(accessToken);
        GraphRequestBatch batch = new GraphRequestBatch(request);

        assertNotNull(AccessToken.getCurrentAccessToken());
        GraphResponse.createResponsesFromString(errorString, connection, batch);
        assertNull(AccessToken.getCurrentAccessToken());
    }
}
