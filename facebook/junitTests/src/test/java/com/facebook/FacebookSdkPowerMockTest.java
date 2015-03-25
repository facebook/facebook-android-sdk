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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ConditionVariable;

import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

@PrepareForTest({ FacebookSdk.class, Utility.class })
public final class FacebookSdkPowerMockTest extends FacebookPowerMockTestCase {

    @Before
    public void before() {
        Whitebox.setInternalState(FacebookSdk.class, "callbackRequestCodeOffset", 0xface);
        Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", false);
        stub(method(Utility.class, "loadAppSettingsAsync")).toReturn(null);

    }

    @Test
    public void testGetExecutor() {
        final ConditionVariable condition = new ConditionVariable();

        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                condition.open();
            }
        });

        boolean success = condition.block(5000);
        assertTrue(success);
    }

    @Test
    public void testSetExecutor() {
        final ConditionVariable condition = new ConditionVariable();

        final Runnable runnable = new Runnable() {
            @Override
            public void run() { }
        };

        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                assertEquals(runnable, command);
                command.run();

                condition.open();
            }
        };

        Executor original = FacebookSdk.getExecutor();
        try {
            FacebookSdk.setExecutor(executor);
            FacebookSdk.getExecutor().execute(runnable);

            boolean success = condition.block(5000);
            assertTrue(success);
        } finally {
            FacebookSdk.setExecutor(original);
        }
    }

    @Test
    public void testFacebookDomain() {
        FacebookSdk.setFacebookDomain("beta.facebook.com");

        String graphUrlBase = ServerProtocol.getGraphUrlBase();
        assertEquals("https://graph.beta.facebook.com", graphUrlBase);

        FacebookSdk.setFacebookDomain("facebook.com");
    }

    @Test
    public void testLoadDefaults() throws Exception {
        stub(method(FacebookSdk.class, "isInitialized")).toReturn(true);
        FacebookSdk.loadDefaultsFromMetadata(mockContextWithAppIdAndClientToken());

        assertEquals("1234", FacebookSdk.getApplicationId());
        assertEquals("abcd", FacebookSdk.getClientToken());
    }


    private Context mockContextWithAppIdAndClientToken() throws Exception {
        Bundle bundle = mock(Bundle.class);

        when(bundle.getString(FacebookSdk.APPLICATION_ID_PROPERTY)).thenReturn("1234");
        when(bundle.getString(FacebookSdk.CLIENT_TOKEN_PROPERTY)).thenReturn("abcd");
        ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        applicationInfo.metaData = bundle;

        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.getApplicationInfo("packageName", PackageManager.GET_META_DATA))
                .thenReturn(applicationInfo);

        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("packageName");
        when(context.getPackageManager()).thenReturn(packageManager);
        return context;
    }

    @Test
    public void testLoadDefaultsDoesNotOverwrite() throws Exception {
        stub(method(FacebookSdk.class, "isInitialized")).toReturn(true);
        FacebookSdk.setApplicationId("hello");
        FacebookSdk.setClientToken("world");

        FacebookSdk.loadDefaultsFromMetadata(mockContextWithAppIdAndClientToken());

        assertEquals("hello", FacebookSdk.getApplicationId());
        assertEquals("world", FacebookSdk.getClientToken());
    }

    @Test
    public void testRequestCodeOffsetAfterInit() throws Exception {
        FacebookSdk.sdkInitialize(Robolectric.application);

        try {
            FacebookSdk.sdkInitialize(Robolectric.application, 1000);
            fail();
        } catch (FacebookException exception) {
            assertEquals(FacebookSdk.CALLBACK_OFFSET_CHANGED_AFTER_INIT, exception.getMessage());
        }
    }

    @Test
    public void testRequestCodeOffsetNegative() throws Exception {
        try {
            // last bit set, so negative
            FacebookSdk.sdkInitialize(Robolectric.application, 0xFACEB00C);
            fail();
        } catch (FacebookException exception) {
            assertEquals(FacebookSdk.CALLBACK_OFFSET_NEGATIVE, exception.getMessage());
        }
    }

    @Test
    public void testRequestCodeOffset() throws Exception {
        FacebookSdk.sdkInitialize(Robolectric.application, 1000);
        assertEquals(1000, FacebookSdk.getCallbackRequestCodeOffset());
    }

    @Test
    public void testRequestCodeRange() {
        FacebookSdk.sdkInitialize(Robolectric.application, 1000);
        assertTrue(FacebookSdk.isFacebookRequestCode(1000));
        assertTrue(FacebookSdk.isFacebookRequestCode(1099));
        assertFalse(FacebookSdk.isFacebookRequestCode(999));
        assertFalse(FacebookSdk.isFacebookRequestCode(1100));
        assertFalse(FacebookSdk.isFacebookRequestCode(0));
    }
}
