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

package com.facebook.login;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.FacebookActivity;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestAsyncTask;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.common.R;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.devicerequests.internal.DeviceRequestsHelper;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.SmartLoginOption;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeviceAuthDialog extends DialogFragment {
    private static final String DEVICE_LOGIN_ENDPOINT = "device/login";
    private static final String DEVICE_LOGIN_STATUS_ENDPOINT = "device/login_status";
    private static final String REQUEST_STATE_KEY = "request_state";

    private static final int LOGIN_ERROR_SUBCODE_EXCESSIVE_POLLING = 1349172;
    private static final int LOGIN_ERROR_SUBCODE_AUTHORIZATION_DECLINED = 1349173;
    private static final int LOGIN_ERROR_SUBCODE_AUTHORIZATION_PENDING = 1349174;
    private static final int LOGIN_ERROR_SUBCODE_CODE_EXPIRED = 1349152;

    private ProgressBar progressBar;
    private TextView confirmationCode;
    private TextView instructions;
    private DeviceAuthMethodHandler deviceAuthMethodHandler;
    private AtomicBoolean completed = new AtomicBoolean();
    private volatile GraphRequestAsyncTask currentGraphRequestPoll;
    private volatile ScheduledFuture scheduledPoll;
    private volatile RequestState currentRequestState;
    private Dialog dialog;

    // Used to tell if we are destroying the fragment because it was dismissed or dismissing the
    // fragment because it is being destroyed.
    private boolean isBeingDestroyed = false;
    private boolean isRetry = false;
    private LoginClient.Request mRequest = null;

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        FacebookActivity facebookActivity = (FacebookActivity) getActivity();
        LoginFragment fragment = (LoginFragment)facebookActivity.getCurrentFragment();
        deviceAuthMethodHandler = (DeviceAuthMethodHandler)fragment
                .getLoginClient()
                .getCurrentHandler();

        if (savedInstanceState != null) {
            RequestState requestState = savedInstanceState.getParcelable(REQUEST_STATE_KEY);
            if (requestState != null) {
                setCurrentRequestState(requestState);
            }
        }

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = new Dialog(getActivity(), R.style.com_facebook_auth_dialog);

        View view = initializeContentView(DeviceRequestsHelper.isAvailable() && !this.isRetry);

        dialog.setContentView(view);
        return dialog;
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!isBeingDestroyed) {
            onCancel();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentRequestState != null) {
            outState.putParcelable(REQUEST_STATE_KEY, currentRequestState);
        }
    }

    @Override
    public void onDestroy() {
        // Set this to true so we know if we are being destroyed and then dismissing the dialog
        // Or if we are dismissing the dialog and then destroying the fragment. In latter we want
        // to do a cancel callback.
        isBeingDestroyed = true;
        completed.set(true);
        super.onDestroy();
        if (currentGraphRequestPoll != null) {
            currentGraphRequestPoll.cancel(true);
        }

        if (scheduledPoll != null) {
            scheduledPoll.cancel(true);
        }
    }

    public void startLogin(final LoginClient.Request request) {
        this.mRequest = request;
        final Bundle parameters = new Bundle();
        parameters.putString("scope", TextUtils.join(",", request.getPermissions()));

        String redirectUriString = request.getDeviceRedirectUriString();
        if (redirectUriString != null) {
            parameters.putString("redirect_uri", redirectUriString);
        }

        String accessToken = Validate.hasAppID()+ "|" + Validate.hasClientToken();
        parameters.putString(GraphRequest.ACCESS_TOKEN_PARAM, accessToken);
        parameters.putString(DeviceRequestsHelper.DEVICE_INFO_PARAM,
                             DeviceRequestsHelper.getDeviceInfo());

        GraphRequest graphRequest = new GraphRequest(
                null,
                DEVICE_LOGIN_ENDPOINT,
                parameters,
                HttpMethod.POST,
                new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                if (isBeingDestroyed) {
                    return;
                }
                if (response.getError() != null) {
                    onError(response.getError().getException());
                    return;
                }

                JSONObject jsonObject = response.getJSONObject();
                RequestState requestState = new RequestState();
                try {
                    requestState.setUserCode(jsonObject.getString("user_code"));
                    requestState.setRequestCode(jsonObject.getString("code"));
                    requestState.setInterval(jsonObject.getLong("interval"));
                } catch (JSONException ex) {
                    onError(new FacebookException(ex));
                    return;
                }

                setCurrentRequestState(requestState);
            }
        });
        graphRequest.executeAsync();
    }

    private void setCurrentRequestState(RequestState currentRequestState) {
        this.currentRequestState = currentRequestState;
        confirmationCode.setText(currentRequestState.getUserCode());
        final Bitmap bitmap =
                DeviceRequestsHelper.generateQRCode(currentRequestState.getAuthorizationUri());
        final BitmapDrawable qrCode = new BitmapDrawable(getResources(), bitmap);
        instructions.setCompoundDrawablesWithIntrinsicBounds(null, qrCode, null, null);
        confirmationCode.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        if (!isRetry) {
            if (DeviceRequestsHelper.startAdvertisementService(currentRequestState.getUserCode())) {
                final AppEventsLogger logger = AppEventsLogger.newLogger(getContext());
                logger.logSdkEvent(AnalyticsEvents.EVENT_SMART_LOGIN_SERVICE, null, null);
            }
        }

        // If we polled within the last interval schedule a poll else start a poll.
        if (currentRequestState.withinLastRefreshWindow()) {
            schedulePoll();
        } else {
            poll();
        }
    }

    private View initializeContentView(boolean isSmartLogin) {
        View view;
        LayoutInflater inflater = this.getActivity().getLayoutInflater();
        if (isSmartLogin) {
            view = inflater.inflate(R.layout.com_facebook_smart_device_dialog_fragment, null);
        } else {
            view = inflater.inflate(R.layout.com_facebook_device_auth_dialog_fragment, null);
        }
        progressBar = (ProgressBar)view.findViewById(R.id.progress_bar);
        confirmationCode = (TextView)view.findViewById(R.id.confirmation_code);

        Button cancelButton = (Button) view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel();
            }
        });

        instructions = (TextView) view.findViewById(
                R.id.com_facebook_device_auth_instructions);
        instructions.setText(
                Html.fromHtml(getString(R.string.com_facebook_device_auth_instructions)));
        return view;
    }

    private void poll() {
        currentRequestState.setLastPoll(new Date().getTime());
        currentGraphRequestPoll = getPollRequest().executeAsync();
    }

    private void schedulePoll() {
        scheduledPoll = DeviceAuthMethodHandler.getBackgroundExecutor().schedule(
                new Runnable() {
                    @Override
                    public void run() {
                        poll();
                    }
                },
                currentRequestState.getInterval(),
                TimeUnit.SECONDS);
    }

    private GraphRequest getPollRequest() {
        Bundle parameters = new Bundle();
        parameters.putString("code", currentRequestState.getRequestCode());
        return new GraphRequest(
                null,
                DEVICE_LOGIN_STATUS_ENDPOINT,
                parameters,
                HttpMethod.POST,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        // Check if the request was already cancelled
                        if (completed.get()) {
                            return;
                        }

                        FacebookRequestError error = response.getError();
                        if (error != null) {
                            // We need to decide if this is a fatal error by checking the error
                            // message text
                            switch (error.getSubErrorCode()) {
                                case LOGIN_ERROR_SUBCODE_AUTHORIZATION_PENDING:
                                case LOGIN_ERROR_SUBCODE_EXCESSIVE_POLLING: {
                                    // Keep polling. If we got the slow down message just ignore
                                    schedulePoll();
                                } break;
                                case LOGIN_ERROR_SUBCODE_CODE_EXPIRED:
                                case LOGIN_ERROR_SUBCODE_AUTHORIZATION_DECLINED: {
                                    onCancel();
                                } break;
                                default: {
                                    onError(response.getError().getException());
                                }
                                break;
                            }
                            return;
                        }

                        try {
                            JSONObject resultObject = response.getJSONObject();
                            onSuccess(resultObject.getString("access_token"));
                        } catch (JSONException ex) {
                            onError(new FacebookException(ex));
                        }
                    }
                });
    }

    private void presentConfirmation(final String userId,
                                     final Utility.PermissionsPair permissions,
                                     final String accessToken,
                                     final String name) {
        final String message = getResources().getString(
            R.string.com_facebook_smart_login_confirmation_title);
        final String continueFormat = getResources().getString(
            R.string.com_facebook_smart_login_confirmation_continue_as);
        final String cancel = getResources().getString(
            R.string.com_facebook_smart_login_confirmation_cancel);
        final String continueText = String.format(continueFormat, name);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(message)
                .setCancelable(true)
                .setNegativeButton(continueText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface alertDialog, int which) {
                        completeLogin(userId, permissions, accessToken);
                    }
                })
                .setPositiveButton(cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface alertDialog, int which) {
                        View view = initializeContentView(false);
                        dialog.setContentView(view);
                        startLogin(mRequest);
                    }
                });
        builder.create().show();
    }
    private void onSuccess(final String accessToken) {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,permissions,name");
        AccessToken temporaryToken = new AccessToken(
                accessToken,
                FacebookSdk.getApplicationId(),
                "0",
                null,
                null,
                null,
                null,
                null);

        GraphRequest request = new GraphRequest(
                temporaryToken,
                "me",
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        if (completed.get()) {
                            return;
                        }

                        if (response.getError() != null) {
                            onError(response.getError().getException());
                            return;
                        }

                        String userId;
                        Utility.PermissionsPair permissions;
                        String name;
                        try {
                            JSONObject jsonObject = response.getJSONObject();
                            userId = jsonObject.getString("id");
                            permissions = Utility.handlePermissionResponse(jsonObject);
                            name = jsonObject.getString("name");
                        } catch (JSONException ex) {
                            onError(new FacebookException(ex));
                            return;
                        }
                        DeviceRequestsHelper.cleanUpAdvertisementService(
                                currentRequestState.getUserCode());

                        boolean requireConfirm =
                                FetchedAppSettingsManager.
                                getAppSettingsWithoutQuery(FacebookSdk.getApplicationId()).
                                getSmartLoginOptions().contains(SmartLoginOption.RequireConfirm);
                        if (requireConfirm && !isRetry) {
                            isRetry = true;
                            presentConfirmation(userId, permissions, accessToken, name);
                            return;
                        }

                        completeLogin(userId, permissions, accessToken);
                    }
                });
        request.executeAsync();
    }

    private void completeLogin(String userId,
                               Utility.PermissionsPair permissions,
                               String accessToken) {
        deviceAuthMethodHandler.onSuccess(
                accessToken,
                FacebookSdk.getApplicationId(),
                userId,
                permissions.getGrantedPermissions(),
                permissions.getDeclinedPermissions(),
                AccessTokenSource.DEVICE_AUTH,
                null,
                null);
        dialog.dismiss();
    }

    private void onError(FacebookException ex) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }

        if (currentRequestState != null) {
            DeviceRequestsHelper.cleanUpAdvertisementService(currentRequestState.getUserCode());
        }
        deviceAuthMethodHandler.onError(ex);
        dialog.dismiss();
    }

    private void onCancel() {
        if (!completed.compareAndSet(false, true)) {
            // Should not have happened but we called cancel twice
            return;
        }

        if (currentRequestState != null) {
            DeviceRequestsHelper.cleanUpAdvertisementService(currentRequestState.getUserCode());
        }

        if (deviceAuthMethodHandler != null) {
            // We are detached and cannot send a cancel message back
            deviceAuthMethodHandler.onCancel();
        }

        dialog.dismiss();
    }

    private static class RequestState implements Parcelable{
        private String authorizationUri;
        private String userCode;
        private String requestCode;
        private long interval;
        private long lastPoll;

        RequestState() {}

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public String getUserCode() {
            return userCode;
        }

        public void setUserCode(String userCode) {
            this.userCode = userCode;
            this.authorizationUri = String.format(
                    Locale.ENGLISH,
                    "https://facebook.com/device?user_code=%1$s&qr=1",
                    userCode);
        }

        public String getRequestCode() {
            return requestCode;
        }

        public void setRequestCode(String requestCode) {
            this.requestCode = requestCode;
        }

        public long getInterval() {
            return interval;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }

        public void setLastPoll(long lastPoll) {
            this.lastPoll = lastPoll;
        }

        protected RequestState(Parcel in) {
            userCode = in.readString();
            requestCode = in.readString();
            interval = in.readLong();
            lastPoll = in.readLong();
        }

        /**
         *
         * @return True if the current time is less than last poll time + polling interval.
         */
        public boolean withinLastRefreshWindow() {
            if (lastPoll == 0) {
                return false;
            }

            long diff = new Date().getTime() - lastPoll - interval * 1000L;
            return diff < 0;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(userCode);
            dest.writeString(requestCode);
            dest.writeLong(interval);
            dest.writeLong(lastPoll);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<RequestState> CREATOR =
                new Parcelable.Creator<RequestState>() {
            @Override
            public RequestState createFromParcel(Parcel in) {
                return new RequestState(in);
            }

            @Override
            public RequestState[] newArray(int size) {
                return new RequestState[size];
            }
        };
    }
}
