/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
   * Fetches the game's catalog for subscribable products.
   *
   * @param context the application context
   * @param callback callback for success and error
   */
  public static void getSubscribableCatalog(Context context, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, null, callback, SDKMessageEnum.GET_SUBSCRIBABLE_CATALOG);
  }

  /**
   * Begins the purchase flow for a specific subscribable product.
   *
   * @param context the application context
   * @param productID the productID of the item to be purchased, obtained from the catalog
   * @param callback callback for success and error
   */
  public static void purchaseSubscription(
      Context context, String productID, DaemonRequest.Callback callback) {
    try {
      JSONObject parameters = (new JSONObject()).put(SDKConstants.PARAM_PRODUCT_ID, productID);
      DaemonRequest.executeAsync(
          context, parameters, callback, SDKMessageEnum.PURCHASE_SUBSCRIPTION);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.PURCHASE_SUBSCRIPTION, e);
    }
  }

  /**
   * Fetches all of the player's subscriptions.
   *
   * @param context the application context
   * @param callback callback for success and error
   */
  public static void getSubscriptions(Context context, DaemonRequest.Callback callback) {
    DaemonRequest.executeAsync(context, null, callback, SDKMessageEnum.GET_SUBSCRIPTIONS);
  }

  /**
   * Starts the asynchronous process of cancelling an existing subscription. This operation will
   * only work if the subscription entitlement is active. If the promise is resolved, this is only
   * an indication that the cancellation has been kicked off and NOT that it has necessarily
   * succeeded. The subscription's deactivationTime and isEntitlementActive properties should be
   * queried for the latest status.
   *
   * @param context the application context
   * @param purchaseToken the purchase token associated with a transaction
   * @param callback callback for success and error
   */
  public static void cancelSubscription(
      Context context, String purchaseToken, DaemonRequest.Callback callback) {
    try {
      JSONObject parameters =
          (new JSONObject()).put(SDKConstants.PARAM_PURCHASE_TOKEN, purchaseToken);
      DaemonRequest.executeAsync(context, parameters, callback, SDKMessageEnum.CANCEL_SUBSCRIPTION);
    } catch (JSONException e) {
      SDKLogger.logInternalError(context, SDKMessageEnum.CANCEL_SUBSCRIPTION, e);
    }
  }
}
