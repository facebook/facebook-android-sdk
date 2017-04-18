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

package com.example.places.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.places.R;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment implements login to Facebook. This step is optional when
 * using the Places Graph SDK.
 *
 * The Places Graph SDK supports two types of authentication tokens:
 * <ul>
 *     <li>Client Token: these do NOT require users to log in Facebook.
 * Refer to {@link com.example.places.MainActivity} to see how to use a Client Token.</li>
 *     <li>User Access Token: these are the tokens obtained when a user logs into Facebook.
 *     This fragment illustrates how to login to Facebook and get a user token.
 *     For more information about Facebook login, see the "HelloFacebookSample".</li>
 * </ul>
 */
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private static final String PUBLIC_PERMISSION = "public_profile";

    private Listener listener;
    private CallbackManager callbackManager;

    public interface Listener {
        void onLoginComplete();
    }

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            listener = (Listener) context;
        }
    }

    public void onDestroy() {
        super.onDestroy();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.login_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LoginButton loginButton = (LoginButton) view.findViewById(R.id.login_button);
        loginButton.setFragment(this);
        List<String> permissions = new ArrayList<>();
        permissions.add(PUBLIC_PERMISSION);
        loginButton.setReadPermissions(permissions);
        callbackManager = CallbackManager.Factory.create();

        loginButton.registerCallback(
            callbackManager,
            new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        listener.onLoginComplete();
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "onCancel");
                        showAlert();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Log.d(TAG, "onError: " + exception);
                        showAlert();
                    }
                });
    }

    private void showAlert() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.cancelled)
                .setMessage(R.string.permission_not_granted)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
