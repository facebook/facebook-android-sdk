/**
 * Copyright 2010-present Facebook.
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

package com.facebook.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.facebook.FacebookException;
import com.facebook.Request;
import com.facebook.Settings;
import com.facebook.model.GraphObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for Android. Use of
 * any of the classes in this package is unsupported, and they may be modified or removed without warning at
 * any time.
 */
public final class Utility {
    static final String LOG_TAG = "FacebookSDK";
    private static final String HASH_ALGORITHM_MD5 = "MD5";
    private static final String HASH_ALGORITHM_SHA1 = "SHA-1";
    private static final String URL_SCHEME = "https";
    private static final String APP_SETTINGS_PREFS_STORE = "com.facebook.internal.preferences.APP_SETTINGS";
    private static final String APP_SETTINGS_PREFS_KEY_FORMAT = "com.facebook.internal.APP_SETTINGS.%s";
    private static final String APP_SETTING_SUPPORTS_ATTRIBUTION = "supports_attribution";
    private static final String APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING = "supports_implicit_sdk_logging";
    private static final String APP_SETTING_NUX_CONTENT = "gdpv4_nux_content";
    private static final String APP_SETTING_NUX_ENABLED = "gdpv4_nux_enabled";
    private static final String APP_SETTING_DIALOG_CONFIGS = "android_dialog_configs";
    private static final String EXTRA_APP_EVENTS_INFO_FORMAT_VERSION = "a1";
    private static final String DIALOG_CONFIG_DIALOG_NAME_FEATURE_NAME_SEPARATOR = "\\|";
    private static final String DIALOG_CONFIG_NAME_KEY = "name";
    private static final String DIALOG_CONFIG_VERSIONS_KEY = "versions";
    private static final String DIALOG_CONFIG_URL_KEY = "url";

    private final static String UTF8 = "UTF-8";

    private static final String[] APP_SETTING_FIELDS = new String[] {
            APP_SETTING_SUPPORTS_ATTRIBUTION,
            APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING,
            APP_SETTING_NUX_CONTENT,
            APP_SETTING_NUX_ENABLED,
            APP_SETTING_DIALOG_CONFIGS
    };
    private static final String APPLICATION_FIELDS = "fields";

    // This is the default used by the buffer streams, but they trace a warning if you do not specify.
    public static final int DEFAULT_STREAM_BUFFER_SIZE = 8192;

    private static Map<String, FetchedAppSettings> fetchedAppSettings =
            new ConcurrentHashMap<String, FetchedAppSettings>();

    private static AsyncTask<Void, Void, GraphObject> initialAppSettingsLoadTask;

    public static class FetchedAppSettings {
        private boolean supportsAttribution;
        private boolean supportsImplicitLogging;
        private String nuxContent;
        private boolean nuxEnabled;
        private Map<String, Map<String, DialogFeatureConfig>> dialogConfigMap;

        private FetchedAppSettings(boolean supportsAttribution,
                                   boolean supportsImplicitLogging,
                                   String nuxContent,
                                   boolean nuxEnabled,
                                   Map<String, Map<String, DialogFeatureConfig>> dialogConfigMap) {
            this.supportsAttribution = supportsAttribution;
            this.supportsImplicitLogging = supportsImplicitLogging;
            this.nuxContent = nuxContent;
            this.nuxEnabled = nuxEnabled;
            this.dialogConfigMap = dialogConfigMap;
        }

        public boolean supportsAttribution() {
            return supportsAttribution;
        }

        public boolean supportsImplicitLogging() {
            return supportsImplicitLogging;
        }

        public String getNuxContent() {
            return nuxContent;
        }

        public boolean getNuxEnabled() {
            return nuxEnabled;
        }

        public Map<String, Map<String, DialogFeatureConfig>> getDialogConfigurations() {
            return dialogConfigMap;
        }
    }

