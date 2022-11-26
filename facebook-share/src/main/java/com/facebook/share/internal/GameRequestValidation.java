/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal;

import com.facebook.internal.Validate;
import com.facebook.share.model.GameRequestContent;

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * <p>Validates GameRequestContent before it is shown by GameRequestDialog
 */
public class GameRequestValidation {

  public static void validate(GameRequestContent content) {
    Validate.notNull(content.getMessage(), "message");
    if (content.getObjectId() != null
        ^ (content.getActionType() == GameRequestContent.ActionType.ASKFOR
            || content.getActionType() == GameRequestContent.ActionType.SEND)) {
      throw new IllegalArgumentException(
          "Object id should be provided if and only if action type is send or askfor");
    }

    // parameters recipients, filters, suggestions are mutually exclusive
    int mutex = 0;
    if (content.getRecipients() != null) {
      mutex++;
    }
    if (content.getSuggestions() != null) {
      mutex++;
    }
    if (content.getFilters() != null) {
      mutex++;
    }
    if (mutex > 1) {
      throw new IllegalArgumentException(
          "Parameters to, filters and suggestions are mutually exclusive");
    }
  }
}
