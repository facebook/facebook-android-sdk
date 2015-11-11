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

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class manages Facebook test users.
 */
public class TestUserManager {

    private static final String LOG_TAG = "TestUserManager";

    private enum Mode {
        PRIVATE,
        SHARED,
    }

    private String testApplicationSecret;
    private String testApplicationId;
    private Map<String, JSONObject> appTestAccounts;

    /**
     * Constructor.
     *
     * @param testApplicationSecret The application secret.
     * @param testApplicationId     The application id.
     */
    public TestUserManager(String testApplicationSecret, String testApplicationId) {
        if (Utility.isNullOrEmpty(testApplicationId)
                || Utility.isNullOrEmpty(testApplicationSecret)) {
            throw new FacebookException("Must provide app ID and secret");
        }

        this.testApplicationSecret = testApplicationSecret;
        this.testApplicationId = testApplicationId;
    }

    /**
     * Gets the access token of the private test user for the application with the requested
     * permissions.
     *
     * @param permissions The requested permissions.
     * @return The access token of the private test user for the application.
     */
    public AccessToken getAccessTokenForPrivateUser(List<String> permissions) {
        return getAccessTokenForUser(permissions, Mode.PRIVATE, null);
    }

    /**
     * Gets the access token of the shared test user for the application with the requested
     * permissions.
     *
     * @param permissions The requested permissions.
     * @return The access token of the shared test user for the application.
     */
    public AccessToken getAccessTokenForSharedUser(List<String> permissions) {
        return getAccessTokenForSharedUser(permissions, null);
    }

    /**
     * Gets the access token of the shared test user with the tag for the application with the
     * requested permissions.
     *
     * @param permissions   The requested permissions.
     * @param uniqueUserTag The user tag.
     * @return The requested shared user.
     */
    public AccessToken getAccessTokenForSharedUser(
            List<String> permissions,
            String uniqueUserTag) {
        return getAccessTokenForUser(permissions, Mode.SHARED, uniqueUserTag);
    }

    /**
     * Getter for the test application id.
     *
     * @return The test application id.
     */
    public synchronized String getTestApplicationId() {
        return testApplicationId;
    }

    /**
     * Getter for the test application secret.
     *
     * @return The test application secret.
     */
    public synchronized String getTestApplicationSecret() {
        return testApplicationSecret;
    }

    private AccessToken getAccessTokenForUser(
            List<String> permissions,
            Mode mode,
            String uniqueUserTag) {

        retrieveTestAccountsForAppIfNeeded();

        if (Utility.isNullOrEmpty(permissions)) {
            permissions = Arrays.asList("email", "publish_actions");
        }

        JSONObject testAccount = null;
        if (mode == Mode.PRIVATE) {
            testAccount = createTestAccount(permissions, mode, uniqueUserTag);
        } else {
            testAccount = findOrCreateSharedTestAccount(permissions, mode, uniqueUserTag);
        }

        return new AccessToken(
                testAccount.optString("access_token"),
                testApplicationId,
                testAccount.optString("id"),
                permissions,
                null,
                AccessTokenSource.TEST_USER,
                null,
                null);
    }

    private synchronized void retrieveTestAccountsForAppIfNeeded() {
        if (appTestAccounts != null) {
            return;
        }

        appTestAccounts = new HashMap<String, JSONObject>();

        // The data we need is split across two different graph API queries. We construct two
        // queries, submit them together (the second one depends on the first one), then
        // cross-reference the results.

        GraphRequest.setDefaultBatchApplicationId(testApplicationId);

        Bundle parameters = new Bundle();
        parameters.putString("access_token", getAppAccessToken());

        GraphRequest requestTestUsers =
                new GraphRequest(null, "app/accounts/test-users", parameters, null);
        requestTestUsers.setBatchEntryName("testUsers");
        requestTestUsers.setBatchEntryOmitResultOnSuccess(false);

        Bundle testUserNamesParam = new Bundle();
        testUserNamesParam.putString("access_token", getAppAccessToken());
        testUserNamesParam.putString("ids", "{result=testUsers:$.data.*.id}");
        testUserNamesParam.putString("fields", "name");

        GraphRequest requestTestUserNames = new GraphRequest(null, "", testUserNamesParam, null);
        requestTestUserNames.setBatchEntryDependsOn("testUsers");

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(requestTestUsers,
                requestTestUserNames);
        if (responses == null || responses.size() != 2) {
            throw new FacebookException("Unexpected number of results from TestUsers batch query");
        }

        JSONObject testAccountsResponse = responses.get(0).getJSONObject();
        JSONArray testAccounts = testAccountsResponse.optJSONArray("data");

        // Response should contain a map of test accounts: { id's => { user } }
        JSONObject userAccountsMap = responses.get(1).getJSONObject();

        populateTestAccounts(testAccounts, userAccountsMap);
    }