    public static class DialogFeatureConfig {
        private static DialogFeatureConfig parseDialogConfig(JSONObject dialogConfigJSON) {
            String dialogNameWithFeature = dialogConfigJSON.optString(DIALOG_CONFIG_NAME_KEY);
            if (Utility.isNullOrEmpty(dialogNameWithFeature)) {
                return null;
            }

            String[] components = dialogNameWithFeature.split(DIALOG_CONFIG_DIALOG_NAME_FEATURE_NAME_SEPARATOR);
            if (components.length != 2) {
                // We expect the format to be dialogName|FeatureName, where both components are non-empty.
                return null;
            }

            String dialogName = components[0];
            String featureName = components[1];
            if (isNullOrEmpty(dialogName) || isNullOrEmpty(featureName)) {
                return null;
            }

            String urlString = dialogConfigJSON.optString(DIALOG_CONFIG_URL_KEY);
            Uri fallbackUri = null;
            if (!Utility.isNullOrEmpty(urlString)) {
                fallbackUri = Uri.parse(urlString);
            }

            JSONArray versionsJSON = dialogConfigJSON.optJSONArray(DIALOG_CONFIG_VERSIONS_KEY);

            int[] featureVersionSpec = parseVersionSpec(versionsJSON);

            return new DialogFeatureConfig(dialogName, featureName, fallbackUri, featureVersionSpec);
        }

        private static int[] parseVersionSpec(JSONArray versionsJSON) {
            // Null signifies no overrides to the min-version as specified by the SDK.
            // An empty array would basically turn off the dialog (i.e no supported versions), so DON'T default to that.
            int[] versionSpec = null;
            if (versionsJSON != null) {
                int numVersions = versionsJSON.length();
                versionSpec = new int[numVersions];
                for (int i = 0; i < numVersions; i++) {
                    // See if the version was stored directly as an Integer
                    int version = versionsJSON.optInt(i, NativeProtocol.NO_PROTOCOL_AVAILABLE);
                    if (version == NativeProtocol.NO_PROTOCOL_AVAILABLE) {
                        // If not, then see if it was stored as a string that can be parsed out.
                        // If even that fails, then we will leave it as NO_PROTOCOL_AVAILABLE
                        String versionString = versionsJSON.optString(i);
                        if (!isNullOrEmpty(versionString)) {
                            try {
                                version = Integer.parseInt(versionString);
                            } catch (NumberFormatException nfe) {
                                logd(LOG_TAG, nfe);
                                version = NativeProtocol.NO_PROTOCOL_AVAILABLE;
                            }
                        }
                    }

                    versionSpec[i] = version;
                }
            }

            return versionSpec;
        }

        private String dialogName;
        private String featureName;
        private Uri fallbackUrl;
        private int[] featureVersionSpec;

        private DialogFeatureConfig(String dialogName, String featureName, Uri fallbackUrl, int[] featureVersionSpec) {
            this.dialogName = dialogName;
            this.featureName = featureName;
            this.fallbackUrl = fallbackUrl;
            this.featureVersionSpec = featureVersionSpec;
        }

        public String getDialogName() {
            return dialogName;
        }

        public String getFeatureName() {
            return featureName;
        }

        public Uri getFallbackUrl() {
            return fallbackUrl;
        }

        public int[] getVersionSpec() {
            return featureVersionSpec;
        }
    }

