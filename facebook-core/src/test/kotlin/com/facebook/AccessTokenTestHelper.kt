/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.os.Bundle

object AccessTokenTestHelper {
  fun toLegacyCacheBundle(accessToken: AccessToken): Bundle {
    val bundle = Bundle()
    LegacyTokenHelper.putToken(bundle, accessToken.token)
    LegacyTokenHelper.putExpirationDate(bundle, accessToken.expires)
    LegacyTokenHelper.putPermissions(bundle, accessToken.permissions as Set<String>)
    LegacyTokenHelper.putDeclinedPermissions(bundle, accessToken.declinedPermissions as Set<String>)
    LegacyTokenHelper.putExpiredPermissions(bundle, accessToken.expiredPermissions as Set<String>)
    LegacyTokenHelper.putSource(bundle, accessToken.source)
    LegacyTokenHelper.putLastRefreshDate(bundle, accessToken.lastRefresh)
    LegacyTokenHelper.putApplicationId(bundle, accessToken.applicationId)
    return bundle
  }
}
