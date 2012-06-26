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
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public final class GraphObjectWrapper {
    private static final HashSet<Class<?>> verifiedGraphObjectClasses = new HashSet<Class<?>>();

    // Pre-loaded Method objects for the methods in java.lang.Object
    private static final Method equalsMethod;
    // Pre-loaded Method objects for the methods in java.util.Map<K,V>
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
        Method[] objectMethods = Object.class.getDeclaredMethods();
        HashMap<String, Method> objectMethodMap = new HashMap<String, Method>();
        for (Method method : objectMethods) {
            objectMethodMap.put(method.getName(), method);
        }

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

        equalsMethod = objectMethodMap.get("equals");

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

    private static <T extends GraphObject> T createGraphObjectProxy(Class<T> graphObjectClass, JSONObject implementation) {
        verifyCanProxyClass(graphObjectClass);

        Class<?>[] interfaces = new Class[] { graphObjectClass };
        GraphObjectProxy graphObjectProxy = new GraphObjectProxy(implementation);

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

    private final static class GraphObjectProxy implements InvocationHandler {
        private final JSONObject state;

        public GraphObjectProxy(JSONObject implementation) {
            this.state = implementation;
        }

        @Override
        public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.d("GraphObjectProxy", "calling method: " + method.toString());

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

        // Declared to return Object just to simplify implementation of proxy helpers.
        private final Object throwUnexpectedMethodSignature(Method method) {
            throw new FacebookGraphObjectException("GraphObjectProxy got an unexpected method signature: "
                    + method.toString());
        }

        private final Object proxyObjectMethods(Object proxy, Method method, Object[] args) throws Throwable {
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
                // TODO check for adding a GraphObject, store underlying implementation instead
                try {
                    this.state.putOpt((String) args[0], args[1]);
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

                Class<?> returnType = method.getReturnType();
                Class<?> valueType = value.getClass();
                if (GraphObject.class.isAssignableFrom(returnType) && !returnType.isAssignableFrom(valueType)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends GraphObject> graphObjectClass = (Class<? extends GraphObject>) returnType;

                    // We need a GraphObject, but we don't have one.
                    if (JSONObject.class.isAssignableFrom(valueType)) {
                        // We can wrap a JSONObject as a GraphObject.
                        value = createGraphObjectProxy(graphObjectClass, (JSONObject) value);
                    } else if (GraphObject.class.isAssignableFrom(valueType)) {
                        // We can cast a GraphObject-derived class to another GraphObject-derived class.
                        value = ((GraphObject) value).cast(graphObjectClass);
                    } else {
                        throw new FacebookGraphObjectException("GraphObjectProxy can't create GraphObject from "
                                + valueType.getName());
                    }
                }

                return value;
            } else if (methodName.startsWith("set") && parameterCount == 1) {
                String key = Utility.convertCamelCaseToLowercaseWithUnderscores(methodName.substring(3));

                Object value = args[0];
                // If this is a wrapped object, store the underlying JSONObject instead, in order to serialize
                // correctly.
                if (GraphObject.class.isAssignableFrom(value.getClass())) {
                    value = ((GraphObject)value).getInnerJSONObject();
                }
                this.state.putOpt(key, value);
                return null;
            }

            return throwUnexpectedMethodSignature(method);
        }
    }

}
