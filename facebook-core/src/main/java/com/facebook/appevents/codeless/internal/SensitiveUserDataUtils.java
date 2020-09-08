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

package com.facebook.appevents.codeless.internal;

import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.view.View;
import android.widget.TextView;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
@AutoHandleExceptions
public class SensitiveUserDataUtils {

  public static boolean isSensitiveUserData(View view) {
    if (view instanceof TextView) {
      TextView textView = (TextView) view;
      return isPassword(textView)
          || isCreditCard(textView)
          || isPersonName(textView)
          || isPostalAddress(textView)
          || isPhoneNumber(textView)
          || isEmail(textView);
    }
    return false;
  }

  private static boolean isPassword(TextView view) {
    int inputType = view.getInputType();
    if (inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
      return true;
    }
    TransformationMethod method = view.getTransformationMethod();
    return method instanceof PasswordTransformationMethod;
  }

  private static boolean isEmail(TextView view) {
    int inputType = view.getInputType();
    if (inputType == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
      return true;
    }
    String text = ViewHierarchy.getTextOfView(view);
    if (text == null || text.length() == 0) {
      return false;
    }
    return android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches();
  }

  private static boolean isPersonName(TextView view) {
    int inputType = view.getInputType();
    return inputType == InputType.TYPE_TEXT_VARIATION_PERSON_NAME;
  }

  private static boolean isPostalAddress(TextView view) {
    int inputType = view.getInputType();
    return inputType == InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS;
  }

  private static boolean isPhoneNumber(TextView view) {
    int inputType = view.getInputType();
    return inputType == InputType.TYPE_CLASS_PHONE;
  }

  private static boolean isCreditCard(TextView view) {
    String ccNumber = ViewHierarchy.getTextOfView(view).replaceAll("\\s", "");
    int length = ccNumber.length();
    if (length < 12 || length > 19) {
      return false;
    }
    int sum = 0;
    boolean alternate = false;
    for (int i = length - 1; i >= 0; i--) {
      char digit = ccNumber.charAt(i);
      if (digit < '0' || digit > '9') {
        return false;
      }
      int n = digit - '0';
      if (alternate) {
        n *= 2;
        if (n > 9) {
          n = (n % 10) + 1;
        }
      }
      sum += n;
      alternate = !alternate;
    }
    return (sum % 10 == 0);
  }
}
