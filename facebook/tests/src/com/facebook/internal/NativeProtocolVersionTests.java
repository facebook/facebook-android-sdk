/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.internal;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.TreeSet;

public class NativeProtocolVersionTests extends AndroidTestCase {
    @SmallTest
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

    @SmallTest
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

    @SmallTest
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

    @SmallTest
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

    @SmallTest
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

    @SmallTest
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

    @SmallTest
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

    @SmallTest
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
