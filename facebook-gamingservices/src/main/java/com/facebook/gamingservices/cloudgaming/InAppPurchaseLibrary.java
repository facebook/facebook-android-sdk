// @lint-ignore LICENSELINT
/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * <p>As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.gamingservices.cloudgaming;

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKLogger;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import org.json.JSONException;
import org.json.JSONObject;

public class InAppPurchaseLibrary {
  // Valid parameter keys
  @Deprecated public static final String PRODUCT_ID = SDKConstants.PARAM_PRODUCT_ID;
  @Deprecated public static final String PURCHASE_TOKEN = SDKConstants.PARAM_PURCHASE_TOKEN;
  @Deprecated public static final String DEVELOPER_PAYLOAD = SDKConstants.PARAM_DEVELOPER_PAYLOAD;

  /**
   * Sets a callback to be triggered when Payments operations are available.
   *
   * @param context the application context
   * @param callback callback for success and error
   */
  public static void onReady(Context context, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, null, callback, SDKMessageEnum.ON_READY);
  }

  /**
   * Fetches the game's product catalog.
   *
   * @param context the application context
   * @param callback callback for success and error
   */
  public static void getCatalog(Context context, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, null, callback, SDKMessageEnum.GET_CATALOG);
  }

  /**
   * Fetches all of the player's unconsumed purchases. The game must fetch the current player's
   * purchases as soon as the client indicates that it is ready to perform payments-related
   * operations, i.e. at game start. The game can then process and consume any purchases that are
   * waiting to be consumed.
   *
   * @param context the application context
   * @param callback callback for success and error
   */
  public static void getPurchases(Context context, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, null, callback, SDKMessageEnum.GET_PURCHASES);
  }

  /**
   * Begins the purchase flow for a specific product.
   *
   * @param context the application context
   * @param productID the productID of the item to be purchased, obtained from the catalog
   * @param developerPayload the optional string payload to be associated with the purchase
   * @param callback callback for success and error
   */
  public static void purchase(
      Context context,
      String productID,
      @Nullable String developerPayload,
      DaemonRequest.Callback callback) {
    try {
      JSONObject parameters =
          (new JSONObject())
              .put(SDKConstants.PARAM_PRODUCT_ID, productID)
              .put(SDKConstants.PARAM_DEVELOPER_PAYLOAD, developerPayload);
      DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.PURCHASE);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.PURCHASE, e);
    }
  }

  /**
   * Consumes a specific purchase belonging to the current player. Before provisioning a product's
   * effects to the player, the game should request the consumption of the purchased product. Once
   * the purchase is successfully consumed, the game should immediately provide the player with the
   * effects of their purchase.
   *
   * @param context the application context
   * @param purchaseToken the purchase token associated with a transaction
   * @param callback callback for success and error
   */
  public static void consumePurchase(
      Context context, String purchaseToken, DaemonRequest.Callback callback) {
    try {
      JSONObject parameters =
          (new JSONObject()).put(SDKConstants.PARAM_PURCHASE_TOKEN, purchaseToken);
      DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.CONSUME_PURCHASE);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.CONSUME_PURCHASE, e);
    }
  }

  /**
   * Sets a callback to be triggered when Payments operations are available.
   *
   * @deprecated Replaced by the overloaded function
   * @param context the application context
   * @param parameters {}
   * @param callback callback for success and error
   */
  public static void onReady(
      Context context, @Nullable JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.ON_READY);
  }

  /**
   * Fetches the game's product catalog.
   *
   * @deprecated Replaced by the overloaded function
   * @param context the application context
   * @param parameters {}
   * @param callback callback for success and error
   */
  public static void getCatalog(
      Context context, @Nullable JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.GET_CATALOG);
  }

  /**
   * Fetches all of the player's unconsumed purchases. The game must fetch the current player's
   * purchases as soon as the client indicates that it is ready to perform payments-related
   * operations, i.e. at game start. The game can then process and consume any purchases that are
   * waiting to be consumed.
   *
   * @deprecated Replaced by the overloaded function
   * @param context the application context
   * @param parameters {}
   * @param callback callback for success and error
   */
  public static void getPurchases(
      Context context, @Nullable JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.GET_PURCHASES);
  }

  /**
   * Begins the purchase flow for a specific product.
   *
   * @deprecated Replaced by the overloaded function
   * @param context the application context
   * @param parameters { PRODUCT_ID: <the productID of the item>, DEVELOPER_PAYLOAD (optional):
   *     <string payload associated with purchase> }
   * @param callback callback for success and error
   */
  public static void purchase(
      Context context, JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.PURCHASE);
  }

  /**
   * Consumes a specific purchase belonging to the current player. Before provisioning a product's
   * effects to the player, the game should request the consumption of the purchased product. Once
   * the purchase is successfully consumed, the game should immediately provide the player with the
   * effects of their purchase.
   *
   * @deprecated Replaced by the overloaded function
   * @param context the application context
   * @param parameters { PURCHASE_TOKEN: <the purchase token associated with a transaction> }
   * @param callback callback for success and error
   */
  public static void consumePurchase(
      Context context, JSONObject parameters, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.CONSUME_PURCHASE);
  }
}
