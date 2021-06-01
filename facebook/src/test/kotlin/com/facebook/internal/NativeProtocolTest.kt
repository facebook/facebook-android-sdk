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

package com.facebook.internal

import com.facebook.FacebookTestCase
import java.util.TreeSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NativeProtocolTest : FacebookTestCase() {
  @Test
  fun `sdk version older than app with version spec open`() {
    // Base case where a feature was enabled a while ago and the SDK and Native app have been
    // updated since then.
    val versionSpec = intArrayOf(3)
    val latestSdkVersion = 7
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(7)
  }

  @Test
  fun `sdk version newer than app with version spec open`() {
    // Base case where a feature was enabled a while ago and the SDK and Native app have been
    // updated since then.
    val versionSpec = intArrayOf(3)
    val latestSdkVersion = 8
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(7)
  }

  @Test
  fun `sdk version older than app with version spec disabled`() {
    // Case where a feature was enabled AND disabled a while ago and the SDK and Native app have
    // been updated since then.
    val versionSpec = intArrayOf(1, 3, 7, 8)
    val latestSdkVersion = 7
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  @Test
  fun `sdk version newer than app with version spec disabled`() {
    // Case where a feature was enabled AND disabled a while ago and the SDK and Native app have
    // been updated since then.
    val versionSpec = intArrayOf(1, 3, 6, 7)
    val latestSdkVersion = 8
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  @Test
  fun `test sdk version older than app with version spec newer and enabled`() {
    // Case where the sdk and app are older, but the app is still enabled
    val versionSpec = intArrayOf(1, 3, 7, 9, 10, 11, 12, 13)
    val latestSdkVersion = 7
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(7)
  }

  @Test
  fun `test sdk version newer than app with version spec newer and enabled`() {
    // Case where the sdk and app are older, but the app is still enabled
    val versionSpec = intArrayOf(1, 3, 7, 9, 10, 11, 12, 13)
    val latestSdkVersion = 8
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(7)
  }

  @Test
  fun `test sdk version older than app with version spec newer and disabled`() {
    // Case where the sdk and app are older, and the app is a disabled version
    val versionSpec = intArrayOf(1, 3, 7, 8, 10, 11, 12, 13)
    val latestSdkVersion = 7
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  @Test
  fun `test sdk version newer than app with version spec newer and disabled`() {
    // Case where the sdk and app are older, and the app is a disabled version
    val versionSpec = intArrayOf(1, 3, 6, 7, 10, 11, 12, 13)
    val latestSdkVersion = 8
    val availableFbAppVersions = intArrayOf(1, 2, 3, 4, 5, 6, 7)

    val resultVersion =
        NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
            getTreeSetFromIntArray(availableFbAppVersions), latestSdkVersion, versionSpec)

    assertThat(resultVersion).isEqualTo(NativeProtocol.NO_PROTOCOL_AVAILABLE)
  }

  fun getTreeSetFromIntArray(array: IntArray): TreeSet<Int> {
    val treeSet = TreeSet<Int>()
    for (a in array) {
      treeSet.add(a)
    }
    return treeSet
  }
}
