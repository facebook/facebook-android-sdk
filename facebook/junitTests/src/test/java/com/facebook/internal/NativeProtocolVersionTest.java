/**
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

package com.facebook.internal;

import android.test.suitebuilder.annotation.SmallTest;

import com.facebook.FacebookTestCase;

import org.junit.Test;

import java.util.TreeSet;

import static org.junit.Assert.*;


public class NativeProtocolVersionTest extends FacebookTestCase {
    @Test
    public void testSdkOlderThanApp_versionSpecOpen() {
        // Base case where a feature was enabled a while ago and the SDK and Native app have been updated
        // since then.
        int[] versionSpec = new int[] {3};
        int latestSdkVersion = 7;
        int[] availableFbAppVersions = new int[] {1,2,3,4,5,6,7,8};

        int resultVersion = NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
                getTreeSetFromIntArray(availableFbAppVersions),
                latestSdkVersion,
                versionSpec);

        assertEquals(resultVersion, 7);
    }

    @Test
    public void testSdkNewerThanApp_versionSpecOpen() {
        // Base case where a feature was enabled a while ago and the SDK and Native app have been updated
        // since then.
        int[] versionSpec = new int[] {3};
        int latestSdkVersion = 8;
        int[] availableFbAppVersions = new int[] {1,2,3,4,5,6,7};

        int resultVersion = NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
                getTreeSetFromIntArray(availableFbAppVersions),
                latestSdkVersion,
                versionSpec);

        assertEquals(resultVersion, 7);
    }

    @Test
    public void testSdkOlderThanApp_versionSpecDisabled() {
        // Case where a feature was enabled AND disabled a while ago and the SDK and Native app have been
        // updated since then.
        int[] versionSpec = new int[] {1,3,7,8};
        int latestSdkVersion = 7;
        int[] availableFbAppVersions = new int[] {1,2,3,4,5,6,7,8};

        int resultVersion = NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
                getTreeSetFromIntArray(availableFbAppVersions),
                latestSdkVersion,
                versionSpec);

        assertEquals(resultVersion, NativeProtocol.NO_PROTOCOL_AVAILABLE);
    }

    @Test
    public void testSdkNewerThanApp_versionSpecDisabled() {
        // Case where a feature was enabled AND disabled a while ago and the SDK and Native app have been
        // updated since then.
        int[] versionSpec = new int[] {1,3,6,7};
        int latestSdkVersion = 8;
        int[] availableFbAppVersions = new int[] {1,2,3,4,5,6,7};

        int resultVersion = NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
                getTreeSetFromIntArray(availableFbAppVersions),
                latestSdkVersion,
                versionSpec);

        assertEquals(resultVersion, NativeProtocol.NO_PROTOCOL_AVAILABLE);
    }

    @Test
    public void testSdkOlderThanApp_versionSpecNewerAndEnabled() {
        // Case where the sdk and app are older, but the app is still enabled
        int[] versionSpec = new int[] {1,3,7,9,10,11,12,13};
        int latestSdkVersion = 7;
        int[] availableFbAppVersions = new int[] {1,2,3,4,5,6,7,8};

        int resultVersion = NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
                getTreeSetFromIntArray(availableFbAppVersions),
                latestSdkVersion,
                versionSpec);

        assertEquals(resultVersion, 7);
    }

    @Test
    public void testSdkNewerThanApp_versionSpecNewerAndEnabled() {
        // Case where the sdk and app are older, but the app is still enabled
        int[] versionSpec = new int[] {1,3,7,9,10,11,12,13};
        int latestSdkVersion = 8;
        int[] availableFbAppVersions = new int[] {1,2,3,4,5,6,7};

        int resultVersion = NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
                getTreeSetFromIntArray(availableFbAppVersions),
                latestSdkVersion,
                versionSpec);

        assertEquals(resultVersion, 7);
    }

    @Test
    public void testSdkOlderThanApp_versionSpecNewerAndDisabled() {
        // Case where the sdk and app are older, and the app is a disabled version
        int[] versionSpec = new int[] {1,3,7,8,10,11,12,13};
        int latestSdkVersion = 7;
        int[] availableFbAppVersions = new int[] {1,2,3,4,5,6,7,8};

        int resultVersion = NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
                getTreeSetFromIntArray(availableFbAppVersions),
                latestSdkVersion,
                versionSpec);

        assertEquals(resultVersion, NativeProtocol.NO_PROTOCOL_AVAILABLE);
    }

    @Test
    public void testSdkNewerThanApp_versionSpecNewerAndDisabled() {
        // Case where the sdk and app are older, and the app is a disabled version
        int[] versionSpec = new int[] {1,3,6,7,10,11,12,13};
        int latestSdkVersion = 8;
        int[] availableFbAppVersions = new int[] {1,2,3,4,5,6,7};

        int resultVersion = NativeProtocol.computeLatestAvailableVersionFromVersionSpec(
                getTreeSetFromIntArray(availableFbAppVersions),
                latestSdkVersion,
                versionSpec);

        assertEquals(resultVersion, NativeProtocol.NO_PROTOCOL_AVAILABLE);
    }

    private TreeSet<Integer> getTreeSetFromIntArray(int[] array) {
        TreeSet<Integer> treeSet = new TreeSet<Integer>();
        for (int a : array) {
            treeSet.add(a);
        }

        return treeSet;
    }
}