    /**
     * Each array represents a set of closed or open Range, like so:
     * [0,10,50,60] - Ranges are {0-9}, {50-59}
     * [20] - Ranges are {20-}
     * [30,40,100] - Ranges are {30-39}, {100-}
     *
     * All Ranges in the array have a closed lower bound. Only the last Range in each array may be open.
     * It is assumed that the passed in arrays are sorted with ascending order.
     * It is assumed that no two elements in a given are equal (i.e. no 0-length ranges)
     *
     * The method returns an intersect of the two passed in Range-sets
     * @param range1
     * @param range2
     * @return
     */
    public static int[] intersectRanges(int[] range1, int[] range2) {
        if (range1 == null) {
            return range2;
        } else if (range2 == null) {
            return range1;
        }

        int[] outputRange = new int[range1.length + range2.length];
        int outputIndex = 0;
        int index1 = 0, lower1, upper1;
        int index2 = 0, lower2, upper2;
        while (index1 < range1.length && index2 < range2.length) {
            int newRangeLower = Integer.MIN_VALUE, newRangeUpper = Integer.MAX_VALUE;
            lower1 = range1[index1];
            upper1 = Integer.MAX_VALUE;

            lower2 = range2[index2];
            upper2 = Integer.MAX_VALUE;

            if (index1 < range1.length - 1) {
                upper1 = range1[index1 + 1];
            }
            if (index2 < range2.length - 1) {
                upper2 = range2[index2 + 1];
            }

            if (lower1 < lower2) {
                if (upper1 > lower2) {
                    newRangeLower = lower2;
                    if (upper1 > upper2) {
                        newRangeUpper = upper2;
                        index2 += 2;
                    } else {
                        newRangeUpper = upper1;
                        index1 += 2;
                    }
                } else {
                    index1 += 2;
                }
            } else {
                if (upper2 > lower1) {
                    newRangeLower = lower1;
                    if (upper2 > upper1) {
                        newRangeUpper = upper1;
                        index1 += 2;
                    } else {
                        newRangeUpper = upper2;
                        index2 += 2;
                    }
                } else {
                    index2 += 2;
                }
            }

            if (newRangeLower != Integer.MIN_VALUE) {
                outputRange[outputIndex ++] = newRangeLower;
                if (newRangeUpper != Integer.MAX_VALUE) {
                    outputRange[outputIndex ++] = newRangeUpper;
                } else {
                    // If we reach an unbounded/open range, then we know we're done.
                    break;
                }
            }
        }

        return Arrays.copyOf(outputRange, outputIndex);
    }

