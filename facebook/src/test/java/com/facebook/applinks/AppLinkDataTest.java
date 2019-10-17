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

package com.facebook.applinks;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.facebook.FacebookSdk;
import com.facebook.FacebookTestCase;
import com.facebook.TestUtils;
import com.facebook.applinks.AppLinkData;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.*;

public class AppLinkDataTest extends FacebookTestCase {
    private static final String BUNDLE_APPLINK_ARGS_KEY = "com.facebook.platform.APPLINK_ARGS";
    private static final String BUNDLE_AL_APPLINK_DATA_KEY = "al_applink_data";
    private static final String TARGET_URI_STRING = "http://test.app/foo";
    private static final String FB_REF_KEY = "fb_ref";
    private static final String FB_REF_VALUE = "foobar";
    private static final String REFERER_DATA_KEY = "referer_data";
    private static final String EXTRA_ARGS_KEY = "extra_args";
    private static final String EXTRA_ARGS_VALUE = "extra_value";
    private static final String TARGET_URL_KEY = "target_url";
    private static final String USER_AGENT_KEY = "user_agent";
    private static final String USER_AGENT_VALUE = "foobarUserAgent";
    private static final String EXTRAS_KEY = "extras";
    private static final String DEEPLINK_CONTEXT_KEY = "deeplink_context";
    private static final String PROMO_CODE_KEY = "promo_code";
    private static final String PROMO_CODE = "PROMO1";

    private static final String JSON_DATA_REGULAR =
            "{"
                    + "\"version\":2,"
                    + "\"bridge_args\": {\"method\": \"applink\"},"
                    + "\"method_args\": {"
                    + "    \"ref\": \"" + FB_REF_VALUE + "\","
                    + "    \"" + TARGET_URL_KEY + "\": \"" + TARGET_URI_STRING + "\""
                    + "  }"
                    + "}";

    private static final String JSON_DATA_REGULAR_WITH_NESTED_ARRAY =
            "{"
                    + "\"version\":2,"
                    + "\"bridge_args\": {\"method\": \"applink\"},"
                    + "\"method_args\": {"
                    + "    \"ref\": \"" + FB_REF_VALUE + "\","
                    + "    \"" + TARGET_URL_KEY + "\": \"" + TARGET_URI_STRING + "\","
                    + "    \"other\": [ [1, 2], [3, 4] ]"
                    + "  }"
                    + "}";

    private static final String JSON_DATA_WITH_REFERER_DATA =
            "{"
                    + "\"version\":2,"
                    + "\"bridge_args\": {\"method\": \"applink\"},"
                    + "\"method_args\": {"
                    + "    \"referer_data\" : {"
                    + "      \"" + FB_REF_KEY + "\": \"" + FB_REF_VALUE + "\","
                    + "      \"" + EXTRA_ARGS_KEY + "\": \"" + EXTRA_ARGS_VALUE + "\""
                    + "    },"
                    + "    \"" + TARGET_URL_KEY + "\": \"" + TARGET_URI_STRING + "\""
                    + "  }"
                    + "}";

    private static final String JSON_DATA_WITH_DEEPLINK_CONTEXT =
            "{"
                    + "\"version\":2,"
                    + "\"bridge_args\": {\"method\": \"applink\"},"
                    + "\"method_args\": {"
                    + "    \"ref\": \"" + FB_REF_VALUE + "\","
                    + "    \"" + TARGET_URL_KEY + "\": \"" + TARGET_URI_STRING + "\","
                    + "    \"" + EXTRAS_KEY + "\": {"
                    + "        \"" + DEEPLINK_CONTEXT_KEY + "\": {"
                    + "            \"" + PROMO_CODE_KEY + "\": \""+ PROMO_CODE + "\""
                    + "        }"
                    + "    }"
                    + "  }"
                    + "}";

