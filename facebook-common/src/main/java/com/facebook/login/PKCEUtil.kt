/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import android.os.Bundle
import android.util.Base64
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.HttpMethod
import java.security.MessageDigest

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

  /**
   * Returns the code challenge of the code verifier
   * @param codeVerifier the original code verifier
   * @param codeChallengeMethod the supplied codeChallengeMethod
   */
  @JvmStatic
  @Throws(FacebookException::class)
  fun generateCodeChallenge(
      codeVerifier: String,
      codeChallengeMethod: CodeChallengeMethod
  ): String {
    if (!isValidCodeVerifier(codeVerifier)) {
      throw FacebookException("Invalid Code Verifier.")
    }

    if (codeChallengeMethod == CodeChallengeMethod.PLAIN) {
      return codeVerifier
    }

    return try {
      // try to generate challenge with S256
      val bytes: ByteArray = codeVerifier.toByteArray(Charsets.US_ASCII)
      val messageDigest = MessageDigest.getInstance("SHA-256")
      messageDigest.update(bytes, 0, bytes.size)
      val digest = messageDigest.digest()

      Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    } catch (ex: Exception) {
      throw FacebookException(ex)
    }
  }

  /**
   * Create the GraphRequest of
   * @param authorizationCode the server generated code needed for code exchange, one time use
   * @param redirectUri the uri specified during the code request
   * @param codeVerifier the original code verifier
   */
  @JvmStatic
  fun createCodeExchangeRequest(
      authorizationCode: String,
      redirectUri: String,
      codeVerifier: String
  ): GraphRequest {
    val parameters = Bundle()
    parameters.putString("code", authorizationCode)
    parameters.putString("client_id", FacebookSdk.getApplicationId())
    parameters.putString("redirect_uri", redirectUri)
    parameters.putString("code_verifier", codeVerifier)
    val graphRequest = GraphRequest.newGraphPathRequest(null, "oauth/access_token", null)
    graphRequest.httpMethod = HttpMethod.GET
    graphRequest.parameters = parameters
    return graphRequest
  }
}
