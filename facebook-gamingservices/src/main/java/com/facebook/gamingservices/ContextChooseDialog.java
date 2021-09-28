/*
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

package com.facebook.gamingservices;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
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
import com.facebook.gamingservices.model.ContextChooseContent;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.FragmentWrapper;
import com.facebook.internal.NativeProtocol;
import com.facebook.share.internal.ResultProcessor;
import com.facebook.share.internal.ShareInternalUtility;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContextChooseDialog
    extends FacebookDialogBase<ContextChooseContent, ContextChooseDialog.Result> {

  private static final int DEFAULT_REQUEST_CODE =
      CallbackManagerImpl.RequestCodeOffset.GamingContextChoose.toRequestCode();
  private @Nullable FacebookCallback mCallback;

  /**
   * Constructs a new ContextChooseDialog.
   *
   * @param activity Activity to use to trigger this Dialog.
   */
  public ContextChooseDialog(final Activity activity) {
    super(activity, DEFAULT_REQUEST_CODE);
  }

  /**
   * Constructs a new ContextChooseDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  public ContextChooseDialog(final Fragment fragment) {
    this(new FragmentWrapper(fragment));
  }

  /**
   * Constructs a new ContextChooseDialog.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  public ContextChooseDialog(final android.app.Fragment fragment) {
    this(new FragmentWrapper(fragment));
  }

  private ContextChooseDialog(FragmentWrapper fragmentWrapper) {
    super(fragmentWrapper, DEFAULT_REQUEST_CODE);
  }

  /**
   * Indicates whether the context choose dialog can be shown.
   *
   * @return true if the dialog can be shown
   */
  @Override
  public boolean canShow(ContextChooseContent content) {
    if (CloudGameLoginHandler.isRunningInCloud()) {
      return true;
    } else {
      return new FacebookAppHandler().canShow(content, true);
    }
  }

  @Override
  protected void showImpl(final ContextChooseContent content, final Object mode) {
    if (CloudGameLoginHandler.isRunningInCloud()) {
      this.showForCloud(content, mode);
      return;
    }
    super.showImpl(content, mode);
  }

  private void showForCloud(final ContextChooseContent content, final Object mode) {
    Context context = this.getActivityContext();

    AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
    if (currentAccessToken == null || currentAccessToken.isExpired()) {
      throw new FacebookException(
          "Attempted to open ContextChooseContent with an invalid access token");
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

    JSONObject parameters = new JSONObject();
    try {
      parameters.put(SDKConstants.PARAM_CONTEXT_FILTERS, content.getFilters());
      parameters.put(SDKConstants.PARAM_CONTEXT_MIN_SIZE, content.getMinSize());

      List<String> filters = content.getFilters();
      if (filters != null && !filters.isEmpty()) {
        JSONArray filtersArray = new JSONArray();
        for (int i = 0; i < filters.size(); i++) {
          filtersArray.put(filters.get(i));
        }
        parameters.put(SDKConstants.PARAM_CONTEXT_FILTERS, filtersArray);
      }

      DaemonRequest.executeAsync(
          context, parameters, requestCallback, SDKMessageEnum.CONTEXT_CHOOSE);
    } catch (JSONException e) {
      if (mCallback != null) {
        mCallback.onError(new FacebookException("Couldn't prepare Context Choose Dialog"));
      }
    }
  }

  @Override
  protected void registerCallbackImpl(
      final CallbackManagerImpl callbackManager, final FacebookCallback callback) {
    mCallback = callback;
    final ResultProcessor resultProcessor =
        (callback == null)
            ? null
            : new ResultProcessor(callback) {
              @Override
              public void onSuccess(AppCall appCall, Bundle results) {
                if (results != null) {
                  if (results.getString("error_message") != null) {
                    callback.onError(new FacebookException(results.getString("error_message")));
                    return;
                  }
                  if (results.getString(SDKConstants.PARAM_CONTEXT_ID) != null) {
                    GamingContext.setCurrentGamingContext(
                        new GamingContext(results.getString(SDKConstants.PARAM_CONTEXT_ID)));
                    callback.onSuccess(new Result(results));
                  }
                  callback.onError(
                      new FacebookException(
                          results.getString("Invalid response received from server.")));
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
  protected List<ModeHandler> getOrderedModeHandlers() {
    ArrayList<ModeHandler> handlers = new ArrayList<>();
    handlers.add(new FacebookAppHandler());

    return handlers;
  }

  @Override
  protected AppCall createBaseAppCall() {
    return new AppCall(getRequestCode());
  }

  /*
   * Describes the result of a Context Choose Dialog.=
   */
  public static final class Result {
    @Nullable String contextID;

    private Result(Bundle results) {
      this.contextID = results.getString(SDKConstants.PARAM_CONTEXT_ID);
    }

    private Result(GraphResponse response) {
      try {
        JSONObject data = response.getJSONObject();
        if (data == null) {
          this.contextID = null;
          return;
        }
        JSONObject nestedData = data.optJSONObject("data");
        this.contextID = nestedData != null ? nestedData.getString("id") : null;
      } catch (JSONException e) {
        this.contextID = null;
      }
    }

    /**
     * Returns the context ID.
     *
     * @return the context ID.
     */
    public @Nullable String getContextID() {
      return contextID;
    }
  }

  private class FacebookAppHandler extends ModeHandler {
    @Override
    public boolean canShow(final ContextChooseContent content, boolean isBestEffort) {
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
    public AppCall createAppCall(final ContextChooseContent content) {
      AppCall appCall = createBaseAppCall();

      Intent intent = new Intent("com.facebook.games.gaming_services.DEEPLINK");
      intent.setType("text/plain");

      AccessToken accessToken = AccessToken.getCurrentAccessToken();
      Bundle args = new Bundle();
      args.putString("deeplink", "CONTEXT_CHOOSE");
      if (accessToken != null) {
        args.putString("game_id", accessToken.getApplicationId());
      } else {
        args.putString("game_id", FacebookSdk.getApplicationId());
      }

      // TODO: Pass along context key
      if (content.getMinSize() != null) {
        args.putString("min_thread_size", String.valueOf(content.getMinSize()));
      }
      if (content.getMaxSize() != null) {
        args.putString("max_thread_size", String.valueOf(content.getMaxSize()));
      }
      if (content.getFilters() != null) {
        JSONArray jsonList = new JSONArray(content.getFilters());
        args.putString("filters", jsonList.toString());
      }

      NativeProtocol.setupProtocolRequestIntent(
          intent, appCall.getCallId().toString(), "", NativeProtocol.getLatestKnownVersion(), args);
      appCall.setRequestIntent(intent);
      return appCall;
    }
  }
}
