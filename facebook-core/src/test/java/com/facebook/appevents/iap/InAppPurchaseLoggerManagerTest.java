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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

@PrepareForTest({
  FacebookSdk.class,
  InAppPurchaseLoggerManager.class,
})
public class InAppPurchaseLoggerManagerTest extends FacebookPowerMockTestCase {
  private final Executor mockExecutor = new FacebookSerialExecutor();
  private SharedPreferences mockPrefs;

  @Before
  @Override
  public void setup() {
    super.setup();
    Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", new AtomicBoolean(true));
    Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
    PowerMockito.spy(FacebookSdk.class);
    PowerMockito.spy(InAppPurchaseLoggerManager.class);

    mockPrefs = Mockito.mock(SharedPreferences.class);
    Context context = Mockito.mock(Context.class);
    Whitebox.setInternalState(FacebookSdk.class, "applicationContext", context);
    Mockito.when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);
    SharedPreferences.Editor editor = Mockito.mock(SharedPreferences.Editor.class);
    Whitebox.setInternalState(InAppPurchaseLoggerManager.class, "sharedPreferences", mockPrefs);

    when(mockPrefs.edit()).thenReturn(editor);
    when(editor.putLong(anyString(), anyLong())).thenReturn(editor);
    when(editor.putString(anyString(), anyString())).thenReturn(editor);
    when(editor.putStringSet(anyString(), ArgumentMatchers.<String>anySet())).thenReturn(editor);
  }

  @Test
  public void testCacheDeDupPurchase() {
    try {
      // Construct purchase details map
      Map<String, JSONObject> mockPurchaseDetailsMap =
          Mockito.spy(new HashMap<String, JSONObject>());
      JSONObject purchaseDetailJson1 =
          new JSONObject(
              "{\"productId\":\"espresso\",\"purchaseToken\":\"ijalfemekipiikbihpefglkb.AO-J1OxmFMlyq76sOLFnOPTowBBRM8PyXFuHzuWuHGtq7sRVilQSaAuMjkJCntoVCf8-X_TzaD1K-ec3ep4CKR6LgNKl60OTn3mQQDS-Wa8E1tnVJfFCDo4\",\"purchaseTime\":1620000000000,\"developerPayload\":null,\"packageName\":\"com.cfsample.coffeeshop\"}");
      mockPurchaseDetailsMap.put("espresso", purchaseDetailJson1);

      // Construct cached purchase map
      long lastClearedTime = 1600000000L;
      Map<String, Long> cachedPurchaseMap = Mockito.spy(new HashMap<String, Long>());
      cachedPurchaseMap.put(
          "ijalfemekipiikbihpefglkb.AO-J1OxmFMlyq76sOLFnOPTowBBRM8PyXFuHzuWuHGtq7sRVilQSaAuMjkJCntoVCf8-X_TzaD1K-ec3ep4CKR6LgNKl60OTn3mQQDS-Wa8E1tnVJfFCDo4",
          lastClearedTime);
      Whitebox.setInternalState(
          InAppPurchaseLoggerManager.class, "cachedPurchaseMap", cachedPurchaseMap);

      // Test duplicate purchase event can be successfully removed from purchase details map
      InAppPurchaseLoggerManager inAppPurchaseLoggerManager = new InAppPurchaseLoggerManager();
      Method privateMethod =
          InAppPurchaseLoggerManager.class.getDeclaredMethod("cacheDeDupPurchase", Map.class);
      privateMethod.setAccessible(true);
      privateMethod.invoke(inAppPurchaseLoggerManager, mockPurchaseDetailsMap);
      assertThat(mockPurchaseDetailsMap).isEmpty();
    } catch (Exception e) {
      fail("Fail when testing test cache de-dup purchase");
    }
  }

  @Test
  public void testClearOutDatedProductInfoInCache() throws Exception {
    // Add clear history data into cachedPurchaseMap
    long lastClearedTime = 1600000000L;
    when(mockPrefs.getLong("LAST_CLEARED_TIME", 0)).thenReturn(lastClearedTime);

    Map<String, Long> cachedPurchaseMap = Mockito.spy(new HashMap<String, Long>());
    cachedPurchaseMap.put(
        "AEuhp4LDP2FB_51qEWpJOLSDtCZoq3-jLL1rRPd4V7k9c5RyHc9Phx8iYBQqvJFYhfI=", lastClearedTime);
    Whitebox.setInternalState(
        InAppPurchaseLoggerManager.class, "cachedPurchaseMap", cachedPurchaseMap);
    assertThat(cachedPurchaseMap).isNotEmpty();

    try {
      InAppPurchaseLoggerManager inAppPurchaseLoggerManager = new InAppPurchaseLoggerManager();
      Method privateMethod =
          InAppPurchaseLoggerManager.class.getDeclaredMethod("clearOutdatedProductInfoInCache");
      privateMethod.setAccessible(true);
      privateMethod.invoke(inAppPurchaseLoggerManager);

      assertThat(cachedPurchaseMap).isEmpty();
    } catch (Exception e) {
      fail("Fail when testing clear out dated product info in cache");
    }
  }

  @Test
  public void testEligibleQueryPurchaseHistory() throws Exception {
    // Mock return last query purchase history time
    long mockLastQueryPurchaseTimeStamp = 0;
    when(mockPrefs.getLong("LAST_QUERY_PURCHASE_HISTORY_TIME", 0))
        .thenReturn(mockLastQueryPurchaseTimeStamp);

    // Test eligible to query purchase history if no last query history time has been set
    boolean result1 = InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory();
    assertThat(result1).isTrue();

    // Test not eligible to query purchase history if the time interval is shorter than 1 day
    mockLastQueryPurchaseTimeStamp = System.currentTimeMillis() / 1000L;
    when(mockPrefs.getLong("LAST_QUERY_PURCHASE_HISTORY_TIME", 0))
        .thenReturn(mockLastQueryPurchaseTimeStamp);
    boolean result2 = InAppPurchaseLoggerManager.eligibleQueryPurchaseHistory();
    assertThat(result2).isFalse();
  }

  @Test
  public void testConstructLoggingReadyMap() {
    try {
      InAppPurchaseLoggerManager inAppPurchaseLoggerManager = new InAppPurchaseLoggerManager();
      Map<String, JSONObject> mockPurchaseDetailsMap = new HashMap<>();
      JSONObject purchaseDetailJson1 =
          new JSONObject(
              "{\"productId\":\"espresso\",\"purchaseToken\":\"ijalfemekipiikbihpefglkb.AO-J1OxmFMlyq76sOLFnOPTowBBRM8PyXFuHzuWuHGtq7sRVilQSaAuMjkJCntoVCf8-X_TzaD1K-ec3ep4CKR6LgNKl60OTn3mQQDS-Wa8E1tnVJfFCDo4\",\"purchaseTime\":1620000000000,\"developerPayload\":null,\"packageName\":\"com.cfsample.coffeeshop\"}");
      mockPurchaseDetailsMap.put("espresso", purchaseDetailJson1);

      Map<String, JSONObject> mockSkuDetailsMap = new HashMap<>();
      JSONObject skuDetailJson =
          new JSONObject(
              "{\"productId\":\"coffee\",\"type\":\"inapp\",\"price\":\"$2.99\",\"price_amount_micros\":2990000,\"price_currency_code\":\"USD\",\"title\":\"coffee (coffeeshop)\",\"description\":\"Basic coffee \",\"skuDetailsToken\":\"AEuhp4LDP2FB_51qEWpJOLSDtCZoq3-jLL1rRPd4V7k9c5RyHc9Phx8iYBQqvJFYhfI=\"}");
      mockSkuDetailsMap.put("espresso", skuDetailJson);

      Method privateMethod =
          InAppPurchaseLoggerManager.class.getDeclaredMethod(
              "constructLoggingReadyMap", Map.class, Map.class);
      privateMethod.setAccessible(true);

      // Test purchase is too old to log
      Map<String, String> result1 =
          (Map<String, String>)
              privateMethod.invoke(
                  inAppPurchaseLoggerManager, mockPurchaseDetailsMap, mockSkuDetailsMap);
      assertThat(result1).isEmpty();

      // Test logging ready events can be added into map
      String newPurchaseDetailString =
          "{\"productId\":\"espresso\",\"purchaseToken\":\"ijalfemekipiikbihpefglkb.AO-J1OxmFMlyq76sOLFnOPTowBBRM8PyXFuHzuWuHGtq7sRVilQSaAuMjkJCntoVCf8-X_TzaD1K-ec3ep4CKR6LgNKl60OTn3mQQDS-Wa8E1tnVJfFCDo4\",\"purchaseTime\":%s,\"developerPayload\":null,\"packageName\":\"com.cfsample.coffeeshop\"}";
      newPurchaseDetailString = String.format(newPurchaseDetailString, System.currentTimeMillis());
      JSONObject newPurchaseDetailStringJson = new JSONObject(newPurchaseDetailString);
      mockPurchaseDetailsMap.clear();
      mockPurchaseDetailsMap.put("espresso", newPurchaseDetailStringJson);

      Map<String, String> result2 =
          (Map<String, String>)
              privateMethod.invoke(
                  inAppPurchaseLoggerManager, mockPurchaseDetailsMap, mockSkuDetailsMap);
      assertThat(result2).isNotEmpty();
    } catch (Exception e) {
      fail("Fail when test construct logging ready map");
    }
  }
}
