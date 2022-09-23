/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/** Represents an error returned from the Facebook service in response to a request. */
class FacebookServiceException
/**
 * Constructs a new FacebookServiceException.
 *
 * @param error the error from the request
 */
(val requestError: FacebookRequestError, errorMessage: String?) : FacebookException(errorMessage) {
  /**
   * Returns an object that encapsulates complete information representing the error returned by
   * Facebook.
   *
   * @return complete information representing the error.
   */
  override fun toString(): String {
    return StringBuilder()
        .append("{FacebookServiceException: ")
        .append("httpResponseCode: ")
        .append(requestError.requestStatusCode)
        .append(", facebookErrorCode: ")
        .append(requestError.errorCode)
        .append(", facebookErrorType: ")
        .append(requestError.errorType)
        .append(", message: ")
        .append(requestError.errorMessage)
        .append("}")
        .toString()
  }

  companion object {
    private const val serialVersionUID: Long = 1
  }
}
