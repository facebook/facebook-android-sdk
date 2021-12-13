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
package com.facebook.appevents.internal

object Constants {
  const val LOG_TIME_APP_EVENT_KEY = "_logTime"
  const val EVENT_NAME_EVENT_KEY = "_eventName"
  const val EVENT_NAME_MD5_EVENT_KEY = "_eventName_md5"

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
