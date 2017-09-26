/**
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

package com.facebook.appevents.internal;

public class Constants {
    public static final String LOG_TIME_APP_EVENT_KEY = "_logTime";
    public static final String EVENT_NAME_EVENT_KEY = "_eventName";
    public static final String EVENT_NAME_MD5_EVENT_KEY = "_eventName_md5";

    // The following are for Automatic Analytics events and parameters
    public static final String AA_TIME_SPENT_EVENT_NAME = "fb_aa_time_spent_on_view";
    public static final String AA_TIME_SPENT_SCREEN_PARAMETER_NAME = "fb_aa_time_spent_view_name";
    public static final String IAP_PRODUCT_ID = "fb_iap_product_id";
    public static final String IAP_PURCHASE_TIME = "fb_iap_purchase_time";
    public static final String IAP_PURCHASE_STATE = "fb_iap_purchase_state";
    public static final String IAP_PURCHASE_TOKEN = "fb_iap_purchase_token";
    public static final String IAP_PRODUCT_TYPE = "fb_iap_product_type";
    public static final String IAP_PRODUCT_TITLE = "fb_iap_product_title";
    public static final String IAP_PRODUCT_DESCRIPTION = "fb_iap_product_description";
    public static final String IAP_PACKAGE_NAME = "fb_iap_package_name";

    public static int getDefaultAppEventsSessionTimeoutInSeconds() {
        return 60;
    }
}
