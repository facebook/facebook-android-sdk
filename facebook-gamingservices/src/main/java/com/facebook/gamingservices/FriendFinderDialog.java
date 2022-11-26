/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.GraphResponse;
import com.facebook.gamingservices.cloudgaming.CloudGameLoginHandler;
import com.facebook.gamingservices.cloudgaming.DaemonRequest;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.FragmentWrapper;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

@AutoHandleExceptions
public class FriendFinderDialog extends FacebookDialogBase<Void, FriendFinderDialog.Result> {

  private static final int DEFAULT_REQUEST_CODE =
      CallbackManagerImpl.RequestCodeOffset.GamingFriendFinder.toRequestCode();
  private FacebookCallback mCallback;
  /**
   * Constructs a new FriendFinderDialog.
   *
   * @param activity Activity to use to trigger this Dialog.
   */
  public FriendFinderDialog(final Activity activity) {
    super(activity, DEFAULT_REQUEST_CODE);
  }
  /**
   * Constructs a new FriendFinderDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  public FriendFinderDialog(final Fragment fragment) {
    super(new FragmentWrapper(fragment), DEFAULT_REQUEST_CODE);
  }
  /**
   * Constructs a new FriendFinderDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  public FriendFinderDialog(final androidx.fragment.app.Fragment fragment) {
    super(new FragmentWrapper(fragment), DEFAULT_REQUEST_CODE);
  }

  /** Shows the FriendFinderDialog. */
  public void show() {
    showImpl();
  }

  @Override
  public void show(final Void content) {
    showImpl();
  }

  protected void showImpl() {
    AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
    if (currentAccessToken == null || currentAccessToken.isExpired()) {
      throw new FacebookException(
          "Attempted to open GamingServices FriendFinder" + " with an invalid access token");
    }

    String app_id = currentAccessToken.getApplicationId();
    boolean isRunningInCloud = CloudGameLoginHandler.isRunningInCloud();

    if (isRunningInCloud) {
      // When running on FB's servers we will just send a message to request the UI to show
      // on top of the game.
      Context context = this.getActivityContext();
      final DaemonRequest.Callback requestCallback =
          new DaemonRequest.Callback() {
            public void onCompleted(GraphResponse response) {
              if (mCallback != null) {
                if (response.getError() != null) {
                  mCallback.onError(new FacebookException(response.getError().getErrorMessage()));
                } else {
                  mCallback.onSuccess(new Result());
                }
              }
            }
          };

      JSONObject parameters = new JSONObject();
      try {
        parameters.put(SDKConstants.PARAM_DEEP_LINK_ID, app_id);
        parameters.put(SDKConstants.PARAM_DEEP_LINK, "FRIEND_FINDER");
        DaemonRequest.executeAsync(
            context, parameters, requestCallback, SDKMessageEnum.OPEN_GAMING_SERVICES_DEEP_LINK);
      } catch (JSONException e) {
        if (mCallback != null) {
          mCallback.onError(new FacebookException("Couldn't prepare Friend Finder Dialog"));
        }
      }
      return;
    }

    String dialog_uri = "https://fb.gg/me/friendfinder/" + app_id;

    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dialog_uri));
    startActivityForResult(intent, getRequestCode());
  }

  @Override
  protected void registerCallbackImpl(
      final CallbackManagerImpl callbackManager, final FacebookCallback callback) {
    mCallback = callback;
    callbackManager.registerCallback(
        getRequestCode(),
        new CallbackManagerImpl.Callback() {
          @Override
          public boolean onActivityResult(int resultCode, Intent data) {
            if (data != null && data.hasExtra("error")) {
              FacebookRequestError error = data.getParcelableExtra("error");
              callback.onError(error.getException());
              return true;
            }
            callback.onSuccess(new Result());
            return true;
          }
        });
  }

  @Override
  protected List<ModeHandler> getOrderedModeHandlers() {
    return null;
  }

  @Override
  protected AppCall createBaseAppCall() {
    return null;
  }

  /*
   * Describes the result of a Friend Finder Dialog.
   * This class is intentionally empty.
   */
  public static class Result {}
}
