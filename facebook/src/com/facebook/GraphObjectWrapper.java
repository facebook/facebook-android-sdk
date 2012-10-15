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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Creates proxies that implement GraphObject, GraphObjectList, and their derived types. These proxies allow access
 * to underlying collections and name/value property bags via strongly-typed property getters and setters.
 * <p/>
 * This supports get/set properties that use primitive types, JSON types, Date, other GraphObject types, Iterable,
 * Collection, List, and GraphObjectList.
 */
public final class GraphObjectWrapper {
    private static final HashSet<Class<?>> verifiedGraphObjectClasses = new HashSet<Class<?>>();
    private static final SimpleDateFormat[] dateFormats = new SimpleDateFormat[] {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US), new SimpleDateFormat("yyyy-MM-dd", Locale.US), };

    // No objects of this type should exist.
    private GraphObjectWrapper() {
    }

    /**
     * Creates a GraphObject proxy that provides typed access to the data in an underlying JSONObject.
     * @param json the JSONObject containing the data to be exposed
     * @return a GraphObject that represents the underlying data
     *
     * @throws FacebookException
     *            If the passed in Class is not a valid GraphObject interface
     */
    public static GraphObject createGraphObject(JSONObject json) {
        return createGraphObject(json, GraphObject.class);
    }

    /**
     * Creates a GraphObject-derived proxy that provides typed access to the data in an underlying JSONObject.
     * @param json the JSONObject containing the data to be exposed
     * @param graphObjectClass the GraphObject-derived type to return
     * @return a graphObjectClass that represents the underlying data
     *
     * @throws FacebookException
     *            If the passed in Class is not a valid GraphObject interface
     */
    public static <T extends GraphObject> T createGraphObject(JSONObject json, Class<T> graphObjectClass) {
        return createGraphObjectProxy(graphObjectClass, json);
    }

    /**
     * Creates a GraphObject proxy that initially contains no data.
     * @return a GraphObject with no data
     *
     * @throws FacebookException
     *            If the passed in Class is not a valid GraphObject interface
     */
    public static GraphObject createGraphObject() {
        return createGraphObject(GraphObject.class);
    }

    /**
     * Creates a GraphObject-derived proxy that initially contains no data.
     * @param graphObjectClass the GraphObject-derived type to return
     * @return a graphObjectClass with no data
     *
     * @throws FacebookException
     *            If the passed in Class is not a valid GraphObject interface
     */
    public static <T extends GraphObject> T createGraphObject(Class<T> graphObjectClass) {
        return createGraphObjectProxy(graphObjectClass, new JSONObject());
    }

    /**
     * Determines if two GraphObjects represent the same underlying graph object, based on their IDs.
     * @param a a graph object
     * @param b another graph object
     * @return true if both graph objects have an ID and it is the same ID, false otherwise
     */
    public static boolean hasSameId(GraphObject a, GraphObject b) {
        if (a == null || b == null || !a.asMap().containsKey("id") || !b.asMap().containsKey("id")) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        Object idA = a.getProperty("id");
        Object idB = b.getProperty("id");
        if (idA == null || idB == null || !(idA instanceof String) || !(idB instanceof String)) {
            return false;
        }
        return idA.equals(idB);
    }

    static <T> GraphObjectList<T> wrapArray(JSONArray array, Class<T> objectClass) {
        return new GraphObjectListImpl<T>(array, objectClass);
    }

    static <T> GraphObjectList<T> createArray(Class<T> objectClass) {
        return wrapArray(new JSONArray(), objectClass);
    }

    private static <T extends GraphObject> T createGraphObjectProxy(Class<T> graphObjectClass, JSONObject state) {
        verifyCanProxyClass(graphObjectClass);

        Class<?>[] interfaces = new Class[] { graphObjectClass };
        GraphObjectProxy graphObjectProxy = new GraphObjectProxy(state, graphObjectClass);

        @SuppressWarnings("unchecked")
        T graphObject = (T) Proxy.newProxyInstance(GraphObject.class.getClassLoader(), interfaces, graphObjectProxy);

        return graphObject;
    }

    private static Map<String, Object> createGraphObjectProxyForMap(JSONObject state) {
        Class<?>[] interfaces = new Class[]{Map.class};
        GraphObjectProxy graphObjectProxy = new GraphObjectProxy(state, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> graphObject = (Map<String, Object>) Proxy
                .newProxyInstance(GraphObject.class.getClassLoader(), interfaces, graphObjectProxy);

        return graphObject;
    }

    private static synchronized <T extends GraphObject> boolean hasClassBeenVerified(Class<T> graphObjectClass) {
        return verifiedGraphObjectClasses.contains(graphObjectClass);
    }

    private static synchronized <T extends GraphObject> void recordClassHasBeenVerified(Class<T> graphObjectClass) {
        verifiedGraphObjectClasses.add(graphObjectClass);
    }

    private static <T extends GraphObject> void verifyCanProxyClass(Class<T> graphObjectClass) {
        if (hasClassBeenVerified(graphObjectClass)) {
            return;
        }

        if (!graphObjectClass.isInterface()) {
            throw new FacebookGraphObjectException("GraphObjectWrapper can only wrap interfaces, not class: "
                    + graphObjectClass.getName());
        }

        Method[] methods = graphObjectClass.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            int parameterCount = method.getParameterTypes().length;
            Class<?> returnType = method.getReturnType();
            boolean hasPropertyNameOverride = method.isAnnotationPresent(PropertyName.class);

            if (method.getDeclaringClass().isAssignableFrom(GraphObject.class)) {
                // Don't worry about any methods from GraphObject or one of its base classes.
                continue;
            } else if (parameterCount == 1 && returnType == Void.TYPE) {
                if (hasPropertyNameOverride) {
                    // If a property override is present, it MUST be valid. We don't fallback
                    // to using the method name
                    if (!Utility.isNullOrEmpty(method.getAnnotation(PropertyName.class).value())) {
                        continue;
                    }
                } else if (methodName.startsWith("set") && methodName.length() > 3) {
                    // Looks like a valid setter
                    continue;
                }
            } else if (parameterCount == 0 && returnType != Void.TYPE) {
                if (hasPropertyNameOverride) {
                    // If a property override is present, it MUST be valid. We don't fallback
                    // to using the method name
                    if (!Utility.isNullOrEmpty(method.getAnnotation(PropertyName.class).value())) {
                        continue;
                    }
                } else if (methodName.startsWith("get") && methodName.length() > 3) {
                    // Looks like a valid getter
                    continue;
                }
            }

            throw new FacebookGraphObjectException("GraphObjectWrapper can't proxy method: " + method.toString());
        }

        recordClassHasBeenVerified(graphObjectClass);
    }

    // If expectedType is a generic type, expectedTypeAsParameterizedType must be provided in order to determine
    // generic parameter types.
    static <U> U coerceValueToExpectedType(Object value, Class<U> expectedType,
            ParameterizedType expectedTypeAsParameterizedType) {
        if (value == null) {
            return null;
        }

        Class<?> valueType = value.getClass();
        if (expectedType.isAssignableFrom(valueType)) {
            @SuppressWarnings("unchecked")
            U result = (U) value;
            return result;
        }

        if (expectedType.isPrimitive()) {
            // If the result is a primitive, let the runtime succeed or fail at unboxing it.
            @SuppressWarnings("unchecked")
            U result = (U) value;
            return result;
        }

        if (GraphObject.class.isAssignableFrom(expectedType)) {
            @SuppressWarnings("unchecked")
            Class<? extends GraphObject> graphObjectClass = (Class<? extends GraphObject>) expectedType;

            // We need a GraphObject, but we don't have one.
            if (JSONObject.class.isAssignableFrom(valueType)) {
                // We can wrap a JSONObject as a GraphObject.
                @SuppressWarnings("unchecked")
                U result = (U) createGraphObjectProxy(graphObjectClass, (JSONObject) value);
                return result;
            } else if (GraphObject.class.isAssignableFrom(valueType)) {
                // We can cast a GraphObject-derived class to another GraphObject-derived class.
                @SuppressWarnings("unchecked")
                U result = (U) ((GraphObject) value).cast(graphObjectClass);
                return result;
            } else {
                throw new FacebookGraphObjectException("Can't create GraphObject from " + valueType.getName());
            }
        } else if (Iterable.class.equals(expectedType) || Collection.class.equals(expectedType)
                || List.class.equals(expectedType) || GraphObjectList.class.equals(expectedType)) {
            if (expectedTypeAsParameterizedType == null) {
                throw new FacebookGraphObjectException("can't infer generic type of: " + expectedType.toString());
            }

            Type[] actualTypeArguments = expectedTypeAsParameterizedType.getActualTypeArguments();

            if (actualTypeArguments == null || actualTypeArguments.length != 1
                    || !(actualTypeArguments[0] instanceof Class<?>)) {
                throw new FacebookGraphObjectException(
                        "Expect collection properties to be of a type with exactly one generic parameter.");
            }
            Class<?> collectionGenericArgument = (Class<?>) actualTypeArguments[0];

            if (JSONArray.class.isAssignableFrom(valueType)) {
                JSONArray jsonArray = (JSONArray) value;
                @SuppressWarnings("unchecked")
                U result = (U) wrapArray(jsonArray, collectionGenericArgument);
                return result;
            } else {
                throw new FacebookGraphObjectException("Can't create Collection from " + valueType.getName());
            }
        } else if (String.class.equals(expectedType)) {
            if (Number.class.isAssignableFrom(valueType)) {
                @SuppressWarnings("unchecked")
                U result = (U) String.format("%d", value);
                return result;
            }
        } else if (Date.class.equals(expectedType)) {
            if (String.class.isAssignableFrom(valueType)) {
                for (SimpleDateFormat format : dateFormats) {
                    try {
                        Date date = format.parse((String) value);
                        if (date != null) {
                            @SuppressWarnings("unchecked")
                            U result = (U) date;
                            return result;
                        }
                    } catch (ParseException e) {
                        // Keep going.
                    }
                }
            }
        }
        throw new FacebookGraphObjectException("Can't convert type" + valueType.getName() + " to "
                + expectedType.getName());
    }

    private static Object getUnderlyingJSONObject(Object obj) {
        Class<?> objClass = obj.getClass();
        if (GraphObject.class.isAssignableFrom(objClass)) {
            GraphObject graphObject = (GraphObject) obj;
            return graphObject.getInnerJSONObject();
        } else if (GraphObjectList.class.isAssignableFrom(objClass)) {
            GraphObjectList<?> graphObjectList = (GraphObjectList<?>) obj;
            return graphObjectList.getInnerJSONArray();
        }
        return obj;
    }

    private abstract static class ProxyBase<STATE> implements InvocationHandler {
        // Pre-loaded Method objects for the methods in java.lang.Object
        private static final String EQUALS_METHOD = "equals";
        private static final String TOSTRING_METHOD = "toString";

        protected final STATE state;

        protected ProxyBase(STATE state) {
            this.state = state;
        }

        // Declared to return Object just to simplify implementation of proxy helpers.
        protected final Object throwUnexpectedMethodSignature(Method method) {
            throw new FacebookGraphObjectException(getClass().getName() + " got an unexpected method signature: "
                    + method.toString());
        }

        protected final Object proxyObjectMethods(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals(EQUALS_METHOD)) {
                Object other = args[0];

                if (other == null) {
                    return false;
                }

                InvocationHandler handler = Proxy.getInvocationHandler(other);
                if (!(handler instanceof GraphObjectProxy)) {
                    return false;
                }
                GraphObjectProxy otherProxy = (GraphObjectProxy) handler;
                return this.state.equals(otherProxy.state);
            } else if (methodName.equals(TOSTRING_METHOD)) {
                return toString();
            }

            // For others, just defer to the implementation object.
            return method.invoke(this.state, args);
        }

    }

    private final static class GraphObjectProxy extends ProxyBase<JSONObject> {
        private static final String CLEAR_METHOD = "clear";
        private static final String CONTAINSKEY_METHOD = "containsKey";
        private static final String CONTAINSVALUE_METHOD = "containsValue";
        private static final String ENTRYSET_METHOD = "entrySet";
        private static final String GET_METHOD = "get";
        private static final String ISEMPTY_METHOD = "isEmpty";
        private static final String KEYSET_METHOD = "keySet";
        private static final String PUT_METHOD = "put";
        private static final String PUTALL_METHOD = "putAll";
        private static final String REMOVE_METHOD = "remove";
        private static final String SIZE_METHOD = "size";
        private static final String VALUES_METHOD = "values";
        private static final String CAST_METHOD = "cast";
        private static final String CASTTOMAP_METHOD = "asMap";
        private static final String GETPROPERTY_METHOD = "getProperty";
        private static final String SETPROPERTY_METHOD = "setProperty";
        private static final String REMOVEPROPERTY_METHOD = "removeProperty";
        private static final String GETINNERJSONOBJECT_METHOD = "getInnerJSONObject";

        private final Class<?> graphObjectClass;

        public GraphObjectProxy(JSONObject state, Class<?> graphObjectClass) {
            super(state);
            this.graphObjectClass = graphObjectClass;
        }

        @Override
        public String toString() {
            return String.format("GraphObject{graphObjectClass=%s, state=%s}", graphObjectClass.getSimpleName(), state);
        }

        @Override
        public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Class<?> declaringClass = method.getDeclaringClass();

            if (declaringClass == Object.class) {
                return proxyObjectMethods(proxy, method, args);
            } else if (declaringClass == Map.class) {
                return proxyMapMethods(method, args);
            } else if (declaringClass == GraphObject.class) {
                return proxyGraphObjectMethods(proxy, method, args);
            } else if (GraphObject.class.isAssignableFrom(declaringClass)) {
                return proxyGraphObjectGettersAndSetters(method, args);
            }

            return throwUnexpectedMethodSignature(method);
        }

        private final Object proxyMapMethods(Method method, Object[] args) {
            String methodName = method.getName();
            if (methodName.equals(CLEAR_METHOD)) {
                Utility.jsonObjectClear(this.state);
                return null;
            } else if (methodName.equals(CONTAINSKEY_METHOD)) {
                return this.state.has((String) args[0]);
            } else if (methodName.equals(CONTAINSVALUE_METHOD)) {
                return Utility.jsonObjectContainsValue(this.state, args[0]);
            } else if (methodName.equals(ENTRYSET_METHOD)) {
                return Utility.jsonObjectEntrySet(this.state);
            } else if (methodName.equals(GET_METHOD)) {
                return this.state.opt((String) args[0]);
            } else if (methodName.equals(ISEMPTY_METHOD)) {
                return this.state.length() == 0;
            } else if (methodName.equals(KEYSET_METHOD)) {
                return Utility.jsonObjectKeySet(this.state);
            } else if (methodName.equals(PUT_METHOD)) {
                return setJSONProperty(args);
            } else if (methodName.equals(PUTALL_METHOD)) {
                Map<String, Object> map = null;
                if (args[0] instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> castMap = (Map<String, Object>) args[0];
                    map = castMap;
                } else if (args[0] instanceof GraphObject) {
                    map = ((GraphObject) args[0]).asMap();
                }
                Utility.jsonObjectPutAll(this.state, map);
                return null;
            } else if (methodName.equals(REMOVE_METHOD)) {
                this.state.remove((String) args[0]);
                return null;
            } else if (methodName.equals(SIZE_METHOD)) {
                return this.state.length();
            } else if (methodName.equals(VALUES_METHOD)) {
                return Utility.jsonObjectValues(this.state);
            }

            return throwUnexpectedMethodSignature(method);
        }

        private final Object proxyGraphObjectMethods(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if (methodName.equals(CAST_METHOD)) {
                @SuppressWarnings("unchecked")
                Class<? extends GraphObject> graphObjectClass = (Class<? extends GraphObject>) args[0];

                if (graphObjectClass != null &&
                        graphObjectClass.isAssignableFrom(this.graphObjectClass)) {
                    return proxy;
                }
                return GraphObjectWrapper.createGraphObjectProxy(graphObjectClass, this.state);
            } else if (methodName.equals(GETINNERJSONOBJECT_METHOD)) {
                InvocationHandler handler = Proxy.getInvocationHandler(proxy);
                GraphObjectProxy otherProxy = (GraphObjectProxy) handler;
                return otherProxy.state;
            } else if (methodName.equals(CASTTOMAP_METHOD)) {
                return GraphObjectWrapper.createGraphObjectProxyForMap(this.state);
            } else if (methodName.equals(GETPROPERTY_METHOD)) {
                return state.opt((String) args[0]);
            } else if (methodName.equals(SETPROPERTY_METHOD)) {
                return setJSONProperty(args);
            } else if (methodName.equals(REMOVEPROPERTY_METHOD)) {
                this.state.remove((String) args[0]);
                return null;
            }

            return throwUnexpectedMethodSignature(method);
        }

        private final Object proxyGraphObjectGettersAndSetters(Method method, Object[] args) throws JSONException {
            String methodName = method.getName();
            int parameterCount = method.getParameterTypes().length;
            PropertyName propertyNameOverride = method.getAnnotation(PropertyName.class);

            String key = propertyNameOverride != null ? propertyNameOverride.value() :
                    Utility.convertCamelCaseToLowercaseWithUnderscores(methodName.substring(3));

            // If it's a get or a set on a GraphObject-derived class, we can handle it.
            if (parameterCount == 0) {
                // Has to be a getter. ASSUMPTION: The GraphObject-derived class has been verified
                Object value = this.state.opt(key);

                Class<?> expectedType = method.getReturnType();

                Type genericReturnType = method.getGenericReturnType();
                ParameterizedType parameterizedReturnType = null;
                if (genericReturnType instanceof ParameterizedType) {
                    parameterizedReturnType = (ParameterizedType) genericReturnType;
                }

                value = coerceValueToExpectedType(value, expectedType, parameterizedReturnType);

                return value;
            } else if (parameterCount == 1) {
                // Has to be a setter. ASSUMPTION: The GraphObject-derived class has been verified
                Object value = args[0];
                // If this is a wrapped object, store the underlying JSONObject instead, in order to serialize
                // correctly.
                if (GraphObject.class.isAssignableFrom(value.getClass())) {
                    value = ((GraphObject) value).getInnerJSONObject();
                } else if (GraphObjectList.class.isAssignableFrom(value.getClass())) {
                    value = ((GraphObjectList<?>) value).getInnerJSONArray();
                } else if (Iterable.class.isAssignableFrom(value.getClass())) {
                    JSONArray jsonArray = new JSONArray();
                    Iterable<?> iterable = (Iterable<?>) value;
                    for (Object o : iterable ) {
                        if (GraphObject.class.isAssignableFrom(o.getClass())) {
                            jsonArray.put(((GraphObject)o).getInnerJSONObject());
                        } else {
                            jsonArray.put(o);
                        }
                    }
                    value = jsonArray;
                }
                this.state.putOpt(key, value);
                return null;
            }

            return throwUnexpectedMethodSignature(method);
        }

        private Object setJSONProperty(Object[] args) {
            String name = (String) args[0];
            Object property = args[1];
            Object value = getUnderlyingJSONObject(property);
            try {
                state.putOpt(name, value);
            } catch (JSONException e) {
                throw new IllegalArgumentException(e);
            }
            return null;
        }
    }

    private final static class GraphObjectListImpl<T> extends AbstractList<T> implements GraphObjectList<T> {
        private final JSONArray state;
        private final Class<?> itemType;

        public GraphObjectListImpl(JSONArray state, Class<?> itemType) {
            Validate.notNull(state, "state");
            Validate.notNull(itemType, "itemType");

            this.state = state;
            this.itemType = itemType;
        }

        @Override
        public String toString() {
            return String.format("GraphObjectList{itemType=%s, state=%s}", itemType.getSimpleName(), state);
        }

        @Override
        public void add(int location, T object) {
            // We only support adding at the end of the list, due to JSONArray restrictions.
            if (location < 0) {
                throw new IndexOutOfBoundsException();
            } else if (location < size()) {
                throw new UnsupportedOperationException("Only adding items at the end of the list is supported.");
            }

            put(location, object);
        }

        @Override
        public T set(int location, T object) {
            checkIndex(location);

            T result = get(location);
            put(location, object);
            return result;
        }

        @Override
        public int hashCode() {
            return state.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (getClass() != obj.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked")
            GraphObjectListImpl<T> other = (GraphObjectListImpl<T>) obj;
            return state.equals(other.state);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T get(int location) {
            checkIndex(location);

            Object value = state.opt(location);

            // Class<?> expectedType = method.getReturnType();
            // Type genericType = method.getGenericReturnType();
            T result = (T) coerceValueToExpectedType(value, itemType, null);

            return result;
        }

        @Override
        public int size() {
            return state.length();
        }

        @Override
        public final <U extends GraphObject> GraphObjectList<U> castToListOf(Class<U> graphObjectClass) {
            if (GraphObject.class.isAssignableFrom(itemType)) {
                if (graphObjectClass.isAssignableFrom(itemType)) {
                    @SuppressWarnings("unchecked")
                    GraphObjectList<U> result = (GraphObjectList<U>)this;
                    return result;
                }

                return wrapArray(state, graphObjectClass);
            } else {
                throw new FacebookGraphObjectException("Can't cast GraphObjectCollection of non-GraphObject type "
                        + itemType);
            }
        }

        @Override
        public final JSONArray getInnerJSONArray() {
            return state;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        private void checkIndex(int index) {
            if (index < 0 || index >= state.length()) {
                throw new IndexOutOfBoundsException();
            }
        }

        private void put(int index, T obj) {
            Object underlyingObject = getUnderlyingJSONObject(obj);
            try {
                state.put(index, underlyingObject);
            } catch (JSONException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
