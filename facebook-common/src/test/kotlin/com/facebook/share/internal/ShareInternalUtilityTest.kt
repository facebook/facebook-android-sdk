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

package com.facebook.share.internal

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookOperationCanceledException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookRequestError
import com.facebook.FacebookSdk
import com.facebook.GraphResponse
import com.facebook.HttpMethod
import com.facebook.internal.AppCall
import com.facebook.internal.CallbackManagerImpl
import com.facebook.internal.NativeProtocol
import com.facebook.internal.Utility.jsonArrayToStringList
import com.facebook.share.Sharer
import com.facebook.share.model.CameraEffectTextures
import com.facebook.share.model.ShareCameraEffectContent
import com.facebook.share.model.ShareMediaContent
import com.facebook.share.model.ShareOpenGraphAction
import com.facebook.share.model.ShareOpenGraphContent
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareStoryContent
import com.facebook.share.model.ShareVideo
import com.facebook.share.model.ShareVideoContent
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.io.File
import java.lang.Exception
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, NativeProtocol::class)
class ShareInternalUtilityTest : FacebookPowerMockTestCase() {
  private lateinit var mockCallback: FacebookCallback<Sharer.Result>
  private lateinit var mockGraphResponse: GraphResponse
  private lateinit var appCall: AppCall

  override fun setup() {
    super.setup()
    mockCallback = mock()
    mockGraphResponse = mock()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(NativeProtocol::class.java)
    // clean pending call
    AppCall.finishPendingCall(UUID.randomUUID(), 0)
    appCall = AppCall(TEST_REQUEST_CODE)
  }

  @Test
  fun testRemoveNamespaceFromNullOGJsonObject() {
    assertThat(ShareInternalUtility.removeNamespacesFromOGJsonObject(null, false)).isNull()
  }

  @Test
  fun testRemoveNamespaceFromComplexOGJsonObject() {
    try {
      var testObject = jsonOGActionTestObject
      testObject = ShareInternalUtility.removeNamespacesFromOGJsonObject(testObject, false)
      val expectedResult = jsonOGActionTestObjectWithoutNamespace
      assertThat(simpleJsonObjComparer(testObject, expectedResult)).isTrue
    } catch (ex: JSONException) {
      // Fail
      assertThat(ex).isNotNull
    }
  }

  @Test
  fun testJsonSerializationOfOpenGraph() {
    val placeId = "1"
    val content =
        ShareOpenGraphContent.Builder()
            .setAction(
                ShareOpenGraphAction.Builder()
                    .putStringArrayList("tags", arrayListOf("2", "4"))
                    .build())
            .setPeopleIds(arrayListOf("1", "1", "2", "3"))
            .setPlaceId(placeId)
            .build()
    try {
      val objectForCall = ShareInternalUtility.toJSONObjectForCall(null, content)
      val peopleIds = jsonArrayToStringList(objectForCall.getJSONArray("tags"))
      assertThat(peopleIds.size.toLong()).isEqualTo(4)
      for (i in 1..4) {
        assertThat(peopleIds).contains(Integer.valueOf(i).toString())
      }
      assertThat(placeId).isEqualTo(objectForCall.getString("place"))
    } catch (ex: JSONException) {
      // Fail
      assertThat(ex).isNotNull
      return
    }
  }

  @Test
  fun testJsonSerializationOfOpenGraphExistingPlace() {
    val content =
        ShareOpenGraphContent.Builder()
            .setAction(ShareOpenGraphAction.Builder().putString("place", "1").build())
            .setPlaceId("2")
            .build()
    try {
      val objectForCall = ShareInternalUtility.toJSONObjectForCall(null, content)
      assertThat(objectForCall.getString("place")).isEqualTo("1")
    } catch (ex: JSONException) {
      // Fail
      assertThat(ex).isNotNull
      return
    }
  }

  @Test
  fun `test invokeCallbackWithException on receiving FacebookException`() {
    val exception = FacebookException("test message")
    ShareInternalUtility.invokeCallbackWithException(mockCallback, exception)
    verify(mockCallback).onError(exception)
  }

  @Test
  fun `test invokeCallbackWithException on receiving normal exception`() {
    val exception = Exception("test message")
    val exceptionCaptor = argumentCaptor<FacebookException>()
    ShareInternalUtility.invokeCallbackWithException(mockCallback, exception)
    verify(mockCallback).onError(exceptionCaptor.capture())
    assertThat(exceptionCaptor.firstValue.message).contains(exception.message)
  }

