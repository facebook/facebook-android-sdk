/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
