/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.fragment.app.Fragment;
import com.facebook.FacebookCallback;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.FragmentWrapper;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.List;

@AutoHandleExceptions
public class GamingGroupIntegration
    extends FacebookDialogBase<Void, GamingGroupIntegration.Result> {

  private static final int DEFAULT_REQUEST_CODE =
      CallbackManagerImpl.RequestCodeOffset.GamingGroupIntegration.toRequestCode();
  private static final String ERROR_KEY = "error";
  /**
   * Constructs a new GamingGroupIntegration.
   *
   * @param activity Activity to use to trigger this Dialog.
   */
  public GamingGroupIntegration(final Activity activity) {
    super(activity, DEFAULT_REQUEST_CODE);
  }
  /**
   * Constructs a new GamingGroupIntegration.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  public GamingGroupIntegration(final android.app.Fragment fragment) {
    super(new FragmentWrapper(fragment), DEFAULT_REQUEST_CODE);
  }
  /**
   * Constructs a new GamingGroupIntegration.
   *
   * @param fragment fragment to use to trigger this Dialog.
   */
  public GamingGroupIntegration(final Fragment fragment) {
    super(new FragmentWrapper(fragment), DEFAULT_REQUEST_CODE);
  }

  /** Shows the GamingGroupIntegration. */
  public void show() {
    showImpl();
  }

  @Override
  public void show(final Void content) {
    showImpl();
  }

  protected void showImpl() {
    String uri = "https://fb.gg/me/community/" + FacebookSdk.getApplicationId();

    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
    startActivityForResult(intent, getRequestCode());
  }

  @Override
  protected void registerCallbackImpl(
      final CallbackManagerImpl callbackManager, final FacebookCallback callback) {
    callbackManager.registerCallback(
        getRequestCode(),
        new CallbackManagerImpl.Callback() {
          @Override
          public boolean onActivityResult(int resultCode, Intent data) {
            if (data != null && data.hasExtra(ERROR_KEY)) {
              FacebookRequestError error = data.getParcelableExtra(ERROR_KEY);
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