    private synchronized void populateTestAccounts(JSONArray testAccounts,
                                                   JSONObject userAccountsMap) {

        for (int i = 0; i < testAccounts.length(); ++i) {
            JSONObject testAccount = testAccounts.optJSONObject(i);
            JSONObject testUser = userAccountsMap.optJSONObject(testAccount.optString("id"));
            try {
                testAccount.put("name", testUser.optString("name"));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Could not set name", e);
            }
            storeTestAccount(testAccount);
        }
    }

    private synchronized void storeTestAccount(JSONObject testAccount) {
        appTestAccounts.put(testAccount.optString("id"), testAccount);
    }

    private synchronized JSONObject findTestAccountMatchingIdentifier(String identifier) {
        for (JSONObject testAccount : appTestAccounts.values()) {
            if (testAccount.optString("name").contains(identifier)) {
                return testAccount;
            }
        }
        return null;
    }

    final String getAppAccessToken() {
        return testApplicationId + "|" + testApplicationSecret;
    }

    private JSONObject findOrCreateSharedTestAccount(List<String> permissions, Mode mode,
                                                     String uniqueUserTag) {

        JSONObject testAccount = findTestAccountMatchingIdentifier(
                getSharedTestAccountIdentifier(permissions, uniqueUserTag));
        if (testAccount != null) {
            return testAccount;
        } else {
            return createTestAccount(permissions, mode, uniqueUserTag);
        }
    }

    private String getSharedTestAccountIdentifier(List<String> permissions,
                                                  String uniqueUserTag) {

        // We use long even though hashes are ints to avoid sign issues.
        long permissionsHash = getPermissionsString(permissions).hashCode() & 0xffffffffL;
        long userTagHash = (uniqueUserTag != null)
                ? uniqueUserTag.hashCode() & 0xffffffffL
                : 0;

        long combinedHash = permissionsHash ^ userTagHash;
        return validNameStringFromInteger(combinedHash);
    }

    private String validNameStringFromInteger(long i) {
        String s = Long.toString(i);
        StringBuilder result = new StringBuilder("Perm");

        // We know each character is a digit. Convert it into a letter 'a'-'j'. Avoid repeated
        //  characters that might make Facebook reject the name by converting every other repeated
        //  character into one 10 higher ('k'-'t').
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

    private JSONObject createTestAccount(
            List<String> permissions,
            Mode mode,
            String uniqueUserTag) {
        Bundle parameters = new Bundle();
        parameters.putString("installed", "true");
        parameters.putString("permissions", getPermissionsString(permissions));
        parameters.putString("access_token", getAppAccessToken());

        // If we're in shared mode, we want to rename this user to encode its permissions, so we can
        // find it later. If we're in private mode, don't bother renaming it since we're just going
        // to delete it at the end.
        if (mode == Mode.SHARED) {
            parameters.putString("name", String.format("Shared %s Testuser",
                    getSharedTestAccountIdentifier(permissions, uniqueUserTag)));
        }

        String graphPath = String.format("%s/accounts/test-users", testApplicationId);
        GraphRequest createUserRequest =
                new GraphRequest(null, graphPath, parameters, HttpMethod.POST);
        GraphResponse response = createUserRequest.executeAndWait();

        FacebookRequestError error = response.getError();
        JSONObject testAccount = response.getJSONObject();
        if (error != null) {
            return null;
        } else {
            assert testAccount != null;

            // If we are in shared mode, store this new account in the dictionary so we can re-use
            // it later.
            if (mode == Mode.SHARED) {
                // Remember the new name we gave it, since we didn't get it back in the results of
                // the create request.
                try {
                    testAccount.put("name", parameters.getString("name"));
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Could not set name", e);
                }
                storeTestAccount(testAccount);
            }

            return testAccount;
        }
    }

    private String getPermissionsString(List<String> permissions) {
        return TextUtils.join(",", permissions);
    }
}
