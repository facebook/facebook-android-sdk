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
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({
  FacebookSdk.class,
  InAppPurchaseBillingClientWrapper.class,
  InAppPurchaseUtils.class,
  Context.class
})
public class InAppPurchaseBillingClientWrapperTest extends FacebookPowerMockTestCase {
  private final Executor mockExecutor = new FacebookSerialExecutor();
  private InAppPurchaseBillingClientWrapper inAppPurchaseBillingClientWrapper;

  private static final String CLASSNAME_SKU_DETAILS_PARAMS =
      "com.android.billingclient.api.SkuDetailsParams";
  private static final String CLASSNAME_SKU_DETAILS_PARAMS_BUILDER =
      "com.android.billingclient.api.SkuDetailsParams$Builder";

  @Before
  @Override
  public void setup() {
    super.setup();
    Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", new AtomicBoolean(true));
    Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
    spy(FacebookSdk.class);
    spy(InAppPurchaseBillingClientWrapper.class);

    Context mockContext = Mockito.mock(Context.class);
    when(FacebookSdk.getApplicationContext()).thenReturn(mockContext);
    inAppPurchaseBillingClientWrapper = Mockito.mock(InAppPurchaseBillingClientWrapper.class);

    when(InAppPurchaseBillingClientWrapper.getOrCreateInstance(FacebookSdk.getApplicationContext()))
        .thenReturn(inAppPurchaseBillingClientWrapper);
  }

  @Test
  public void testHelperClassCanSuccessfullyCreateWrapper() {
    // Test InAppPurchaseBillingClientWrapper
    assertThat(inAppPurchaseBillingClientWrapper).isNotNull();

    // Test BillingClientStateListenerWrapper
    InAppPurchaseBillingClientWrapper.BillingClientStateListenerWrapper
        billingClientStateListenerWrapper =
            new InAppPurchaseBillingClientWrapper.BillingClientStateListenerWrapper();
    assertThat(billingClientStateListenerWrapper).isNotNull();

    // Test PurchasesUpdatedListenerWrapper
    InAppPurchaseBillingClientWrapper.PurchasesUpdatedListenerWrapper
        purchasesUpdatedListenerWrapper =
            new InAppPurchaseBillingClientWrapper.PurchasesUpdatedListenerWrapper();
    assertThat(purchasesUpdatedListenerWrapper).isNotNull();
  }

  @Test
  public void testBillingClientWrapper() {
    Runnable runnable = Mockito.mock(Runnable.class);
    InAppPurchaseBillingClientWrapper.PurchaseHistoryResponseListenerWrapper
        purchaseHistoryResponseListenerWrapper =
            inAppPurchaseBillingClientWrapper.new PurchaseHistoryResponseListenerWrapper(runnable);
    assertThat(purchaseHistoryResponseListenerWrapper).isNotNull();

    // Test getPurchaseHistoryRecord
    try {
      Method privateMethod =
          InAppPurchaseBillingClientWrapper.PurchaseHistoryResponseListenerWrapper.class
              .getDeclaredMethod("getPurchaseHistoryRecord", List.class);
      privateMethod.setAccessible(true);

      Map<String, JSONObject> purchaseDetailsMap =
          Whitebox.getInternalState(InAppPurchaseBillingClientWrapper.class, "purchaseDetailsMap");
      Whitebox.setInternalState(
          inAppPurchaseBillingClientWrapper, "historyPurchaseSet", new HashSet<>());

      List<Object> mockList = new ArrayList<>();
      String purchaseHistoryRecord =
          "{\"productId\":\"coffee\",\"purchaseToken\":\"aedeglbgcjhjcjnabndchooe.AO-J1Oydf8j_hBxWxvsAvKHLC1h8Kw6YPDtGERpjCWDKSB0Hd6asHyo5E_NjbPg1u1hW5rW-s4go3d0D_DjFstxDA6zn9H_85ReDVbQBdgb2VAAyTX39jcM\",\"purchaseTime\":1614677061238,\"developerPayload\":null}\n";
      mockList.add(purchaseHistoryRecord);

      PowerMockito.mockStatic(InAppPurchaseUtils.class);
      when(InAppPurchaseUtils.invokeMethod(null, null, purchaseHistoryRecord))
          .thenReturn(purchaseHistoryRecord);

      // Mock context
      Context mockContext = Mockito.mock(Context.class);
      Whitebox.setInternalState(inAppPurchaseBillingClientWrapper, "context", mockContext);
      when(mockContext.getPackageName()).thenReturn("value");

      privateMethod.invoke(purchaseHistoryResponseListenerWrapper, mockList);
      assertThat(purchaseDetailsMap).isNotEmpty();
    } catch (Exception e) {
      fail("Fail when test BillingClientWrapper ParseHistoryRecord");
    }
  }

  @Test
  public void testSkuDetailsResponseListenerWrapper() {
    // Test can successfully create skuDetailsResponseListenerWrapper
    Runnable runnable = Mockito.mock(Runnable.class);
    InAppPurchaseBillingClientWrapper.SkuDetailsResponseListenerWrapper
        skuDetailsResponseListenerWrapper =
            inAppPurchaseBillingClientWrapper.new SkuDetailsResponseListenerWrapper(runnable);
    assertThat(skuDetailsResponseListenerWrapper).isNotNull();

    // Test parseSkuDetails
    try {
      Method privateMethod =
          InAppPurchaseBillingClientWrapper.SkuDetailsResponseListenerWrapper.class
              .getDeclaredMethod("parseSkuDetails", List.class);
      privateMethod.setAccessible(true);

      List<Object> mockList = new ArrayList<>();
      String skuDetailExample =
          "{\"productId\":\"coffee\",\"type\":\"inapp\",\"price\":\"$0.99\",\"price_amount_micros\":990000,\"price_currency_code\":\"USD\",\"title\":\"cf (coffeeshop)\",\"description\":\"cf\",\"skuDetailsToken\":\"AEuhp4I4Fby7vHeunJbyRTraiO-Z04Y5GPKRYgZtHVCTfmiIhxHj41Rt7kgywkTtIRxP\"}\n";
      mockList.add(skuDetailExample);

      PowerMockito.mockStatic(InAppPurchaseUtils.class);
      when(InAppPurchaseUtils.invokeMethod(null, null, skuDetailExample))
          .thenReturn(skuDetailExample);
      Map<String, JSONObject> skuDetailsMap =
          Whitebox.getInternalState(InAppPurchaseBillingClientWrapper.class, "skuDetailsMap");

      privateMethod.invoke(skuDetailsResponseListenerWrapper, mockList);
      assertThat(skuDetailsMap).isNotEmpty();
    } catch (Exception e) {
      fail("Fail when test BillingClientWrapper ParserSkuDetails");
    }
  }
}