    private static class MockActivityWithAppLinkData extends Activity {
        public Intent getIntent() {
            Uri targetUri = Uri.parse(TARGET_URI_STRING);
            Intent intent = new Intent(Intent.ACTION_VIEW, targetUri);
            Bundle applinks = new Bundle();
            Bundle refererData = new Bundle();
            Bundle extras = new Bundle();
            String deeplinkContext = "{\"" + PROMO_CODE_KEY + "\": \""+ PROMO_CODE + "\"}";
            extras.putString(DEEPLINK_CONTEXT_KEY, deeplinkContext);
            refererData.putString(FB_REF_KEY, FB_REF_VALUE);
            refererData.putString(EXTRA_ARGS_KEY, EXTRA_ARGS_VALUE);
            applinks.putBundle(REFERER_DATA_KEY, refererData);
            applinks.putString(TARGET_URL_KEY, TARGET_URI_STRING);
            applinks.putString(USER_AGENT_KEY, USER_AGENT_VALUE);
            applinks.putBundle(EXTRAS_KEY, extras);
            intent.putExtra(BUNDLE_AL_APPLINK_DATA_KEY, applinks);
            return intent;
        }
    }

    private static class MockActivityWithJsonData extends Activity {
        private String jsonString;

        public MockActivityWithJsonData(String jsonString) {
            this.jsonString = jsonString;
        }
        public Intent getIntent() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(BUNDLE_APPLINK_ARGS_KEY, jsonString);
            return intent;
        }
    }

    @Test
    public void testCreateFromAlApplinkData() {
        AppLinkData appLinkData = AppLinkData.createFromActivity(new MockActivityWithAppLinkData());
        assertNotNull("app link data not null", appLinkData);
        assertEquals("ref param", FB_REF_VALUE, appLinkData.getRef());
        assertEquals("target_url", TARGET_URI_STRING, appLinkData.getTargetUri().toString());
        Bundle args = appLinkData.getArgumentBundle();
        assertNotNull("app link args not null", args);
        assertEquals("user agent", USER_AGENT_VALUE, args.getString(USER_AGENT_KEY));
        Bundle refererData = appLinkData.getRefererData();
        assertNotNull("referer data not null", refererData);
        assertEquals("ref param in referer data", FB_REF_VALUE, refererData.getString(FB_REF_KEY));
        assertEquals("extra param", EXTRA_ARGS_VALUE, refererData.getString(EXTRA_ARGS_KEY));
        assertEquals("promo_code", PROMO_CODE, appLinkData.getPromotionCode());
    }

    @Test
    public void testCreateFromJson() {
        AppLinkData appLinkData =
                AppLinkData.createFromActivity(new MockActivityWithJsonData(JSON_DATA_REGULAR));
        assertNotNull("app link data not null", appLinkData);
        assertEquals("ref param", FB_REF_VALUE, appLinkData.getRef());
        assertEquals("target_url", TARGET_URI_STRING, appLinkData.getTargetUri().toString());
        Bundle args = appLinkData.getArgumentBundle();
        assertNotNull("app link args not null", args);
        assertNull("user agent", args.getString(USER_AGENT_KEY));
        Bundle refererData = appLinkData.getRefererData();
        assertNull("referer data", refererData);
    }

    @Test
    public void testCreateFromJsonWithNestedArray() {
        AppLinkData appLinkData =
                AppLinkData.createFromActivity(
                        new MockActivityWithJsonData(JSON_DATA_REGULAR_WITH_NESTED_ARRAY));
        assertNull(appLinkData);
    }

    @Test
    public void testCreateFromJsonWithRefererData() {
        AppLinkData appLinkData =
                AppLinkData.createFromActivity(
                        new MockActivityWithJsonData(JSON_DATA_WITH_REFERER_DATA));
        assertNotNull("app link data not null", appLinkData);
        assertEquals("ref param", FB_REF_VALUE, appLinkData.getRef());
        assertEquals("target_url", TARGET_URI_STRING, appLinkData.getTargetUri().toString());
        Bundle args = appLinkData.getArgumentBundle();
        assertNotNull("app link args not null", args);
        assertNull("user agent", args.getString(USER_AGENT_KEY));
        Bundle refererData = appLinkData.getRefererData();
        assertNotNull("referer data", refererData);
        assertEquals("ref param in referer data", FB_REF_VALUE, refererData.getString(FB_REF_KEY));
        assertEquals("extra param", EXTRA_ARGS_VALUE, refererData.getString(EXTRA_ARGS_KEY));
    }

    @Test
    public void testCreateFromJsonWithDeeplinkContext() {
        AppLinkData appLinkData =
                AppLinkData.createFromActivity(
                        new MockActivityWithJsonData(JSON_DATA_WITH_DEEPLINK_CONTEXT));
        assertNotNull("app link data not null", appLinkData);
        assertEquals("ref param", FB_REF_VALUE, appLinkData.getRef());
        assertEquals("target_url", TARGET_URI_STRING, appLinkData.getTargetUri().toString());
        assertEquals("promo_code", PROMO_CODE, appLinkData.getPromotionCode());
        Bundle args = appLinkData.getArgumentBundle();
        assertNotNull("app link args not null", args);
        assertNull("user agent", args.getString(USER_AGENT_KEY));
        Bundle refererData = appLinkData.getRefererData();
        assertNull("referer data", refererData);
    }

    @Test
    public void testGetAppLinkData() throws JSONException {
        Intent intent = new Intent();
        intent.putExtra(BUNDLE_AL_APPLINK_DATA_KEY, new Bundle());
        AppLinkData data;
        String urlString;
        JSONObject expectedData;

        // Case 1: url without host and data
        urlString = "fb123://";
        intent.setData(Uri.parse(urlString));
        data = AppLinkData.createFromAlApplinkData(intent);
        assertNull(data.getAppLinkData());

        // Case 2: url with data
        urlString = "fb123://applinks?al_applink_data=%7B%22product_id%22%3A+123%2C+%22is_fb_auto_applink%22%3A+true%7D";
        intent.setData(Uri.parse(urlString));
        data = AppLinkData.createFromAlApplinkData(intent);
        expectedData = new JSONObject();
        expectedData.put("product_id", 123);
        expectedData.put("is_fb_auto_applink", true);
        TestUtils.assertEquals(expectedData, data.getAppLinkData());
    }

    @Test
    public void testIsAutoAppLink() {
        Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
        FacebookSdk.setApplicationId("123");
        Intent intent = new Intent();
        intent.putExtra(BUNDLE_AL_APPLINK_DATA_KEY, new Bundle());
        AppLinkData data;
        String urlString;

        // Case 1: url without host and data
        urlString = "fb123://";
        intent.setData(Uri.parse(urlString));
        data = AppLinkData.createFromAlApplinkData(intent);
        assertFalse(data.isAutoAppLink());

        // Case 2: url with al_applink_data but without flag
        urlString = "fb123://";
        intent.setData(Uri.parse(urlString));
        data = AppLinkData.createFromAlApplinkData(intent);
        assertFalse(data.isAutoAppLink());

        // Case 3: url with both al_applink_data and flag
        urlString = "fb123://applinks?al_applink_data=%7B%22product_id%22%3A+123%2C+%22is_fb_auto_applink%22%3A+true%7D";
        intent.setData(Uri.parse(urlString));
        data = AppLinkData.createFromAlApplinkData(intent);
        assertTrue(data.isAutoAppLink());

        // Case 4: url with wrong app id
        urlString = "fb1234://applinks?al_applink_data=%7B%22product_id%22%3A+%22123%22%7D";
        intent.setData(Uri.parse(urlString));
        data = AppLinkData.createFromAlApplinkData(intent);
        assertFalse(data.isAutoAppLink());
    }

    @Test
    public void testParcelableAppLinkData() {
        Intent intent = new MockActivityWithAppLinkData().getIntent();
        intent.putExtra(BUNDLE_AL_APPLINK_DATA_KEY, new Bundle());
        String urlString =
                "fb123://applinks?al_applink_data=%7B%22product_id%22%3A+123%2C+%22is_fb_auto_applink%22%3A+true%7D";
        intent.setData(Uri.parse(urlString));
        AppLinkData data = AppLinkData.createFromAlApplinkData(intent);

        intent.putExtra(AppLinks.AUTO_APPLINK_DATA_KEY, data);
        AppLinkData parcelableData = intent.getParcelableExtra(AppLinks.AUTO_APPLINK_DATA_KEY);
        assertEquals(data.getPromotionCode(), parcelableData.getPromotionCode());
        assertEquals(data.getRef(), parcelableData.getRef());
        TestUtils.assertEqualContentsWithoutOrder(data.getArgumentBundle(), parcelableData.getArgumentBundle());
        TestUtils.assertEquals(data.getAppLinkData(), parcelableData.getAppLinkData());
    }
}
