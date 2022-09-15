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

import java.util.Collections
import java.util.UUID
import kotlin.collections.HashSet

/** Configuration class to use for logging in with facebook */
class LoginConfiguration {

  /** The requested permissions for the login attempt. Defaults to an empty set. */
  val permissions: Set<String>

  /** he nonce that the configuration was created with. */
  val nonce: String

  /** PKCE code_verifier for getting the access_token and/or id_token */
  val codeVerifier: String

  /**
   * create a new configuration with the expected parameters.
   *
   * @param permissions the requested permissions for a login attempt. Permissions must be an array
   * of strings that do not contain whitespace.
   * @param nonce an optional nonce to use for the login attempt. A valid nonce must be a non-empty
   * string without whitespace. Creation of the configuration will fail if the nonce is invalid.
   */
  @JvmOverloads
  constructor(
      permissions: Collection<String>?,
      nonce: String = UUID.randomUUID().toString()
  ) : this(permissions, nonce, PKCEUtil.generateCodeVerifier())

  /**
   * create a new configuration with the expected parameters.
   *
   * @param permissions the requested permissions for a login attempt. Permissions must be an array
   * of strings that do not contain whitespace.
   * @param nonce an optional nonce to use for the login attempt. A valid nonce must be a non-empty
   * string without whitespace. Creation of the configuration will fail if the nonce is invalid.
   * @param codeVerifier a cryptographically random string using the characters A-Z, a-z, 0-9, and
   * the punctuation characters -._~ (hyphen, period, underscore, and tilde), between 43 and 128
   * characters long.
   */
  constructor(
      permissions: Collection<String>? = null,
      nonce: String = UUID.randomUUID().toString(),
      codeVerifier: String
  ) {
    require(NonceUtil.isValidNonce(nonce) && PKCEUtil.isValidCodeVerifier(codeVerifier))

    val permissions = if (permissions != null) HashSet(permissions) else HashSet()
    permissions.add(OPENID)
    this.permissions = Collections.unmodifiableSet(permissions)
    this.nonce = nonce
    this.codeVerifier = codeVerifier
  }

  companion object {
    const val OPENID = "openid"
  }
}
