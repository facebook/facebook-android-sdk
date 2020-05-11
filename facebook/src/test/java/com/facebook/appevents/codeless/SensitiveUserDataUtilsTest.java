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

package com.facebook.appevents.codeless;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

import android.text.InputType;
import android.widget.TextView;
import com.facebook.appevents.codeless.internal.SensitiveUserDataUtils;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class SensitiveUserDataUtilsTest extends CodelessTestBase {

  @Mock private TextView textView;

  @Test
  public void testIsSensitiveUserData() {
    when(textView.getText()).thenReturn("");

    // input type == Password
    when(textView.getInputType()).thenReturn(InputType.TYPE_TEXT_VARIATION_PASSWORD);
    assertTrue(SensitiveUserDataUtils.isSensitiveUserData(textView));

    // input type == Text
    when(textView.getInputType()).thenReturn(InputType.TYPE_CLASS_TEXT);
    assertFalse(SensitiveUserDataUtils.isSensitiveUserData(textView));

    // input type == Person Name
    when(textView.getInputType()).thenReturn(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
    assertTrue(SensitiveUserDataUtils.isSensitiveUserData(textView));

    // input type == Postal Address
    when(textView.getInputType()).thenReturn(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
    assertTrue(SensitiveUserDataUtils.isSensitiveUserData(textView));

    // input type == Phone
    Mockito.when(textView.getInputType()).thenReturn(InputType.TYPE_CLASS_PHONE);
    assertTrue(SensitiveUserDataUtils.isSensitiveUserData(textView));

    // Credit Card
    when(textView.getInputType()).thenReturn(InputType.TYPE_CLASS_TEXT);
    when(textView.getText()).thenReturn("4030122707427751");
    assertTrue(SensitiveUserDataUtils.isSensitiveUserData(textView));
  }
}
