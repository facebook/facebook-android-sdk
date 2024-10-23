package com.facebook.appevents.iap

import java.math.BigDecimal
import java.util.Currency

data class InAppPurchase(val amount: BigDecimal, val currency: Currency)
