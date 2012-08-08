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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class GraphObjectWrapper {
    private static final HashSet<Class<?>> verifiedGraphObjectClasses = new HashSet<Class<?>>();
    private static final SimpleDateFormat[] dateFormats = new SimpleDateFormat[] {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US), new SimpleDateFormat("yyyy-MM-dd", Locale.US), };

    // No objects of this type should exist.
    private GraphObjectWrapper() {
    }

    public static GraphObject wrapJson(JSONObject json) {
        return wrapJson(json, GraphObject.class);
    }

    public static <T extends GraphObject> T wrapJson(JSONObject json, Class<T> graphObjectClass) {
        return createGraphObjectProxy(graphObjectClass, json);
    }

    public static GraphObject createGraphObject() {
        return createGraphObject(GraphObject.class);
    }

    public static <T extends GraphObject> T createGraphObject(Class<T> graphObjectClass) {
        return createGraphObjectProxy(graphObjectClass, new JSONObject());
    }

    public static boolean hasSameId(GraphObject a, GraphObject b) {
        if (a == null || b == null || !a.containsKey("id") || !b.containsKey("id")) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        Object idA = a.get("id");
        Object idB = b.get("id");
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
        GraphObjectProxy graphObjectProxy = new GraphObjectProxy(state);

        @SuppressWarnings("unchecked")
        T graphObject = (T) Proxy.newProxyInstance(GraphObject.class.getClassLoader(), interfaces, graphObjectProxy);

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

            if (method.getDeclaringClass().isAssignableFrom(GraphObject.class)) {
                // Don't worry about any methods from GraphObject or one of its base classes.
                continue;
            } else if (methodName.startsWith("set") && methodName.length() > 3 && parameterCount == 1
                    && returnType == Void.TYPE) {
                // Looks like a valid setter
                continue;
            } else if (methodName.startsWith("get") && methodName.length() > 3 && parameterCount == 0
                    && returnType != Void.TYPE) {
                // Looks like a valid getter
                continue;
            }

            throw new FacebookGraphObjectException("GraphObjectWrapper can't proxy method: " + method.toString());
        }

        recordClassHasBeenVerified(graphObjectClass);
    }

    // If expectedType is a generic type, expectedTypeAsParameterizedType must be provided in order to determine
    // generic parameter types.
    protected static <U> U coerceValueToExpectedType(Object value, Class<U> expectedType,
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
                // TODO once we are extracting JSONObjects out of GraphObjects and storing those, this will probably
                // go away.

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
            // TODO there are probably other conversions we want to do here, in order to be less strict about what
            // JSON we accept. JSONObject, for instance, will parse our 'id' field sometimes as Long, sometimes
            // as String, depending on the formatting. Need more robust test cases around this to determine what
            // set of conversions is appropriate.

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
        private static final Method equalsMethod;
        static {
            // We do this rather than use Class.getMethod because that is only available in API >= 9. It is simple
            // because we know there are no overloaded methods on either of these classes.
            Method[] objectMethods = Object.class.getDeclaredMethods();
            HashMap<String, Method> objectMethodMap = new HashMap<String, Method>();
            for (Method method : objectMethods) {
                objectMethodMap.put(method.getName(), method);
            }

            equalsMethod = objectMethodMap.get("equals");
        }

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
            if (method.equals(equalsMethod)) {
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
            }

            // For others, just defer to the implementation object.
            return method.invoke(this.state, args);
        }

    }

    private final static class GraphObjectProxy extends ProxyBase<JSONObject> {
        // Pre-loaded Method objects for the methods in java.util.Map
        private static final Method clearMethod;
        private static final Method containsKeyMethod;
        private static final Method containsValueMethod;
        private static final Method entrySetMethod;
        private static final Method getMethod;
        private static final Method isEmptyMethod;
        private static final Method keySetMethod;
        private static final Method putMethod;
        private static final Method putAllMethod;
        private static final Method removeMethod;
        private static final Method sizeMethod;
        private static final Method valuesMethod;
        // Pre-loaded Method objects for the methods in GraphObject
        private static final Method castMethod;
        private static final Method getInnerJSONObjectMethod;

        static {
            // We do this rather than use Class.getMethod because that is only available in API >= 9. It is simple
            // because we know there are no overloaded methods on either of these classes.
            Method[] mapMethods = Map.class.getDeclaredMethods();
            HashMap<String, Method> mapMethodMap = new HashMap<String, Method>();
            for (Method method : mapMethods) {
                mapMethodMap.put(method.getName(), method);
            }

            Method[] graphObjectMethods = GraphObject.class.getDeclaredMethods();
            HashMap<String, Method> graphObjectMethodMap = new HashMap<String, Method>();
            for (Method method : graphObjectMethods) {
                graphObjectMethodMap.put(method.getName(), method);
            }

            clearMethod = mapMethodMap.get("clear");
            containsKeyMethod = mapMethodMap.get("containsKey");
            containsValueMethod = mapMethodMap.get("containsValue");
            entrySetMethod = mapMethodMap.get("entrySet");
            getMethod = mapMethodMap.get("get");
            isEmptyMethod = mapMethodMap.get("isEmpty");
            keySetMethod = mapMethodMap.get("keySet");
            putMethod = mapMethodMap.get("put");
            putAllMethod = mapMethodMap.get("putAll");
            removeMethod = mapMethodMap.get("remove");
            sizeMethod = mapMethodMap.get("size");
            valuesMethod = mapMethodMap.get("values");

            castMethod = graphObjectMethodMap.get("cast");
            getInnerJSONObjectMethod = graphObjectMethodMap.get("getInnerJSONObject");
        }

        public GraphObjectProxy(JSONObject state) {
            super(state);
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
            if (method.equals(clearMethod)) {
                Utility.jsonObjectClear(this.state);
                return null;
            } else if (method.equals(containsKeyMethod)) {
                return this.state.has((String) args[0]);
            } else if (method.equals(containsValueMethod)) {
                return Utility.jsonObjectContainsValue(this.state, args[0]);
            } else if (method.equals(entrySetMethod)) {
                return Utility.jsonObjectEntrySet(this.state);
            } else if (method.equals(getMethod)) {
                return this.state.opt((String) args[0]);
            } else if (method.equals(isEmptyMethod)) {
                return this.state.length() == 0;
            } else if (method.equals(keySetMethod)) {
                return Utility.jsonObjectKeySet(this.state);
            } else if (method.equals(putMethod)) {
                // TODO port: check for adding a GraphObject, store underlying implementation instead
                Object value = getUnderlyingJSONObject(args[1]);
                try {
                    this.state.putOpt((String) args[0], value);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
                return null;
            } else if (method.equals(putAllMethod)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) args[0];
                Utility.jsonObjectPutAll(this.state, map);
                return null;
            } else if (method.equals(removeMethod)) {
                this.state.remove((String) args[0]);
                return null;
            } else if (method.equals(sizeMethod)) {
                return this.state.length();
            } else if (method.equals(valuesMethod)) {
                return Utility.jsonObjectValues(this.state);
            }

            return throwUnexpectedMethodSignature(method);
        }

        private final Object proxyGraphObjectMethods(Object proxy, Method method, Object[] args) {
            if (method.equals(castMethod)) {
                @SuppressWarnings("unchecked")
                Class<? extends GraphObject> graphObjectClass = (Class<? extends GraphObject>) args[0];

                return GraphObjectWrapper.createGraphObjectProxy(graphObjectClass, this.state);
            } else if (method.equals(getInnerJSONObjectMethod)) {
                InvocationHandler handler = Proxy.getInvocationHandler(proxy);
                GraphObjectProxy otherProxy = (GraphObjectProxy) handler;
                return otherProxy.state;
            }

            return throwUnexpectedMethodSignature(method);
        }

        private final Object proxyGraphObjectGettersAndSetters(Method method, Object[] args) throws JSONException {
            String methodName = method.getName();
            int parameterCount = method.getParameterTypes().length;

            // If it's a get or a set on a GraphObject-derived class, we can handle it.
            if (methodName.startsWith("get") && parameterCount == 0) {
                String key = Utility.convertCamelCaseToLowercaseWithUnderscores(methodName.substring(3));

                Object value = this.state.opt(key);

                Class<?> expectedType = method.getReturnType();

                Type genericReturnType = method.getGenericReturnType();
                ParameterizedType parameterizedReturnType = null;
                if (genericReturnType instanceof ParameterizedType) {
                    parameterizedReturnType = (ParameterizedType) genericReturnType;
                }

                value = coerceValueToExpectedType(value, expectedType, parameterizedReturnType);

                return value;
            } else if (methodName.startsWith("set") && parameterCount == 1) {
                String key = Utility.convertCamelCaseToLowercaseWithUnderscores(methodName.substring(3));

                Object value = args[0];
                // If this is a wrapped object, store the underlying JSONObject instead, in order to serialize
                // correctly.
                if (GraphObject.class.isAssignableFrom(value.getClass())) {
                    value = ((GraphObject) value).getInnerJSONObject();
                }
                this.state.putOpt(key, value);
                return null;
            }

            return throwUnexpectedMethodSignature(method);
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
                return wrapArray(state, graphObjectClass);
            } else {
                throw new FacebookGraphObjectException("Can't cast GraphObjectCollection of non-GraphObject type"
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