  @Test
  fun `test invokeCallbackWithException with null callback`() {
    val exception = Exception("test message")
    ShareInternalUtility.invokeCallbackWithException(null, exception)
  }

  @Test
  fun `test invokeCallbackWithError`() {
    val errorMessage = "test message"
    val exceptionCaptor = argumentCaptor<FacebookException>()
    ShareInternalUtility.invokeCallbackWithError(mockCallback, errorMessage)
    verify(mockCallback).onError(exceptionCaptor.capture())
    assertThat(exceptionCaptor.firstValue.message).contains(errorMessage)
  }

  @Test
  fun `test invokeCallbackWithError with null callback`() {
    val errorMessage = "test message"
    ShareInternalUtility.invokeCallbackWithError(null, errorMessage)
  }

  @Test
  fun `test invokeCallbackWithResults on receiving success result`() {
    val testPostId = "12345"
    whenever(mockGraphResponse.error).thenReturn(null)
    val resultCaptor = argumentCaptor<Sharer.Result>()
    ShareInternalUtility.invokeCallbackWithResults(mockCallback, testPostId, mockGraphResponse)
    verify(mockCallback).onSuccess(resultCaptor.capture())
    assertThat(resultCaptor.firstValue.postId).isEqualTo(testPostId)
  }

  @Test
  fun `test invokeCallbackWithResults on receiving error response`() {
    val error = FacebookRequestError(400, "test error type", "test error")
    whenever(mockGraphResponse.error).thenReturn(error)
    val errorCaptor = argumentCaptor<FacebookException>()
    ShareInternalUtility.invokeCallbackWithResults(mockCallback, null, mockGraphResponse)
    verify(mockCallback).onError(errorCaptor.capture())
    assertThat(errorCaptor.firstValue.message).contains(error.errorMessage)
  }

  @Test
  fun `test invokeCallbackWithResults will generate non-empty error message on receiving unknown error`() {
    val error = mock<FacebookRequestError>()
    whenever(mockGraphResponse.error).thenReturn(error)
    val errorCaptor = argumentCaptor<FacebookException>()
    ShareInternalUtility.invokeCallbackWithResults(mockCallback, null, mockGraphResponse)
    verify(mockCallback).onError(errorCaptor.capture())
    assertThat(errorCaptor.firstValue.message).isNotEmpty
  }

  @Test
  fun `test getting a native dialog completion gesture`() {
    val result = Bundle()
    val gesture = "post"
    result.putString(NativeProtocol.EXTRA_DIALOG_COMPLETION_GESTURE_KEY, gesture)
    assertThat(ShareInternalUtility.getNativeDialogCompletionGesture(result)).isEqualTo(gesture)
  }

  @Test
  fun `test getting the native dialog completion gesture in result args`() {
    val result = Bundle()
    val gesture = "post"
    result.putString(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY, gesture)
    assertThat(ShareInternalUtility.getNativeDialogCompletionGesture(result)).isEqualTo(gesture)
  }

  @Test
  fun `test getting a post id`() {
    val result = Bundle()
    val postId = "12345"
    result.putString(ShareConstants.RESULT_POST_ID, postId)
    assertThat(ShareInternalUtility.getShareDialogPostId(result)).isEqualTo(postId)
  }

  @Test
  fun `test handleActivityResult with success result`() {
    // prepare appCall and make it pending
    val mockIntent = mock<Intent>()
    val successResult = Bundle()
    whenever(NativeProtocol.getCallIdFromIntent(mockIntent)).thenReturn(appCall.callId)
    whenever(NativeProtocol.getSuccessResultsFromIntent(mockIntent)).thenReturn(successResult)
    appCall.setPending()

    val mockResultProcessor = mock<ResultProcessor>()
    val result =
        ShareInternalUtility.handleActivityResult(
            TEST_REQUEST_CODE, 0, mockIntent, mockResultProcessor)
    assertThat(result).isTrue
    verify(mockResultProcessor).onSuccess(appCall, successResult)
  }

