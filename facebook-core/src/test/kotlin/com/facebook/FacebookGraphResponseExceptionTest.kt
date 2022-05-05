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

package com.facebook

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class FacebookGraphResponseExceptionTest : FacebookPowerMockTestCase() {
  @Ignore // TODO: Re-enable when flakiness is fixed T117960751
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
