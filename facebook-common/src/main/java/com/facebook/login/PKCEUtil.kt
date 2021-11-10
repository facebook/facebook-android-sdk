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

package com.facebook.login

internal object PKCEUtil {

  /**
   * codeVerifier: high-entropy cryptographic random STRING using the unreserved characters [A-Z] /
   * [a-z] / [0-9] / "-" / "." / "_" / "~" from Section 2.3 of [RFC3986], with a minimum length of
   * 43 characters and a maximum length of 128 characters. Doc Reference:
   * https://datatracker.ietf.org/doc/html/rfc7636
   */
  @JvmStatic
  fun isValidCodeVerifier(codeVerifier: String?): Boolean {
    if (codeVerifier.isNullOrEmpty() || codeVerifier.length < 43 || codeVerifier.length > 128) {
      return false
    }

    val regex = Regex("^[-._~A-Za-z0-9]+\$")
    return regex matches codeVerifier
  }

  /**
   * Generate a code verifier which follows the pattern [A-Z] / [a-z] / [0-9] / "-" / "." / "_" /
   * "~" and with a minimum length of 43 characters and a maximum length of 128 characters
   */
  @JvmStatic
  fun generateCodeVerifier(): String {
    val random43to128 = (43..128).random()
    val allowedCharSet = ('a'..'z') + ('A'..'Z') + ('0'..'9') + ('-') + ('.') + ('_') + ('~')
    return List(random43to128) { allowedCharSet.random() }.joinToString("")
  }
}
