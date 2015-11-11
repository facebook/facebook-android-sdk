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

package com.facebook.internal;

import com.facebook.FacebookRequestError;
import com.facebook.FacebookTestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class FacebookRequestErrorClassificationTest extends FacebookTestCase {
    private final String errorClassificationJSON =
        "{" +
        "   \"android_sdk_error_categories\": [" +
        "      {" +
        "         \"name\": \"other\"," +
        "         \"items\": [" +
        "           { \"code\": 102, \"subcodes\": [ 459, 464 ] }," +
        "           { \"code\": 190, \"subcodes\": [ 459, 464 ] }" +
        "         ]" +
        "      }," +
        "      {" +
        "         \"name\": \"login_recoverable\"," +
        "         \"items\": [ { \"code\": 102 }, { \"code\": 190 } ]," +
        "         \"recovery_message\": \"Please log into this app again to reconnect your Facebook account.\"" +
        "      }," +
        "      {" +
        "         \"name\": \"transient\"," +
        "         \"items\": [ { \"code\": 1 }, { \"code\": 2 }, { \"code\": 4 }, { \"code\": 9 }, { \"code\": 17 }, { \"code\": 341 } ]" +
        "      }" +
        "   ]," +
        "   \"id\": \"233936543368280\"" +
        "}";


    @Test
    public void testX() throws Exception {
        JSONObject serverResponse = new JSONObject(errorClassificationJSON);
        JSONArray jsonArray = serverResponse.getJSONArray("android_sdk_error_categories");
        FacebookRequestErrorClassification errorClassification =
                FacebookRequestErrorClassification.createFromJSON(jsonArray);
        assertNotNull(errorClassification);
        assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.OTHER));
        assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.TRANSIENT));
        assertNotNull(errorClassification.getRecoveryMessage(
                FacebookRequestError.Category.LOGIN_RECOVERABLE));
        assertEquals(2, errorClassification.getOtherErrors().size());
        assertEquals(2, errorClassification.getLoginRecoverableErrors().size());
        assertEquals(6, errorClassification.getTransientErrors().size());
        // test subcodes
        assertEquals(2, errorClassification.getOtherErrors().get(102).size());
        assertNull(errorClassification.getLoginRecoverableErrors().get(102));
    }
}
