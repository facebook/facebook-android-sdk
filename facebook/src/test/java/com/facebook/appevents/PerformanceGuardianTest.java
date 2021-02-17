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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.internal.Utility;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({
  PerformanceGuardian.class,
  FacebookSdk.class,
  Utility.class,
})
public class PerformanceGuardianTest extends FacebookPowerMockTestCase {
  private final Executor mockExecutor = new FacebookSerialExecutor();

  @Before
  @Override
  public void setup() {
    super.setup();
    Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", new AtomicBoolean(true));
    Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
    PowerMockito.spy(PerformanceGuardian.class);
    PowerMockito.spy(FacebookSdk.class);
    PowerMockito.spy(Utility.class);
  }

  @Test
  public void testIsBannedActivity() throws Exception {
    // Initialize
    SharedPreferences mockPrefs = Mockito.mock(SharedPreferences.class);
    Context context = Mockito.mock(Context.class);
    when(FacebookSdk.getApplicationContext()).thenReturn(context);
    when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);

    // Mock return app version
    when(mockPrefs.getString("app_version", "")).thenReturn("1.2.0");
    PowerMockito.mockStatic(Utility.class);
    BDDMockito.given(Utility.getAppVersion()).willReturn("1.2.0");

    // Mock return banned activity set
    Set<String> mockBannedCodelessActivitySet = new HashSet<>();
    mockBannedCodelessActivitySet.add("banned_activity_1");

    Set<String> mockBannedSuggestedEventActivitySet = new HashSet<>();
    mockBannedSuggestedEventActivitySet.add("banned_activity_2");

    when(mockPrefs.getStringSet(
            PerformanceGuardian.UseCase.CODELESS.toString(), new HashSet<String>()))
        .thenReturn(mockBannedCodelessActivitySet);
    when(mockPrefs.getStringSet(
            PerformanceGuardian.UseCase.SUGGESTED_EVENT.toString(), new HashSet<String>()))
        .thenReturn(mockBannedSuggestedEventActivitySet);

    // Banned codeless activity
    boolean result1 =
        PerformanceGuardian.isBannedActivity(
            "banned_activity_1", PerformanceGuardian.UseCase.CODELESS);
    assertThat(result1).isTrue();

    // Banned suggested event activity is not banned for codeless
    boolean result2 =
        PerformanceGuardian.isBannedActivity(
            "banned_activity_2", PerformanceGuardian.UseCase.CODELESS);
    assertThat(result2).isFalse();