  @Test
  fun `test handleActivityResult with cancel result`() {
    val mockIntent = mock<Intent>()
    val cancelException = FacebookOperationCanceledException()
    whenever(NativeProtocol.getCallIdFromIntent(mockIntent)).thenReturn(appCall.callId)
    whenever(NativeProtocol.getExceptionFromErrorData(anyOrNull())).thenReturn(cancelException)
    appCall.setPending()

    val mockResultProcessor = mock<ResultProcessor>()
    val result =
        ShareInternalUtility.handleActivityResult(
            TEST_REQUEST_CODE, 0, mockIntent, mockResultProcessor)
    assertThat(result).isTrue
    verify(mockResultProcessor).onCancel(appCall)
  }

  @Test
  fun `test handleActivityResult with error result`() {
    val mockIntent = mock<Intent>()
    whenever(NativeProtocol.getCallIdFromIntent(mockIntent)).thenReturn(appCall.callId)
    val exception = FacebookException("unknown")
    whenever(NativeProtocol.getExceptionFromErrorData(anyOrNull())).thenReturn(exception)
    appCall.setPending()

    val mockResultProcessor = mock<ResultProcessor>()
    val result =
        ShareInternalUtility.handleActivityResult(
            TEST_REQUEST_CODE, 0, mockIntent, mockResultProcessor)
    assertThat(result).isTrue
    verify(mockResultProcessor).onError(appCall, exception)
  }

  @Test
  fun `test handleActivityResult without pending app call`() {
    val mockIntent = mock<Intent>()
    val mockResultProcessor = mock<ResultProcessor>()
    val result =
        ShareInternalUtility.handleActivityResult(
            TEST_REQUEST_CODE, 0, mockIntent, mockResultProcessor)
    assertThat(result).isFalse
  }

  @Test
  fun `test sharing result processor with success result`() {
    val resultProcessor = ShareInternalUtility.getShareResultProcessor(mockCallback)
    val result = Bundle()
    val postId = "12345"
    result.putString(ShareConstants.RESULT_POST_ID, postId)
    result.putString(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY, "Post")
    resultProcessor.onSuccess(appCall, result)
    val resultCaptor = argumentCaptor<Sharer.Result>()
    verify(mockCallback).onSuccess(resultCaptor.capture())
    assertThat(resultCaptor.firstValue.postId).isEqualTo(postId)
  }

  @Test
  fun `test sharing result processor with cancel result`() {
    val resultProcessor = ShareInternalUtility.getShareResultProcessor(mockCallback)
    val result = Bundle()
    result.putString(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY, "Cancel")
    resultProcessor.onSuccess(appCall, result)
    verify(mockCallback).onCancel()
  }

  @Test
  fun `test sharing result processor with error`() {
    val resultProcessor = ShareInternalUtility.getShareResultProcessor(mockCallback)
    val result = Bundle()
    result.putString(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY, "Crash")
    resultProcessor.onSuccess(appCall, result)
    verify(mockCallback).onError(any())
  }

  @Test
  fun `test sharing result processor with empty bundle`() {
    val resultProcessor = ShareInternalUtility.getShareResultProcessor(mockCallback)
    val result = Bundle()
    resultProcessor.onSuccess(appCall, result)
    verify(mockCallback).onError(any())
  }

  @Test
  fun `test registering callback on a callback manager`() {
    val mockCallbackManager = mock<CallbackManagerImpl>()
    val mockReturnIntern = mock<Intent>()
    // prepare the result bundle
    val successResultBundle = Bundle()
    val postId = "12345"
    successResultBundle.putString(ShareConstants.RESULT_POST_ID, postId)
    successResultBundle.putString(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY, "Post")
    whenever(NativeProtocol.getCallIdFromIntent(mockReturnIntern)).thenReturn(appCall.callId)
    whenever(NativeProtocol.getSuccessResultsFromIntent(mockReturnIntern))
        .thenReturn(successResultBundle)
    appCall.setPending()

    ShareInternalUtility.registerSharerCallback(
        TEST_REQUEST_CODE, mockCallbackManager, mockCallback)

    val callbackCaptor = argumentCaptor<CallbackManagerImpl.Callback>()
    verify(mockCallbackManager).registerCallback(eq(TEST_REQUEST_CODE), callbackCaptor.capture())
    callbackCaptor.firstValue.onActivityResult(0, mockReturnIntern)
    val successResultCaptor = argumentCaptor<Sharer.Result>()
    verify(mockCallback).onSuccess(successResultCaptor.capture())
    assertThat(successResultCaptor.firstValue.postId).isEqualTo("12345")
  }

