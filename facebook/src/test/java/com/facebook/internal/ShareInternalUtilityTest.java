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

import com.facebook.FacebookTestCase;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ShareInternalUtilityTest extends FacebookTestCase {

    @Test
    public void testRemoveNamespaceFromNullOGJsonObject() {
        assertNull(ShareInternalUtility.removeNamespacesFromOGJsonObject(null, false));
    }

    @Test
    public void testRemoveNamespaceFromComplexOGJsonObject() {
        try {
            JSONObject testObject = getJsonOGActionTestObject();
            testObject = ShareInternalUtility.removeNamespacesFromOGJsonObject(testObject, false);
            JSONObject expectedResult = getJsonOGActionTestObjectWithoutNamespace();
            if(!simpleJsonObjComparer(testObject, expectedResult)){
                fail(String.format(
                        Locale.ROOT,
                        "Actual: %s\nExpected: %s",
                        testObject.toString(),
                        expectedResult.toString()));
            }
        } catch (JSONException ex) {
            // Fail
            assertNotNull(ex);
        }
    }

    @Test
    public void testJsonSerializationOfOpenGraph() {
        String placeId = "1";
        ShareOpenGraphContent content = new ShareOpenGraphContent.Builder()
                .setAction(
                        new ShareOpenGraphAction.Builder()
                                .putStringArrayList("tags", new ArrayList<String>() {{
                                    add("2");
                                    add("4");
                                }})
                                .build()
                ).setPeopleIds(new ArrayList<String>() {{
                    add("1");
                    add("1");
                    add("2");
                    add("3");
                }}).setPlaceId(placeId)
                .build();

        try {
            JSONObject object = ShareInternalUtility.toJSONObjectForCall(null, content);
            List<String> peopleIds = Utility.jsonArrayToStringList(object.getJSONArray("tags"));
            assertEquals(4, peopleIds.size());
            for (int i = 1; i < 5; ++i) {
                assertTrue(peopleIds.contains(new Integer(i).toString()));
            }

            assertEquals(placeId, object.getString("place"));
        } catch (JSONException ex) {
            // Fail
            assertNotNull(ex);
            return;
        }
    }

    @Test
    public void testJsonSerializationOfOpenGraphExistingPlace() {
        ShareOpenGraphContent content = new ShareOpenGraphContent.Builder()
                .setAction(
                        new ShareOpenGraphAction.Builder()
                                .putString("place", "1")
                                .build()
                ).setPlaceId("2")
                .build();

        try {
            JSONObject object = ShareInternalUtility.toJSONObjectForCall(null, content);
            assertEquals("1", object.getString("place"));
        } catch (JSONException ex) {
            // Fail
            assertNotNull(ex);
            return;
        }
    }

    private static JSONObject getJsonOGActionTestObject() throws JSONException {
        JSONObject testAction = new JSONObject();
        testAction.put("og:field", 1);
        testAction.put("namespaced:custom:field", 3);

        JSONObject testOGContent = getJsonOGContentTestObject();
        testAction.put("namespaced:content", testOGContent);
        testAction.put("array", getJsonOGArrayTestObject());

        return testAction;
    }

    private static JSONObject getJsonOGActionTestObjectWithoutNamespace() throws JSONException {
        JSONObject testAction = new JSONObject();
        testAction.put("field", 1);
        testAction.put("custom:field", 3);

        JSONObject testOGContent = getJsonOGContentTestObjectWithoutNamespace();
        testAction.put("content", testOGContent);
        testAction.put("array", getJsonOGArrayTestObjectWithoutNamespace());

        return testAction;
    }

    private static JSONArray getJsonOGArrayTestObject() throws JSONException {
        JSONArray testArray = new JSONArray();
        testArray.put(10);
        testArray.put(getJsonOGContentTestObject());
        return testArray;
    }

    private static JSONArray getJsonOGArrayTestObjectWithoutNamespace() throws JSONException {
        JSONArray testArray = new JSONArray();
        testArray.put(10);
        testArray.put(getJsonOGContentTestObjectWithoutNamespace());
        return testArray;
    }

    private static JSONObject getJsonOGContentTestObject() throws JSONException {
        JSONObject testOGContent = new JSONObject();
        testOGContent.put("fbsdk:create", true);
        testOGContent.put("namespaced:field", 4);
        testOGContent.put("og:field", 5);
        testOGContent.put("custom:namespaced:field", 6);

        JSONObject innerContent = new JSONObject();
        innerContent.put("namespaced:field", 7);
        innerContent.put("og:field", 8);
        testOGContent.put("namespaced:innerContent", innerContent);
        return testOGContent;
    }

    private static JSONObject getJsonOGContentTestObjectWithoutNamespace() throws JSONException {
        JSONObject testOGContent = new JSONObject();
        testOGContent.put("fbsdk:create", true);
        testOGContent.put("field", 5);

        JSONObject innerContent = new JSONObject();
        innerContent.put("field", 8);
        JSONObject innerData = new JSONObject();
        innerData.put("field", 7);
        innerContent.put("data", innerData);

        JSONObject data = new JSONObject();
        data.put("field", 4);
        data.put("namespaced:field", 6);
        data.put("innerContent", innerContent);

        testOGContent.put("data", data);
        return testOGContent;
    }

    private boolean simpleJsonObjComparer(JSONObject obj1, JSONObject obj2) {
        if (obj1.names().length() != obj2.names().length()) {
            return false;
        }

        Iterator<String> keys = obj1.keys();
        while (keys.hasNext()) {
            try {
                String key = keys.next();
                Object value1 = obj1.get(key);
                Object value2 = obj2.get(key);
                if (!jsonObjectValueComparer(value1, value2)){
                    return false;
                }
            } catch (Exception ex) {
                return false;
            }
        }

        return true;
    }

    private boolean simpleJsonArrayComparer(JSONArray array1, JSONArray array2)
            throws JSONException{
        if(array1.length() != array2.length()) {
            return  false;
        }

        for(int i = 0; i < array1.length(); ++i) {
            if (!jsonObjectValueComparer(array1.get(i), array2.get(i))){
                return false;
            }
        }
        return true;
    }

    private boolean jsonObjectValueComparer(Object value1, Object value2)
    throws JSONException{
        if (value1 instanceof JSONObject) {
            if (!simpleJsonObjComparer((JSONObject) value1, (JSONObject) value2)) {
                return false;
            }
        } else if (value1 instanceof JSONArray) {
            if (!simpleJsonArrayComparer((JSONArray) value1, (JSONArray) value2)) {
                return false;
            }
        } else if (value1 != value2) {
            return false;
        }

        return true;
    }
}
