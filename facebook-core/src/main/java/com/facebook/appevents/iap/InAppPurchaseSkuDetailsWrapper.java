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

package com.facebook.appevents.iap;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InAppPurchaseSkuDetailsWrapper {
  private @Nullable static InAppPurchaseSkuDetailsWrapper mInstance = null;
  private static final AtomicBoolean initialized = new AtomicBoolean(false);

  private final Class<?> skuDetailsParamsClazz;
  private final Class<?> builderClazz;
  private final Method newBuilderMethod;
  private final Method setTypeMethod;
  private final Method setSkusListMethod;
  private final Method buildMethod;

  private static final String CLASSNAME_SKU_DETAILS_PARAMS =
      "com.android.billingclient.api.SkuDetailsParams";
  private static final String CLASSNAME_SKU_DETAILS_PARAMS_BUILDER =
      "com.android.billingclient.api.SkuDetailsParams$Builder";
  private static final String METHOD_NEW_BUILDER = "newBuilder";
  private static final String METHOD_SET_TYPE = "setType";
  private static final String METHOD_SET_SKU_LIST = "setSkusList";
  private static final String METHOD_BUILD = "build";

  public InAppPurchaseSkuDetailsWrapper(
      Class<?> skuDetailsParamsClazz,
      Class<?> builderClazz,
      Method newBuilderMethod,
      Method setTypeMethod,
      Method setSkusListMethod,
      Method buildMethod) {
    this.skuDetailsParamsClazz = skuDetailsParamsClazz;
    this.builderClazz = builderClazz;
    this.newBuilderMethod = newBuilderMethod;
    this.setTypeMethod = setTypeMethod;
    this.setSkusListMethod = setSkusListMethod;
    this.buildMethod = buildMethod;
  }

  @Nullable
  public static InAppPurchaseSkuDetailsWrapper getOrCreateInstance() {
    if (initialized.get()) {
      return mInstance;
    }
    createInstance();
    initialized.set(true);
    return mInstance;
  }

  private static void createInstance() {
    Class<?> skuDetailsParamsClazz = InAppPurchaseUtils.getClass(CLASSNAME_SKU_DETAILS_PARAMS);
    Class<?> builderClazz = InAppPurchaseUtils.getClass(CLASSNAME_SKU_DETAILS_PARAMS_BUILDER);
    if (skuDetailsParamsClazz == null || builderClazz == null) {
      return;
    }

    Method newBuilderMethod =
        InAppPurchaseUtils.getMethod(skuDetailsParamsClazz, METHOD_NEW_BUILDER);
    Method setTypeMethod =
        InAppPurchaseUtils.getMethod(builderClazz, METHOD_SET_TYPE, String.class);
    Method setSkusListMethod =
        InAppPurchaseUtils.getMethod(builderClazz, METHOD_SET_SKU_LIST, List.class);
    Method buildMethod = InAppPurchaseUtils.getMethod(builderClazz, METHOD_BUILD);
    if (newBuilderMethod == null
        || setTypeMethod == null
        || setSkusListMethod == null
        || buildMethod == null) {
      return;
    }
    mInstance =
        new InAppPurchaseSkuDetailsWrapper(
            skuDetailsParamsClazz,
            builderClazz,
            newBuilderMethod,
            setTypeMethod,
            setSkusListMethod,
            buildMethod);
  }

  public Class<?> getSkuDetailsParamsClazz() {
    return skuDetailsParamsClazz;
  }

  @Nullable
  public Object getSkuDetailsParams(String skuType, List<String> skuIDs) {
    // 1. newBuilder()
    Object builder = InAppPurchaseUtils.invokeMethod(skuDetailsParamsClazz, newBuilderMethod, null);
    if (builder == null) {
      return null;
    }

    // 2. setType(skuType)
    builder = InAppPurchaseUtils.invokeMethod(builderClazz, setTypeMethod, builder, skuType);
    if (builder == null) {
      return null;
    }

    // 3. setSkusList(skuIDs)
    builder = InAppPurchaseUtils.invokeMethod(builderClazz, setSkusListMethod, builder, skuIDs);
    if (builder == null) {
      return null;
    }

    // 4. build()
    return InAppPurchaseUtils.invokeMethod(builderClazz, buildMethod, builder);
  }
}
