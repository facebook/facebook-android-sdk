/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import com.facebook.AccessToken
import com.facebook.AuthenticationToken

/**
 * Represents the results of a login operation.
 *
 * @property accessToken The new access token.
 * @property authenticationToken The new authentication token
 * @property recentlyGrantedPermissions The recently granted permissions.
 * @property recentlyDeniedPermissions The recently denied permissions.
 */
data class LoginResult
@JvmOverloads
constructor(
    val accessToken: AccessToken,
    val authenticationToken: AuthenticationToken? = null,
    val recentlyGrantedPermissions: Set<String>,
    val recentlyDeniedPermissions: Set<String>
)
