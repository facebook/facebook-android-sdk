/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

@file:JvmName("JSONObjectExtensions")

package com.facebook.internal

import androidx.annotation.RestrictTo
import org.json.JSONObject

/** Same as `optString(name, null)`, whose fallback parameter is annotated non-null. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun JSONObject.optStringOrNull(name: String): String? = opt(name)?.toString()
