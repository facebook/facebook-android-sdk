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
    "kotlin.test.*")
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
