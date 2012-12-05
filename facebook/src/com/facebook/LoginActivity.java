/**
 * Copyright 2012 Facebook
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

package com.facebook;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.CookieSyncManager;
import com.facebook.android.*;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Utility;
import com.facebook.widget.WebDialog;

import java.util.ArrayList;

/**
 * This class addresses the issue of a potential window leak during
 * dialog authorization if the Activity containing the dialog is destroyed
 * (e.g. if the user rotates the device).
 * <p/>
 * Add this activity to your AndroidManifest.xml to ensure proper handling
 * of dialog authorization.
 */
public class LoginActivity extends Activity {
    static final String EXTRA_APPLICATION_ID = "com.facebook.sdk.extra.APPLICATION_ID";
    static final String EXTRA_PERMISSIONS = "com.facebook.sdk.extra.PERMISSIONS";
    static final String EXTRA_IS_LEGACY = "com.facebook.sdk.extra.IS_LEGACY";
    static final String EXTRA_DEFAULT_AUDIENCE = "com.facebook.sdk.extra.DEFAULT_AUDIENCE";

    private static final String BASIC_INFO = "basic_info";

    static final String LOGIN_FAILED = "Login attempt failed.";
    static final String INTERNET_PERMISSIONS_NEEDED = "WebView login requires INTERNET permission";
    static final String ERROR_KEY = "error";

    private static final int DEFAULT_REQUEST_CODE = 0xface;
    private static final String NULL_CALLING_PKG_ERROR_MSG =
            "Cannot call LoginActivity with a null calling package. " +
            "This can occur if the launchMode of the caller is singleInstance.";
    private static final String SAVED_CALLING_PKG_KEY = "callingPackage";
    private static final String SAVED_STARTED_KATANA = "startedKatana";

