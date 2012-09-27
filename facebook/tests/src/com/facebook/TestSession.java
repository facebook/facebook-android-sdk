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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class TestSession extends Session {
    private enum Mode {
        PRIVATE, SHARED
    }

    private static final String LOG_TAG = SdkRuntime.LOG_TAG_BASE + "TestSession";

    private static Map<String, TestAccount> appTestAccounts;
    private static String testApplicationSecret;
    private static String testApplicationId;
    private static String machineUniqueUserTag;

    private final String sessionUniqueUserTag;
    private final List<String> requestedPermissions;
    private final Mode mode;
    private String testAccountId;

    private boolean wasAskedToExtendAccessToken;

    // This is necessary because the parent class implements Externalizable
    public TestSession() {
        sessionUniqueUserTag = null;
        requestedPermissions = null;
        mode = null;
    }

    protected TestSession(Activity activity, List<String> permissions, TokenCache tokenCache,
            String machineUniqueUserTag, String sessionUniqueUserTag, Mode mode) {
        super(activity, TestSession.testApplicationId, permissions, tokenCache);

        Validate.notNull(permissions, "permissions");

        // Validate these as if they were arguments even though they are statics.
        Validate.notNullOrEmpty(testApplicationId, "testApplicationId");
        Validate.notNullOrEmpty(testApplicationSecret, "testApplicationSecret");

        this.sessionUniqueUserTag = sessionUniqueUserTag;
        this.mode = mode;
        this.requestedPermissions = permissions;
    }

    public static TestSession createSessionWithPrivateUser(Activity activity, List<String> permissions) {
        return createTestSession(activity, permissions, Mode.PRIVATE, null);
    }

    public static TestSession createSessionWithSharedUser(Activity activity, List<String> permissions) {
        return createSessionWithSharedUser(activity, permissions, null);
    }

    public static TestSession createSessionWithSharedUser(Activity activity, List<String> permissions,
            String sessionUniqueUserTag) {
        return createTestSession(activity, permissions, Mode.SHARED, sessionUniqueUserTag);
    }

    public static final String getAppAccessToken() {
        return testApplicationId + "|" + testApplicationSecret;
    }

    public static synchronized String getTestApplicationId() {
        return testApplicationId;
    }

    public static synchronized void setTestApplicationId(String value) {
        if (testApplicationId != null && !testApplicationId.equals(value)) {
            throw new FacebookException("Can't have more than one test application ID");
        }
        testApplicationId = value;
    }

    public static synchronized String getTestApplicationSecret() {
        return testApplicationSecret;
    }

    public static synchronized void setTestApplicationSecret(String value) {
        if (testApplicationSecret != null && !testApplicationSecret.equals(value)) {
            throw new FacebookException("Can't have more than one test application secret");
        }
        testApplicationSecret = value;
    }

    public static synchronized String getMachineUniqueUserTag() {
        return machineUniqueUserTag;
    }

    public static synchronized void setMachineUniqueUserTag(String value) {
        if (machineUniqueUserTag != null && !machineUniqueUserTag.equals(value)) {
            throw new FacebookException("Can't have more than one machine-unique user tag");
        }
        machineUniqueUserTag = value;
    }

    private static synchronized TestSession createTestSession(Activity activity, List<String> permissions, Mode mode,
            String sessionUniqueUserTag) {
        if (Utility.isNullOrEmpty(testApplicationId) || Utility.isNullOrEmpty(testApplicationSecret)) {
            throw new FacebookException("Must provide app ID and secret");
        }

        if (Utility.isNullOrEmpty(permissions)) {
            permissions = Arrays.asList("email", "publish_actions");
        }

        return new TestSession(activity, permissions, new TestTokenCache(), machineUniqueUserTag, sessionUniqueUserTag,
                mode);
    }

    private static synchronized void retrieveTestAccountsForAppIfNeeded() {
        if (appTestAccounts != null) {
            return;
        }

        appTestAccounts = new HashMap<String, TestAccount>();

        // The data we need is split across two different FQL tables. We construct two queries, submit them
        // together (the second one refers to the first one), then cross-reference the results.

        // Get the test accounts for this app.
        String testAccountQuery = String.format("SELECT id,access_token FROM test_account WHERE app_id = %s",
                testApplicationId);
        // Get the user names for those accounts.
        String userQuery = "SELECT uid,name FROM user WHERE uid IN (SELECT id FROM #test_accounts)";

        Bundle parameters = new Bundle();

        // Build a JSON string that contains our queries and pass it as the 'q' parameter of the query.
        JSONObject multiquery;
        try {
            multiquery = new JSONObject();
            multiquery.put("test_accounts", testAccountQuery);
            multiquery.put("users", userQuery);
        } catch (JSONException exception) {
            throw new FacebookException(exception);
        }
        parameters.putString("q", multiquery.toString());

        // We need to authenticate as this app.
        parameters.putString("access_token", getAppAccessToken());

        Request request = new Request(null, "fql", parameters, null);
        Response response = request.execute();

        if (response.getError() != null) {
            throw response.getError();
        }

        FqlResponse fqlResponse = response.getGraphObjectAs(FqlResponse.class);

        GraphObjectList<FqlResult> fqlResults = fqlResponse.getData();
        if (fqlResults == null || fqlResults.size() != 2) {
            throw new FacebookException("Unexpected number of results from FQL query");
        }

        // We get back two sets of results. The first is from the test_accounts query, the second from the users query.
        Collection<TestAccount> testAccounts = fqlResults.get(0).getFqlResultSet().castToListOf(TestAccount.class);
        Collection<UserAccount> userAccounts = fqlResults.get(1).getFqlResultSet().castToListOf(UserAccount.class);

        // Use both sets of results to populate our static array of accounts.
        populateTestAccounts(testAccounts, userAccounts);

        return;
    }

    private static synchronized void populateTestAccounts(Collection<TestAccount> testAccounts,
            Collection<UserAccount> userAccounts) {
        // We get different sets of data from each of these queries. We want to combine them into a single data
        // structure. We have added a Name property to the TestAccount interface, even though we don't really get
        // a name back from the service from that query. We stick the Name from the corresponding UserAccount in it.
        for (TestAccount testAccount : testAccounts) {
            storeTestAccount(testAccount);
        }

        for (UserAccount userAccount : userAccounts) {
            TestAccount testAccount = appTestAccounts.get(userAccount.getUid());
            if (testAccount != null) {
                testAccount.setName(userAccount.getName());
            }
        }
    }

    private static synchronized void storeTestAccount(TestAccount testAccount) {
        appTestAccounts.put(testAccount.getId(), testAccount);
    }

    private static synchronized TestAccount findTestAccountMatchingIdentifier(String identifier) {
        retrieveTestAccountsForAppIfNeeded();

        for (TestAccount testAccount : appTestAccounts.values()) {
            if (testAccount.getName().contains(identifier)) {
                return testAccount;
            }
        }
        return null;
    }

    public final String getTestUserId() {
        return testAccountId;
    }

    @Override
    void authorize(Activity activity, AuthRequest request) {
        if (mode == Mode.PRIVATE) {
            createTestAccountAndFinishAuth(activity);
        } else {
            findOrCreateSharedTestAccount(activity);
        }
    }

    @Override
    void postStateChange(final SessionState oldState, final SessionState newState, final Exception error) {
        // Make sure this doesn't get overwritten.
        String id = testAccountId;

        super.postStateChange(oldState, newState, error);

        if (newState.getIsClosed() && id != null && mode == Mode.PRIVATE) {
            deleteTestAccount(id, getAppAccessToken());
        }
    }

    public boolean getWasAskedToExtendAccessToken() {
        return wasAskedToExtendAccessToken;
    }

    public void forceExtendAccessToken(boolean forceExtendAccessToken) {
        AccessToken currentToken = getTokenInfo();
        setTokenInfo(
                new AccessToken(currentToken.getToken(), new Date(), currentToken.getPermissions(), true, new Date(0)));
        setLastAttemptedTokenExtendDate(new Date(0));
    }

    @Override
    boolean shouldExtendAccessToken() {
        boolean result = super.shouldExtendAccessToken();
        wasAskedToExtendAccessToken = false;
        return result;
    }

    @Override
    void extendAccessToken() {
        wasAskedToExtendAccessToken = true;
        super.extendAccessToken();
    }

    void fakeTokenRefreshAttempt() {
        setCurrentTokenRefreshRequest(new TokenRefreshRequest());
    }

    private void findOrCreateSharedTestAccount(Activity activity) {
        TestAccount testAccount = findTestAccountMatchingIdentifier(getSharedTestAccountIdentifier());
        if (testAccount != null) {
            finishAuthWithTestAccount(activity, testAccount);
        } else {
            createTestAccountAndFinishAuth(activity);
        }
    }

    private void finishAuthWithTestAccount(Activity activity, TestAccount testAccount) {
        testAccountId = testAccount.getId();

        AccessToken accessToken = AccessToken.createFromString(testAccount.getAccessToken(), requestedPermissions);
        finishAuth(activity, accessToken, null);
    }

    private TestAccount createTestAccountAndFinishAuth(Activity activity) {
        Bundle parameters = new Bundle();
        parameters.putString("installed", "true");
        parameters.putString("permissions", getPermissionsString());
        parameters.putString("access_token", getAppAccessToken());

        // If we're in shared mode, we want to rename this user to encode its permissions, so we can find it later
        // in another shared session. If we're in private mode, don't bother renaming it since we're just going to
        // delete it at the end of the session.
        if (mode == Mode.SHARED) {
            parameters.putString("name", String.format("Shared %s Testuser", getSharedTestAccountIdentifier()));
        }

        String graphPath = String.format("%s/accounts/test-users", testApplicationId);
        Request createUserRequest = new Request(null, graphPath, parameters, Request.POST_METHOD);
        Response response = createUserRequest.execute();

        FacebookException error = response.getError();
        TestAccount testAccount = response.getGraphObjectAs(TestAccount.class);
        if (error != null) {
            finishAuth(activity, null, error);
            return null;
        } else {
            assert testAccount != null;

            // If we are in shared mode, store this new account in the dictionary so we can re-use it later.
            if (mode == Mode.SHARED) {
                // Remember the new name we gave it, since we didn't get it back in the results of the create request.
                testAccount.setName(parameters.getString("name"));
                storeTestAccount(testAccount);
            }

            finishAuthWithTestAccount(activity, testAccount);

            return testAccount;
        }
    }

    private void deleteTestAccount(String testAccountId, String appAccessToken) {
        Bundle parameters = new Bundle();
        parameters.putString("access_token", appAccessToken);

        Request request = new Request(null, testAccountId, parameters, Request.DELETE_METHOD);
        Response response = request.execute();

        Exception error = response.getError();
        GraphObject graphObject = response.getGraphObject();
        if (error != null) {
            Log.w(LOG_TAG, String.format("Could not delete test account %s: %s", testAccountId, error.toString()));
        } else if (graphObject.get(Response.NON_JSON_RESPONSE_PROPERTY) == (Boolean) false) {
            Log.w(LOG_TAG, String.format("Could not delete test account %s: unknown reason", testAccountId));
        }
    }

    private String getPermissionsString() {
        return TextUtils.join(",", requestedPermissions);
    }

    private String getSharedTestAccountIdentifier() {
        // TODO port: use common hash algorithm across iOS and Android to avoid conflicts
        // We use long even though hashes are ints to avoid sign issues.
        long permissionsHash = getPermissionsString().hashCode() & 0xffffffffL;
        long machineTagHash = (machineUniqueUserTag != null) ? machineUniqueUserTag.hashCode() & 0xffffffffL : 0;
        long sessionTagHash = (sessionUniqueUserTag != null) ? sessionUniqueUserTag.hashCode() & 0xffffffffL : 0;

        long combinedHash = permissionsHash ^ machineTagHash ^ sessionTagHash;
        return validNameStringFromInteger(combinedHash);
    }

    private String validNameStringFromInteger(long i) {
        String s = Long.toString(i);
        StringBuilder result = new StringBuilder("Perm");

        // We know each character is a digit. Convert it into a letter 'a'-'j'. Avoid repeated characters
        //  that might make Facebook reject the name by converting every other repeated character into one
        //  10 higher ('k'-'t').
        char lastChar = 0;
        for (char c : s.toCharArray()) {
            if (c == lastChar) {
                c += 10;
            }
            result.append((char) (c + 'a' - '0'));
            lastChar = c;
        }

        return result.toString();
    }

    private interface TestAccount extends GraphObject {
        String getId();

        String getAccessToken();

        // Note: We don't actually get Name from our FQL query. We fill it in by correlating with UserAccounts.
        String getName();

        void setName(String name);
    }

    private interface UserAccount extends GraphObject {
        String getUid();

        String getName();

        void setName(String name);
    }

    private interface FqlResult extends GraphObject {
        GraphObjectList<GraphObject> getFqlResultSet();

    }

    private interface FqlResponse extends GraphObject {
        GraphObjectList<FqlResult> getData();
    }

    private static final class TestTokenCache extends TokenCache {
        private Bundle bundle;

        @Override
        public Bundle load() {
            return bundle;
        }

        @Override
        public void save(Bundle value) {
            bundle = value;
        }

        @Override
        public void clear() {
            bundle = null;
        }
    }
}
