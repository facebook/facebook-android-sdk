/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookSdk;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.CustomTabUtils;
import com.facebook.internal.DialogPresenter;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.FragmentWrapper;
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

/**
 * Provides functionality to send requests in games.
 *
 * @deprecated This class is now deprecated,
 *     <p>Use {@link com.facebook.gamingservices.GameRequestDialog}
 *     <p>see https://developers.facebook.com/docs/games/requests
 */
@Deprecated
public class GameRequestDialog
    extends FacebookDialogBase<GameRequestContent, GameRequestDialog.Result> {

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
    handlers.add(new ChromeCustomTabHandler());
    handlers.add(new WebHandler());

    return handlers;
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
}