    // Banned suggested event activity
    boolean result3 =
        PerformanceGuardian.isBannedActivity(
            "banned_activity_2", PerformanceGuardian.UseCase.SUGGESTED_EVENT);
    assertThat(result3).isTrue();
  }

  @Test
  public void testLimitProcessTime() throws Exception {
    // Mock initialize is done
    PowerMockito.doNothing().when(PerformanceGuardian.class, "initializeIfNotYet");
    SharedPreferences mockPrefs = Mockito.mock(SharedPreferences.class);
    Whitebox.setInternalState(PerformanceGuardian.class, "sharedPreferences", mockPrefs);
    SharedPreferences.Editor editor = Mockito.mock(SharedPreferences.Editor.class);
    when(mockPrefs.edit()).thenReturn(editor);
    when(editor.putStringSet(anyString(), ArgumentMatchers.<String>anySet())).thenReturn(editor);
    when(editor.putString(anyString(), anyString())).thenReturn(editor);

    PowerMockito.mockStatic(Utility.class);
    BDDMockito.given(Utility.getAppVersion()).willReturn("1.2.0");

    Map<String, Integer> mockCodelessActivityMap = new HashMap<>();
    mockCodelessActivityMap.put("activity_1", 1);
    mockCodelessActivityMap.put("activity_2", 2);
    Whitebox.setInternalState(
        PerformanceGuardian.class, "activityProcessTimeMapCodeless", mockCodelessActivityMap);
    Map<String, Integer> mockSuggestedEventActivityMap = new HashMap<>();
    mockSuggestedEventActivityMap.put("activity_3", 1);
    Whitebox.setInternalState(
        PerformanceGuardian.class, "activityProcessTimeMapSe", mockSuggestedEventActivityMap);

    Set<String> bannedCodelessActivitySet =
        Whitebox.getInternalState(PerformanceGuardian.class, "bannedCodelessActivitySet");

    Set<String> bannedSuggestedEventActivitySet =
        Whitebox.getInternalState(PerformanceGuardian.class, "bannedSuggestedEventActivitySet");

    // Test codeless banned activity
    // Test activity not exceed max count
    PerformanceGuardian.limitProcessTime(
        "activity_1", PerformanceGuardian.UseCase.CODELESS, 0, 100);
    assertThat(bannedCodelessActivitySet.contains("activity_1")).isFalse();
    assertThat(mockCodelessActivityMap.get("activity_1")).isEqualTo(2);

    // Test activity exceed max count
    PerformanceGuardian.limitProcessTime(
        "activity_2", PerformanceGuardian.UseCase.CODELESS, 0, 100);
    assertThat(bannedCodelessActivitySet.contains("activity_2")).isTrue();
    assertThat(mockCodelessActivityMap.get("activity_2")).isEqualTo(3);

    // Test suggested event activity should not effect codeless
    PerformanceGuardian.limitProcessTime(
        "activity_1", PerformanceGuardian.UseCase.SUGGESTED_EVENT, 0, 100);
    assertThat(mockSuggestedEventActivityMap.get("activity_1")).isEqualTo(1);
    assertThat(bannedSuggestedEventActivitySet.contains("activity_1")).isFalse();

    assertThat(mockCodelessActivityMap.get("activity_1")).isEqualTo(2);
    assertThat(bannedCodelessActivitySet.contains("activity_1")).isFalse();

    // Test suggested event not exceed threshold
    PerformanceGuardian.limitProcessTime(
        "activity_3", PerformanceGuardian.UseCase.SUGGESTED_EVENT, 0, 10);
    assertThat(mockSuggestedEventActivityMap.get("activity_3")).isEqualTo(1);
    assertThat(bannedSuggestedEventActivitySet.contains("activity_3")).isFalse();

    // Test new suggested event exceed threshold
    PerformanceGuardian.limitProcessTime(
        "activity_4", PerformanceGuardian.UseCase.SUGGESTED_EVENT, 0, 100);
    assertThat(mockSuggestedEventActivityMap.get("activity_4")).isEqualTo(1);
    assertThat(bannedSuggestedEventActivitySet.contains("activity_4")).isFalse();
  }

  @Test
  public void testIsCacheValid() throws Exception {
    PerformanceGuardian performanceGuardian = new PerformanceGuardian();
    Method privateMethod =
        PerformanceGuardian.class.getDeclaredMethod("isCacheValid", String.class);
    privateMethod.setAccessible(true);
    boolean result;

    // Current app version returns null and cached version is empty
    PowerMockito.mockStatic(Utility.class);
    BDDMockito.given(Utility.getAppVersion()).willReturn(null);
    result = (boolean) privateMethod.invoke(performanceGuardian, "");
    assertThat(result).isFalse();

    // Current app version returns null while cached version returns value
    result = (boolean) privateMethod.invoke(performanceGuardian, "1.2.0");
    assertThat(result).isFalse();

    // Cached app version is empty while current version returns value
    PowerMockito.mockStatic(Utility.class);
    BDDMockito.given(Utility.getAppVersion()).willReturn("1.2.0");
    result = (boolean) privateMethod.invoke(performanceGuardian, "");
    assertThat(result).isFalse();

    // Current app version matches cached app version
    result = (boolean) privateMethod.invoke(performanceGuardian, "1.2.0");
    assertThat(result).isTrue();

    // Current app version does not match cached app version
    result = (boolean) privateMethod.invoke(performanceGuardian, "1.0.0");
    assertThat(result).isFalse();
  }
}
