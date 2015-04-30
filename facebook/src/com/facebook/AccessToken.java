/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * This class represents an immutable access token for using Facebook APIs. It also includes
 * associated metadata such as expiration date and permissions.
 * <p/>
 * For more information on access tokens, see
 * <a href="https://developers.facebook.com/docs/facebook-login/access-tokens/">Access Tokens</a>.
 */
public final class AccessToken implements Parcelable {
    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String EXPIRES_IN_KEY = "expires_in";
    public static final String USER_ID_KEY = "user_id";

    private static final Date MAX_DATE = new Date(Long.MAX_VALUE);
    private static final Date DEFAULT_EXPIRATION_TIME = MAX_DATE;
    private static final Date DEFAULT_LAST_REFRESH_TIME = new Date();
    private static final AccessTokenSource DEFAULT_ACCESS_TOKEN_SOURCE =
            AccessTokenSource.FACEBOOK_APPLICATION_WEB;

    // Constants related to JSON serialization.
    private static final int CURRENT_JSON_FORMAT = 1;
    private static final String VERSION_KEY = "version";
    private static final String EXPIRES_AT_KEY = "expires_at";
    private static final String PERMISSIONS_KEY = "permissions";
    private static final String DECLINED_PERMISSIONS_KEY = "declined_permissions";
    private static final String TOKEN_KEY = "token";
    private static final String SOURCE_KEY = "source";
    private static final String LAST_REFRESH_KEY = "last_refresh";
    private static final String APPLICATION_ID_KEY = "application_id";

    private final Date expires;
    private final Set<String> permissions;
    private final Set<String> declinedPermissions;
    private final String token;
    private final AccessTokenSource source;
    private final Date lastRefresh;
    private final String applicationId;
    private final String userId;

    /**
     * Creates a new AccessToken using the supplied information from a previously-obtained access
     * token (for instance, from an already-cached access token obtained prior to integration with
     * the Facebook SDK). Note that the caller is asserting that all parameters provided are correct
     * with respect to the access token string; no validation is done to verify they are correct.
     *
     * @param accessToken         the access token string obtained from Facebook
     * @param applicationId       the ID of the Facebook Application associated with this access
     *                            token
     * @param userId              the id of the user
     * @param permissions         the permissions that were requested when the token was obtained
     *                            (or when it was last reauthorized); may be null if permission set
     *                            is unknown
     * @param declinedPermissions the permissions that were declined when the token was obtained;
     *                            may be null if permission set is unknown
     * @param accessTokenSource   an enum indicating how the token was originally obtained (in most
     *                            cases, this will be either AccessTokenSource.FACEBOOK_APPLICATION
     *                            or AccessTokenSource.WEB_VIEW); if null, FACEBOOK_APPLICATION is
     *                            assumed.
     * @param expirationTime      the expiration date associated with the token; if null, an
     *                            infinite expiration time is assumed (but will become correct when
     *                            the token is refreshed)
     * @param lastRefreshTime     the last time the token was refreshed (or when it was first
     *                            obtained); if null, the current time is used.
     */
    public AccessToken(
            final String accessToken,
            final String applicationId,
            final String userId,
            @Nullable
            final Collection<String> permissions,
            @Nullable
            final Collection<String> declinedPermissions,
            @Nullable
            final AccessTokenSource accessTokenSource,
            @Nullable
            final Date expirationTime,
            @Nullable
            final Date lastRefreshTime
    ) {
        Validate.notNullOrEmpty(accessToken, "accessToken");
        Validate.notNullOrEmpty(applicationId, "applicationId");
        Validate.notNullOrEmpty(userId, "userId");

        this.expires = expirationTime != null ? expirationTime : DEFAULT_EXPIRATION_TIME;
        this.permissions = Collections.unmodifiableSet(
                permissions != null ? new HashSet<String>(permissions) : new HashSet<String>());
        this.declinedPermissions = Collections.unmodifiableSet(
                declinedPermissions != null
                        ? new HashSet<String>(declinedPermissions)
                        : new HashSet<String>());
        this.token = accessToken;
        this.source = accessTokenSource != null ? accessTokenSource : DEFAULT_ACCESS_TOKEN_SOURCE;
        this.lastRefresh = lastRefreshTime != null ? lastRefreshTime : DEFAULT_LAST_REFRESH_TIME;
        this.applicationId = applicationId;
        this.userId = userId;
    }

