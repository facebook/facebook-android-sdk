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

package com.facebook.samples.loginsample.accountkit;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import com.facebook.samples.loginsample.R;

@SuppressWarnings("All")
class InflateFragment extends Fragment {
    @TargetApi(11)
    @Override
    public void onInflate(final AttributeSet attrs, final Bundle savedInstanceState) {
        super.onInflate(attrs, savedInstanceState);
        handleAttributes(attrs);
    }

    @TargetApi(21)
    @Override
    public void onInflate(
            final Activity activity,
            final AttributeSet attrs,
            final Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        handleAttributes(attrs);
    }

    @TargetApi(23)
    @Override
    public void onInflate(
            final Context context,
            final AttributeSet attrs,
            final Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        handleAttributes(attrs);
    }

    protected void handleAttributes(final AttributeSet attrs) {
    }
}
