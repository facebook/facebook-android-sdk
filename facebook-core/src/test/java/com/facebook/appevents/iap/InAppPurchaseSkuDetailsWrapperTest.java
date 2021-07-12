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

package com.facebook.appevents.iap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.powermock.api.mockito.PowerMockito.when;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({
  FacebookSdk.class,
})
public class InAppPurchaseSkuDetailsWrapperTest extends FacebookPowerMockTestCase {
  private final Executor mockExecutor = new FacebookSerialExecutor();

  private static final String CLASSNAME_SKU_DETAILS_PARAMS =
      "com.android.billingclient.api.SkuDetailsParams";
  private static final String CLASSNAME_SKU_DETAILS_PARAMS_BUILDER =
      "com.android.billingclient.api.SkuDetailsParams$Builder";
  private static final String METHOD_NEW_BUILDER = "newBuilder";
  private static final String METHOD_SET_TYPE = "setType";
  private static final String METHOD_SET_SKU_LIST = "setSkusList";
  private static final String METHOD_BUILD = "build";

  @Before
  @Override
  public void setup() {
    super.setup();
    PowerMockito.mockStatic(FacebookSdk.class);
    when(FacebookSdk.isInitialized()).thenReturn(true);
    when(FacebookSdk.getExecutor()).thenReturn(mockExecutor);
  }

  @Test
  public void testGetSkuDetailsParams() {
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
      fail("Fail to get class");
      return;
    }

    InAppPurchaseSkuDetailsWrapper inAppPurchaseSkuDetailsWrapper =
        new InAppPurchaseSkuDetailsWrapper(
            skuDetailsParamsClazz,
            builderClazz,
            newBuilderMethod,
            setTypeMethod,
            setSkusListMethod,
            buildMethod);

    String skuType = "inapp";
    List<String> skuIDs = new ArrayList<>();
    skuIDs.add("test sku ID");

    Object skuDetailsParams = inAppPurchaseSkuDetailsWrapper.getSkuDetailsParams(skuType, skuIDs);
    assertThat(skuDetailsParams).isNotNull();
  }
}
