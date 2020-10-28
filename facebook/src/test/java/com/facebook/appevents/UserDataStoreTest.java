/*
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

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.MockSharedPreference;
import com.facebook.TestUtils;
import com.facebook.internal.Utility;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.Assert;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

@PrepareForTest({
  InternalAppEventsLogger.class,
  PreferenceManager.class,
})
public class UserDataStoreTest extends FacebookPowerMockTestCase {

  private final Executor mockExecutor = new FacebookSerialExecutor();

  @Before
  @Override
  public void setup() {
    super.setup();

    FacebookSdk.setApplicationId("123456789");
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application);

    PowerMockito.spy(PreferenceManager.class);
    PowerMockito.spy(InternalAppEventsLogger.class);
    PowerMockito.when(InternalAppEventsLogger.getAnalyticsExecutor()).thenReturn(mockExecutor);
  }

  @Test
  public void testInitStore() throws Exception {
    // Test initStore without cache in SharedPreference
    Whitebox.setInternalState(UserDataStore.class, "initialized", new AtomicBoolean(false));
    MockSharedPreference mockPreference = new MockSharedPreference();
    PowerMockito.doReturn(mockPreference)
        .when(PreferenceManager.class, "getDefaultSharedPreferences", Matchers.any(Context.class));

    UserDataStore.initStore();
    ConcurrentHashMap<String, String> externalHashedUserData =
        Whitebox.getInternalState(UserDataStore.class, "externalHashedUserData");
    Assert.assertTrue(externalHashedUserData.isEmpty());

    // Test initStore with cache in SharedPreference
    Map<String, String> cacheData = new HashMap<>();
    cacheData.put("key1", "val1");
    cacheData.put("key2", "val2");
    Whitebox.setInternalState(UserDataStore.class, "initialized", new AtomicBoolean(false));
    mockPreference
        .edit()
        .putString(
            "com.facebook.appevents.UserDataStore.userData",
            (new JSONObject(cacheData)).toString());
    PowerMockito.doReturn(mockPreference)
        .when(PreferenceManager.class, "getDefaultSharedPreferences", Matchers.any(Context.class));

    UserDataStore.initStore();
    externalHashedUserData =
        Whitebox.getInternalState(UserDataStore.class, "externalHashedUserData");
    Assert.assertEquals(cacheData, externalHashedUserData);
  }

  @Test
  public void testSetUserDataAndHash() throws Exception {
    MockSharedPreference mockPreference = new MockSharedPreference();
    PowerMockito.doReturn(mockPreference)
        .when(PreferenceManager.class, "getDefaultSharedPreferences", Matchers.any(Context.class));
    Whitebox.setInternalState(UserDataStore.class, "initialized", new AtomicBoolean(false));

    String email = "test@fb.com";
    String phone = "8008007000";
    UserDataStore.setUserDataAndHash(email, null, null, phone, null, null, null, null, null, null);
    Map<String, String> expectedData = new HashMap<>();
    expectedData.put(UserDataStore.EMAIL, Utility.sha256hash(email));
    expectedData.put(UserDataStore.PHONE, Utility.sha256hash(phone));
    JSONObject expected = new JSONObject(expectedData);
    JSONObject actual = new JSONObject(UserDataStore.getHashedUserData());
    TestUtils.assertEquals(expected, actual);

    Bundle bundleData = new Bundle();
    bundleData.putString(UserDataStore.EMAIL, "android@fb.com");
    UserDataStore.setUserDataAndHash(bundleData);
    expectedData.put(UserDataStore.EMAIL, Utility.sha256hash("android@fb.com"));
    expected = new JSONObject(expectedData);
    actual = new JSONObject(UserDataStore.getHashedUserData());
    TestUtils.assertEquals(expected, actual);
  }

  @Test
  public void testClear() throws Exception {
    MockSharedPreference mockPreference = new MockSharedPreference();
    PowerMockito.doReturn(mockPreference)
        .when(PreferenceManager.class, "getDefaultSharedPreferences", Matchers.any(Context.class));
    Whitebox.setInternalState(UserDataStore.class, "initialized", new AtomicBoolean(false));

    UserDataStore.setUserDataAndHash(
        "test@fb.com", null, null, "8008007000", null, null, null, null, null, null);

    UserDataStore.clear();
    Assert.assertTrue(UserDataStore.getHashedUserData().isEmpty());
  }
}