    /**
     * Getter for the access token that is current for the application.
     *
     * @return The access token that is current for the application.
     */
    public static AccessToken getCurrentAccessToken() {
        return AccessTokenManager.getInstance().getCurrentAccessToken();
    }

    /**
     * Setter for the access token that is current for the application.
     *
     * @param accessToken The access token to set.
     */
    public static void setCurrentAccessToken(AccessToken accessToken) {
        AccessTokenManager.getInstance().setCurrentAccessToken(accessToken);
    }

    /**
     * Updates the current access token with up to date permissions,
     * and extends the expiration date, if extension is possible.
     *
     * This function must be run from the UI thread.
     */
    public static void refreshCurrentAccessTokenAsync() {
        AccessTokenManager.getInstance().refreshCurrentAccessToken();
    }

    /**
     * Gets the string representing the access token.
     *
     * @return the string representing the access token
     */
    public String getToken() {
        return this.token;
    }

    /**
     * Gets the date at which the access token expires.
     *
     * @return the expiration date of the token
     */
    public Date getExpires() {
        return this.expires;
    }

    /**
     * Gets the list of permissions associated with this access token. Note that the most up-to-date
     * list of permissions is maintained by Facebook, so this list may be outdated if
     * permissions have been added or removed since the time the AccessToken object was created. For
     * more information on permissions, see
     * https://developers.facebook.com/docs/reference/login/#permissions.
     *
     * @return a read-only list of strings representing the permissions granted via this access
     * token
     */
    public Set<String> getPermissions() {
        return this.permissions;
    }

    /**
     * Gets the list of permissions declined by the user with this access token.  It represents the
     * entire set of permissions that have been requested and declined.  Note that the most
     * up-to-date list of permissions is maintained by Facebook, so this list may be
     * outdated if permissions have been granted or declined since the last time an AccessToken
     * object was created.
     *
     * @return a read-only list of strings representing the permissions declined by the user
     */
    public Set<String> getDeclinedPermissions() {
        return this.declinedPermissions;
    }

    /**
     * Gets the {@link AccessTokenSource} indicating how this access token was obtained.
     *
     * @return the enum indicating how the access token was obtained
     */
    public AccessTokenSource getSource() {
        return source;
    }

    /**
     * Gets the date at which the token was last refreshed. Since tokens expire, the Facebook SDK
     * will attempt to renew them periodically.
     *
     * @return the date at which this token was last refreshed
     */
    public Date getLastRefresh() {
        return this.lastRefresh;
    }

    /**
     * Gets the ID of the Facebook Application associated with this access token.
     *
     * @return the application ID
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Returns the user id for this access token.
     *
     * @return The user id for this access token.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * A callback for creating an access token from a NativeLinkingIntent
     */
    public interface AccessTokenCreationCallback {
        /**
         * The method called on a successful creation of an AccessToken.
         *
         * @param token the access token created from the native link intent.
         */
        public void onSuccess(AccessToken token);

        public void onError(FacebookException error);
    }

