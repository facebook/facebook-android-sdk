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
import androidx.appcompat.content.res.AppCompatResources;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.share.R;

/**
 * A button to share content on Facebook. Tapping the receiver will invoke the {@link
 * com.facebook.share.widget.ShareDialog} with the attached shareContent.
 */
public final class ShareButton extends ShareButtonBase {

  public ShareButton(final Context context) {
    super(
        context,
        null,
        0,
        AnalyticsEvents.EVENT_SHARE_BUTTON_CREATE,
        AnalyticsEvents.EVENT_SHARE_BUTTON_DID_TAP);
  }

  public ShareButton(final Context context, final AttributeSet attrs) {
    super(
        context,
        attrs,
        0,
        AnalyticsEvents.EVENT_SHARE_BUTTON_CREATE,
        AnalyticsEvents.EVENT_SHARE_BUTTON_DID_TAP);
  }

  public ShareButton(final Context context, final AttributeSet attrs, final int defStyleAttr) {
    super(
        context,
        attrs,
        defStyleAttr,
        AnalyticsEvents.EVENT_SHARE_BUTTON_CREATE,
        AnalyticsEvents.EVENT_SHARE_BUTTON_DID_TAP);
  }

  @Override
  protected int getDefaultStyleResource() {
    return R.style.com_facebook_button_share;
  }

  @Override
  protected int getDefaultRequestCode() {
    return CallbackManagerImpl.RequestCodeOffset.Share.toRequestCode();
  }

  @Override
  protected ShareDialog getDialog() {
    final ShareDialog dialog;
    if (ShareButton.this.getFragment() != null) {
      dialog = new ShareDialog(ShareButton.this.getFragment(), getRequestCode());
    } else if (ShareButton.this.getNativeFragment() != null) {
      dialog = new ShareDialog(ShareButton.this.getNativeFragment(), getRequestCode());
    } else {
      dialog = new ShareDialog(getActivity(), getRequestCode());
    }
    dialog.setCallbackManager(getCallbackManager());
    return dialog;
  }

  @Override
  protected void configureButton(
      final Context context,
      final AttributeSet attrs,
      final int defStyleAttr,
      final int defStyleRes) {
    super.configureButton(context, attrs, defStyleAttr, defStyleRes);
    this.setCompoundDrawablesWithIntrinsicBounds(
        AppCompatResources.getDrawable(
            getContext(), com.facebook.common.R.drawable.com_facebook_button_icon),
        null,
        null,
        null);
  }
}
