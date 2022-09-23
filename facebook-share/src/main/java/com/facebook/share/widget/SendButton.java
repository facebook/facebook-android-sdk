/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.widget;

import android.content.Context;
import android.util.AttributeSet;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.share.R;

/**
 * A button to share content through Messenger. Tapping the receiver will invoke the {@link
 * com.facebook.share.widget.MessageDialog} with the attached shareContent.
 */
public final class SendButton extends ShareButtonBase {
  public SendButton(final Context context) {
    super(
        context,
        null,
        0,
        AnalyticsEvents.EVENT_SEND_BUTTON_CREATE,
        AnalyticsEvents.EVENT_SEND_BUTTON_DID_TAP);
  }

  public SendButton(final Context context, final AttributeSet attrs) {
    super(
        context,
        attrs,
        0,
        AnalyticsEvents.EVENT_SEND_BUTTON_CREATE,
        AnalyticsEvents.EVENT_SEND_BUTTON_DID_TAP);
  }

  public SendButton(final Context context, final AttributeSet attrs, final int defStyleAttr) {
    super(
        context,
        attrs,
        defStyleAttr,
        AnalyticsEvents.EVENT_SEND_BUTTON_CREATE,
        AnalyticsEvents.EVENT_SEND_BUTTON_DID_TAP);
  }

  @Override
  protected int getDefaultStyleResource() {
    return R.style.com_facebook_button_send;
  }

  @Override
  protected int getDefaultRequestCode() {
    return CallbackManagerImpl.RequestCodeOffset.Message.toRequestCode();
  }

  @Override
  protected ShareDialog getDialog() {
    final MessageDialog dialog;
    if (SendButton.this.getFragment() != null) {
      dialog = new MessageDialog(SendButton.this.getFragment(), getRequestCode());
    } else if (SendButton.this.getNativeFragment() != null) {
      dialog = new MessageDialog(SendButton.this.getNativeFragment(), getRequestCode());
    } else {
      dialog = new MessageDialog(getActivity(), getRequestCode());
    }
    dialog.setCallbackManager(getCallbackManager());
    return dialog;
  }
}
