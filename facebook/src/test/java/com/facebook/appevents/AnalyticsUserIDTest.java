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

package com.facebook.appevents;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

public class AnalyticsUserIDTest extends FacebookPowerMockTestCase {
    @Before
    public void init() {
        FacebookSdk.setApplicationId("123456789");
        FacebookSdk.sdkInitialize(RuntimeEnvironment.application);
        AnalyticsUserIDStore.initStore();
    }

    @Test
    public void testUserIDPersistence() throws Exception {
        String userID = "123456789";

        AppEventsLogger.setUserID(userID);
        // The userID is saved async so we must wait before checking for the value
        Thread.sleep(10);
        Assert.assertEquals(userID, AppEventsLogger.getUserID());
        AppEventsLogger.clearUserID();
        Thread.sleep(10);
        Assert.assertNull(AppEventsLogger.getUserID());
    }

    @Test
    public void testUserIDAddedToAppEvent() throws Exception {
        String userID = "123456789";
        AppEventsLogger.setUserID(userID);
        AppEvent appEvent = AppEventTestUtilities.getTestAppEvent();
        JSONObject jsonObject = appEvent.getJSONObject();
        Assert.assertEquals(jsonObject.getString("_app_user_id"), userID);
    }

}
