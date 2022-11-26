/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FacebookGraphResponseExceptionTest : FacebookPowerMockTestCase() {
  @Test
  fun `test exception to string`() {
    val mockResponse = mock<GraphResponse>()
    val mockRequestError = mock<FacebookRequestError>()
    whenever(mockRequestError.requestStatusCode).thenReturn(404)
    whenever(mockRequestError.errorCode).thenReturn(1)
    whenever(mockRequestError.errorMessage).thenReturn("Not found")
    whenever(mockRequestError.errorType).thenReturn("an error type")
    whenever(mockResponse.error).thenReturn(mockRequestError)
    val errorMessage = "an error message"
    val exception = FacebookGraphResponseException(mockResponse, errorMessage)

    Assert.assertEquals(
        "{FacebookGraphResponseException: an error message httpResponseCode: 404, facebookErrorCode: 1, facebookErrorType: an error type, message: Not found}",
        exception.toString())
  }
}
