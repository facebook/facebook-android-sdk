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

import static org.mockito.ArgumentMatchers.any;

import android.os.Bundle;
import com.facebook.appevents.InternalAppEventsLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;

@PrepareForTest({UserSettingsManager.class})
public class UserSettingsManagerTest extends FacebookPowerMockTestCase {

  @Before
  @Override
  public void setup() {
    super.setup();

    FacebookSdk.setApplicationId("123456789");
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application);
  }

  @Test
  public void testAutoInitEnabled() {
    PowerMockito.mockStatic(UserSettingsManager.class);

    FacebookSdk.getAutoInitEnabled();
    PowerMockito.verifyStatic(UserSettingsManager.class);
    UserSettingsManager.getAutoInitEnabled();

    FacebookSdk.setAutoInitEnabled(false);
    PowerMockito.verifyStatic(UserSettingsManager.class);
    UserSettingsManager.setAutoInitEnabled(false);
  }

  @Test
  public void testAutoLogEnabled() {
    PowerMockito.mockStatic(UserSettingsManager.class);

    FacebookSdk.getAutoLogAppEventsEnabled();
    PowerMockito.verifyStatic(UserSettingsManager.class);
    UserSettingsManager.getAutoLogAppEventsEnabled();

    FacebookSdk.setAutoLogAppEventsEnabled(false);
    PowerMockito.verifyStatic(UserSettingsManager.class);
    UserSettingsManager.setAutoLogAppEventsEnabled(false);
  }

  @Test
  public void testAdvertiserIDCollectionEnabled() {
    PowerMockito.mockStatic(UserSettingsManager.class);

    FacebookSdk.getAdvertiserIDCollectionEnabled();
    PowerMockito.verifyStatic(UserSettingsManager.class);
    UserSettingsManager.getAdvertiserIDCollectionEnabled();

    FacebookSdk.setAdvertiserIDCollectionEnabled(false);
    PowerMockito.verifyStatic(UserSettingsManager.class);
    UserSettingsManager.setAdvertiserIDCollectionEnabled(false);
  }

  @Test
  public void testCodelessSetupEnabled() {
    PowerMockito.mockStatic(UserSettingsManager.class);

    FacebookSdk.getCodelessSetupEnabled();
    PowerMockito.verifyStatic(UserSettingsManager.class);
    UserSettingsManager.getCodelessSetupEnabled();
  }

  @Test
  public void testLogIfSDKSettingsChanged() throws Exception {
    InternalAppEventsLogger mockLogger = PowerMockito.mock(InternalAppEventsLogger.class);

    PowerMockito.whenNew(InternalAppEventsLogger.class).withAnyArguments().thenReturn(mockLogger);

    UserSettingsManager.setAdvertiserIDCollectionEnabled(false);
    Mockito.verify(mockLogger).logChangedSettingsEvent(any(Bundle.class));
  }
}
