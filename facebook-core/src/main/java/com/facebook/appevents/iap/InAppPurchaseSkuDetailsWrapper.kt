/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import androidx.annotation.RestrictTo
import com.facebook.appevents.iap.InAppPurchaseUtils.getClass
import com.facebook.appevents.iap.InAppPurchaseUtils.getMethod
import com.facebook.appevents.iap.InAppPurchaseUtils.invokeMethod
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InAppPurchaseSkuDetailsWrapper(
    val skuDetailsParamsClazz: Class<*>,
    private val builderClazz: Class<*>,
    private val newBuilderMethod: Method,
    private val setTypeMethod: Method,
    private val setSkusListMethod: Method,
    private val buildMethod: Method
) {
    fun getSkuDetailsParams(
        productType: InAppPurchaseUtils.IAPProductType,
        skuIDs: List<String?>?
    ): Any? {
        // 1. newBuilder()
        var builder: Any? =
            invokeMethod(skuDetailsParamsClazz, newBuilderMethod, null) ?: return null

        // 2. setType(skuType)
        builder = invokeMethod(builderClazz, setTypeMethod, builder, productType.type)
        if (builder == null) {
            return null
        }

        // 3. setSkusList(skuIDs)
        builder = invokeMethod(builderClazz, setSkusListMethod, builder, skuIDs)
        return if (builder == null) {
            null
        } else invokeMethod(builderClazz, buildMethod, builder)

        // 4. build()
    }

    companion object {
        private var instance: InAppPurchaseSkuDetailsWrapper? = null
        private const val CLASSNAME_SKU_DETAILS_PARAMS =
            "com.android.billingclient.api.SkuDetailsParams"
        private const val CLASSNAME_SKU_DETAILS_PARAMS_BUILDER =
            "com.android.billingclient.api.SkuDetailsParams\$Builder"
        private const val METHOD_NEW_BUILDER = "newBuilder"
        private const val METHOD_SET_TYPE = "setType"
        private const val METHOD_SET_SKU_LIST = "setSkusList"
        private const val METHOD_BUILD = "build"

        @Synchronized
        @JvmStatic
        fun getOrCreateInstance(): InAppPurchaseSkuDetailsWrapper? {
            return instance ?: createInstance()
        }

        private fun createInstance(): InAppPurchaseSkuDetailsWrapper? {
            val skuDetailsParamsClazz = getClass(CLASSNAME_SKU_DETAILS_PARAMS)
            val builderClazz = getClass(CLASSNAME_SKU_DETAILS_PARAMS_BUILDER)
            if (skuDetailsParamsClazz == null || builderClazz == null) {
                return null
            }
            val newBuilderMethod = getMethod(skuDetailsParamsClazz, METHOD_NEW_BUILDER)
            val setTypeMethod = getMethod(builderClazz, METHOD_SET_TYPE, String::class.java)
            val setSkusListMethod =
                getMethod(builderClazz, METHOD_SET_SKU_LIST, MutableList::class.java)
            val buildMethod = getMethod(builderClazz, METHOD_BUILD)
            if (newBuilderMethod == null ||
                setTypeMethod == null ||
                setSkusListMethod == null ||
                buildMethod == null
            ) {
                return null
            }
            instance =
                InAppPurchaseSkuDetailsWrapper(
                    skuDetailsParamsClazz,
                    builderClazz,
                    newBuilderMethod,
                    setTypeMethod,
                    setSkusListMethod,
                    buildMethod
                )
            return instance
        }
    }
}
