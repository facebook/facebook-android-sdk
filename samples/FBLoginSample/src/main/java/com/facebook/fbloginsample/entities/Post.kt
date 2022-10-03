/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.entities

class Post(
    val message: String?,
    val createdTime: String,
    val id: String,
    val picture: String?,
    val fromName: String,
    val fromId: String
)
