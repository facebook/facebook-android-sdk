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

import android.os.Bundle;

import com.facebook.FacebookTestCase;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class AppEventTest extends FacebookTestCase {
    @Test
    public void testChecksumOfAppEvent() throws Exception {
        AppEvent appEvent = getTestAppEvent();
        Assert.assertTrue(getTestAppEvent().isChecksumValid());
        appEvent.getJSONObject().put("new_key", "corrupted");
        Assert.assertFalse(appEvent.isChecksumValid());
    }

    @Test
    public void testAppEventSerializedChecksum() throws Exception {
        AppEvent appEvent1 = getTestAppEvent();

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

    public AppEvent getTestAppEvent() throws Exception {
        Bundle customParams = new Bundle();
        customParams.putString("key1", "value1");
        customParams.putString("key2", "value2");
        AppEvent appEvent = new AppEvent(
                "contextName",
                "eventName",
                1.0,
                customParams,
                false,
                UUID.fromString("65565271-1ace-4580-bd13-b2bc6d0df035"));
        appEvent.isChecksumValid();
        return appEvent;
    }
}
