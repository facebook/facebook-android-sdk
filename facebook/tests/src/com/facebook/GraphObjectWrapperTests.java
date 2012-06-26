/**
 * Copyright 2010 Facebook, Inc.
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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;

public final class GraphObjectWrapperTests extends AndroidTestCase {

    public void testCreateEmptyGraphObject() {
        GraphObject graphObject = GraphObjectWrapper.createGraphObject();
    }

    public void testCanTreatAsMap() {
        GraphObject graphObject = GraphObjectWrapper.createGraphObject();

        graphObject.put("hello", "world");
        assertEquals("world", (String) graphObject.get("hello"));
    }

    public void testCanTreatAsGraphPlace() {
        GraphPlace graphPlace = GraphObjectWrapper.createGraphObject(GraphPlace.class);

        graphPlace.setName("hello");
        assertEquals("hello", graphPlace.getName());
    }

    public void testCanTreatAsGraphUser() {
        GraphUser graphUser = GraphObjectWrapper.createGraphObject(GraphUser.class);

        graphUser.setFirstName("Michael");
        assertEquals("Michael", graphUser.getFirstName());
        assertEquals("Michael", graphUser.get("first_name"));

        graphUser.put("last_name", "Scott");
        assertEquals("Scott", graphUser.get("last_name"));
        assertEquals("Scott", graphUser.getLastName());
    }

    public void testCanCastBetweenGraphObjectTypes() {
        GraphObject graphObject = GraphObjectWrapper.createGraphObject();

        graphObject.put("first_name", "Mickey");

        GraphUser graphUser = graphObject.cast(GraphUser.class);
        assertTrue(graphUser != null);
        // Should see the name we set earlier as a GraphObject.
        assertEquals("Mickey", graphUser.getFirstName());

        // Changes to GraphUser should be reflected in GraphObject version.
        graphUser.setLastName("Mouse");
        assertEquals("Mouse", graphObject.get("last_name"));
    }

    public void testCanSetComplexTypes() {
        GraphLocation graphLocation = GraphObjectWrapper.createGraphObject(GraphLocation.class);
        graphLocation.setCity("Seattle");

        GraphPlace graphPlace = GraphObjectWrapper.createGraphObject(GraphPlace.class);
        graphPlace.setLocation(graphLocation);

        assertEquals(graphLocation, graphPlace.getLocation());
        assertEquals("Seattle", graphPlace.getLocation().getCity());
    }

    public void testCanConvertFromJSON() throws JSONException {
        JSONObject jsonLocation = new JSONObject();
        jsonLocation.put("city", "Paris");
        jsonLocation.put("country", "France");

        JSONObject jsonPlace = new JSONObject();
        jsonPlace.put("location", jsonLocation);
        jsonPlace.put("name", "Eiffel Tower");

        GraphPlace graphPlace = GraphObjectWrapper.wrapJson(jsonPlace, GraphPlace.class);
        GraphLocation graphLocation = graphPlace.getLocation();
        assertEquals("Paris", graphLocation.getCity());
    }

    public void testCanConvertFromGraphObject() throws JSONException {
        GraphObject graphObject = GraphObjectWrapper.createGraphObject();
        graphObject.put("city", "Paris");
        graphObject.put("country", "France");

        JSONObject jsonPlace = new JSONObject();
        jsonPlace.put("location", graphObject);
        jsonPlace.put("name", "Eiffel Tower");

        GraphPlace graphPlace = GraphObjectWrapper.wrapJson(jsonPlace, GraphPlace.class);
        GraphLocation graphLocation = graphPlace.getLocation();
        assertEquals("Paris", graphLocation.getCity());
    }

    private abstract class GraphObjectClass implements GraphObject {
    }

    public void testCantWrapNonInterface() {
        try {
            GraphObjectWrapper.createGraphObject(GraphObjectClass.class);
            fail("Expected exception");
        } catch (FacebookGraphObjectException exception) {
        }
    }

    private interface BadMethodNameGraphObject extends GraphObject {
        void floppityFlee();
    }

    public void testCantWrapBadMethodName() {
        try {
            GraphObjectWrapper.createGraphObject(BadMethodNameGraphObject.class);
            fail("Expected exception");
        } catch (FacebookGraphObjectException exception) {
        }
    }

    private interface BadGetterNameGraphObject extends GraphObject {
        void get();
    }

    public void testCantWrapBadGetterName() {
        try {
            GraphObjectWrapper.createGraphObject(BadGetterNameGraphObject.class);
            fail("Expected exception");
        } catch (FacebookGraphObjectException exception) {
        }
    }

    private interface BadGetterParamsGraphObject extends GraphObject {
        Object getFoo(Object obj);
    }

    public void testCantWrapBadGetterParams() {
        try {
            GraphObjectWrapper.createGraphObject(BadGetterParamsGraphObject.class);
            fail("Expected exception");
        } catch (FacebookGraphObjectException exception) {
        }
    }

    private interface BadGetterReturnTypeGraphObject extends GraphObject {
        void getFoo();
    }

    public void testCantWrapBadGetterReturnType() {
        try {
            GraphObjectWrapper.createGraphObject(BadGetterReturnTypeGraphObject.class);
            fail("Expected exception");
        } catch (FacebookGraphObjectException exception) {
        }
    }

    private interface BadSetterNameGraphObject extends GraphObject {
        void set();
    }

    public void testCantWrapBadSetterName() {
        try {
            GraphObjectWrapper.createGraphObject(BadSetterNameGraphObject.class);
            fail("Expected exception");
        } catch (FacebookGraphObjectException exception) {
        }
    }

    private interface BadSetterParamsGraphObject extends GraphObject {
        void setFoo();
    }

    public void testCantWrapBadSetterParams() {
        try {
            GraphObjectWrapper.createGraphObject(BadSetterParamsGraphObject.class);
            fail("Expected exception");
        } catch (FacebookGraphObjectException exception) {
        }
    }

    private interface BadSetterReturnTypeGraphObject extends GraphObject {
        Object setFoo(Object obj);
    }

    public void testCantWrapBadSetterReturnType() {
        try {
            GraphObjectWrapper.createGraphObject(BadSetterReturnTypeGraphObject.class);
            fail("Expected exception");
        } catch (FacebookGraphObjectException exception) {
        }
    }

    private interface BadBaseInterfaceGraphObject extends BadSetterReturnTypeGraphObject {
        void setBar(Object obj);

        Object getBar();
    }

    public void testCantWrapBadBaseInterface() {
        try {
            GraphObjectWrapper.createGraphObject(BadBaseInterfaceGraphObject.class);
            fail("Expected exception");
        } catch (FacebookGraphObjectException exception) {
        }
    }

    public void testObjectEquals() {
        GraphObject graphObject = GraphObjectWrapper.createGraphObject();
        graphObject.put("aKey", "aValue");

        assertTrue(graphObject.equals(graphObject));

        GraphPlace graphPlace = graphObject.cast(GraphPlace.class);
        assertTrue(graphObject.equals(graphPlace));
        assertTrue(graphPlace.equals(graphObject));

        GraphObject aDifferentGraphObject = GraphObjectWrapper.createGraphObject();
        aDifferentGraphObject.put("aKey", "aDifferentValue");
        assertFalse(graphObject.equals(aDifferentGraphObject));
    }

    public void testMapClear() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);

        assertEquals(1, jsonObject.length());

        graphObject.clear();

        assertEquals(0, jsonObject.length());
    }

    public void testMapContainsKey() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);

        assertTrue(graphObject.containsKey("hello"));
        assertFalse(graphObject.containsKey("hocus"));
    }

    public void testMapContainsValue() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);

        assertTrue(graphObject.containsValue("world"));
        assertFalse(graphObject.containsValue("pocus"));
    }

    public void testMapEntrySet() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);

        Set<Entry<String, Object>> entrySet = graphObject.entrySet();
        assertEquals(2, entrySet.size());
    }

    public void testMapGet() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);
        assertEquals("world", graphObject.get("hello"));
        assertTrue(graphObject.get("fred") == null);
    }

    public void testMapIsEmpty() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);
        assertTrue(graphObject.isEmpty());

        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");
        assertFalse(graphObject.isEmpty());
    }

    public void testMapKeySet() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);

        Set<String> keySet = graphObject.keySet();
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("hello"));
        assertTrue(keySet.contains("hocus"));
        assertFalse(keySet.contains("world"));
    }

    public void testMapPut() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);
        graphObject.put("hello", "world");
        graphObject.put("hocus", "pocus");

        assertEquals("pocus", jsonObject.get("hocus"));
        assertEquals(2, jsonObject.length());
    }

    public void testMapPutAll() throws JSONException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("hello", "world");
        map.put("hocus", "pocus");

        JSONObject jsonObject = new JSONObject();
        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);

        graphObject.putAll(map);
        assertEquals("pocus", jsonObject.get("hocus"));
        assertEquals(2, jsonObject.length());
    }

    public void testMapRemove() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);
        graphObject.remove("hello");

        assertEquals(1, jsonObject.length());
    }

    public void testMapSize() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);

        assertEquals(2, graphObject.size());
    }

    public void testMapValues() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);

        Collection<Object> values = graphObject.values();

        assertEquals(2, values.size());
        assertTrue(values.contains("world"));
    }

    public void testGetInnerJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hello", "world");
        jsonObject.put("hocus", "pocus");

        GraphObject graphObject = GraphObjectWrapper.wrapJson(jsonObject);

        assertEquals(jsonObject, graphObject.getInnerJSONObject());
    }

    public void testSettingGraphObjectProxyStoresJSONObject() throws JSONException {
        GraphPlace graphPlace = GraphObjectWrapper.createGraphObject(GraphPlace.class);
        GraphLocation graphLocation = GraphObjectWrapper.createGraphObject(GraphLocation.class);

        graphPlace.setLocation(graphLocation);

        assertEquals(graphLocation.getInnerJSONObject(), graphPlace.getInnerJSONObject().get("location"));

    }
}
