/*
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

package com.facebook.appevents.codeless.internal;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ParameterComponent {
  private static final String PARAMETER_NAME_KEY = "name";
  private static final String PARAMETER_PATH_KEY = "path";
  private static final String PARAMETER_VALUE_KEY = "value";

  public final String name;
  public final String value;
  public final List<PathComponent> path;
  public final String pathType;

  public ParameterComponent(final JSONObject component) throws JSONException {
    name = component.getString(PARAMETER_NAME_KEY);
    value = component.optString(PARAMETER_VALUE_KEY);

    ArrayList<PathComponent> pathComponents = new ArrayList<>();
    JSONArray jsonPathArray = component.optJSONArray(PARAMETER_PATH_KEY);
    if (null != jsonPathArray) {
      for (int i = 0; i < jsonPathArray.length(); i++) {
        PathComponent pathComponent = new PathComponent((jsonPathArray.getJSONObject(i)));
        pathComponents.add(pathComponent);
      }
    }
    path = pathComponents;
    pathType =
        component.optString(Constants.EVENT_MAPPING_PATH_TYPE_KEY, Constants.PATH_TYPE_ABSOLUTE);
  }
}
