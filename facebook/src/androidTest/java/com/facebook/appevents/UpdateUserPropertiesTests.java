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
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.TestBlocker;

public class UpdateUserPropertiesTests extends FacebookTestCase {
    public void testUserUpdateProperties() throws Exception {
        final TestBlocker blocker = getTestBlocker();
        Bundle parameters = new Bundle();
        parameters.putString("custom_value", "1");
        AppEventsLogger.setUserID("1");
        AppEventsLogger.updateUserProperties(
                parameters,
                getApplicationId(),
                new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                if (response.getError() != null) {
                    blocker.setException(response.getError().getException());
                }

                blocker.signal();
            }
        });

        blocker.waitForSignals(1);
        if (blocker.getException() != null) {
            throw blocker.getException();
        }
    }
}
