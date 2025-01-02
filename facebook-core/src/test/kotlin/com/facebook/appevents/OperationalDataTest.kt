package com.facebook.appevents

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.internal.Constants.IAP_PACKAGE_NAME
import com.facebook.appevents.internal.Constants.IAP_PRODUCT_DESCRIPTION
import com.facebook.appevents.internal.Constants.IAP_PRODUCT_ID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OperationalDataTest : FacebookPowerMockTestCase() {

    @Test
    fun testAddParameters() {
        val params = Bundle()
        val operationalData = OperationalData()
        val type = OperationalDataEnum.IAPParameters
        OperationalData.addParameter(
            type,
            IAP_PACKAGE_NAME,
            "package_name",
            params,
            operationalData
        )
        OperationalData.addParameter(type, IAP_PRODUCT_ID, "product_id", params, operationalData)
        OperationalData.addParameter(
            type,
            IAP_PRODUCT_DESCRIPTION,
            "product_description",
            params,
            operationalData
        )
        assertThat(operationalData.getParameter(type, IAP_PACKAGE_NAME)).isEqualTo("package_name")
        assertThat(operationalData.getParameter(type, IAP_PRODUCT_ID)).isEqualTo("product_id")
        assertThat(operationalData.getParameter(type, IAP_PRODUCT_DESCRIPTION)).isNull()
        assertThat(params.getCharSequence(IAP_PACKAGE_NAME)).isNull()
        assertThat(params.getCharSequence(IAP_PRODUCT_ID)).isEqualTo("product_id")
        assertThat(params.getCharSequence(IAP_PRODUCT_DESCRIPTION)).isEqualTo("product_description")
    }
}
