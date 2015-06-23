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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;

import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import rx.subjects.BehaviorSubject;

final class ProfileManager {
    static final String ACTION_CURRENT_PROFILE_CHANGED =
            "com.facebook.sdk.ACTION_CURRENT_PROFILE_CHANGED";
    static final String EXTRA_OLD_PROFILE =
            "com.facebook.sdk.EXTRA_OLD_PROFILE";
    static final String EXTRA_NEW_PROFILE =
            "com.facebook.sdk.EXTRA_NEW_PROFILE";

    private static volatile ProfileManager instance;

    private final LocalBroadcastManager localBroadcastManager;
    private final ProfileCache profileCache;
    private Profile currentProfile;
    private final BehaviorSubject<Pair<Profile, Profile>> profileSubject;

    ProfileManager(
            LocalBroadcastManager localBroadcastManager,
            ProfileCache profileCache) {
        Validate.notNull(localBroadcastManager, "localBroadcastManager");
        Validate.notNull(profileCache, "profileCache");
        this.localBroadcastManager = localBroadcastManager;
        this.profileCache = profileCache;

        final Pair<Profile, Profile> initValue = Pair.create(null, null);
        this.profileSubject = BehaviorSubject.create(initValue);
    }

    static ProfileManager getInstance() {
        if (instance == null) {
            synchronized (ProfileManager.class) {
                if (instance == null) {
                    Context applicationContext = FacebookSdk.getApplicationContext();
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(
                            applicationContext);

                    instance = new ProfileManager(localBroadcastManager, new ProfileCache());
                }
            }
        }
        return instance;
    }

    Profile getCurrentProfile() {
        return currentProfile;
    }

    BehaviorSubject<Pair<Profile, Profile>> getProfileSubject() {
        return profileSubject;
    }

    boolean loadCurrentProfile() {
        Profile profile = profileCache.load();

        if (profile != null) {
            setCurrentProfile(profile, false);
            return true;
        }

        return false;
    }

    void setCurrentProfile(Profile currentProfile) {
        setCurrentProfile(currentProfile, true);
    }

    private void setCurrentProfile(Profile currentProfile, boolean writeToCache) {
        Profile oldProfile = this.currentProfile;
        this.currentProfile = currentProfile;

        if (writeToCache) {
            if (currentProfile != null) {
                profileCache.save(currentProfile);
            } else {
                profileCache.clear();
            }
        }

        if (!Utility.areObjectsEqual(oldProfile, currentProfile)) {
            sendCurrentProfileChangedBroadcast(oldProfile, currentProfile);
            emitCurrentProfile(oldProfile, currentProfile);
        }
    }

    private void sendCurrentProfileChangedBroadcast(
            Profile oldProfile,
            Profile currentProfile) {
        Intent intent = new Intent(ACTION_CURRENT_PROFILE_CHANGED);

        intent.putExtra(EXTRA_OLD_PROFILE, oldProfile);
        intent.putExtra(EXTRA_NEW_PROFILE, currentProfile);

        localBroadcastManager.sendBroadcast(intent);
    }

    private void emitCurrentProfile(Profile oldProfile, Profile currentProfile) {
        if (FacebookSdk.hasRxJavaSupport()) {
            profileSubject.onNext(Pair.create(oldProfile, currentProfile));
        }
    }
}
