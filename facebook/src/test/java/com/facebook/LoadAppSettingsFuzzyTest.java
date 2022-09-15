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

package com.facebook;

import static org.junit.Assert.fail;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Utility;
import java.util.Collection;
import org.json.JSONObject;
import org.junit.Before;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

/** Tests that passing wrong type in any part of SDK settings doesn't cause SDK to throw */
@LooperMode(LEGACY)
@PrepareForTest({
  FacebookSdk.class,
  FetchedAppSettingsManager.class,
  Utility.class,
  UserSettingsManager.class,
})
public class LoadAppSettingsFuzzyTest extends FacebookFuzzyInputPowerMockTestCase {

  @ParameterizedRobolectricTestRunner.Parameters
  public static Collection<Object[]> getParameters() {
    String jsonString =
        "{\"supports_implicit_sdk_logging\":true,\"gdpv4_nux_enabled\":false,\"android_sdk_error_categories\":[{\"name\":\"login_recoverable\",\"items\":[{\"code\":102},{\"code\":190}],\"recovery_message\":\"Please log into this app again to reconnect your Facebook account.\"}],\"app_events_session_timeout\":60,\"app_events_feature_bitmask\":5,\"seamless_login\":1,\"smart_login_bookmark_icon_url\":\"https:\\/\\/static.xx.fbcdn.net\\/rsrc.php\\/v3\\/ys\\/r\\/C6ZutYDSaaV.png\",\"smart_login_menu_icon_url\":\"https:\\/\\/static.xx.fbcdn.net\\/rsrc.php\\/v3\\/ys\\/r\\/0iarpnwdmEx.png\",\"restrictive_data_filter_params\":\"{}\",\"aam_rules\":\"{}\",\"suggested_events_setting\":\"{\\\"production_events\\\":[],\\\"eligible_for_prediction_events\\\":[\\\"fb_mobile_add_to_cart\\\",\\\"fb_mobile_purchase\\\",\\\"fb_mobile_complete_registration\\\",\\\"fb_mobile_initiated_checkout\\\"]}\",\"id\":\"479829488705076\"}";
    return getParametersForJSONString(jsonString);
  }

  @Before
  public void before() {
    MockSharedPreference mockPreference = new MockSharedPreference();
    Whitebox.setInternalState(UserSettingsManager.class, "userSettingPref", mockPreference);
    FacebookSdk.setAutoLogAppEventsEnabled(false);
    FacebookSdk.setApplicationId("123456789");
    FacebookSdk.setClientToken("abcdefg");
  }

  @Override
  public void functionToTest(JSONObject inputJSON) {
    stub(method(FetchedAppSettingsManager.class, "getAppSettingsQueryResponse"))
        .toReturn(inputJSON);
    try {
      FacebookSdk.sdkInitialize(RuntimeEnvironment.application);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
