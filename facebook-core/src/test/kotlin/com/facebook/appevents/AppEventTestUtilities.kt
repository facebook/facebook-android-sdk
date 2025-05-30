/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.os.Bundle
import com.facebook.FacebookTestUtility.assertEqualContentsWithoutOrder
import org.mockito.ArgumentMatcher
import java.util.UUID

object AppEventTestUtilities {
    fun getTestAppEvent(): AppEvent {
        val customParams = Bundle()
        customParams.putString("key1", "value1")
        customParams.putString("key2", "value2")
        val operationalParams = OperationalData()
        operationalParams.addParameter(OperationalDataEnum.IAPParameters, "key3", "value3")
        operationalParams.addParameter(OperationalDataEnum.IAPParameters, "key4", "value4")

        val appEvent =
            AppEvent(
                "contextName",
                "eventName",
                1.0,
                customParams,
                false,
                false,
                UUID.fromString("65565271-1ace-4580-bd13-b2bc6d0df035"),
                operationalParams
            )
        return appEvent
    }

    class BundleMatcher(private val wanted: Bundle) : ArgumentMatcher<Bundle> {
        override fun matches(bundle: Bundle?): Boolean {
            if (bundle == null) {
                return false
            }
            assertEqualContentsWithoutOrder(wanted, bundle)
            return true
        }
    }
}
