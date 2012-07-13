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
    // TODO port: isSSO
    // TODO port: lastRefresh
    	
    private AccessToken(String token, Date expires, List<String> permissions) {
        this.expires = expires;
        this.permissions = permissions;
        this.token = token;
    }
    	
    String getToken() { return this.token; }
    Date getExpires() { return this.expires; }
    List<String> getPermissions() { return this.permissions; }

    static AccessToken createEmptyToken(List<String> permissions) {
        return new AccessToken("", MIN_DATE, permissions);
    }

    static AccessToken createFromString(String token, List<String> permissions) {
        return new AccessToken(token, MAX_DATE, permissions);
    }
    
    static AccessToken createFromDialog(List<String> requestedPermissions, Bundle bundle) {
        String token = bundle.getString(ACCESS_TOKEN_KEY);
        Date expires = Utility.getBundleStringSecondsFromNow(bundle, EXPIRES_IN_KEY);

        if (Utility.isNullOrEmpty(token) || (expires == null)) {
            return null;
        }

        return new AccessToken(token, expires, requestedPermissions);
    }

    static AccessToken createFromSSO(List<String> requestedPermissions, Intent data) {
        // Dialogs and SSO happen to return the same format.
        return createFromDialog(requestedPermissions, data.getExtras());
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

        return new AccessToken(
                bundle.getString(TokenCache.TOKEN_KEY),
                Utility.getBundleDate(
                        bundle,
                        TokenCache.EXPIRATION_DATE_KEY),
                permissions);
    }

    Bundle toCacheBundle() {
        Bundle bundle = new Bundle();

        bundle.putString(TokenCache.TOKEN_KEY, this.token);
        Utility.putBundleDate(
                bundle,
                TokenCache.EXPIRATION_DATE_KEY,
                this.expires);
        bundle.putStringArrayList(TokenCache.PERMISSIONS_KEY, new ArrayList<String>(this.permissions));

        return bundle;
    }
    
    boolean isInvalid() {
        return Utility.isNullOrEmpty(this.token) || new Date().after(this.expires);
    }

    @Override public String toString() {
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
}
