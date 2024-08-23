/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

object Constants {
  const val LOG_TIME_APP_EVENT_KEY = "_logTime"
  const val EVENT_NAME_EVENT_KEY = "_eventName"

  // The following are for Automatic Analytics events and parameters
  const val AA_TIME_SPENT_EVENT_NAME = "fb_aa_time_spent_on_view"
  const val AA_TIME_SPENT_SCREEN_PARAMETER_NAME = "fb_aa_time_spent_view_name"

  // The following are in app purchase related parameters.
  const val IAP_PRODUCT_ID = "fb_iap_product_id"
  const val IAP_PURCHASE_TIME = "fb_iap_purchase_time"
  const val IAP_PURCHASE_TOKEN = "fb_iap_purchase_token"
  const val IAP_PRODUCT_TYPE = "fb_iap_product_type"
  const val IAP_PRODUCT_TITLE = "fb_iap_product_title"
  const val IAP_PRODUCT_DESCRIPTION = "fb_iap_product_description"
  const val IAP_PACKAGE_NAME = "fb_iap_package_name"
  const val IAP_SUBSCRIPTION_AUTORENEWING = "fb_iap_subs_auto_renewing"
  const val IAP_SUBSCRIPTION_PERIOD = "fb_iap_subs_period"
  const val IAP_FREE_TRIAL_PERIOD = "fb_free_trial_period"
  const val IAP_INTRO_PRICE_AMOUNT_MICROS = "fb_intro_price_amount_micros"
  const val IAP_INTRO_PRICE_CYCLES = "fb_intro_price_cycles"
  // The following are product catalog related parameters.
  /**
   * Parameter key used to specify the unique ID for product item for
   * EVENT_NAME_PRODUCT_CATALOG_UPDATE event.
   */
  const val EVENT_PARAM_PRODUCT_ITEM_ID = "fb_product_item_id"

  /** Parameter key used to specify the availability for EVENT_NAME_PRODUCT_CATALOG_UPDATE event. */
  const val EVENT_PARAM_PRODUCT_AVAILABILITY = "fb_product_availability"

  /** Parameter key used to specify the condition for EVENT_NAME_PRODUCT_CATALOG_UPDATE event. */
  const val EVENT_PARAM_PRODUCT_CONDITION = "fb_product_condition"

  /**
   * Parameter key used to specify the product description for EVENT_NAME_PRODUCT_CATALOG_UPDATE
   * event.
   */
  const val EVENT_PARAM_PRODUCT_DESCRIPTION = "fb_product_description"

  /** Parameter key used to specify the image link for EVENT_NAME_PRODUCT_CATALOG_UPDATE event. */
  const val EVENT_PARAM_PRODUCT_IMAGE_LINK = "fb_product_image_link"

  /**
   * Parameter key used to specify the product item link for EVENT_NAME_PRODUCT_CATALOG_UPDATE
   * event.
   */
  const val EVENT_PARAM_PRODUCT_LINK = "fb_product_link"

  /**
   * Parameter key used to specify the product title for EVENT_NAME_PRODUCT_CATALOG_UPDATE event.
   */
  const val EVENT_PARAM_PRODUCT_TITLE = "fb_product_title"

  /**
   * Parameter key used to specify the gtin, mpn or brand for EVENT_NAME_PRODUCT_CATALOG_UPDATE
   * event.
   */
  const val EVENT_PARAM_PRODUCT_GTIN = "fb_product_gtin"
  const val EVENT_PARAM_PRODUCT_MPN = "fb_product_mpn"
  const val EVENT_PARAM_PRODUCT_BRAND = "fb_product_brand"

  /**
   * Parameter key used to specify the product price for EVENT_NAME_PRODUCT_CATALOG_UPDATE event.
   */
  const val EVENT_PARAM_PRODUCT_PRICE_AMOUNT = "fb_product_price_amount"

  /**
   * Parameter key used to specify the product price currecy for EVENT_NAME_PRODUCT_CATALOG_UPDATE
   * event.
   */
  const val EVENT_PARAM_PRODUCT_PRICE_CURRENCY = "fb_product_price_currency"

  @JvmStatic fun getDefaultAppEventsSessionTimeoutInSeconds(): Int = 60
}
