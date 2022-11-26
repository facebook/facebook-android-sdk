/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.widget;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.facebook.FacebookCallback;
import com.facebook.appevents.InternalAppEventsLogger;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.DialogFeature;
import com.facebook.internal.DialogPresenter;
import com.facebook.internal.FragmentWrapper;
import com.facebook.share.Sharer;
import com.facebook.share.internal.LegacyNativeDialogParameters;
import com.facebook.share.internal.MessageDialogFeature;
import com.facebook.share.internal.NativeDialogParameters;
import com.facebook.share.internal.ShareContentValidation;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides functionality to send content via the Facebook Message Dialog
 *
 * <p>SUPPORTED SHARE TYPES - ShareLinkContent - ShareCameraEffectContent
 *
 * <p>UNSUPPORTED SHARE TYPES (DEPRECATED AUGUST 2018) - SharePhotoContent - ShareVideoContent - Any
 * other types that are not one of the four supported types listed above
 */
public final class MessageDialog extends ShareDialog implements Sharer {

  private static final int DEFAULT_REQUEST_CODE =
      CallbackManagerImpl.RequestCodeOffset.Message.toRequestCode();

  private boolean shouldFailOnDataError = false;

  /**
   * Helper to show the provided {@link com.facebook.share.model.ShareContent} using the provided
   * Activity. No callback will be invoked.
   *
   * @param activity Activity to use to send the provided content
   * @param shareContent Content to send
   */
  public static void show(final Activity activity, final ShareContent shareContent) {
    new MessageDialog(activity).show(shareContent);
  }

  /**
   * Helper to show the provided {@link com.facebook.share.model.ShareContent} using the provided
   * Fragment. No callback will be invoked.
   *
   * @param fragment androidx.fragment.app.Fragment to use to send the provided content
   * @param shareContent Content to send
   */
  public static void show(final Fragment fragment, final ShareContent shareContent) {
    show(new FragmentWrapper(fragment), shareContent);
  }

  /**
   * Helper to show the provided {@link com.facebook.share.model.ShareContent} using the provided
   * Fragment. No callback will be invoked.
   *
   * @param fragment android.app.Fragment to use to send the provided content
   * @param shareContent Content to send
   */
  public static void show(final android.app.Fragment fragment, final ShareContent shareContent) {
    show(new FragmentWrapper(fragment), shareContent);
  }

  private static void show(final FragmentWrapper fragmentWrapper, final ShareContent shareContent) {
    new MessageDialog(fragmentWrapper).show(shareContent);
  }

  /**
   * Indicates whether it is possible to show the dialog for {@link
   * com.facebook.share.model.ShareContent} of the specified type.
   *
   * @param contentType Class of the intended {@link com.facebook.share.model.ShareContent} to send.
   * @return True if the specified content type can be shown via the dialog
   */
  public static boolean canShow(Class<? extends ShareContent<?, ?>> contentType) {
    DialogFeature feature = getFeature(contentType);

    return feature != null && DialogPresenter.canPresentNativeDialogWithFeature(feature);
  }

  /**
   * Constructs a MessageDialog.
   *
   * @param activity Activity to use to send the provided content.
   */
  public MessageDialog(Activity activity) {
    super(activity, DEFAULT_REQUEST_CODE);

    ShareInternalUtility.registerStaticShareCallback(DEFAULT_REQUEST_CODE);
  }

  /**
   * Constructs a MessageDialog.
   *
   * @param fragment androidx.fragment.app.Fragment to use to send the provided content.
   */
  public MessageDialog(Fragment fragment) {
    this(new FragmentWrapper(fragment));
  }

  /**
   * Constructs a MessageDialog.
   *
   * @param fragment android.app.Fragment to use to send the provided content.
   */
  public MessageDialog(android.app.Fragment fragment) {
    this(new FragmentWrapper(fragment));
  }

  private MessageDialog(FragmentWrapper fragmentWrapper) {
    super(fragmentWrapper, DEFAULT_REQUEST_CODE);

    ShareInternalUtility.registerStaticShareCallback(DEFAULT_REQUEST_CODE);
  }

