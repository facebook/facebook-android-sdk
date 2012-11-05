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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import com.facebook.internal.Utility;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

final class AccessToken implements Serializable {
    private static final long serialVersionUID = 1L;
    static final String ACCESS_TOKEN_KEY = "access_token";
    static final String EXPIRES_IN_KEY = "expires_in";
    private static final Date MIN_DATE = new Date(Long.MIN_VALUE);
    private static final Date MAX_DATE = new Date(Long.MAX_VALUE);

    private final Date expires;
    private final List<String> permissions;
    private final String token;
    private final AccessTokenSource source;
    private final Date lastRefresh;

    AccessToken(String token, Date expires, List<String> permissions, AccessTokenSource source, Date lastRefresh) {
        if (permissions == null) {
            permissions = Collections.emptyList();
        }

        this.expires = expires;
        this.permissions = permissions;
        this.token = token;
        this.source = source;
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

    AccessTokenSource getSource() {
        return source;
    }

    Date getLastRefresh() {
        return this.lastRefresh;
    }

    static AccessToken createEmptyToken(List<String> permissions) {
        return new AccessToken("", MIN_DATE, permissions, AccessTokenSource.NONE, MIN_DATE);
    }

    static AccessToken createFromString(String token, List<String> permissions, AccessTokenSource source) {
        return new AccessToken(token, MAX_DATE, permissions, source, new Date());
    }

    static AccessToken createFromDialog(List<String> requestedPermissions, Bundle bundle) {
        return createNew(requestedPermissions, bundle, AccessTokenSource.WEB_VIEW, new Date());
    }

    static AccessToken createFromSSO(List<String> requestedPermissions, Intent data) {
        return createNew(requestedPermissions, data.getExtras(), AccessTokenSource.FACEBOOK_APPLICATION, new Date());
    }

    @SuppressLint("FieldGetter")
    static AccessToken createForRefresh(AccessToken current, Bundle bundle) {
        // Only tokens obtained via SSO support refresh. Token refresh returns the expiration date in
        // seconds from the epoch rather than seconds from now.
        assert current.source == AccessTokenSource.FACEBOOK_APPLICATION;
        return createNew(current.getPermissions(), bundle, AccessTokenSource.FACEBOOK_APPLICATION, new Date(0));
    }

    private static AccessToken createNew(List<String> requestedPermissions, Bundle bundle, AccessTokenSource source,
            Date expirationBase) {
        String token = bundle.getString(ACCESS_TOKEN_KEY);
        Date expires = getExpiresInDate(bundle, expirationBase);

        if (Utility.isNullOrEmpty(token) || (expires == null)) {
            return null;
        }

        return new AccessToken(token, expires, requestedPermissions, source, new Date());
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
                TokenCache.EXPIRATION_DATE_KEY), permissions, TokenCache.getSource(bundle),
                TokenCache.getDate(bundle, TokenCache.LAST_REFRESH_DATE_KEY));
    }

    Bundle toCacheBundle() {
        Bundle bundle = new Bundle();

        bundle.putString(TokenCache.TOKEN_KEY, this.token);
        TokenCache.putDate(bundle, TokenCache.EXPIRATION_DATE_KEY, expires);
        bundle.putStringArrayList(TokenCache.PERMISSIONS_KEY, new ArrayList<String>(permissions));
        bundle.putSerializable(TokenCache.TOKEN_SOURCE_KEY, source);
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

    private static class SerializationProxyV1 implements Serializable {
        private static final long serialVersionUID = -2488473066578201069L;
        private final Date expires;
        private final List<String> permissions;
        private final String token;
        private final AccessTokenSource source;
        private final Date lastRefresh;

        private SerializationProxyV1(String token, Date expires,
                List<String> permissions, AccessTokenSource source, Date lastRefresh) {
            this.expires = expires;
            this.permissions = permissions;
            this.token = token;
            this.source = source;
            this.lastRefresh = lastRefresh;
        }

        private Object readResolve() {
            return new AccessToken(token, expires, permissions, source, lastRefresh);
        }
    }

    private Object writeReplace() {
        return new SerializationProxyV1(token, expires, permissions, source, lastRefresh);
    }

    // have a readObject that throws to prevent spoofing
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Cannot readObject, serialization proxy required");
    }


    private static Date getExpiresInDate(Bundle bundle, Date expirationBase) {
        if (bundle == null) {
            return null;
        }

        long secondsFromBase = Long.MIN_VALUE;

        Object secondsObject = bundle.get(EXPIRES_IN_KEY);
        if (secondsObject instanceof Long) {
            secondsFromBase = (Long) secondsObject;
        } else if (secondsObject instanceof String) {
            try {
                secondsFromBase = Long.parseLong((String) secondsObject);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }

        if (secondsFromBase == 0) {
            return new Date(Long.MAX_VALUE);
        } else {
            return new Date(expirationBase.getTime() + (secondsFromBase * 1000L));
        }
    }
}
