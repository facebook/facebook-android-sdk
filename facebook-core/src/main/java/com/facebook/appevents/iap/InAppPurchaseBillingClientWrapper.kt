package com.facebook.appevents.iap

interface InAppPurchaseBillingClientWrapper {
    val billingClient: Any
    fun queryPurchases(productType: InAppPurchaseUtils.IAPProductType, completionHandler: Runnable)
    fun queryPurchaseHistory(
        productType: InAppPurchaseUtils.IAPProductType,
        completionHandler: Runnable
    )
}
