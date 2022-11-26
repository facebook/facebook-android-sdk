/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.annotation.SuppressLint
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

// ShadowLog is used to redirect the android.util.Log calls to System.out
@SuppressLint("RunWithRobolectricTestRunner")
@Config(shadows = [ShadowLog::class], manifest = Config.NONE, sdk = [21])
@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore(
    "org.mockito.*",
    "org.robolectric.*",
    "androidx.*",
    "android.*",
    "org.json.*",
    "javax.xml.*",
    "org.xml.sax.*",
    "org.w3c.dom.*",
    "org.springframework.context.*",
    "org.apache.log4j.*",
    "kotlin.*")
/**
 * Base class for PowerMock tests. Important: the classes that derive from this should end with Test
 * (i.e. not Tests) otherwise the gradle task "test" doesn't pick them up.
 */
abstract class FacebookPowerMockTestCase {
  @Rule @JvmField var rule = PowerMockRule()
  @Before
  open fun setup() {
    ShadowLog.stream = System.out
    MockitoAnnotations.initMocks(this)
  }

  @After
  fun clearMocks() {
    org.mockito.Mockito.framework().clearInlineMocks()
  }

  class FacebookSerialExecutor : Executor {
    override fun execute(command: Runnable) {
      command.run()
    }
  }

  class FacebookSerialThreadPoolExecutor(corePoolSize: Int) :
      ScheduledThreadPoolExecutor(corePoolSize) {
    override fun execute(command: Runnable) {
      command.run()
    }
  }

  class FacebookSerialThreadPoolMockExecutor(corePoolSize: Int) :
      ScheduledThreadPoolExecutor(corePoolSize) {
    override fun execute(command: Runnable) {
      // do nothing
    }
  }
}
