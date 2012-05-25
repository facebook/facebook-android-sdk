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

import java.util.List;

import org.apache.http.HttpRequest;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;

public class Request {

    public Request() {
    }

    public Request(Session session, String graphPath) {
    }

    public Request(Session session, String graphPath, Bundle parameters, String httpMethod) {
    }

    public static Request newPostRequest(Session session, String graphPath, Object object) {
        return null;
    }

    public static Request newRestRequest(
        Session session, String restMethod, Bundle parameters, String httpMethod) {
        return null;
    }

    public static Request newMeRequest(Session session) {
        return null;
    }

    public static Request newMyFriendsRequest(Session session) {
        return null;
    }

    public static Request newUploadPhotoRequest(Session session, Bitmap image) {
        return null;
    }

    public static Request newPlacesSearchRequest(
        Session session, Location location, int radiusInMeters, int resultsLimit, String searchText)
    {
        return null;
    }

    public final Object getGraphObject() {
        return null;
    }

    public final void setGraphObject(Object graphObject) {
    }

    public final String getGraphPath() {
        return null;
    }

    public final void setGraphPath(String graphPath) {
    }

    public final String getHttpMethod() {
        return null;
    }

    public final void setHttpMethod(String method) {
    }

    public final Bundle getParameters() {
        return null;
    }

    public final String getRestMethod() {
        return null;
    }

    public final void setRestMethod(String restMethod) {
    }

    public final Session getSession() {
        return null;
    }

    public final void setSession(Session session) {
    }

    public static HttpRequest toHttpRequest(RequestContext context, Request... requests) {
		return null;
    }

    public static Response execute(Request request) {
    	return null;
    }

    public static Response execute(RequestContext context, Request request) {
    	return null;
    }

    public static List<Response> executeBatch(Request...requests) {
    	return null;
    }

    public static List<Response> executeBatch(RequestContext context, Request...requests) {
    	return null;
    }

    @Override public String toString() {
        return null;
    }
}
