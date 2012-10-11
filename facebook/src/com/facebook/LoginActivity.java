package com.facebook;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieSyncManager;
import com.facebook.android.*;

/**
 * This class addresses the issue of a potential window leak during
 * dialog authorization if the Activity containing the dialog is destroyed
 * (e.g. if the user rotates the device).
 * <p/>
 * Add this activity to your AndroidManifest.xml to ensure proper handling
 * of dialog authorization.
 */
public class LoginActivity extends Activity {
    private Dialog loginDialog;
    private Dialog errorDialog;

    @Override
    public void onResume() {
        super.onResume();
        int permissionCheck = checkCallingOrSelfPermission(Manifest.permission.INTERNET);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("AndroidManifest Error")
                   .setMessage("WebView login requires INTERNET permission")
                   .setCancelable(true)
                   .setPositiveButton(R.string.com_facebook_dialogloginactivity_ok_button,
                           new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialogInterface, int i) {
                                   finish();
                               }
                           })
                   .setOnCancelListener(new DialogInterface.OnCancelListener() {
                       @Override
                       public void onCancel(DialogInterface dialogInterface) {
                           finish();
                       }
                   });
            errorDialog = builder.create();
            errorDialog.show();
            setResult(Activity.RESULT_CANCELED);
            return;
        }

        Bundle parameters = new Bundle();
        String permissions = getIntent().getStringExtra("scope");
        if (!Utility.isNullOrEmpty(permissions)) {
            parameters.putString(ServerProtocol.DIALOG_PARAM_SCOPE, permissions);
        }

        // The call to clear cookies will create the first instance of CookieSyncManager if necessary
        Utility.clearFacebookCookies(this);

        Facebook.DialogListener listener = new Facebook.DialogListener() {
            public void onComplete(Bundle bundle) {
                // Ensure any cookies set by the dialog are saved
                CookieSyncManager.getInstance().sync();
                setResultAndFinish(Activity.RESULT_OK, bundle);
            }

            public void onError(DialogError error) {
                Bundle bundle = null;
                if (error != null) {
                    bundle = new Bundle();
                    bundle.putInt(Session.WEB_VIEW_ERROR_CODE_KEY, error.getErrorCode());
                    bundle.putString(Session.WEB_VIEW_FAILING_URL_KEY, error.getFailingUrl());
                    bundle.putString("error", error.getMessage());
                }
                setResultAndFinish(Activity.RESULT_OK, bundle);
            }

            public void onFacebookError(FacebookError error) {
                Bundle bundle = null;
                if (error != null && error.getMessage() != null) {
                    bundle = new Bundle();
                    bundle.putString("error", error.getMessage());
                }
                setResultAndFinish(Activity.RESULT_OK, bundle);
            }

            public void onCancel() {
                setResultAndFinish(Activity.RESULT_CANCELED, null);
            }

            private void setResultAndFinish(int resultCode, Bundle bundle) {
                if (bundle != null) {
                    Intent intent = new Intent();
                    intent.putExtras(bundle);
                    setResult(resultCode, intent);
                } else {
                    setResult(resultCode);
                }
                finish();
            }
        };

        parameters.putString(ServerProtocol.DIALOG_PARAM_DISPLAY, "touch");
        parameters.putString(ServerProtocol.DIALOG_PARAM_REDIRECT_URI, "fbconnect://success");
        parameters.putString(ServerProtocol.DIALOG_PARAM_TYPE, "user_agent");
        parameters.putString(ServerProtocol.DIALOG_PARAM_CLIENT_ID, getIntent().getStringExtra("client_id"));

        Uri uri = Utility.buildUri(ServerProtocol.DIALOG_AUTHORITY, ServerProtocol.DIALOG_OAUTH_PATH, parameters);
        loginDialog = new FbDialog(this, uri.toString(), listener);
        loginDialog.show();
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
}
