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

import com.facebook.FacebookSdk;
import com.facebook.FacebookTestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class AppEventTest extends FacebookTestCase {
    @Before
    public void init() {
        FacebookSdk.setApplicationId("123456789");
        FacebookSdk.sdkInitialize(RuntimeEnvironment.application);
    }

    @Test
    public void testChecksumOfAppEvent() throws Exception {
        AppEvent appEvent = AppEventTestUtilities.getTestAppEvent();
        Assert.assertTrue(appEvent.isChecksumValid());
        appEvent.getJSONObject().put("new_key", "corrupted");
        Assert.assertFalse(appEvent.isChecksumValid());
    }

    @Test
    public void testAppEventSerializedChecksum() throws Exception {
        AppEvent appEvent1 = AppEventTestUtilities.getTestAppEvent();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(appEvent1);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        AppEvent appEvent2 = (AppEvent)objectInputStream.readObject();
        Assert.assertTrue(appEvent2.isChecksumValid());

        // A secondary validation ensure that the json string matches the original
        Assert.assertTrue(
                appEvent1.getJSONObject().toString().equals(appEvent2.getJSONObject().toString()));
    }
}
