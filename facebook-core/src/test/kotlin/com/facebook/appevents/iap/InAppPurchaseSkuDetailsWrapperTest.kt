/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.getExecutor
import com.facebook.FacebookSdk.isInitialized
import com.facebook.FacebookTestUtility.assertNotNull
import com.facebook.appevents.iap.InAppPurchaseUtils.getClass
import com.facebook.appevents.iap.InAppPurchaseUtils.getMethod
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
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
        val setTypeMethod =
            assertNotNull(getMethod(builderClazz, METHOD_SET_TYPE, String::class.java))
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
                buildMethod
            )
        val skuType = InAppPurchaseUtils.IAPProductType.INAPP
        val skuIDs: MutableList<String?> = mutableListOf("test sku ID")
        val skuDetailsParams = inAppPurchaseSkuDetailsWrapper.getSkuDetailsParams(skuType, skuIDs)
        assertThat(skuDetailsParams).isNotNull
        assertThat((skuDetailsParams as FakeSkuDetailsParams).skuType).isEqualTo(skuType.type)
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
