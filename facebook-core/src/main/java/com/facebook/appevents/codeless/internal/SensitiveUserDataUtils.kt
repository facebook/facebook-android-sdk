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

package com.facebook.appevents.codeless.internal

import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.TextView
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

@AutoHandleExceptions
object SensitiveUserDataUtils {
  @JvmStatic
  fun isSensitiveUserData(view: View?): Boolean {
    if (view is TextView) {
      return (isPassword(view) ||
          isCreditCard(view) ||
          isPersonName(view) ||
          isPostalAddress(view) ||
          isPhoneNumber(view) ||
          isEmail(view))
    }
    return false
  }

  private fun isPassword(view: TextView): Boolean {
    val inputType = view.inputType
    if (inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
      return true
    }
    val method = view.transformationMethod
    return method is PasswordTransformationMethod
  }

  private fun isEmail(view: TextView): Boolean {
    val inputType = view.inputType
    if (inputType == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
      return true
    }
    val text = ViewHierarchy.getTextOfView(view)
    return if (text == null || text.isEmpty()) {
      false
    } else Patterns.EMAIL_ADDRESS.matcher(text).matches()
  }

  private fun isPersonName(view: TextView): Boolean {
    val inputType = view.inputType
    return inputType == InputType.TYPE_TEXT_VARIATION_PERSON_NAME
  }

  private fun isPostalAddress(view: TextView): Boolean {
    val inputType = view.inputType
    return inputType == InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
  }

  private fun isPhoneNumber(view: TextView): Boolean {
    val inputType = view.inputType
    return inputType == InputType.TYPE_CLASS_PHONE
  }

  private fun isCreditCard(view: TextView): Boolean {
    val ccNumber = ViewHierarchy.getTextOfView(view).replace("\\s".toRegex(), "")
    val length = ccNumber.length
    if (length < 12 || length > 19) {
      return false
    }
    var sum = 0
    var alternate = false
    for (i in length - 1 downTo 0) {
      val digit = ccNumber[i]
      if (!digit.isDigit()) {
        return false
      }
      var n = digit.digitToInt()
      if (alternate) {
        n *= 2
        if (n > 9) {
          n = n % 10 + 1
        }
      }
      sum += n
      alternate = !alternate
    }
    return sum % 10 == 0
  }
}
