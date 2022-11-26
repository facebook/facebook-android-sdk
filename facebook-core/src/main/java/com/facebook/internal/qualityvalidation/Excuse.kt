/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.qualityvalidation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/** @see ExcusesForDesignViolations */
@Retention(RetentionPolicy.SOURCE)
@Target
annotation class Excuse(val type: String, val reason: String)
