/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook;

import static junit.framework.Assert.assertEquals;

import com.facebook.internal.FacebookRequestErrorClassification;
import org.junit.Test;

public class ErrorClassificationTest extends FacebookTestCase {

  @Test
  public void testDefaultErrorClassification() {
    FacebookRequestErrorClassification errorClassification =
        FacebookRequestErrorClassification.getDefaultErrorClassification();
    // Test transient takes precedence
    assertEquals(
        FacebookRequestError.Category.TRANSIENT,
        errorClassification.classify(FacebookRequestErrorClassification.EC_INVALID_TOKEN, 0, true));

    assertEquals(
        FacebookRequestError.Category.TRANSIENT,
        errorClassification.classify(
            FacebookRequestErrorClassification.EC_APP_NOT_INSTALLED, 0, true));

    assertEquals(
        FacebookRequestError.Category.LOGIN_RECOVERABLE,
        errorClassification.classify(
            FacebookRequestErrorClassification.EC_INVALID_SESSION, 0, false));

    assertEquals(
        FacebookRequestError.Category.LOGIN_RECOVERABLE,
        errorClassification.classify(
            FacebookRequestErrorClassification.EC_INVALID_TOKEN, 0, false));

    assertEquals(
        FacebookRequestError.Category.LOGIN_RECOVERABLE,
        errorClassification.classify(
            FacebookRequestErrorClassification.EC_APP_NOT_INSTALLED, 0, false));

    assertEquals(
        FacebookRequestError.Category.TRANSIENT,
        errorClassification.classify(
            FacebookRequestErrorClassification.EC_SERVICE_UNAVAILABLE, 0, false));

    assertEquals(
        FacebookRequestError.Category.TRANSIENT,
        errorClassification.classify(
            FacebookRequestErrorClassification.EC_APP_TOO_MANY_CALLS, 0, false));

    assertEquals(
        FacebookRequestError.Category.TRANSIENT,
        errorClassification.classify(FacebookRequestErrorClassification.EC_RATE, 0, false));

    assertEquals(
        FacebookRequestError.Category.TRANSIENT,
        errorClassification.classify(
            FacebookRequestErrorClassification.EC_USER_TOO_MANY_CALLS, 0, false));

    assertEquals(
        FacebookRequestError.Category.TRANSIENT,
        errorClassification.classify(
            FacebookRequestErrorClassification.EC_TOO_MANY_USER_ACTION_CALLS, 0, false));
  }
}
