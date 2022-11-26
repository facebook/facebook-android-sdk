/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