    // Returns true iff all items in subset are in superset, treating null and
    // empty collections as
    // the same.
    public static <T> boolean isSubset(Collection<T> subset, Collection<T> superset) {
        if ((superset == null) || (superset.size() == 0)) {
            return ((subset == null) || (subset.size() == 0));
        }

        HashSet<T> hash = new HashSet<T>(superset);
        for (T t : subset) {
            if (!hash.contains(t)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean isNullOrEmpty(Collection<T> c) {
        return (c == null) || (c.size() == 0);
    }

    public static boolean isNullOrEmpty(String s) {
        return (s == null) || (s.length() == 0);
    }

    /**
     * Use this when you want to normalize empty and null strings
     * This way, Utility.areObjectsEqual can used for comparison, where a null string is to be treated the same as
     * an empty string.
     *
     * @param s
     * @param valueIfNullOrEmpty
     * @return
     */
    public static String coerceValueIfNullOrEmpty(String s, String valueIfNullOrEmpty) {
        if (isNullOrEmpty(s)) {
            return valueIfNullOrEmpty;
        }

        return s;
    }

    public static <T> Collection<T> unmodifiableCollection(T... ts) {
        return Collections.unmodifiableCollection(Arrays.asList(ts));
    }

    public static <T> ArrayList<T> arrayList(T... ts) {
        ArrayList<T> arrayList = new ArrayList<T>(ts.length);
        for (T t : ts) {
            arrayList.add(t);
        }
        return arrayList;
    }

    static String md5hash(String key) {
        return hashWithAlgorithm(HASH_ALGORITHM_MD5, key);
    }

    static String sha1hash(String key) {
        return hashWithAlgorithm(HASH_ALGORITHM_SHA1, key);
    }

    static String sha1hash(byte[] bytes) {
        return hashWithAlgorithm(HASH_ALGORITHM_SHA1, bytes);
    }

    private static String hashWithAlgorithm(String algorithm, String key) {
        return hashWithAlgorithm(algorithm, key.getBytes());
    }

    private static String hashWithAlgorithm(String algorithm, byte[] bytes) {
        MessageDigest hash;
        try {
            hash = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return hashBytes(hash, bytes);
    }

    private static String hashBytes(MessageDigest hash, byte[] bytes) {
        hash.update(bytes);
        byte[] digest = hash.digest();
        StringBuilder builder = new StringBuilder();
        for (int b : digest) {
            builder.append(Integer.toHexString((b >> 4) & 0xf));
            builder.append(Integer.toHexString((b >> 0) & 0xf));
        }
        return builder.toString();
    }

    public static Uri buildUri(String authority, String path, Bundle parameters) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(URL_SCHEME);
        builder.authority(authority);
        builder.path(path);
        for (String key : parameters.keySet()) {
            Object parameter = parameters.get(key);
            if (parameter instanceof String) {
                builder.appendQueryParameter(key, (String) parameter);
            }
        }
        return builder.build();
    }

    public static Bundle parseUrlQueryString(String queryString) {
        Bundle params = new Bundle();
        if (!isNullOrEmpty(queryString)) {
            String array[] = queryString.split("&");
            for (String parameter : array) {
                String keyValuePair[] = parameter.split("=");

                try {
                    if (keyValuePair.length == 2) {
                        params.putString(
                                URLDecoder.decode(keyValuePair[0], UTF8),
                                URLDecoder.decode(keyValuePair[1], UTF8));
                    } else if (keyValuePair.length == 1) {
                        params.putString(
                                URLDecoder.decode(keyValuePair[0], UTF8),
                                "");
                    }
                } catch (UnsupportedEncodingException e) {
                    // shouldn't happen
                    logd(LOG_TAG, e);
                }
            }
        }
        return params;
    }

    public static void putObjectInBundle(Bundle bundle, String key, Object value) {
        if (value instanceof String) {
            bundle.putString(key, (String) value);
        } else if (value instanceof Parcelable) {
            bundle.putParcelable(key, (Parcelable) value);
        } else if (value instanceof byte[]) {
            bundle.putByteArray(key, (byte[]) value);
        } else {
            throw new FacebookException("attempted to add unsupported type to Bundle");
        }
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    public static void disconnectQuietly(URLConnection connection) {
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).disconnect();
        }
    }

    public static String getMetadataApplicationId(Context context) {
        Validate.notNull(context, "context");

        Settings.loadDefaultsFromMetadata(context);

        return Settings.getApplicationId();
    }

    static Map<String, Object> convertJSONObjectToHashMap(JSONObject jsonObject) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        JSONArray keys = jsonObject.names();
        for (int i = 0; i < keys.length(); ++i) {
            String key;
            try {
                key = keys.getString(i);
                Object value = jsonObject.get(key);
                if (value instanceof JSONObject) {
                    value = convertJSONObjectToHashMap((JSONObject) value);
                }
                map.put(key, value);
            } catch (JSONException e) {
            }
        }
        return map;
    }

    // Returns either a JSONObject or JSONArray representation of the 'key' property of 'jsonObject'.
    public static Object getStringPropertyAsJSON(JSONObject jsonObject, String key, String nonJSONPropertyKey)
            throws JSONException {
        Object value = jsonObject.opt(key);
        if (value != null && value instanceof String) {
            JSONTokener tokener = new JSONTokener((String) value);
            value = tokener.nextValue();
        }

        if (value != null && !(value instanceof JSONObject || value instanceof JSONArray)) {
            if (nonJSONPropertyKey != null) {
                // Facebook sometimes gives us back a non-JSON value such as
                // literal "true" or "false" as a result.
                // If we got something like that, we present it to the caller as
                // a GraphObject with a single
                // property. We only do this if the caller wants that behavior.
                jsonObject = new JSONObject();
                jsonObject.putOpt(nonJSONPropertyKey, value);
                return jsonObject;
            } else {
                throw new FacebookException("Got an unexpected non-JSON object.");
            }
        }

        return value;

    }

    public static String readStreamToString(InputStream inputStream) throws IOException {
        BufferedInputStream bufferedInputStream = null;
        InputStreamReader reader = null;
        try {
            bufferedInputStream = new BufferedInputStream(inputStream);
            reader = new InputStreamReader(bufferedInputStream);
            StringBuilder stringBuilder = new StringBuilder();

            final int bufferSize = 1024 * 2;
            char[] buffer = new char[bufferSize];
            int n = 0;
            while ((n = reader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, n);
            }

            return stringBuilder.toString();
        } finally {
            closeQuietly(bufferedInputStream);
            closeQuietly(reader);
        }
    }

    public static boolean stringsEqualOrEmpty(String a, String b) {
        boolean aEmpty = TextUtils.isEmpty(a);
        boolean bEmpty = TextUtils.isEmpty(b);

        if (aEmpty && bEmpty) {
            // Both null or empty, they match.
            return true;
        }
        if (!aEmpty && !bEmpty) {
            // Both non-empty, check equality.
            return a.equals(b);
        }
        // One empty, one non-empty, can't match.
        return false;
    }

    private static void clearCookiesForDomain(Context context, String domain) {
        // This is to work around a bug where CookieManager may fail to instantiate if CookieSyncManager
        // has never been created.
        CookieSyncManager syncManager = CookieSyncManager.createInstance(context);
        syncManager.sync();

        CookieManager cookieManager = CookieManager.getInstance();

        String cookies = cookieManager.getCookie(domain);
        if (cookies == null) {
            return;
        }

        String[] splitCookies = cookies.split(";");
        for (String cookie : splitCookies) {
            String[] cookieParts = cookie.split("=");
            if (cookieParts.length > 0) {
                String newCookie = cookieParts[0].trim() + "=;expires=Sat, 1 Jan 2000 00:00:01 UTC;";
                cookieManager.setCookie(domain, newCookie);
            }
        }
        cookieManager.removeExpiredCookie();
    }

    public static void clearFacebookCookies(Context context) {
        // setCookie acts differently when trying to expire cookies between builds of Android that are using
        // Chromium HTTP stack and those that are not. Using both of these domains to ensure it works on both.
        clearCookiesForDomain(context, "facebook.com");
        clearCookiesForDomain(context, ".facebook.com");
        clearCookiesForDomain(context, "https://facebook.com");
        clearCookiesForDomain(context, "https://.facebook.com");
    }

    public static void logd(String tag, Exception e) {
        if (Settings.isDebugEnabled() && tag != null && e != null) {
            Log.d(tag, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public static void logd(String tag, String msg) {
        if (Settings.isDebugEnabled() && tag != null && msg != null) {
            Log.d(tag, msg);
        }
    }

    public static void logd(String tag, String msg, Throwable t) {
        if (Settings.isDebugEnabled() && !isNullOrEmpty(tag)) {
            Log.d(tag, msg, t);
        }
    }

    public static <T> boolean areObjectsEqual(T a, T b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    public static void loadAppSettingsAsync(final Context context, final String applicationId) {
        if (Utility.isNullOrEmpty(applicationId) ||
                fetchedAppSettings.containsKey(applicationId) ||
                initialAppSettingsLoadTask != null) {
            return;
        }

        final String settingsKey = String.format(APP_SETTINGS_PREFS_KEY_FORMAT, applicationId);

        initialAppSettingsLoadTask = new AsyncTask<Void, Void, GraphObject>() {
            @Override
            protected GraphObject doInBackground(Void... params) {
                return getAppSettingsQueryResponse(applicationId);
            }

            @Override
            protected void onPostExecute(GraphObject result) {
                if (result != null) {
                    JSONObject resultJSON = result.getInnerJSONObject();
                    parseAppSettingsFromJSON(applicationId, resultJSON);

                    SharedPreferences sharedPrefs = context.getSharedPreferences(
                            APP_SETTINGS_PREFS_STORE,
                            Context.MODE_PRIVATE);
                    sharedPrefs.edit().putString(settingsKey, resultJSON.toString()).apply();
                }

                initialAppSettingsLoadTask = null;
            }
        };
        initialAppSettingsLoadTask.execute((Void[])null);

        // Also see if we had a cached copy and use that immediately.
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                APP_SETTINGS_PREFS_STORE,
                Context.MODE_PRIVATE);
        String settingsJSONString = sharedPrefs.getString(settingsKey, null);
        if (!isNullOrEmpty(settingsJSONString)) {
            JSONObject settingsJSON = null;
            try {
                settingsJSON = new JSONObject(settingsJSONString);
            } catch (JSONException je) {
                logd(LOG_TAG, je);
            }
            if (settingsJSON != null) {
                parseAppSettingsFromJSON(applicationId, settingsJSON);
            }
        }
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the main thread.
    public static FetchedAppSettings queryAppSettings(final String applicationId, final boolean forceRequery) {
        // Cache the last app checked results.
        if (!forceRequery && fetchedAppSettings.containsKey(applicationId)) {
            return fetchedAppSettings.get(applicationId);
        }

        GraphObject response = getAppSettingsQueryResponse(applicationId);
        if (response == null) {
            return null;
        }

        return parseAppSettingsFromJSON(applicationId, response.getInnerJSONObject());
    }

    private static FetchedAppSettings parseAppSettingsFromJSON(String applicationId, JSONObject settingsJSON) {
        FetchedAppSettings result = new FetchedAppSettings(
                settingsJSON.optBoolean(APP_SETTING_SUPPORTS_ATTRIBUTION, false),
                settingsJSON.optBoolean(APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING, false),
                settingsJSON.optString(APP_SETTING_NUX_CONTENT, ""),
                settingsJSON.optBoolean(APP_SETTING_NUX_ENABLED, false),
                parseDialogConfigurations(settingsJSON.optJSONObject(APP_SETTING_DIALOG_CONFIGS))
        );

        fetchedAppSettings.put(applicationId, result);

        return result;
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the main thread.
    private static GraphObject getAppSettingsQueryResponse(String applicationId) {
        Bundle appSettingsParams = new Bundle();
        appSettingsParams.putString(APPLICATION_FIELDS, TextUtils.join(",", APP_SETTING_FIELDS));

        Request request = Request.newGraphPathRequest(null, applicationId, null);
        request.setSkipClientToken(true);
        request.setParameters(appSettingsParams);

        GraphObject response = request.executeAndWait().getGraphObject();
        return response;
    }

    public static DialogFeatureConfig getDialogFeatureConfig(String applicationId, String actionName, String featureName) {
        if (Utility.isNullOrEmpty(actionName) || Utility.isNullOrEmpty(featureName)) {
            return null;
        }

        FetchedAppSettings settings = fetchedAppSettings.get(applicationId);
        if (settings != null) {
            Map<String, DialogFeatureConfig> featureMap = settings.getDialogConfigurations().get(actionName);
            if (featureMap != null) {
                return featureMap.get(featureName);
            }
        }
        return null;
    }

    private static Map<String, Map<String, DialogFeatureConfig>> parseDialogConfigurations(JSONObject dialogConfigResponse) {
        HashMap<String, Map<String, DialogFeatureConfig>> dialogConfigMap = new HashMap<String, Map<String, DialogFeatureConfig>>();

        if (dialogConfigResponse != null) {
            JSONArray dialogConfigData = dialogConfigResponse.optJSONArray("data");
            if (dialogConfigData != null) {
                for (int i = 0; i < dialogConfigData.length(); i++) {
                    DialogFeatureConfig dialogConfig = DialogFeatureConfig.parseDialogConfig(dialogConfigData.optJSONObject(i));
                    if (dialogConfig == null) {
                        continue;
                    }

                    String dialogName = dialogConfig.getDialogName();
                    Map<String, DialogFeatureConfig> featureMap = dialogConfigMap.get(dialogName);
                    if (featureMap == null) {
                        featureMap = new HashMap<String, DialogFeatureConfig>();
                        dialogConfigMap.put(dialogName, featureMap);
                    }
                    featureMap.put(dialogConfig.getFeatureName(), dialogConfig);
                }
            }
        }

        return dialogConfigMap;
    }

    public static boolean safeGetBooleanFromResponse(GraphObject response, String propertyName) {
        Object result = false;
        if (response != null) {
            result = response.getProperty(propertyName);
        }
        if (!(result instanceof Boolean)) {
            result = false;
        }
        return (Boolean) result;
    }

    public static String safeGetStringFromResponse(GraphObject response, String propertyName) {
        Object result = "";
        if (response != null) {
            result = response.getProperty(propertyName);
        }
        if (!(result instanceof String)) {
            result = "";
        }
        return (String) result;
    }

    public static JSONObject tryGetJSONObjectFromResponse(GraphObject response, String propertyKey) {
        if (response == null) {
            return null;
        }
        Object property = response.getProperty(propertyKey);
        if (!(property instanceof JSONObject)) {
            return null;
        }
        return (JSONObject) property;
    }

    public static JSONArray tryGetJSONArrayFromResponse(GraphObject response, String propertyKey) {
        if (response == null) {
            return null;
        }
        Object property = response.getProperty(propertyKey);
        if (!(property instanceof JSONArray)) {
            return null;
        }
        return (JSONArray) property;
    }

    public static void clearCaches(Context context) {
        ImageDownloader.clearCache(context);
    }

    public static void deleteDirectory(File directoryOrFile) {
        if (!directoryOrFile.exists()) {
            return;
        }

        if (directoryOrFile.isDirectory()) {
            for (File child : directoryOrFile.listFiles()) {
                deleteDirectory(child);
            }
        }
        directoryOrFile.delete();
    }

    public static <T> List<T> asListNoNulls(T... array) {
        ArrayList<T> result = new ArrayList<T>();
        for (T t : array) {
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }

    // Return a hash of the android_id combined with the appid.  Intended to dedupe requests on the server side
    // in order to do counting of users unknown to Facebook.  Because we put the appid into the key prior to hashing,
    // we cannot do correlation of the same user across multiple apps -- this is intentional.  When we transition to
    // the Google advertising ID, we'll get rid of this and always send that up.
    public static String getHashedDeviceAndAppID(Context context, String applicationId) {
        String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

        if (androidId == null) {
            return null;
        } else {
            return sha1hash(androidId + applicationId);
        }
    }

    public static void setAppEventAttributionParameters(GraphObject params,
                                                        AttributionIdentifiers attributionIdentifiers, String hashedDeviceAndAppId, boolean limitEventUsage) {
        // Send attributionID if it exists, otherwise send a hashed device+appid specific value as the advertiser_id.
        if (attributionIdentifiers != null && attributionIdentifiers.getAttributionId() != null) {
            params.setProperty("attribution", attributionIdentifiers.getAttributionId());
        }

        if (attributionIdentifiers != null && attributionIdentifiers.getAndroidAdvertiserId() != null) {
            params.setProperty("advertiser_id", attributionIdentifiers.getAndroidAdvertiserId());
            params.setProperty("advertiser_tracking_enabled", !attributionIdentifiers.isTrackingLimited());
        } else if (hashedDeviceAndAppId != null) {
            params.setProperty("advertiser_id", hashedDeviceAndAppId);
        }

        params.setProperty("application_tracking_enabled", !limitEventUsage);
    }

    public static void setAppEventExtendedDeviceInfoParameters(GraphObject params, Context appContext) {
        JSONArray extraInfoArray = new JSONArray();
        extraInfoArray.put(EXTRA_APP_EVENTS_INFO_FORMAT_VERSION);

        // Application Manifest info:
        String pkgName = appContext.getPackageName();
        int versionCode = -1;
        String versionName = "";

        try {
            PackageInfo pi = appContext.getPackageManager().getPackageInfo(pkgName, 0);
            versionCode = pi.versionCode;
            versionName = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Swallow
        }

        // Application Manifest info:
        extraInfoArray.put(pkgName);
        extraInfoArray.put(versionCode);
        extraInfoArray.put(versionName);

        params.setProperty("extinfo", extraInfoArray.toString());
    }

    public static Method getMethodQuietly(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static Method getMethodQuietly(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> clazz = Class.forName(className);
            return getMethodQuietly(clazz, methodName, parameterTypes);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    public static Object invokeMethodQuietly(Object receiver, Method method, Object... args) {
        try {
            return method.invoke(receiver, args);
        } catch (IllegalAccessException ex) {
            return null;
        } catch (InvocationTargetException ex) {
            return null;
        }
    }

    /**
     * Returns the name of the current activity if the context is an activity, otherwise return "unknown"
     */
    public static String getActivityName(Context context) {
        if (context == null) {
            return "null";
        } else if (context == context.getApplicationContext()) {
            return "unknown";
        } else {
            return context.getClass().getSimpleName();
        }
    }
}
