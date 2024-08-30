package com.facebook.appevents.iap

object InAppPurchaseConstants {
    const val PRODUCT_ID = "productId"
    const val PACKAGE_NAME = "packageName"


    /**
     * Google Play Billing Library V2 - V7
     */
    // Class names
    const val CLASSNAME_BILLING_CLIENT = "com.android.billingclient.api.BillingClient"
    const val CLASSNAME_PURCHASE = "com.android.billingclient.api.Purchase"
    const val CLASSNAME_PURCHASE_HISTORY_RECORD =
        "com.android.billingclient.api.PurchaseHistoryRecord"
    const val CLASSNAME_BILLING_RESULT = "com.android.billingclient.api.BillingResult"
    const val CLASSNAME_BILLING_CLIENT_BUILDER =
        "com.android.billingclient.api.BillingClient\$Builder"

    // Class names: Listeners
    const val CLASSNAME_BILLING_CLIENT_STATE_LISTENER =
        "com.android.billingclient.api.BillingClientStateListener"
    const val CLASSNAME_PURCHASE_HISTORY_RESPONSE_LISTENER =
        "com.android.billingclient.api.PurchaseHistoryResponseListener"

    // Method names
    const val METHOD_GET_RESPONSE_CODE = "getResponseCode"
    const val METHOD_GET_ORIGINAL_JSON = "getOriginalJson"
    const val METHOD_QUERY_PURCHASE_HISTORY_ASYNC = "queryPurchaseHistoryAsync"
    const val METHOD_NEW_BUILDER = "newBuilder"
    const val METHOD_BUILD = "build"
    const val METHOD_ENABLE_PENDING_PURCHASES = "enablePendingPurchases"
    const val METHOD_SET_LISTENER = "setListener"
    const val METHOD_START_CONNECTION = "startConnection"

    // Method names: Listeners
    const val METHOD_ON_BILLING_SETUP_FINISHED = "onBillingSetupFinished"
    const val METHOD_ON_BILLING_SERVICE_DISCONNECTED = "onBillingServiceDisconnected"
    const val METHOD_ON_PURCHASE_HISTORY_RESPONSE = "onPurchaseHistoryResponse"


    /**
     * Google Play Billing Library V2 - V4
     */
    // Class names
    const val CLASSNAME_PURCHASES_RESULT =
        "com.android.billingclient.api.Purchase\$PurchasesResult"
    const val CLASSNAME_SKU_DETAILS = "com.android.billingclient.api.SkuDetails"

    // Class names: Listeners
    const val CLASSNAME_SKU_DETAILS_RESPONSE_LISTENER =
        "com.android.billingclient.api.SkuDetailsResponseListener"
    const val CLASSNAME_PURCHASE_UPDATED_LISTENER =
        "com.android.billingclient.api.PurchasesUpdatedListener"

    // Method names
    const val METHOD_QUERY_PURCHASES = "queryPurchases"
    const val METHOD_GET_PURCHASE_LIST = "getPurchasesList"
    const val METHOD_QUERY_SKU_DETAILS_ASYNC = "querySkuDetailsAsync"

    // Method names: Listeners
    const val METHOD_ON_SKU_DETAILS_RESPONSE = "onSkuDetailsResponse"


    /**
     * Google Play Billing Library V5 - V7
     */
    // Class names
    const val CLASSNAME_PRODUCT_DETAILS = "com.android.billingclient.api.ProductDetails"
    const val CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT =
        "com.android.billingclient.api.QueryProductDetailsParams\$Product"

    // Class names: Listeners
    const val CLASSNAME_PURCHASES_UPDATED_LISTENER =
        "com.android.billingclient.api.PurchasesUpdatedListener"
    const val CLASSNAME_PRODUCT_DETAILS_RESPONSE_LISTENER =
        "com.android.billingclient.api.ProductDetailsResponseListener"
    const val CLASSNAME_PURCHASES_RESPONSE_LISTENER =
        "com.android.billingclient.api.PurchasesResponseListener"
    
    // Class names: Params
    const val CLASSNAME_PENDING_PURCHASES_PARAMS =
        "com.android.billingclient.api.PendingPurchasesParams"
    const val CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS =
        "com.android.billingclient.api.QueryProductDetailsParams"
    const val CLASSNAME_QUERY_PURCHASE_HISTORY_PARAMS =
        "com.android.billingclient.api.QueryPurchaseHistoryParams"
    const val CLASSNAME_QUERY_PURCHASES_PARAMS =
        "com.android.billingclient.api.QueryPurchasesParams"

    // Class names: Builders
    const val CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_BUILDER =
        "com.android.billingclient.api.QueryProductDetailsParams\$Builder"
    const val CLASSNAME_QUERY_PURCHASE_HISTORY_PARAMS_BUILDER =
        "com.android.billingclient.api.QueryPurchaseHistoryParams\$Builder"
    const val CLASSNAME_QUERY_PURCHASES_PARAMS_BUILDER =
        "com.android.billingclient.api.QueryPurchasesParams\$Builder"
    const val CLASSNAME_PENDING_PURCHASES_PARAMS_BUILDER =
        "com.android.billingclient.api.PendingPurchasesParams\$Builder"
    const val CLASSNAME_QUERY_PRODUCT_DETAILS_PARAMS_PRODUCT_BUILDER =
        "com.android.billingclient.api.QueryProductDetailsParams\$Product\$Builder"

    // Method names
    const val METHOD_SET_PRODUCT_ID = "setProductId"
    const val METHOD_SET_PRODUCT_TYPE = "setProductType"
    const val METHOD_SET_PRODUCT_LIST = "setProductList"
    const val METHOD_GET_DEBUG_MESSAGE = "getDebugMessage"
    const val METHOD_TO_STRING = "toString"
    const val METHOD_QUERY_PURCHASES_ASYNC = "queryPurchasesAsync"
    const val METHOD_QUERY_PRODUCT_DETAILS_ASYNC = "queryProductDetailsAsync"

    // Method names: Listeners
    const val METHOD_ON_QUERY_PURCHASES_RESPONSE = "onQueryPurchasesResponse"
    const val METHOD_ON_PRODUCT_DETAILS_RESPONSE = "onProductDetailsResponse"

}
