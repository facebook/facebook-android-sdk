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

import android.os.Bundle;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.ArgumentMatcher;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import static com.facebook.TestUtils.assertEqualContentsWithoutOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AppEventTestUtilities {
    public static AppEvent getTestAppEvent() throws Exception {
        Bundle customParams = new Bundle();
        customParams.putString("key1", "value1");
        customParams.putString("key2", "value2");
        AppEvent appEvent = new AppEvent(
                "contextName",
                "eventName",
                1.0,
                customParams,
                false,
                false,
                UUID.fromString("65565271-1ace-4580-bd13-b2bc6d0df035"));
        appEvent.isChecksumValid();
        return appEvent;
    }

    public static class BundleMatcher extends ArgumentMatcher<Bundle> {

        private Bundle wanted;

        public BundleMatcher(Bundle wanted) {
            this.wanted = wanted;
        }

        public boolean matches(Object bundle) {
            if (!(bundle instanceof Bundle)) {
                return false;
            }
            assertEqualContentsWithoutOrder(this.wanted, (Bundle)bundle);
            return true;
        }
    }
}
