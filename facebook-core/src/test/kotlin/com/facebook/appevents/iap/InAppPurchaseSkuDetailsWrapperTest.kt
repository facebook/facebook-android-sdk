/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.appevents.iap

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getExecutor
import com.facebook.FacebookSdk.isInitialized
import com.facebook.FacebookTestUtility.assertNotNull
import com.facebook.appevents.iap.InAppPurchaseUtils.getClass
import com.facebook.appevents.iap.InAppPurchaseUtils.getMethod
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class InAppPurchaseSkuDetailsWrapperTest : FacebookPowerMockTestCase() {
  companion object {
    private const val CLASSNAME_SKU_DETAILS_PARAMS =
        "com.facebook.appevents.iap.InAppPurchaseSkuDetailsWrapperTest\$FakeSkuDetailsParams"
    private const val CLASSNAME_SKU_DETAILS_PARAMS_BUILDER =
        "com.facebook.appevents.iap.InAppPurchaseSkuDetailsWrapperTest\$FakeSkuDetailsParams\$Builder"
    private const val METHOD_NEW_BUILDER = "newBuilder"
    private const val METHOD_SET_TYPE = "setType"
    private const val METHOD_SET_SKU_LIST = "setSkusList"
    private const val METHOD_BUILD = "build"
  }

  private val mockExecutor: Executor = FacebookSerialExecutor()

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(isInitialized()).thenReturn(true)
    whenever(getExecutor()).thenReturn(mockExecutor)
  }

  @Test
  fun testGetSkuDetailsParams() {
    val skuDetailsParamsClazz = assertNotNull(getClass(CLASSNAME_SKU_DETAILS_PARAMS))
    val builderClazz = assertNotNull(getClass(CLASSNAME_SKU_DETAILS_PARAMS_BUILDER))

    val newBuilderMethod = assertNotNull(getMethod(skuDetailsParamsClazz, METHOD_NEW_BUILDER))
    val setTypeMethod = assertNotNull(getMethod(builderClazz, METHOD_SET_TYPE, String::class.java))
    val setSkusListMethod =
        assertNotNull(getMethod(builderClazz, METHOD_SET_SKU_LIST, MutableList::class.java))
    val buildMethod = assertNotNull(getMethod(builderClazz, METHOD_BUILD))

    val inAppPurchaseSkuDetailsWrapper =
        InAppPurchaseSkuDetailsWrapper(
            skuDetailsParamsClazz,
            builderClazz,
            newBuilderMethod,
            setTypeMethod,
            setSkusListMethod,
            buildMethod)
    val skuType = "inapp"
    val skuIDs: MutableList<String?> = mutableListOf("test sku ID")
    val skuDetailsParams = inAppPurchaseSkuDetailsWrapper.getSkuDetailsParams(skuType, skuIDs)
    assertThat(skuDetailsParams).isNotNull
    assertThat((skuDetailsParams as FakeSkuDetailsParams).skuType).isEqualTo(skuType)
    assertThat(skuDetailsParams.skusList).containsExactlyElementsOf(skuIDs)
  }

  class FakeSkuDetailsParams {
    var skuType: String? = null
      private set
    var skusList: List<String>? = null
      private set

    companion object {
      @JvmStatic
      fun newBuilder(): Builder {
        return Builder()
      }
    }

    class Builder internal constructor() {
      private val params: FakeSkuDetailsParams = FakeSkuDetailsParams()

      fun setSkusList(skusList: List<String>): Builder {
        params.skusList = skusList
        return this
      }

      fun setType(type: String): Builder {
        params.skuType = type
        return this
      }

      fun build(): FakeSkuDetailsParams = params
    }
  }
}
