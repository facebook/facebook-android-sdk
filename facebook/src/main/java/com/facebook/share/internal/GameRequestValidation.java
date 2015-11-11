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

package com.facebook.share.internal;

import com.facebook.internal.Validate;
import com.facebook.share.model.GameRequestContent;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 *
 * Validates GameRequestContent before it is shown by GameRequestDialog
 */
public class GameRequestValidation {

    public static void validate(GameRequestContent content) {
        Validate.notNull(content.getMessage(), "message");
        if (content.getObjectId() != null ^
                (content.getActionType() == GameRequestContent.ActionType.ASKFOR
                || content.getActionType() == GameRequestContent.ActionType.SEND)) {
            throw new IllegalArgumentException(
                    "Object id should be provided if and only if action type is send or askfor");
        }

        // parameters recipients, filters, suggestions are mutually exclusive
        int mutex = 0;
        if (content.getRecipients() != null) {
            mutex++;
        }
        if (content.getSuggestions() != null) {
            mutex++;
        }
        if (content.getFilters() != null) {
            mutex++;
        }
        if (mutex > 1) {
            throw new IllegalArgumentException(
                    "Parameters to, filters and suggestions are mutually exclusive");
        }
    }

}
