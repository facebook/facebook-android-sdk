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

package com.facebook.internal;

import android.test.suitebuilder.annotation.LargeTest;

import com.facebook.FacebookRequestError;
import com.facebook.FacebookTestCase;

public class UtilityTest extends FacebookTestCase {

    @LargeTest
    public void testFetchedAppSettingsErrorClassification() throws Exception {
        Utility.FetchedAppSettings fetchedAppSettings =
                Utility.queryAppSettings(getApplicationId(), false);
        FacebookRequestErrorClassification errorClassification = fetchedAppSettings
                .getErrorClassification();
        assertNotNull(errorClassification);

        assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.OTHER));
        assertNull(errorClassification.getRecoveryMessage(FacebookRequestError.Category.TRANSIENT));
        assertNotNull(errorClassification.getRecoveryMessage(
                FacebookRequestError.Category.LOGIN_RECOVERABLE));

        assertNull(errorClassification.getOtherErrors());
        assertEquals(2, errorClassification.getLoginRecoverableErrors().size());
        assertNull(errorClassification.getTransientErrors());
    }
}
