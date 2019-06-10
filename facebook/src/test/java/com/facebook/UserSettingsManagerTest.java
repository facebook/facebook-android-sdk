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

import android.content.Context;
import android.os.Bundle;

import com.facebook.appevents.InternalAppEventsLogger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest( {
        UserSettingsManager.class,
        FacebookSdk.class})

public class UserSettingsManagerTest extends FacebookPowerMockTestCase {

    @Before
    @Override
    public void setup() {
        super.setup();
        PowerMockito.mockStatic(UserSettingsManager.class);
        Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
    }

    @Test
    public void testAutoInitEnabled() {
        FacebookSdk.getAutoInitEnabled();
        PowerMockito.verifyStatic();
        UserSettingsManager.getAutoInitEnabled();

        FacebookSdk.setAutoInitEnabled(false);
        PowerMockito.verifyStatic();
        UserSettingsManager.setAutoInitEnabled(false);
    }

    @Test
    public void testAutoLogEnabled() {
        FacebookSdk.getAutoLogAppEventsEnabled();
        PowerMockito.verifyStatic();
        UserSettingsManager.getAutoLogAppEventsEnabled();

        FacebookSdk.setAutoLogAppEventsEnabled(false);
        PowerMockito.verifyStatic();
        UserSettingsManager.setAutoLogAppEventsEnabled(false);
    }

    @Test
    public void testAdvertiserIDCollectionEnabled() {
        FacebookSdk.getAdvertiserIDCollectionEnabled();
        PowerMockito.verifyStatic();
        UserSettingsManager.getAdvertiserIDCollectionEnabled();

        FacebookSdk.setAdvertiserIDCollectionEnabled(false);
        PowerMockito.verifyStatic();
        UserSettingsManager.setAdvertiserIDCollectionEnabled(false);
    }

    @Test
    public void testCodelessSetupEnabled() {
        FacebookSdk.getCodelessSetupEnabled();
        PowerMockito.verifyStatic();
        UserSettingsManager.getCodelessSetupEnabled();
    }

    @Test
    public void testLogIfSDKSettingsChanged() throws Exception {

        Bundle mockBundle = PowerMockito.mock(Bundle.class);
        InternalAppEventsLogger mockLogger = PowerMockito.mock(InternalAppEventsLogger.class);

        PowerMockito.whenNew(Bundle.class).withNoArguments().thenReturn(mockBundle);
        PowerMockito.whenNew(InternalAppEventsLogger.class).withArguments(Matchers.any(Context.class)).thenReturn(mockLogger);

        UserSettingsManager.setAdvertiserIDCollectionEnabled(false);
        PowerMockito.verifyStatic(Mockito.times(3));
        mockBundle.putInt(Matchers.anyString(), Matchers.anyInt());
        mockLogger.logEventImplicitly(Matchers.anyString(), Matchers.any(Bundle.class));
    }
}
