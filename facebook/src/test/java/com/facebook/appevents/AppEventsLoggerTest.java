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
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.appevents.internal.AppEventsLoggerUtility;
import com.facebook.internal.Utility;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
        AppEventUtility.class,
        AppEventsLogger.class,
        FacebookSdk.class,
        Utility.class,
})
public class AppEventsLoggerTest extends FacebookPowerMockTestCase {

    private final Executor executor = new FacebookSerialExecutor();

    @Before
    public void init() throws Exception {
        mockStatic(FacebookSdk.class);
        when(FacebookSdk.isInitialized()).thenReturn(true);
        when(FacebookSdk.getApplicationContext()).thenReturn(RuntimeEnvironment.application);

        // Mock Utility class with empty stub functions, which will be called in
        // AppEventsLoggerUtility.getJSONObjectForGraphAPICall
        mockStatic(Utility.class);

        // Disable AppEventUtility.isMainThread since executor now runs in main thread
        spy(AppEventUtility.class);
        doReturn(false).when(AppEventUtility.class, "isMainThread");
        mockStatic(AppEventsLogger.class);
        spy(AppEventsLogger.class);
        doReturn(executor).when(AppEventsLogger.class, "getAnalyticsExecutor");
    }

    @Test
    public void testSetAndClearUserID() throws Exception {
        String userID = "12345678";
        AppEventsLogger.setUserID(userID);
        assertEquals(AppEventsLogger.getUserID(), userID);
        AppEventsLogger.clearUserID();
        assertNull(AppEventsLogger.getUserID());
    }

    @Test
    public void testUserIDAddedToAppEvent() throws Exception {
        String userID = "12345678";
        AppEventsLogger.setUserID(userID);
        JSONObject jsonObject =AppEventsLoggerUtility.getJSONObjectForGraphAPICall(
                AppEventsLoggerUtility.GraphAPIActivityType.MOBILE_INSTALL_EVENT,
                null,
                "123",
                true,
                FacebookSdk.getApplicationContext());
        assertEquals(jsonObject.getString("app_user_id"), userID);
    }
}
