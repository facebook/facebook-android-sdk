/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

// ShadowLog is used to redirect the android.util.Log calls to System.out
@Config(shadows = [ShadowLog::class], sdk = [21])
@RunWith(RobolectricTestRunner::class)
/**
 * Base class for Robolectric tests. Important: the classes that derive from this should end with
 * Test (i.e. not Tests) otherwise the gradle task "test" doesn't pick them up.
 */
abstract class FacebookTestCase {
  @Before
  open fun setUp() {
    ShadowLog.stream = System.out
    MockitoAnnotations.initMocks(this)
  }
}
