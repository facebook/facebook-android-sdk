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

package com.facebook.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public final class Utility {
    static final String LOG_TAG = "FacebookSDK";
    private static final String HASH_ALGORITHM_MD5 = "MD5";
    private static final String HASH_ALGORITHM_SHA1 = "SHA-1";
    private static final String URL_SCHEME = "https";
    private static final String APP_SETTINGS_PREFS_STORE =
            "com.facebook.internal.preferences.APP_SETTINGS";
    private static final String APP_SETTINGS_PREFS_KEY_FORMAT =
            "com.facebook.internal.APP_SETTINGS.%s";
    private static final String APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING =
            "supports_implicit_sdk_logging";
    private static final String APP_SETTING_NUX_CONTENT = "gdpv4_nux_content";
    private static final String APP_SETTING_NUX_ENABLED = "gdpv4_nux_enabled";
    private static final String APP_SETTING_DIALOG_CONFIGS = "android_dialog_configs";
    private static final String APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES =
            "android_sdk_error_categories";
    private static final String EXTRA_APP_EVENTS_INFO_FORMAT_VERSION = "a1";
    private static final String DIALOG_CONFIG_DIALOG_NAME_FEATURE_NAME_SEPARATOR = "\\|";
    private static final String DIALOG_CONFIG_NAME_KEY = "name";
    private static final String DIALOG_CONFIG_VERSIONS_KEY = "versions";
    private static final String DIALOG_CONFIG_URL_KEY = "url";

    private final static String UTF8 = "UTF-8";

    private static final String[] APP_SETTING_FIELDS = new String[]{
            APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING,
            APP_SETTING_NUX_CONTENT,
            APP_SETTING_NUX_ENABLED,
            APP_SETTING_DIALOG_CONFIGS,
            APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES
    };
    private static final String APPLICATION_FIELDS = "fields";

    // This is the default used by the buffer streams, but they trace a warning if you do not
    // specify.
    public static final int DEFAULT_STREAM_BUFFER_SIZE = 8192;

    private static Map<String, FetchedAppSettings> fetchedAppSettings =
            new ConcurrentHashMap<String, FetchedAppSettings>();

    private static AtomicBoolean loadingSettings = new AtomicBoolean(false);

    public static class FetchedAppSettings {
        private boolean supportsImplicitLogging;
        private String nuxContent;
        private boolean nuxEnabled;
        private Map<String, Map<String, DialogFeatureConfig>> dialogConfigMap;
        private FacebookRequestErrorClassification errorClassification;

        private FetchedAppSettings(boolean supportsImplicitLogging,
                                   String nuxContent,
                                   boolean nuxEnabled,
                                   Map<String, Map<String, DialogFeatureConfig>> dialogConfigMap,
                                   FacebookRequestErrorClassification errorClassification) {
            this.supportsImplicitLogging = supportsImplicitLogging;
            this.nuxContent = nuxContent;
            this.nuxEnabled = nuxEnabled;
            this.dialogConfigMap = dialogConfigMap;
            this.errorClassification = errorClassification;
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

        public FacebookRequestErrorClassification getErrorClassification() {
            return errorClassification;
        }
    }

    public static class DialogFeatureConfig {
        private static DialogFeatureConfig parseDialogConfig(JSONObject dialogConfigJSON) {
            String dialogNameWithFeature = dialogConfigJSON.optString(DIALOG_CONFIG_NAME_KEY);
            if (Utility.isNullOrEmpty(dialogNameWithFeature)) {
                return null;
            }

            String[] components = dialogNameWithFeature.split(
                    DIALOG_CONFIG_DIALOG_NAME_FEATURE_NAME_SEPARATOR);
            if (components.length != 2) {
                // We expect the format to be dialogName|FeatureName, where both components are
                // non-empty.
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

            return new DialogFeatureConfig(
                    dialogName, featureName, fallbackUri, featureVersionSpec);
        }

        private static int[] parseVersionSpec(JSONArray versionsJSON) {
            // Null signifies no overrides to the min-version as specified by the SDK.
            // An empty array would basically turn off the dialog (i.e no supported versions), so
            // DON'T default to that.
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

        private DialogFeatureConfig(
                String dialogName,
                String featureName,
                Uri fallbackUrl,
                int[] featureVersionSpec) {
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
     * Each array represents a set of closed or open Range, like so: [0,10,50,60] - Ranges are
     * {0-9}, {50-59} [20] - Ranges are {20-} [30,40,100] - Ranges are {30-39}, {100-}
     * <p/>
     * All Ranges in the array have a closed lower bound. Only the last Range in each array may be
     * open. It is assumed that the passed in arrays are sorted with ascending order. It is assumed
     * that no two elements in a given are equal (i.e. no 0-length ranges)
     * <p/>
     * The method returns an intersect of the two passed in Range-sets
     *
     * @param range1 The first range
     * @param range2 The second range
     * @return The intersection of the two ranges.
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
                outputRange[outputIndex++] = newRangeLower;
                if (newRangeUpper != Integer.MAX_VALUE) {
                    outputRange[outputIndex++] = newRangeUpper;
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
     * This way, Utility.areObjectsEqual can used for comparison, where a null string is to be
     * treated the same as an empty string.
     *
     * @param s                  The string to coerce
     * @param valueIfNullOrEmpty The value if s is null or empty.
     * @return The original string s if it's not null or empty, otherwise the valueIfNullOrEmpty
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

    public static <T> HashSet<T> hashSet(T... ts) {
        HashSet<T> hashSet = new HashSet<T>(ts.length);
        for (T t : ts) {
            hashSet.add(t);
        }
        return hashSet;
    }

    public static String md5hash(String key) {
        return hashWithAlgorithm(HASH_ALGORITHM_MD5, key);
    }

    public static String sha1hash(String key) {
        return hashWithAlgorithm(HASH_ALGORITHM_SHA1, key);
    }

    public static String sha1hash(byte[] bytes) {
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
        if (parameters != null) {
            for (String key : parameters.keySet()) {
                Object parameter = parameters.get(key);
                if (parameter instanceof String) {
                    builder.appendQueryParameter(key, (String) parameter);
                }
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

    public static void putNonEmptyString(Bundle b, String key, String value) {
        if (!Utility.isNullOrEmpty(value)) {
            b.putString(key, value);
        }
    }

    public static void putCommaSeparatedStringList(Bundle b, String key, ArrayList<String> list) {
        if (list != null) {
            StringBuilder builder = new StringBuilder();
            for (String string : list) {
                builder.append(string);
                builder.append(",");
            }
            String commaSeparated = "";
            if (builder.length() > 0) {
                commaSeparated = builder.substring(0, builder.length() - 1);
            }
            b.putString(key, commaSeparated);
        }
    }

    public static void putUri(Bundle b, String key, Uri uri) {
        if (uri != null) {
            Utility.putNonEmptyString(b, key, uri.toString());
        }
    }

    public static boolean putJSONValueInBundle(Bundle bundle, String key, Object value) {
        if (value == null) {
            bundle.remove(key);
        } else if (value instanceof Boolean) {
            bundle.putBoolean(key, (boolean) value);
        } else if (value instanceof boolean[]) {
            bundle.putBooleanArray(key, (boolean[]) value);
        } else if (value instanceof Double) {
            bundle.putDouble(key, (double) value);
        } else if (value instanceof double[]) {
            bundle.putDoubleArray(key, (double[]) value);
        } else if (value instanceof Integer) {
            bundle.putInt(key, (int) value);
        } else if (value instanceof int[]) {
            bundle.putIntArray(key, (int[]) value);
        } else if (value instanceof Long) {
            bundle.putLong(key, (long) value);
        } else if (value instanceof long[]) {
            bundle.putLongArray(key, (long[]) value);
        } else if (value instanceof String) {
            bundle.putString(key, (String) value);
        } else if (value instanceof JSONArray) {
            bundle.putString(key, ((JSONArray) value).toString());
        } else if (value instanceof JSONObject) {
            bundle.putString(key, ((JSONObject) value).toString());
        } else {
            return false;
        }
        return true;
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

        FacebookSdk.sdkInitialize(context);

        return FacebookSdk.getApplicationId();
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

    // Returns either a JSONObject or JSONArray representation of the 'key' property of
    // 'jsonObject'.
    public static Object getStringPropertyAsJSON(
            JSONObject jsonObject,
            String key,
            String nonJSONPropertyKey
    ) throws JSONException {
        Object value = jsonObject.opt(key);
        if (value != null && value instanceof String) {
            JSONTokener tokener = new JSONTokener((String) value);
            value = tokener.nextValue();
        }

        if (value != null && !(value instanceof JSONObject || value instanceof JSONArray)) {
            if (nonJSONPropertyKey != null) {
                // Facebook sometimes gives us back a non-JSON value such as
                // literal "true" or "false" as a result.
                // If we got something like that, we present it to the caller as a JSONObject
                // with a single property. We only do this if the caller wants that behavior.
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

    public static int copyAndCloseInputStream(InputStream inputStream, OutputStream outputStream)
            throws IOException {
        BufferedInputStream bufferedInputStream = null;
        int totalBytes = 0;
        try {
            bufferedInputStream = new BufferedInputStream(inputStream);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
        } finally {
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return totalBytes;
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
        // This is to work around a bug where CookieManager may fail to instantiate if
        // CookieSyncManager has never been created.
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
                String newCookie = cookieParts[0].trim() +
                        "=;expires=Sat, 1 Jan 2000 00:00:01 UTC;";
                cookieManager.setCookie(domain, newCookie);
            }
        }
        cookieManager.removeExpiredCookie();
    }

    public static void clearFacebookCookies(Context context) {
        // setCookie acts differently when trying to expire cookies between builds of Android that
        // are using Chromium HTTP stack and those that are not. Using both of these domains to
        // ensure it works on both.
        clearCookiesForDomain(context, "facebook.com");
        clearCookiesForDomain(context, ".facebook.com");
        clearCookiesForDomain(context, "https://facebook.com");
        clearCookiesForDomain(context, "https://.facebook.com");
    }

    public static void logd(String tag, Exception e) {
        if (FacebookSdk.isDebugEnabled() && tag != null && e != null) {
            Log.d(tag, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public static void logd(String tag, String msg) {
        if (FacebookSdk.isDebugEnabled() && tag != null && msg != null) {
            Log.d(tag, msg);
        }
    }

    public static void logd(String tag, String msg, Throwable t) {
        if (FacebookSdk.isDebugEnabled() && !isNullOrEmpty(tag)) {
            Log.d(tag, msg, t);
        }
    }

    public static <T> boolean areObjectsEqual(T a, T b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    public static boolean hasSameId(JSONObject a, JSONObject b) {
        if (a == null || b == null || !a.has("id") || !b.has("id")) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        String idA = a.optString("id");
        String idB = b.optString("id");
        if (idA == null || idB == null) {
            return false;
        }
        return idA.equals(idB);
    }

    public static void loadAppSettingsAsync(
            final Context context,
            final String applicationId
    ) {
        boolean canStartLoading = loadingSettings.compareAndSet(false, true);
        if (Utility.isNullOrEmpty(applicationId) ||
                fetchedAppSettings.containsKey(applicationId) ||
                !canStartLoading) {
            return;
        }

        final String settingsKey = String.format(APP_SETTINGS_PREFS_KEY_FORMAT, applicationId);

        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                JSONObject resultJSON = getAppSettingsQueryResponse(applicationId);
                if (resultJSON != null) {
                    parseAppSettingsFromJSON(applicationId, resultJSON);

                    SharedPreferences sharedPrefs = context.getSharedPreferences(
                            APP_SETTINGS_PREFS_STORE,
                            Context.MODE_PRIVATE);
                    sharedPrefs.edit()
                            .putString(settingsKey, resultJSON.toString())
                            .apply();
                }

                loadingSettings.set(false);
            }
        });

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

    // This call only gets the app settings if they're already fetched
    public static FetchedAppSettings getAppSettingsWithoutQuery(final String applicationId) {
        return applicationId != null ? fetchedAppSettings.get(applicationId) : null;
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread.
    public static FetchedAppSettings queryAppSettings(
            final String applicationId,
            final boolean forceRequery) {
        // Cache the last app checked results.
        if (!forceRequery && fetchedAppSettings.containsKey(applicationId)) {
            return fetchedAppSettings.get(applicationId);
        }

        JSONObject response = getAppSettingsQueryResponse(applicationId);
        if (response == null) {
            return null;
        }

        return parseAppSettingsFromJSON(applicationId, response);
    }

    private static FetchedAppSettings parseAppSettingsFromJSON(
            String applicationId,
            JSONObject settingsJSON) {
        JSONArray errorClassificationJSON =
                settingsJSON.optJSONArray(APP_SETTING_ANDROID_SDK_ERROR_CATEGORIES);
        FacebookRequestErrorClassification errorClassification =
                errorClassificationJSON == null
                        ? FacebookRequestErrorClassification.getDefaultErrorClassification()
                        : FacebookRequestErrorClassification.createFromJSON(
                        errorClassificationJSON
                );
        FetchedAppSettings result = new FetchedAppSettings(
                settingsJSON.optBoolean(APP_SETTING_SUPPORTS_IMPLICIT_SDK_LOGGING, false),
                settingsJSON.optString(APP_SETTING_NUX_CONTENT, ""),
                settingsJSON.optBoolean(APP_SETTING_NUX_ENABLED, false),
                parseDialogConfigurations(settingsJSON.optJSONObject(APP_SETTING_DIALOG_CONFIGS)),
                errorClassification
        );

        fetchedAppSettings.put(applicationId, result);

        return result;
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread.
    private static JSONObject getAppSettingsQueryResponse(String applicationId) {
        Bundle appSettingsParams = new Bundle();
        appSettingsParams.putString(APPLICATION_FIELDS, TextUtils.join(",", APP_SETTING_FIELDS));

        GraphRequest request = GraphRequest.newGraphPathRequest(null, applicationId, null);
        request.setSkipClientToken(true);
        request.setParameters(appSettingsParams);

        return request.executeAndWait().getJSONObject();
    }

    public static DialogFeatureConfig getDialogFeatureConfig(
            String applicationId,
            String actionName,
            String featureName) {
        if (Utility.isNullOrEmpty(actionName) || Utility.isNullOrEmpty(featureName)) {
            return null;
        }

        FetchedAppSettings settings = fetchedAppSettings.get(applicationId);
        if (settings != null) {
            Map<String, DialogFeatureConfig> featureMap =
                    settings.getDialogConfigurations().get(actionName);
            if (featureMap != null) {
                return featureMap.get(featureName);
            }
        }
        return null;
    }

    private static Map<String, Map<String, DialogFeatureConfig>> parseDialogConfigurations(
            JSONObject dialogConfigResponse) {
        HashMap<String, Map<String, DialogFeatureConfig>> dialogConfigMap = new HashMap<String, Map<String, DialogFeatureConfig>>();

        if (dialogConfigResponse != null) {
            JSONArray dialogConfigData = dialogConfigResponse.optJSONArray("data");
            if (dialogConfigData != null) {
                for (int i = 0; i < dialogConfigData.length(); i++) {
                    DialogFeatureConfig dialogConfig = DialogFeatureConfig.parseDialogConfig(
                            dialogConfigData.optJSONObject(i));
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

    public static String safeGetStringFromResponse(JSONObject response, String propertyName) {
        return response != null ? response.optString(propertyName, "") : "";
    }

    public static JSONObject tryGetJSONObjectFromResponse(JSONObject response, String propertyKey) {
        return response != null ? response.optJSONObject(propertyKey) : null;
    }

    public static JSONArray tryGetJSONArrayFromResponse(JSONObject response, String propertyKey) {
        return response != null ? response.optJSONArray(propertyKey) : null;
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

    public static List<String> jsonArrayToStringList(JSONArray jsonArray) throws JSONException {
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            result.add(jsonArray.getString(i));
        }

        return result;
    }

    public static void setAppEventAttributionParameters(
            JSONObject params,
            AttributionIdentifiers attributionIdentifiers,
            String anonymousAppDeviceGUID,
            boolean limitEventUsage) throws JSONException {
        if (attributionIdentifiers != null && attributionIdentifiers.getAttributionId() != null) {
            params.put("attribution", attributionIdentifiers.getAttributionId());
        }

        if (attributionIdentifiers != null &&
                attributionIdentifiers.getAndroidAdvertiserId() != null) {
            params.put("advertiser_id", attributionIdentifiers.getAndroidAdvertiserId());
            params.put("advertiser_tracking_enabled", !attributionIdentifiers.isTrackingLimited());
        }

        params.put("anon_id", anonymousAppDeviceGUID);
        params.put("application_tracking_enabled", !limitEventUsage);
    }

    public static void setAppEventExtendedDeviceInfoParameters(
            JSONObject params,
            Context appContext
    ) throws JSONException {
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

        params.put("extinfo", extraInfoArray.toString());
    }

    public static Method getMethodQuietly(
            Class<?> clazz,
            String methodName,
            Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static Method getMethodQuietly(
            String className,
            String methodName,
            Class<?>... parameterTypes) {
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
     * Returns the name of the current activity if the context is an activity, otherwise return
     * "unknown"
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

    public interface Predicate<T> {
        public boolean apply(T item);
    }

    public static <T> List<T> filter(final List<T> target, final Predicate<T> predicate) {
        if (target == null) {
            return null;
        }
        final List<T> list = new ArrayList<T>();
        for (T item : target) {
            if (predicate.apply(item)) {
                list.add(item);
            }
        }
        return (list.size() == 0 ? null : list);
    }

    public interface Mapper<T, K> {
        public K apply(T item);
    }

    public static <T, K> List<K> map(final List<T> target, final Mapper<T, K> mapper) {
        if (target == null) {
            return null;
        }
        final List<K> list = new ArrayList<K>();
        for (T item : target) {
            final K mappedItem = mapper.apply(item);
            if (mappedItem != null) {
                list.add(mappedItem);
            }
        }
        return (list.size() == 0 ? null : list);
    }

    public static String getUriString(final Uri uri) {
        return (uri == null ? null : uri.toString());
    }

    public static boolean isWebUri(final Uri uri) {
        return (uri != null)
                && ("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()));
    }

    public static boolean isContentUri(final Uri uri) {
        return (uri != null) && ("content".equalsIgnoreCase(uri.getScheme()));
    }

    public static boolean isFileUri(final Uri uri) {
        return (uri != null) && ("file".equalsIgnoreCase(uri.getScheme()));
    }

    public static Date getBundleLongAsDate(Bundle bundle, String key, Date dateBase) {
        if (bundle == null) {
            return null;
        }

        long secondsFromBase = Long.MIN_VALUE;

        Object secondsObject = bundle.get(key);
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
            return new Date(dateBase.getTime() + (secondsFromBase * 1000L));
        }
    }

    public static void writeStringMapToParcel(Parcel parcel, final Map<String, String> map) {
        if (map == null) {
            // 0 is for empty map, -1 to indicate null
            parcel.writeInt(-1);
        } else {
            parcel.writeInt(map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                parcel.writeString(entry.getKey());
                parcel.writeString(entry.getValue());
            }
        }
    }

    public static Map<String, String> readStringMapFromParcel(Parcel parcel) {
        int size = parcel.readInt();
        if (size < 0) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(parcel.readString(), parcel.readString());
        }
        return map;
    }

    public static boolean isCurrentAccessToken(AccessToken token) {
        return token != null ? token.equals(AccessToken.getCurrentAccessToken()) : false;
    }

    public interface GraphMeRequestWithCacheCallback {
        void onSuccess(JSONObject userInfo);

        void onFailure(FacebookException error);
    }

    public static void getGraphMeRequestWithCacheAsync(
            final String accessToken,
            final GraphMeRequestWithCacheCallback callback) {
        JSONObject cachedValue = ProfileInformationCache.getProfileInformation(accessToken);
        if (cachedValue != null) {
            callback.onSuccess(cachedValue);
            return;
        }

        GraphRequest.Callback graphCallback = new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                if (response.getError() != null) {
                    callback.onFailure(response.getError().getException());
                } else {
                    ProfileInformationCache.putProfileInformation(
                            accessToken,
                            response.getJSONObject());
                    callback.onSuccess(response.getJSONObject());
                }
            }
        };
        GraphRequest graphRequest = getGraphMeRequestWithCache(accessToken);
        graphRequest.setCallback(graphCallback);
        graphRequest.executeAsync();
    }

    public static JSONObject awaitGetGraphMeRequestWithCache(
            final String accessToken) {
        JSONObject cachedValue = ProfileInformationCache.getProfileInformation(accessToken);
        if (cachedValue != null) {
            return cachedValue;
        }

        GraphRequest graphRequest = getGraphMeRequestWithCache(accessToken);
        GraphResponse response = graphRequest.executeAndWait();
        if (response.getError() != null) {
            return null;
        }

        return response.getJSONObject();
    }

    private static GraphRequest getGraphMeRequestWithCache(
            final String accessToken) {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,first_name,middle_name,last_name,link");
        parameters.putString("access_token", accessToken);
        GraphRequest graphRequest = new GraphRequest(
                null,
                "me",
                parameters,
                HttpMethod.GET,
                null);
        return graphRequest;
    }
}

