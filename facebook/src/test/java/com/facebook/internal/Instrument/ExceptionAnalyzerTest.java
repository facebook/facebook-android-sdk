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

package com.facebook.internal.Instrument;

import android.content.Context;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.MockSharedPreference;
import com.facebook.internal.instrument.ExceptionAnalyzer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({
        FacebookSdk.class
})
public class ExceptionAnalyzerTest extends FacebookPowerMockTestCase {

    @Test
    public void testExecute() throws Exception {
        MockSharedPreference preference = new MockSharedPreference();
        Context context = PowerMockito.mock(Context.class);
        PowerMockito.when(context.getSharedPreferences(Matchers.anyString(), Matchers.anyInt()))
                .thenReturn(preference);
        PowerMockito.spy(FacebookSdk.class);
        PowerMockito.doReturn(false).when(
                FacebookSdk.class, "getAutoLogAppEventsEnabled");
        Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
        Whitebox.setInternalState(FacebookSdk.class, "applicationContext", context);
        Whitebox.setInternalState(ExceptionAnalyzer.class, "enabled", true);

        Exception e = new Exception();
        StackTraceElement[] trace = new StackTraceElement[] {
                new StackTraceElement(
                        "com.facebook.appevents.codeless.CodelessManager",
                        "onActivityResumed",
                        "file",
                        10)
        };
        e.setStackTrace(trace);
        ExceptionAnalyzer.execute(e);

        Assert.assertEquals(FacebookSdk.getSdkVersion(),
                preference.getString("FBSDKFeatureCodelessEvents", null));
    }
}
