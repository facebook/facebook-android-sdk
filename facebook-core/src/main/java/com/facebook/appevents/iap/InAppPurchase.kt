package com.facebook.appevents.iap

import java.util.Currency

data class InAppPurchase(val eventName: String, val amount: Double, val currency: Currency)