  @Test
  fun `test getting the photo urls from a photo content`() {
    val testPhotoImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
    val photoContent =
        SharePhotoContent.Builder()
            .addPhoto(SharePhoto.Builder().setBitmap(testPhotoImage).build())
            .build()
    val returnList = ShareInternalUtility.getPhotoUrls(photoContent, appCall.callId)
    assertThat(returnList.size).isEqualTo(1)
  }

  @Test
  fun `test getting a video url`() {
    val tempFile = File(UUID.randomUUID().toString())
    tempFile.createNewFile()
    tempFile.deleteOnExit()
    val shareVideo = ShareVideo.Builder().setLocalUrl(Uri.fromFile(tempFile)).build()
    val shareVideoContent = ShareVideoContent.Builder().setVideo(shareVideo).build()
    val videoUrl = ShareInternalUtility.getVideoUrl(shareVideoContent, appCall.callId)
    assertThat(videoUrl).isNotEmpty()
  }

  @Test
  fun `test getting the media infos from a media content`() {
    val testPhotoImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
    val sharePhoto = SharePhoto.Builder().setBitmap(testPhotoImage).build()
    val mediaContent = ShareMediaContent.Builder().addMedia(listOf(sharePhoto)).build()
    val mediaInfos = ShareInternalUtility.getMediaInfos(mediaContent, appCall.callId)
    assertThat(mediaInfos.size).isEqualTo(1)
    val mediaInfo = checkNotNull(mediaInfos[0])
    assertThat(mediaInfo.getString(ShareConstants.MEDIA_TYPE))
        .isEqualTo(sharePhoto.mediaType.toString())
    assertThat(mediaInfo.getString(ShareConstants.MEDIA_URI)).isNotEmpty
  }

  @Test
  fun `test getting the background media from a story content`() {
    val testPhotoImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
    val sharePhoto = SharePhoto.Builder().setBitmap(testPhotoImage).build()
    val shareStoryContent = ShareStoryContent.Builder().setBackgroundAsset(sharePhoto).build()
    val mediaInfo =
        checkNotNull(
            ShareInternalUtility.getBackgroundAssetMediaInfo(shareStoryContent, appCall.callId))
    assertThat(mediaInfo.getString(ShareConstants.MEDIA_TYPE))
        .isEqualTo(sharePhoto.mediaType.toString())
  }

  @Test
  fun `test getting the sticker url from a story content`() {
    val testPhotoImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
    val sharePhoto = SharePhoto.Builder().setBitmap(testPhotoImage).build()
    val shareStoryContent = ShareStoryContent.Builder().setStickerAsset(sharePhoto).build()
    val stickerInfo =
        checkNotNull(ShareInternalUtility.getStickerUrl(shareStoryContent, appCall.callId))
    assertThat(stickerInfo.getString(ShareConstants.MEDIA_URI)).isNotEmpty()
  }

  @Test
  fun `test getting the texture url bundle from a camera effect`() {
    val testPhotoImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
    val texture = CameraEffectTextures.Builder().putTexture("test", testPhotoImage).build()
    val effect = ShareCameraEffectContent.Builder().setTextures(texture).build()
    val urlBundle = ShareInternalUtility.getTextureUrlBundle(effect, appCall.callId)
    assertThat(urlBundle.getString("test")).isNotNull
  }

  @Test
  fun `test creating upload a bitmap as staging resource`() {
    val testPhotoImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
    val request =
        ShareInternalUtility.newUploadStagingResourceWithImageRequest(null, testPhotoImage, null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.POST)
    assertThat(request.parameters.getParcelable<Bitmap>(ShareInternalUtility.STAGING_PARAM))
        .isEqualTo(testPhotoImage)
    assertThat(request.graphPath).isEqualTo(ShareInternalUtility.MY_STAGING_RESOURCES)
  }

  @Test
  fun `test creating upload a content uri as staging resource`() {
    val fileUri = Uri.parse("content://tmp/test.png")
    val request = ShareInternalUtility.newUploadStagingResourceWithImageRequest(null, fileUri, null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.POST)
    assertThat(request.graphPath).isEqualTo(ShareInternalUtility.MY_STAGING_RESOURCES)
  }

