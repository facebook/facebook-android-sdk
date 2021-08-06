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
  fun getSkuDetailsParams(skuType: String?, skuIDs: List<String?>?): Any? {
    // 1. newBuilder()
    var builder: Any? = invokeMethod(skuDetailsParamsClazz, newBuilderMethod, null) ?: return null

    // 2. setType(skuType)
    builder = invokeMethod(builderClazz, setTypeMethod, builder, skuType)
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
    private val initialized = AtomicBoolean(false)
    private const val CLASSNAME_SKU_DETAILS_PARAMS =
        "com.android.billingclient.api.SkuDetailsParams"
    private const val CLASSNAME_SKU_DETAILS_PARAMS_BUILDER =
        "com.android.billingclient.api.SkuDetailsParams\$Builder"
    private const val METHOD_NEW_BUILDER = "newBuilder"
    private const val METHOD_SET_TYPE = "setType"
    private const val METHOD_SET_SKU_LIST = "setSkusList"
    private const val METHOD_BUILD = "build"

    @JvmStatic
    fun getOrCreateInstance(): InAppPurchaseSkuDetailsWrapper? {
      if (initialized.get()) {
        return instance
      }
      createInstance()
      initialized.set(true)
      return instance
    }

    private fun createInstance() {
      val skuDetailsParamsClazz = getClass(CLASSNAME_SKU_DETAILS_PARAMS)
      val builderClazz = getClass(CLASSNAME_SKU_DETAILS_PARAMS_BUILDER)
      if (skuDetailsParamsClazz == null || builderClazz == null) {
        return
      }
      val newBuilderMethod = getMethod(skuDetailsParamsClazz, METHOD_NEW_BUILDER)
      val setTypeMethod = getMethod(builderClazz, METHOD_SET_TYPE, String::class.java)
      val setSkusListMethod = getMethod(builderClazz, METHOD_SET_SKU_LIST, MutableList::class.java)
      val buildMethod = getMethod(builderClazz, METHOD_BUILD)
      if (newBuilderMethod == null ||
          setTypeMethod == null ||
          setSkusListMethod == null ||
          buildMethod == null) {
        return
      }
      instance =
          InAppPurchaseSkuDetailsWrapper(
              skuDetailsParamsClazz,
              builderClazz,
              newBuilderMethod,
              setTypeMethod,
              setSkusListMethod,
              buildMethod)
    }
  }
}
