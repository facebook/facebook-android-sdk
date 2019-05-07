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

package com.facebook.appevents.codeless;

import android.app.Activity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.FacebookPowerMockTestCase;

import org.junit.Before;
import org.robolectric.Robolectric;

public abstract class CodelessTestBase extends FacebookPowerMockTestCase {

    LinearLayout root;

    protected Activity activity;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        activity = Robolectric.buildActivity(Activity.class).create().get();
        root = new LinearLayout(activity);

        activity.setContentView(root);

        TextView outerLabel = new TextView(activity);
        outerLabel.setText("Outer Label");
        root.addView(outerLabel);

        LinearLayout inner = new LinearLayout(activity);
        root.addView(inner);

        TextView innerLabel = new TextView(activity);
        innerLabel.setText("Inner Label");
        inner.addView(innerLabel);
    }

}
