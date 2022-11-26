/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphResponse;
import com.facebook.gamingservices.cloudgaming.CloudGameLoginHandler;
import com.facebook.gamingservices.cloudgaming.DaemonRequest;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.CustomTabUtils;
import com.facebook.internal.DialogPresenter;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.FragmentWrapper;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.ServerProtocol;
import com.facebook.internal.Validate;
import com.facebook.share.internal.GameRequestValidation;
import com.facebook.share.internal.ResultProcessor;
import com.facebook.share.internal.ShareConstants;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.internal.WebDialogParameters;
import com.facebook.share.model.GameRequestContent;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides functionality to send requests in games.
 *
 * <p>See https://developers.facebook.com/docs/games/requests
 */
public class GameRequestDialog
    extends FacebookDialogBase<GameRequestContent, GameRequestDialog.Result> {

  private FacebookCallback mCallback;

  /** Helper object for handling the result from a requests dialog */
  public static final class Result {
    String requestId;
    List<String> to;

    private Result(Bundle results) {
      this.requestId = results.getString(ShareConstants.WEB_DIALOG_RESULT_PARAM_REQUEST_ID);
      this.to = new ArrayList<String>();
      while (results.containsKey(
          String.format(ShareConstants.WEB_DIALOG_RESULT_PARAM_TO_ARRAY_MEMBER, this.to.size()))) {
        this.to.add(
            results.getString(
                String.format(
                    ShareConstants.WEB_DIALOG_RESULT_PARAM_TO_ARRAY_MEMBER, this.to.size())));
      }
    }

    private Result(GraphResponse response) {
      try {
        JSONObject data = response.getJSONObject();
        JSONObject nestedData = data.optJSONObject("data");
        if (nestedData != null) {
          data = nestedData;
        }
        this.requestId = data.getString("request_id");
        this.to = new ArrayList<>();
        JSONArray recipients = data.getJSONArray("to");
        for (int i = 0; i < recipients.length(); i++) {
          this.to.add(recipients.getString(i));
        }
      } catch (JSONException e) {
        this.requestId = null;
        this.to = new ArrayList<>();
      }
    }

    /**
     * Returns the request ID.
     *
     * @return the request ID.
     */
    public String getRequestId() {
      return requestId;
    }

    /**
     * Returns request recipients.
     *
     * @return request recipients
     */
    public List<String> getRequestRecipients() {
      return to;
    }
  }

  // The actual value of the string is different since that is what the web dialog is actually
  // called on the server.
  private static final String GAME_REQUEST_DIALOG = "apprequests";

  private static final int DEFAULT_REQUEST_CODE =
      CallbackManagerImpl.RequestCodeOffset.GameRequest.toRequestCode();

  /**
   * Indicates whether the game request dialog can be shown.
   *
   * @return true if the dialog can be shown
   */
  public static boolean canShow() {
    return true;
  }

  /**
   * Shows a {@link GameRequestDialog} to send a request, using the passed in activity. No callback
   * will be invoked.
   *
   * @param activity Activity hosting the dialog.
   * @param gameRequestContent Content of the request.
   */
  public static void show(final Activity activity, final GameRequestContent gameRequestContent) {
    new GameRequestDialog(activity).show(gameRequestContent);
  }

  /**
   * Shows a {@link GameRequestDialog} to send a request, using the passed in activity. No callback
   * will be invoked.
   *
   * @param fragment androidx.fragment.app.Fragment hosting the dialog.
   * @param gameRequestContent Content of the request.
   */
  public static void show(final Fragment fragment, final GameRequestContent gameRequestContent) {
    show(new FragmentWrapper(fragment), gameRequestContent);
  }

  /**
   * Shows a {@link GameRequestDialog} to send a request, using the passed in activity. No callback
   * will be invoked.
   *
   * @param fragment android.app.Fragment hosting the dialog.
   * @param gameRequestContent Content of the request.
   */
  public static void show(
      final android.app.Fragment fragment, final GameRequestContent gameRequestContent) {
    show(new FragmentWrapper(fragment), gameRequestContent);
  }

  private static void show(
      final FragmentWrapper fragmentWrapper, final GameRequestContent gameRequestContent) {
    new GameRequestDialog(fragmentWrapper).show(gameRequestContent);
  }

  /**
   * Constructs a new RequestDialog.
   *
   * @param activity Activity hosting the dialog.
   */
  public GameRequestDialog(Activity activity) {
    super(activity, DEFAULT_REQUEST_CODE);
  }

  /**
   * Constructs a new RequestDialog.
   *
   * @param fragment androidx.fragment.app.Fragment hosting the dialog.
   */
  public GameRequestDialog(Fragment fragment) {
    this(new FragmentWrapper(fragment));
  }

  /**
   * Constructs a new RequestDialog.
   *
   * @param fragment android.app.Fragment hosting the dialog.
   */
  public GameRequestDialog(android.app.Fragment fragment) {
    this(new FragmentWrapper(fragment));
  }

  private GameRequestDialog(FragmentWrapper fragmentWrapper) {
    super(fragmentWrapper, DEFAULT_REQUEST_CODE);
  }

  @Override
  protected void registerCallbackImpl(
      final CallbackManagerImpl callbackManager, final FacebookCallback<Result> callback) {
    mCallback = callback;
    final ResultProcessor resultProcessor =
        (callback == null)
            ? null
            : new ResultProcessor(callback) {
              @Override
              public void onSuccess(AppCall appCall, Bundle results) {
                if (results != null) {
                  callback.onSuccess(new Result(results));
                } else {
                  onCancel(appCall);
                }
              }
            };

    callbackManager.registerCallback(
        getRequestCode(),
        new CallbackManagerImpl.Callback() {
          @Override
          public boolean onActivityResult(int resultCode, Intent data) {
            return ShareInternalUtility.handleActivityResult(
                getRequestCode(), resultCode, data, resultProcessor);
          }
        });
  }

  @Override
  protected AppCall createBaseAppCall() {
    return new AppCall(getRequestCode());
  }

  @Override
  protected List<ModeHandler> getOrderedModeHandlers() {
    ArrayList<ModeHandler> handlers = new ArrayList<>();
    handlers.add(new FacebookAppHandler());
    handlers.add(new ChromeCustomTabHandler());
    handlers.add(new WebHandler());

    return handlers;
  }

  @Override
  protected void showImpl(final GameRequestContent content, final Object mode) {
    if (CloudGameLoginHandler.isRunningInCloud()) {
      this.showForCloud(content, mode);
      return;
    }
    super.showImpl(content, mode);
  }

  private void showForCloud(final GameRequestContent content, final Object mode) {
    Context context = this.getActivityContext();

    AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
    if (currentAccessToken == null || currentAccessToken.isExpired()) {
      throw new FacebookException(
          "Attempted to open GameRequestDialog with an invalid access token");
    }

    final DaemonRequest.Callback requestCallback =
        new DaemonRequest.Callback() {
          @Override
          public void onCompleted(GraphResponse response) {
            if (mCallback != null) {
              if (response.getError() != null) {
                mCallback.onError(new FacebookException(response.getError().getErrorMessage()));
              } else {
                mCallback.onSuccess(new Result(response));
              }
            }
          }
        };

    String app_id = currentAccessToken.getApplicationId();
    String action_type = content.getActionType() != null ? content.getActionType().name() : null;

    JSONObject parameters = new JSONObject();
    JSONArray to = new JSONArray();
    try {
      parameters.put(SDKConstants.PARAM_APP_ID, app_id);
      parameters.put(SDKConstants.PARAM_GAME_REQUESTS_ACTION_TYPE, action_type);
      parameters.put(SDKConstants.PARAM_GAME_REQUESTS_MESSAGE, content.getMessage());
      parameters.put(SDKConstants.PARAM_GAME_REQUESTS_CTA, content.getCta());
      parameters.put(SDKConstants.PARAM_GAME_REQUESTS_TITLE, content.getTitle());
      parameters.put(SDKConstants.PARAM_GAME_REQUESTS_DATA, content.getData());
      parameters.put(SDKConstants.PARAM_GAME_REQUESTS_OPTIONS, content.getFilters());
      if (content.getRecipients() != null) {
        for (String recipient : content.getRecipients()) {
          to.put(recipient);
        }
      }
      parameters.put(SDKConstants.PARAM_GAME_REQUESTS_TO, to);

      DaemonRequest.executeAsync(
          context, parameters, requestCallback, SDKMessageEnum.OPEN_GAME_REQUESTS_DIALOG);
    } catch (JSONException e) {
      if (mCallback != null) {
        mCallback.onError(new FacebookException("Couldn't prepare Game Request Dialog"));
      }
    }
  }

  private class ChromeCustomTabHandler extends ModeHandler {
    @Override
    public boolean canShow(final GameRequestContent content, boolean isBestEffort) {
      String chromePackage = CustomTabUtils.getChromePackage();
      return chromePackage != null
          && Validate.hasCustomTabRedirectActivity(
              getActivityContext(), CustomTabUtils.getDefaultRedirectURI());
    }

    @Override
    public AppCall createAppCall(final GameRequestContent content) {
      GameRequestValidation.validate(content);
      AppCall appCall = createBaseAppCall();

      Bundle params = WebDialogParameters.create(content);

      AccessToken accessToken = AccessToken.getCurrentAccessToken();
      if (accessToken != null) {
        params.putString(ServerProtocol.DIALOG_PARAM_APP_ID, accessToken.getApplicationId());
      } else {
        String applicationId = FacebookSdk.getApplicationId();
        params.putString(ServerProtocol.DIALOG_PARAM_APP_ID, applicationId);
      }

      params.putString(
          ServerProtocol.DIALOG_PARAM_REDIRECT_URI, CustomTabUtils.getDefaultRedirectURI());

      DialogPresenter.setupAppCallForCustomTabDialog(appCall, GAME_REQUEST_DIALOG, params);

      return appCall;
    }
  }

  private class WebHandler extends ModeHandler {
    @Override
    public boolean canShow(final GameRequestContent content, boolean isBestEffort) {
      return true;
    }

    @Override
    public AppCall createAppCall(final GameRequestContent content) {
      GameRequestValidation.validate(content);
      AppCall appCall = createBaseAppCall();
      DialogPresenter.setupAppCallForWebDialog(
          appCall, GAME_REQUEST_DIALOG, WebDialogParameters.create(content));

      return appCall;
    }
  }

  private class FacebookAppHandler extends ModeHandler {
    @Override
    public boolean canShow(final GameRequestContent content, boolean isBestEffort) {
      PackageManager packageManager = getActivityContext().getPackageManager();
      Intent intent = new Intent("com.facebook.games.gaming_services.DEEPLINK");
      intent.setType("text/plain");
      boolean fbAppCanShow = intent.resolveActivity(packageManager) != null;

      AccessToken currentToken = AccessToken.getCurrentAccessToken();
      boolean isGamingLoggedIn =
          currentToken != null
              && currentToken.getGraphDomain() != null
              && FacebookSdk.GAMING.equals(currentToken.getGraphDomain());
      return fbAppCanShow && isGamingLoggedIn;
    }

    @Override
    public AppCall createAppCall(final GameRequestContent content) {
      AppCall appCall = createBaseAppCall();

      Intent intent = new Intent("com.facebook.games.gaming_services.DEEPLINK");
      intent.setType("text/plain");

      AccessToken accessToken = AccessToken.getCurrentAccessToken();
      Bundle args = new Bundle();
      args.putString("deeplink", "GAME_REQUESTS");
      if (accessToken != null) {
        args.putString("app_id", accessToken.getApplicationId());
      } else {
        args.putString("app_id", FacebookSdk.getApplicationId());
      }

      String action_type = content.getActionType() != null ? content.getActionType().name() : null;
      args.putString(SDKConstants.PARAM_GAME_REQUESTS_ACTION_TYPE, action_type);
      args.putString(SDKConstants.PARAM_GAME_REQUESTS_MESSAGE, content.getMessage());
      args.putString(SDKConstants.PARAM_GAME_REQUESTS_TITLE, content.getTitle());
      args.putString(SDKConstants.PARAM_GAME_REQUESTS_DATA, content.getData());
      args.putString(SDKConstants.PARAM_GAME_REQUESTS_CTA, content.getCta());
      List<String> recipients = content.getRecipients();
      JSONArray to = new JSONArray();
      if (content.getRecipients() != null) {
        for (String recipient : content.getRecipients()) {
          to.put(recipient);
        }
      }
      args.putString(SDKConstants.PARAM_GAME_REQUESTS_TO, to.toString());

      NativeProtocol.setupProtocolRequestIntent(
          intent, appCall.getCallId().toString(), "", NativeProtocol.getLatestKnownVersion(), args);
      appCall.setRequestIntent(intent);
      return appCall;
    }
  }
}
