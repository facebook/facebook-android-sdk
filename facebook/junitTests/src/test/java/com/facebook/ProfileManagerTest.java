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

package com.facebook;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;

import java.util.InputMismatchException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

@PrepareForTest( { ProfileCache.class })
public class ProfileManagerTest extends FacebookPowerMockTestCase {

    @Before
    public void before() {
        FacebookSdk.sdkInitialize(Robolectric.application);
    }

    @Test
    public void testLoadCurrentProfileEmptyCache() {
        ProfileCache profileCache = mock(ProfileCache.class);
        LocalBroadcastManager localBroadcastManager = mock(LocalBroadcastManager.class);
        ProfileManager profileManager = new ProfileManager(
                localBroadcastManager,
                profileCache
        );
        assertFalse(profileManager.loadCurrentProfile());
        verify(profileCache, times(1)).load();
    }

    @Test
    public void testLoadCurrentProfileWithCache() {
        ProfileCache profileCache = mock(ProfileCache.class);
        Profile profile = ProfileTest.createDefaultProfile();
        when(profileCache.load()).thenReturn(profile);
        LocalBroadcastManager localBroadcastManager = mock(LocalBroadcastManager.class);
        ProfileManager profileManager = new ProfileManager(
                localBroadcastManager,
                profileCache
        );
        assertTrue(profileManager.loadCurrentProfile());
        verify(profileCache, times(1)).load();

        // Verify that we don't save it back
        verify(profileCache, never()).save(any(Profile.class));

        // Verify that we broadcast
        verify(localBroadcastManager).sendBroadcast(any(Intent.class));

        // Verify that if we set the same (semantically) profile there is no additional broadcast.
        profileManager.setCurrentProfile(ProfileTest.createDefaultProfile());
        verify(localBroadcastManager, times(1)).sendBroadcast(any(Intent.class));

        // Verify that if we unset the profile there is a broadcast
        profileManager.setCurrentProfile(null);
        verify(localBroadcastManager, times(2)).sendBroadcast(any(Intent.class));
    }
}