  // for SendButton use only
  MessageDialog(Activity activity, int requestCode) {
    super(activity, requestCode);

    ShareInternalUtility.registerStaticShareCallback(requestCode);
  }

  // for SendButton use only
  MessageDialog(Fragment fragment, int requestCode) {
    this(new FragmentWrapper(fragment), requestCode);
  }

  MessageDialog(android.app.Fragment fragment, int requestCode) {
    this(new FragmentWrapper(fragment), requestCode);
  }

  private MessageDialog(FragmentWrapper fragmentWrapper, int requestCode) {
    super(fragmentWrapper, requestCode);

    ShareInternalUtility.registerStaticShareCallback(requestCode);
  }

  @Override
  protected void registerCallbackImpl(
      final CallbackManagerImpl callbackManager, final FacebookCallback<Result> callback) {
    ShareInternalUtility.registerSharerCallback(getRequestCode(), callbackManager, callback);
  }

  @Override
  public boolean getShouldFailOnDataError() {
    return this.shouldFailOnDataError;
  }

  @Override
  public void setShouldFailOnDataError(boolean shouldFailOnDataError) {
    this.shouldFailOnDataError = shouldFailOnDataError;
  }

  @Override
  protected AppCall createBaseAppCall() {
    return new AppCall(getRequestCode());
  }

  @Override
  protected List<ModeHandler> getOrderedModeHandlers() {
    ArrayList<ModeHandler> handlers = new ArrayList<>();
    handlers.add(new NativeHandler());

    return handlers;
  }

  private class NativeHandler extends ModeHandler {
    @Override
    public boolean canShow(final ShareContent shareContent, boolean isBestEffort) {
      return shareContent != null
          && MessageDialog.canShow((Class<? extends ShareContent<?, ?>>) shareContent.getClass());
    }

    @Override
    public AppCall createAppCall(final ShareContent content) {

      ShareContentValidation.validateForMessage(content);

      final AppCall appCall = createBaseAppCall();
      final boolean shouldFailOnDataError = getShouldFailOnDataError();

      logDialogShare(getActivityContext(), content, appCall);

      DialogPresenter.setupAppCallForNativeDialog(
          appCall,
          new DialogPresenter.ParameterProvider() {
            @Override
            public Bundle getParameters() {
              return NativeDialogParameters.create(
                  appCall.getCallId(), content, shouldFailOnDataError);
            }

            @Override
            public Bundle getLegacyParameters() {
              return LegacyNativeDialogParameters.create(
                  appCall.getCallId(), content, shouldFailOnDataError);
            }
          },
          getFeature(content.getClass()));

      return appCall;
    }
  }

  private static DialogFeature getFeature(Class<? extends ShareContent> type) {
    if (ShareLinkContent.class.isAssignableFrom(type)) {
      return MessageDialogFeature.MESSAGE_DIALOG;
    }
    return null;
  }

  private static void logDialogShare(Context context, ShareContent content, AppCall appCall) {
    String contentType;
    DialogFeature dialogFeature = getFeature(content.getClass());
    if (dialogFeature == MessageDialogFeature.MESSAGE_DIALOG) {
      contentType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_STATUS;
    } else if (dialogFeature == MessageDialogFeature.MESSENGER_GENERIC_TEMPLATE) {
      contentType = AnalyticsEvents.PARAMETER_SHARE_MESSENGER_GENERIC_TEMPLATE;
    } else if (dialogFeature == MessageDialogFeature.MESSENGER_MEDIA_TEMPLATE) {
      contentType = AnalyticsEvents.PARAMETER_SHARE_MESSENGER_MEDIA_TEMPLATE;
    } else {
      contentType = AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_UNKNOWN;
    }

    InternalAppEventsLogger logger = new InternalAppEventsLogger(context);
    Bundle parameters = new Bundle();
    parameters.putString(AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_TYPE, contentType);
    parameters.putString(
        AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_UUID, appCall.getCallId().toString());
    parameters.putString(
        AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_PAGE_ID, content.getPageId());

    logger.logEventImplicitly(AnalyticsEvents.EVENT_SHARE_MESSENGER_DIALOG_SHOW, parameters);
  }
}
