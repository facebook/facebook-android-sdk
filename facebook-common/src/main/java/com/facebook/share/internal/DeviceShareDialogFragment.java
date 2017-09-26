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

package com.facebook.share.internal;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.common.R;
import com.facebook.devicerequests.internal.DeviceRequestsHelper;
import com.facebook.internal.Validate;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphContent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class DeviceShareDialogFragment extends DialogFragment {
    public static final String TAG = "DeviceShareDialogFragment";
    private static final String DEVICE_SHARE_ENDPOINT = "device/share";
    private static final String REQUEST_STATE_KEY = "request_state";
    private ProgressBar progressBar;
    private TextView confirmationCode;
    private Dialog dialog;
    private volatile RequestState currentRequestState;
    private volatile ScheduledFuture codeExpiredFuture;
    private static ScheduledThreadPoolExecutor backgroundExecutor;
    private ShareContent shareContent;

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
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
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.com_facebook_device_auth_dialog_fragment, null);
        progressBar = (ProgressBar)view.findViewById(R.id.progress_bar);
        confirmationCode = (TextView)view.findViewById(R.id.confirmation_code);

        Button cancelButton = (Button) view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        TextView instructions = (TextView)view.findViewById(
                R.id.com_facebook_device_auth_instructions);
        instructions.setText(
                Html.fromHtml(getString(R.string.com_facebook_device_auth_instructions)));

        dialog.setContentView(view);

        this.startShare();
        return dialog;
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (codeExpiredFuture != null) {
            codeExpiredFuture.cancel(true);
        }
        Intent resultIntent = new Intent();
        finishActivity(Activity.RESULT_OK, resultIntent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentRequestState != null) {
            outState.putParcelable(REQUEST_STATE_KEY, currentRequestState);
        }
    }

    private void finishActivity(int resultCode, Intent data) {
        DeviceRequestsHelper.cleanUpAdvertisementService(currentRequestState.getUserCode());

        if (isAdded()) {
            Activity activity = getActivity();
            activity.setResult(resultCode, data);
            activity.finish();
        }
    }

    private void detach() {
        if (isAdded()) {
            this.getFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    public void setShareContent(ShareContent shareContent) {
        this.shareContent = shareContent;
    }

    private Bundle getGraphParametersForShareContent() {
        ShareContent content = this.shareContent;
        if (content == null) {
            return null;
        }
        if (content instanceof ShareLinkContent) {
            return WebDialogParameters.create((ShareLinkContent)content);
        } else if (content instanceof ShareOpenGraphContent) {
            return WebDialogParameters.create((ShareOpenGraphContent)content);
        }
        return null;
    }

    private void startShare() {
        Bundle parameters = getGraphParametersForShareContent();
        if (parameters == null || parameters.size() == 0) {
            this.finishActivityWithError(
                    new FacebookRequestError(0, "", "Failed to get share content"));
        }

        String accessToken = Validate.hasAppID()+ "|" + Validate.hasClientToken();
        parameters.putString(GraphRequest.ACCESS_TOKEN_PARAM, accessToken);
        parameters.putString(DeviceRequestsHelper.DEVICE_INFO_PARAM,
                             DeviceRequestsHelper.getDeviceInfo());

        GraphRequest graphRequest = new GraphRequest(
                null,
                DEVICE_SHARE_ENDPOINT,
                parameters,
                HttpMethod.POST,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        FacebookRequestError error = response.getError();
                        if (error != null) {
                            finishActivityWithError(error);
                            return;
                        }

                        JSONObject jsonObject = response.getJSONObject();
                        RequestState requestState = new RequestState();
                        try {
                            requestState.setUserCode(jsonObject.getString("user_code"));
                            requestState.setExpiresIn(jsonObject.getLong("expires_in"));
                        } catch (JSONException ex) {
                            finishActivityWithError(
                                    new FacebookRequestError(0, "", "Malformed server response"));
                            return;
                        }

                        setCurrentRequestState(requestState);
                    }
                });
        graphRequest.executeAsync();
    }

    private void finishActivityWithError(FacebookRequestError error) {
        // detach so that we don't send a cancellation message back ondismiss.
        detach();
        Intent intent = new Intent();
        intent.putExtra("error", error);
        finishActivity(Activity.RESULT_OK, intent);
    }

    private static synchronized ScheduledThreadPoolExecutor getBackgroundExecutor() {
        if (backgroundExecutor == null) {
            backgroundExecutor = new ScheduledThreadPoolExecutor(1);
        }
        return backgroundExecutor;
    }

    private void setCurrentRequestState(RequestState currentRequestState) {
        this.currentRequestState = currentRequestState;
        confirmationCode.setText(currentRequestState.getUserCode());
        confirmationCode.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        codeExpiredFuture = getBackgroundExecutor().schedule(
                new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                },
                currentRequestState.getExpiresIn(),
                TimeUnit.SECONDS);
    }

    private static class RequestState implements Parcelable {
        private String userCode;
        private long expiresIn;

        RequestState() {}

        public String getUserCode() {
            return userCode;
        }

        public void setUserCode(String userCode) {
            this.userCode = userCode;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
        }


        protected RequestState(Parcel in) {
            userCode = in.readString();
            expiresIn = in.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(userCode);
            dest.writeLong(expiresIn);
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
