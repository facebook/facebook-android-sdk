package com.facebook.appevents.iap

interface InAppPurchaseBillingClientWrapper {
    val billingClient: Any
    fun queryPurchases(productType: InAppPurchaseUtils.IAPProductType, runnable: Runnable)
    fun queryPurchaseHistory(productType: InAppPurchaseUtils.IAPProductType, runnable: Runnable)
}