  @Test
  fun `test creating upload a file as staging resource`() {
    val tempFile = File(UUID.randomUUID().toString())
    tempFile.createNewFile()
    tempFile.deleteOnExit()

    val request =
        ShareInternalUtility.newUploadStagingResourceWithImageRequest(null, tempFile, null)
    assertThat(request.httpMethod).isEqualTo(HttpMethod.POST)
    assertThat(request.graphPath).isEqualTo(ShareInternalUtility.MY_STAGING_RESOURCES)
  }

  @Test
  fun `test getting the uri extension`() {
    assertThat(ShareInternalUtility.getUriExtension(Uri.parse("file://test.tar.gz")))
        .isEqualTo(".gz")
    assertThat(ShareInternalUtility.getUriExtension(Uri.parse("file://test-file"))).isNull()
  }

  companion object {
    const val TEST_REQUEST_CODE = 0xface

    private val jsonOGActionTestObject: JSONObject
      get() {
        val testAction = JSONObject()
        testAction.put("og:field", 1)
        testAction.put("namespaced:custom:field", 3)
        val testOGContent = jsonOGContentTestObject
        testAction.put("namespaced:content", testOGContent)
        testAction.put("array", jsonOGArrayTestObject)
        return testAction
      }

    private val jsonOGActionTestObjectWithoutNamespace: JSONObject
      get() {
        val testAction = JSONObject()
        testAction.put("field", 1)
        testAction.put("custom:field", 3)
        val testOGContent = jsonOGContentTestObjectWithoutNamespace
        testAction.put("content", testOGContent)
        testAction.put("array", jsonOGArrayTestObjectWithoutNamespace)
        return testAction
      }

    private val jsonOGArrayTestObject: JSONArray
      get() {
        val testArray = JSONArray()
        testArray.put(10)
        testArray.put(jsonOGContentTestObject)
        return testArray
      }

    private val jsonOGArrayTestObjectWithoutNamespace: JSONArray
      get() {
        val testArray = JSONArray()
        testArray.put(10)
        testArray.put(jsonOGContentTestObjectWithoutNamespace)
        return testArray
      }

    private val jsonOGContentTestObject: JSONObject
      get() {
        val testOGContent = JSONObject()
        testOGContent.put("fbsdk:create", true)
        testOGContent.put("namespaced:field", 4)
        testOGContent.put("og:field", 5)
        testOGContent.put("custom:namespaced:field", 6)
        val innerContent = JSONObject()
        innerContent.put("namespaced:field", 7)
        innerContent.put("og:field", 8)
        testOGContent.put("namespaced:innerContent", innerContent)
        return testOGContent
      }

    private val jsonOGContentTestObjectWithoutNamespace: JSONObject
      get() {
        val testOGContent = JSONObject()
        testOGContent.put("fbsdk:create", true)
        testOGContent.put("field", 5)
        val innerContent = JSONObject()
        innerContent.put("field", 8)
        val innerData = JSONObject()
        innerData.put("field", 7)
        innerContent.put("data", innerData)
        val data = JSONObject()
        data.put("field", 4)
        data.put("namespaced:field", 6)
        data.put("innerContent", innerContent)
        testOGContent.put("data", data)
        return testOGContent
      }
    private fun simpleJsonObjComparer(obj1: JSONObject, obj2: JSONObject): Boolean {
      if (obj1.length() != obj2.length()) {
        return false
      }
      val keys = obj1.keys()
      while (keys.hasNext()) {
        try {
          val key = keys.next()
          val value1 = obj1[key]
          val value2 = obj2[key]
          if (!jsonObjectValueComparer(value1, value2)) {
            return false
          }
        } catch (ex: Exception) {
          return false
        }
      }
      return true
    }

    private fun simpleJsonArrayComparer(array1: JSONArray, array2: JSONArray): Boolean {
      if (array1.length() != array2.length()) {
        return false
      }
      for (i in 0 until array1.length()) {
        if (!jsonObjectValueComparer(array1[i], array2[i])) {
          return false
        }
      }
      return true
    }

    private fun jsonObjectValueComparer(value1: Any, value2: Any): Boolean {
      if (value1 is JSONObject) {
        if (!simpleJsonObjComparer(value1, value2 as JSONObject)) {
          return false
        }
      } else if (value1 is JSONArray) {
        if (!simpleJsonArrayComparer(value1, value2 as JSONArray)) {
          return false
        }
      } else if (value1 !== value2) {
        return false
      }
      return true
    }
  }
}