    private Dialog loginDialog;
    private boolean isLegacy;
    private Dialog errorDialog;
    private SessionLoginBehavior loginBehavior;
    private String callingPackage;
    private boolean startedKatana;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            callingPackage = savedInstanceState.getString(SAVED_CALLING_PKG_KEY);
            startedKatana = savedInstanceState.getBoolean(SAVED_STARTED_KATANA);
            isLegacy = savedInstanceState.getBoolean(EXTRA_IS_LEGACY);
        } else {
            callingPackage = getCallingPackage();
            startedKatana = false;
            isLegacy = getIntent().getBooleanExtra(EXTRA_IS_LEGACY, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // If the calling package is null, this generally means that the callee was started
        // with a launchMode of singleInstance. Unfortunately, Android does not allow a result
        // to be set when the callee is a singleInstance, so we throw an exception here.
        if (callingPackage == null) {
            throw new FacebookException(NULL_CALLING_PKG_ERROR_MSG);
        }

        String action = getIntent().getAction();
        if (action != null) {
            loginBehavior = SessionLoginBehavior.valueOf(action);
        } else {
            // default to SSO with fallback
            loginBehavior = SessionLoginBehavior.SSO_WITH_FALLBACK;
        }

        startAuth();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (errorDialog != null && errorDialog.isShowing()) {
            errorDialog.dismiss();
        }
        if (loginDialog != null && loginDialog.isShowing()) {
            loginDialog.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_CALLING_PKG_KEY, callingPackage);
        outState.putBoolean(SAVED_STARTED_KATANA, startedKatana);
        outState.putBoolean(EXTRA_IS_LEGACY, isLegacy);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DEFAULT_REQUEST_CODE) {
            if (isServiceDisabledResult20121101(data)) {
                // Fall back to legacy auth
                isLegacy = true;
                startedKatana = false;
                startAuth();
            } else {
                setResult(resultCode, data);
                finish();
            }
        }
    }

    private void startAuth() {
        boolean started = startedKatana;
        if (!started && allowKatana(loginBehavior)) {
            started = tryKatanaAuth();
        }
        if (!started && allowWebView(loginBehavior)) {
            started = tryDialogAuth();
        }
        if (!started) {
            finishWithResultOk(getErrorResultBundle("Login attempt failed."));
        }
    }

    private boolean tryKatanaAuth() {
        Bundle extras = getIntent().getExtras();
        boolean started = false;

        if (!isLegacy) {
            Intent intent = getLoginDialog20121101Intent(this, extras);
            started = tryKatanaIntent(this, intent);
        }

        if (!started) {
            Intent intent = getProxyAuthIntent(this, extras);
            started = tryKatanaIntent(this, intent);
        }

        startedKatana = started;
        return started;
    }

    static boolean tryKatanaIntent(Activity activity, Intent intent) {
        if (intent == null) {
            return false;
        }

        try {
            activity.startActivityForResult(intent, DEFAULT_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            return false;
        }

        return true;
    }

    static Intent getProxyAuthIntent(Context context, Bundle extras) {
        String applicationId = extras.getString(EXTRA_APPLICATION_ID);
        ArrayList<String> permissions = extras.getStringArrayList(EXTRA_PERMISSIONS);

        Intent intent = new Intent()
                .setClassName(NativeProtocol.KATANA_PACKAGE, NativeProtocol.KATANA_PROXY_AUTH_ACTIVITY)
                .putExtra(ServerProtocol.DIALOG_PARAM_CLIENT_ID, applicationId);

        if (!Utility.isNullOrEmpty(permissions)) {
            intent.putExtra(ServerProtocol.DIALOG_PARAM_SCOPE, TextUtils.join(",", permissions));
        }

        return validateKatanaIntent(context, intent);
    }

    static Intent getLoginDialog20121101Intent(Context context, Bundle extras) {
        String applicationId = extras.getString(EXTRA_APPLICATION_ID);
        ArrayList<String> permissions = extras.getStringArrayList(EXTRA_PERMISSIONS);
        String audience = extras.getString(EXTRA_DEFAULT_AUDIENCE);

        Intent intent = new Intent()
                .setAction(NativeProtocol.INTENT_ACTION_PLATFORM_ACTIVITY)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .putExtra(NativeProtocol.EXTRA_PROTOCOL_VERSION, NativeProtocol.PROTOCOL_VERSION_20121101)
                .putExtra(NativeProtocol.EXTRA_PROTOCOL_ACTION, NativeProtocol.ACTION_LOGIN_DIALOG)
                .putExtra(NativeProtocol.EXTRA_APPLICATION_ID, applicationId)
                .putStringArrayListExtra(NativeProtocol.EXTRA_PERMISSIONS, ensureDefaultPermissions(permissions))
                .putExtra(NativeProtocol.EXTRA_WRITE_PRIVACY, ensureDefaultAudience(audience));

        return validateKatanaIntent(context, intent);
    }

    private static String ensureDefaultAudience(String audience) {
        if (Utility.isNullOrEmpty(audience)) {
            return NativeProtocol.AUDIENCE_ME;
        } else {
            return audience;
        }
    }

    private static ArrayList<String> ensureDefaultPermissions(ArrayList<String> permissions) {
        ArrayList<String> updated;

        // Return if we are doing publish, or if basic_info is already included
        if (Utility.isNullOrEmpty(permissions)) {
            updated = new ArrayList<String>();
        } else {
            for (String permission : permissions) {
                if (Session.isPublishPermission(permission) || BASIC_INFO.equals(permission)) {
                    return permissions;
                }
            }
            updated = new ArrayList<String>(permissions);
        }

        updated.add(BASIC_INFO);
        return updated;
    }

    private boolean isServiceDisabledResult20121101(Intent data) {
        int protocolVersion = data.getIntExtra(NativeProtocol.EXTRA_PROTOCOL_VERSION, 0);
        String errorType = data.getStringExtra(NativeProtocol.STATUS_ERROR_TYPE);

        return ((NativeProtocol.PROTOCOL_VERSION_20121101 == protocolVersion) &&
                NativeProtocol.ERROR_SERVICE_DISABLED.equals(errorType));
    }

    private static Intent validateKatanaIntent(Context context, Intent intent) {
        if (intent == null) {
            return null;
        }

        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return null;
        }

        if (!NativeProtocol.validateSignature(context, resolveInfo.activityInfo.packageName)) {
            return null;
        }

        return intent;
    }

    private boolean tryDialogAuth() {
        int permissionCheck = checkCallingOrSelfPermission(Manifest.permission.INTERNET);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.com_facebook_internet_permission_error_title)
                    .setMessage(R.string.com_facebook_internet_permission_error_message)
                    .setCancelable(true)
                    .setPositiveButton(R.string.com_facebook_dialogloginactivity_ok_button,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finishWithResultOk(
                                            getErrorResultBundle(INTERNET_PERMISSIONS_NEEDED));
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            finishWithResultOk(getErrorResultBundle(INTERNET_PERMISSIONS_NEEDED));
                        }
                    });
            errorDialog = builder.create();
            errorDialog.show();
            finishWithResultOk(getErrorResultBundle(LOGIN_FAILED));
            return false;
        }

        Bundle extras = getIntent().getExtras();
        String applicationId = extras.getString(EXTRA_APPLICATION_ID);
        ArrayList<String> permissions = extras.getStringArrayList(EXTRA_PERMISSIONS);

        Bundle parameters = new Bundle();
        if (!Utility.isNullOrEmpty(permissions)) {
            parameters.putString(ServerProtocol.DIALOG_PARAM_SCOPE, TextUtils.join(",", permissions));
        }

        // The call to clear cookies will create the first instance of CookieSyncManager if necessary
        Utility.clearFacebookCookies(this);

        WebDialog.OnCompleteListener listener = new WebDialog.OnCompleteListener() {
            @Override
            public void onComplete(Bundle values, FacebookException error) {
                if (values != null) {
                    // Ensure any cookies set by the dialog are saved
                    CookieSyncManager.getInstance().sync();
                    finishWithResultOk(values);
                } else {
                    Bundle bundle = new Bundle();
                    if (error instanceof FacebookDialogException) {
                        FacebookDialogException dialogException = (FacebookDialogException) error;
                        bundle.putInt(Session.WEB_VIEW_ERROR_CODE_KEY, dialogException.getErrorCode());
                        bundle.putString(Session.WEB_VIEW_FAILING_URL_KEY, dialogException.getFailingUrl());
                    } else if (error instanceof FacebookOperationCanceledException) {
                        finishWithResultCancel(null);
                    }
                    bundle.putString(ERROR_KEY, error.getMessage());
                    finishWithResultOk(bundle);
                }
            }
        };

        WebDialog.Builder builder =
                new AuthDialogBuilder(this, applicationId, parameters)
                .setOnCompleteListener(listener);
        loginDialog = builder.build();
        loginDialog.show();

        return true;
    }

    static boolean allowKatana(SessionLoginBehavior loginBehavior) {
        return !SessionLoginBehavior.SUPPRESS_SSO.equals(loginBehavior);
    }

    static boolean allowWebView(SessionLoginBehavior loginBehavior) {
        return !SessionLoginBehavior.SSO_ONLY.equals(loginBehavior);
    }

    private void finishWithResultOk(Bundle extras) {
        finishWithResult(true, extras);
    }

    private void finishWithResultCancel(Bundle extras) {
        finishWithResult(false, extras);
    }

    private void finishWithResult(boolean success, Bundle extras) {
        int resultStatus = (success) ? RESULT_OK : RESULT_CANCELED;
        if (extras == null) {
            setResult(resultStatus);
        } else {
            Intent resultIntent = new Intent();
            resultIntent.putExtras(extras);
            setResult(resultStatus, resultIntent);
        }
        finish();
    }

    private Bundle getErrorResultBundle(String error) {
        Bundle result = new Bundle();
        result.putString(ERROR_KEY, error);
        return result;
    }

    static class AuthDialogBuilder extends WebDialog.Builder {
        private static final String OAUTH_DIALOG = "oauth";
        static final String REDIRECT_URI = "fbconnect://success";

        public AuthDialogBuilder(Context context, String applicationId, Bundle parameters) {
            super(context, applicationId, OAUTH_DIALOG, parameters);
        }

        @Override
        public WebDialog build() {
            Bundle parameters = getParameters();
            parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, REDIRECT_URI);
            parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, getApplicationId());

            return new WebDialog(getContext(), OAUTH_DIALOG, parameters, getTheme(), getListener());
        }
    }
}
