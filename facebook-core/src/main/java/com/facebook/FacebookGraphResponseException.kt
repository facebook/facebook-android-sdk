/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

/**
 * Represents an issue that's returned by the Graph API.
 *
 * @param graphResponse The graph response with issue.
 * @param errorMessage The error message.
 */
class FacebookGraphResponseException(val graphResponse: GraphResponse?, errorMessage: String?) :
    FacebookException(errorMessage) {
  /**
   * Getter for the graph response with the issue.
   *
   * @return the graph response with the issue.
   */
  override fun toString(): String {
    val requestError = graphResponse?.error
    val errorStringBuilder = StringBuilder().append("{FacebookGraphResponseException: ")
    val message = message
    if (message != null) {
      errorStringBuilder.append(message)
      errorStringBuilder.append(" ")
    }
    if (requestError != null) {
      errorStringBuilder
          .append("httpResponseCode: ")
          .append(requestError.requestStatusCode)
          .append(", facebookErrorCode: ")
          .append(requestError.errorCode)
          .append(", facebookErrorType: ")
          .append(requestError.errorType)
          .append(", message: ")
          .append(requestError.errorMessage)
          .append("}")
    }
    return errorStringBuilder.toString()
  }
}
