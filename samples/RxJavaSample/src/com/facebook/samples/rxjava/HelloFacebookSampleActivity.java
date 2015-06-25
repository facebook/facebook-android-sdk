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

package com.facebook.samples.rxjava;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.Profile;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.android.app.AppObservable;
import rx.android.app.support.RxFragmentActivity;
import rx.android.lifecycle.LifecycleObservable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

public class HelloFacebookSampleActivity extends RxFragmentActivity {

    @InjectView(R.id.textView_userName) TextView textView_userName;

    private CallbackManager                                 callbackManager;
    private BehaviorSubject<Pair<AccessToken, AccessToken>> accessTokenSubject;
    private BehaviorSubject<Pair<Profile, Profile>>         profileSubject;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // layout loads the LoginButton, you have to initialize the SDK first
        FacebookSdk.sdkInitialize(this.getApplicationContext());

        setContentView(R.layout.main);
        ButterKnife.inject(this);

        callbackManager = CallbackManager.Factory.create();
        accessTokenSubject = AccessToken.getAccessTokenSubject();
        profileSubject = Profile.getProfileSubject();

        Profile.getCurrentProfile();
    }

    @Override
    protected void onStart() {

        super.onStart();
        bindObservables();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void bindObservables() {

        LifecycleObservable.bindActivityLifecycle(lifecycle(),
                AppObservable.bindActivity(this, accessTokenSubject))
                .filter(new Func1<Pair<AccessToken, AccessToken>, Boolean>() {

                    @Override
                    public Boolean call(Pair<AccessToken, AccessToken> pair) {

                        return pair.second == null;
                    }
                })
                .subscribe(new Action1<Pair<AccessToken, AccessToken>>() {

                    @Override
                    public void call(Pair<AccessToken, AccessToken> pair) {

                        textView_userName.setText(getString(R.string.please_login));
                    }
                });

        LifecycleObservable.bindActivityLifecycle(lifecycle(),
                AppObservable.bindActivity(this, profileSubject))
                .filter(new Func1<Pair<Profile, Profile>, Boolean>() {

                    @Override
                    public Boolean call(Pair<Profile, Profile> pair) {

                        return pair.second != null;
                    }
                })
                .subscribe(new Action1<Pair<Profile, Profile>>() {

                    @Override
                    public void call(Pair<Profile, Profile> pair) {

                        textView_userName.setText(getString(R.string.hello_user, pair.second.getFirstName()));
                    }
                });
    }
}
