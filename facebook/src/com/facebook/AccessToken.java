/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;

final class AccessToken {
    static final String ACCESS_TOKEN_KEY = "access_token";
    static final String EXPIRES_IN_KEY = "expires_in";
    private static final Date MIN_DATE = new Date(Long.MIN_VALUE);
    private static final Date MAX_DATE = new Date(Long.MAX_VALUE);

    private final Date expires;
    private final List<String> permissions;
    private final String token;
    private final boolean isSSO;
    private final Date lastRefresh;

    private AccessToken(String token, Date expires, List<String> permissions, boolean isSSO, Date lastRefresh) {
        this.expires = expires;
        this.permissions = permissions;
        this.token = token;
        this.isSSO = isSSO;
        this.lastRefresh = lastRefresh;
    }

    String getToken() {
        return this.token;
    }

    Date getExpires() {
        return this.expires;
    }

    List<String> getPermissions() {
        return this.permissions;
    }

    boolean getIsSSO() {
        return this.isSSO;
    }

    Date getLastRefresh() {
        return this.lastRefresh;
    }

    static AccessToken createEmptyToken(List<String> permissions) {
        return new AccessToken("", MIN_DATE, permissions, false, MIN_DATE);
    }

    static AccessToken createFromString(String token, List<String> permissions) {
        return new AccessToken(token, MAX_DATE, permissions, false, new Date());
    }

    static AccessToken createFromDialog(List<String> requestedPermissions, Bundle bundle) {
        return createNew(requestedPermissions, bundle, false);
    }

    static AccessToken createFromSSO(List<String> requestedPermissions, Intent data) {
        return createNew(requestedPermissions, data.getExtras(), true);
    }

    static AccessToken createForRefresh(AccessToken current, Bundle bundle) {
        // isSSO is set true since only SSO tokens support refresh.
        return createNew(current.getPermissions(), bundle, true);
    }

    private static AccessToken createNew(List<String> requestedPermissions, Bundle bundle, boolean isSSO) {
        String token = bundle.getString(ACCESS_TOKEN_KEY);
        Date expires = getExpiresInDate(bundle);

        if (Utility.isNullOrEmpty(token) || (expires == null)) {
            return null;
        }

        return new AccessToken(token, expires, requestedPermissions, isSSO, new Date());
    }

    static AccessToken createFromCache(Bundle bundle) {
        // Copy the list so we can guarantee immutable
        List<String> originalPermissions = bundle.getStringArrayList(TokenCache.PERMISSIONS_KEY);
        List<String> permissions;
        if (originalPermissions == null) {
            permissions = Collections.emptyList();
        } else {
            permissions = Collections.unmodifiableList(new ArrayList<String>(originalPermissions));
        }

        return new AccessToken(bundle.getString(TokenCache.TOKEN_KEY), TokenCache.getDate(bundle,
                TokenCache.EXPIRATION_DATE_KEY), permissions, bundle.getBoolean(TokenCache.IS_SSO_KEY),
                TokenCache.getDate(bundle, TokenCache.LAST_REFRESH_DATE_KEY));
    }

    Bundle toCacheBundle() {
        Bundle bundle = new Bundle();

        bundle.putString(TokenCache.TOKEN_KEY, this.token);
        TokenCache.putDate(bundle, TokenCache.EXPIRATION_DATE_KEY, expires);
        bundle.putStringArrayList(TokenCache.PERMISSIONS_KEY, new ArrayList<String>(permissions));
        bundle.putBoolean(TokenCache.IS_SSO_KEY, isSSO);
        TokenCache.putDate(bundle, TokenCache.LAST_REFRESH_DATE_KEY, lastRefresh);

        return bundle;
    }

    boolean isInvalid() {
        return Utility.isNullOrEmpty(this.token) || new Date().after(this.expires);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("{AccessToken");
        builder.append(" token:").append(tokenToString());
        appendPermissions(builder);
        builder.append("}");

        return builder.toString();
    }

    private String tokenToString() {
        if (this.token == null) {
            return "null";
        } else if (Settings.isLoggingBehaviorEnabled(LoggingBehaviors.INCLUDE_ACCESS_TOKENS)) {
            return this.token;
        } else {
            return "ACCESS_TOKEN_REMOVED";
        }
    }

    private void appendPermissions(StringBuilder builder) {
        builder.append(" permissions:");
        if (this.permissions == null) {
            builder.append("null");
        } else {
            builder.append("[");
            for (int i = 0; i < this.permissions.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(this.permissions.get(i));
            }
        }
    }

    private static Date getExpiresInDate(Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        long secondsFromNow = bundle.getLong(EXPIRES_IN_KEY, Long.MIN_VALUE);
        if (secondsFromNow == Long.MIN_VALUE) {
            String numberString = bundle.getString(EXPIRES_IN_KEY);
            if (numberString == null) {
                return null;
            }

            try {
                secondsFromNow = Long.parseLong(numberString);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (secondsFromNow == 0) {
            return new Date(Long.MAX_VALUE);
        } else {
            return new Date(new Date().getTime() + (secondsFromNow * 1000L));
        }
    }
}