    /**
     * Creates a new AccessToken using the information contained in an Intent populated by the
     * Facebook application in order to launch a native link. For more information on native
     * linking, please see https://developers.facebook.com/docs/mobile/android/deep_linking/.
     *
     * @param intent        the Intent that was used to start an Activity; must not be null
     * @param applicationId the ID of the Facebook Application associated with this access token
     */
    public static void createFromNativeLinkingIntent(
            Intent intent,
            final String applicationId,
            final AccessTokenCreationCallback accessTokenCallback) {
        Validate.notNull(intent, "intent");
        if (intent.getExtras() == null) {
            accessTokenCallback.onError(
                    new FacebookException("No extras found on intent"));
            return;
        }
        final Bundle extras = new Bundle(intent.getExtras());

        String accessToken = extras.getString(ACCESS_TOKEN_KEY);
        if (accessToken == null || accessToken.isEmpty()) {
            accessTokenCallback.onError(new FacebookException("No access token found on intent"));
            return;
        }

        String userId = extras.getString(USER_ID_KEY);
        // Old versions of facebook for android don't provide the UserId. Obtain the id if missing
        if (userId == null || userId.isEmpty()) {
            Utility.getGraphMeRequestWithCacheAsync(accessToken,
                new Utility.GraphMeRequestWithCacheCallback() {
                    @Override
                    public void onSuccess(JSONObject userInfo) {
                        try {
                            String userId = userInfo.getString("id");
                            extras.putString(USER_ID_KEY, userId);
                            accessTokenCallback.onSuccess(createFromBundle(
                                    null,
                                    extras,
                                    AccessTokenSource.FACEBOOK_APPLICATION_WEB,
                                    new Date(),
                                    applicationId));
                        } catch (JSONException ex) {
                            accessTokenCallback.onError(
                                    new FacebookException(
                                        "Unable to generate access token due to missing user id"));
                        }

                    }

                    @Override
                    public void onFailure(FacebookException error) {
                        accessTokenCallback.onError(error);
                    }
                });
        } else {
            accessTokenCallback.onSuccess(createFromBundle(
                    null,
                    extras,
                    AccessTokenSource.FACEBOOK_APPLICATION_WEB,
                    new Date(),
                    applicationId));
        }
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof AccessToken)) {
            return false;
        }

        AccessToken o = (AccessToken) other;

        return expires.equals(o.expires) &&
                permissions.equals(o.permissions) &&
                declinedPermissions.equals(o.declinedPermissions) &&
                token.equals(o.token) &&
                source == o.source &&
                lastRefresh.equals(o.lastRefresh) &&
                (applicationId == null ?
                        o.applicationId == null :
                        applicationId.equals(o.applicationId)) &&
                userId.equals(o.userId);
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 31 + expires.hashCode();
        result = result * 31 + permissions.hashCode();
        result = result * 31 + declinedPermissions.hashCode();
        result = result * 31 + token.hashCode();
        result = result * 31 + source.hashCode();
        result = result * 31 + lastRefresh.hashCode();
        result = result * 31 + (applicationId == null ? 0 : applicationId.hashCode());
        result = result * 31 + userId.hashCode();

        return result;
    }

    @SuppressLint("FieldGetter")
    static AccessToken createFromRefresh(AccessToken current, Bundle bundle) {
        // Only tokens obtained via SSO support refresh. Token refresh returns the expiration date
        // in seconds from the epoch rather than seconds from now.
        if (current.source != AccessTokenSource.FACEBOOK_APPLICATION_WEB &&
                current.source != AccessTokenSource.FACEBOOK_APPLICATION_NATIVE &&
                current.source != AccessTokenSource.FACEBOOK_APPLICATION_SERVICE) {
            throw new FacebookException("Invalid token source: " + current.source);
        }

        Date expires = Utility.getBundleLongAsDate(bundle, EXPIRES_IN_KEY, new Date(0));
        String token = bundle.getString(ACCESS_TOKEN_KEY);

        if (Utility.isNullOrEmpty(token)) {
            return null;
        }
        return new AccessToken(
                token,
                current.applicationId,
                current.getUserId(),
                current.getPermissions(),
                current.getDeclinedPermissions(),
                current.source,
                expires,
                new Date());
    }

    static AccessToken createFromLegacyCache(Bundle bundle) {
        List<String> permissions = getPermissionsFromBundle(
                bundle,
                LegacyTokenHelper.PERMISSIONS_KEY);
        List<String> declinedPermissions = getPermissionsFromBundle(
                bundle,
                LegacyTokenHelper.DECLINED_PERMISSIONS_KEY);

        String applicationId = LegacyTokenHelper.getApplicationId(bundle);
        if (Utility.isNullOrEmpty(applicationId)) {
            applicationId = FacebookSdk.getApplicationId();
        }

        String tokenString = LegacyTokenHelper.getToken(bundle);
        String userId;
        JSONObject userInfo = Utility.awaitGetGraphMeRequestWithCache(tokenString);
        try {
            userId = userInfo.getString("id");
        } catch (JSONException ex) {
            // This code is only used by AccessTokenCache. If we for any reason fail to get the
            // user id just return null.
            return null;
        }

        return new AccessToken(
                tokenString,
                applicationId,
                userId,
                permissions,
                declinedPermissions,
                LegacyTokenHelper.getSource(bundle),
                LegacyTokenHelper.getDate(
                        bundle,
                        LegacyTokenHelper.EXPIRATION_DATE_KEY),
                LegacyTokenHelper.getDate(
                        bundle,
                        LegacyTokenHelper.LAST_REFRESH_DATE_KEY)
        );
    }

    static List<String> getPermissionsFromBundle(Bundle bundle, String key) {
        // Copy the list so we can guarantee immutable
        List<String> originalPermissions = bundle.getStringArrayList(key);
        List<String> permissions;
        if (originalPermissions == null) {
            permissions = Collections.emptyList();
        } else {
            permissions = Collections.unmodifiableList(new ArrayList<String>(originalPermissions));
        }
        return permissions;
    }

    /**
     * Shows if the token is expired.
     *
     * @return true if the token is expired.
     */
    public boolean isExpired() {
        return new Date().after(this.expires);
    }

    JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(VERSION_KEY, CURRENT_JSON_FORMAT);
        jsonObject.put(TOKEN_KEY, token);
        jsonObject.put(EXPIRES_AT_KEY, expires.getTime());
        JSONArray permissionsArray = new JSONArray(permissions);
        jsonObject.put(PERMISSIONS_KEY, permissionsArray);
        JSONArray declinedPermissionsArray = new JSONArray(declinedPermissions);
        jsonObject.put(DECLINED_PERMISSIONS_KEY, declinedPermissionsArray);
        jsonObject.put(LAST_REFRESH_KEY, lastRefresh.getTime());
        jsonObject.put(SOURCE_KEY, source.name());
        jsonObject.put(APPLICATION_ID_KEY, applicationId);
        jsonObject.put(USER_ID_KEY, userId);

        return jsonObject;
    }

    static AccessToken createFromJSONObject(JSONObject jsonObject) throws JSONException {
        int version = jsonObject.getInt(VERSION_KEY);
        if (version > CURRENT_JSON_FORMAT) {
            throw new FacebookException("Unknown AccessToken serialization format.");
        }

        String token = jsonObject.getString(TOKEN_KEY);
        Date expiresAt = new Date(jsonObject.getLong(EXPIRES_AT_KEY));
        JSONArray permissionsArray = jsonObject.getJSONArray(PERMISSIONS_KEY);
        JSONArray declinedPermissionsArray = jsonObject.getJSONArray(DECLINED_PERMISSIONS_KEY);
        Date lastRefresh = new Date(jsonObject.getLong(LAST_REFRESH_KEY));
        AccessTokenSource source = AccessTokenSource.valueOf(jsonObject.getString(SOURCE_KEY));
        String applicationId = jsonObject.getString(APPLICATION_ID_KEY);
        String userId = jsonObject.getString(USER_ID_KEY);

        return new AccessToken(
                token,
                applicationId,
                userId,
                Utility.jsonArrayToStringList(permissionsArray),
                Utility.jsonArrayToStringList(declinedPermissionsArray),
                source,
                expiresAt,
                lastRefresh);
    }

    private static AccessToken createFromBundle(
            List<String> requestedPermissions,
            Bundle bundle,
            AccessTokenSource source,
            Date expirationBase,
            String applicationId) {
        String token = bundle.getString(ACCESS_TOKEN_KEY);
        Date expires = Utility.getBundleLongAsDate(bundle, EXPIRES_IN_KEY, expirationBase);
        String userId = bundle.getString(USER_ID_KEY);

        if (Utility.isNullOrEmpty(token) || (expires == null)) {
            return null;
        }

        return new AccessToken(
                token,
                applicationId,
                userId,
                requestedPermissions,
                null,
                source,
                expires,
                new Date());
    }

    private String tokenToString() {
        if (this.token == null) {
            return "null";
        } else if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.INCLUDE_ACCESS_TOKENS)) {
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
            builder.append(TextUtils.join(", ", permissions));
            builder.append("]");
        }
    }

    AccessToken(Parcel parcel) {
        this.expires = new Date(parcel.readLong());
        ArrayList<String> permissionsList = new ArrayList<>();
        parcel.readStringList(permissionsList);
        this.permissions = Collections.unmodifiableSet(new HashSet<String>(permissionsList));
        permissionsList.clear();
        parcel.readStringList(permissionsList);
        this.declinedPermissions = Collections.unmodifiableSet(
                new HashSet<String>(permissionsList));
        this.token = parcel.readString();
        this.source = AccessTokenSource.valueOf(parcel.readString());
        this.lastRefresh = new Date(parcel.readLong());
        this.applicationId = parcel.readString();
        this.userId = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(expires.getTime());
        dest.writeStringList(new ArrayList<String>(permissions));
        dest.writeStringList(new ArrayList<String>(declinedPermissions));
        dest.writeString(token);
        dest.writeString(source.name());
        dest.writeLong(lastRefresh.getTime());
        dest.writeString(applicationId);
        dest.writeString(userId);
    }

    public static final Parcelable.Creator<AccessToken> CREATOR = new Parcelable.Creator() {

        @Override
        public AccessToken createFromParcel(Parcel source) {
            return new AccessToken(source);
        }

        @Override
        public AccessToken[] newArray(int size) {
            return new AccessToken[size];
        }
    };
}
