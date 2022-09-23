/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.entities

import android.net.Uri

class User(
    val picture: Uri,
    val name: String,
    val id: String,
    val email: String?,
    val permissions: String
)
