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

import android.app.Activity;

import com.facebook.junittests.MainActivity;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ApplicationTest extends FacebookTestCase {
    @Test
    public void testCreateActivity() throws Exception {
        Activity activity = Robolectric.buildActivity(MainActivity.class).create().get();
        assertTrue(activity != null);
    }

    @Test
    public void testSdkInitializeCallback() throws Exception{
        final CountDownLatch lock = new CountDownLatch(1);
        Activity activity = Robolectric.buildActivity(MainActivity.class).create().get();
        final AtomicBoolean initialized = new AtomicBoolean(false);
        FacebookSdk.sdkInitialize(activity, new FacebookSdk.InitializeCallback() {
            @Override
            public void onInitialized() {
                initialized.set(true);
                lock.countDown();
            }
        });

        lock.await(100, TimeUnit.MILLISECONDS);

        assertTrue(initialized.get());
    }
}
