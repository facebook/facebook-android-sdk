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

import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
public class EventBinding {
  private final String eventName;
  private final MappingMethod method;
  private final ActionType type;
  private final String appVersion;
  private final List<PathComponent> path;
  private final List<ParameterComponent> parameters;
  private final String componentId;
  private final String pathType;
  private final String activityName;

  public EventBinding(
      final String eventName,
      final MappingMethod method,
      final ActionType type,
      final String appVersion,
      final List<PathComponent> path,
      final List<ParameterComponent> parameters,
      final String componentId,
      final String pathType,
      final String activityName) {
    this.eventName = eventName;
    this.method = method;
    this.type = type;
    this.appVersion = appVersion;
    this.path = path;
    this.parameters = parameters;
    this.componentId = componentId;
    this.pathType = pathType;
    this.activityName = activityName;
  }

  public static List<EventBinding> parseArray(JSONArray array) {
    List<EventBinding> eventBindings = new ArrayList<>();

    try {
      int length = array != null ? array.length() : 0;
      for (int i = 0; i < length; i++) {
        EventBinding eventBinding = getInstanceFromJson(array.getJSONObject(i));
        eventBindings.add(eventBinding);
      }
    } catch (JSONException e) {
      // Ignore
    } catch (IllegalArgumentException e) {
      // Ignore
    }

    return eventBindings;
  }

  public static EventBinding getInstanceFromJson(final JSONObject mapping)
      throws JSONException, IllegalArgumentException {
    String eventName = mapping.getString("event_name");
    MappingMethod method =
        MappingMethod.valueOf(mapping.getString("method").toUpperCase(Locale.ENGLISH));
    ActionType type =
        ActionType.valueOf(mapping.getString("event_type").toUpperCase(Locale.ENGLISH));
    String appVersion = mapping.getString("app_version");
    JSONArray jsonPathArray = mapping.getJSONArray("path");
    List<PathComponent> path = new ArrayList<>();
    for (int i = 0; i < jsonPathArray.length(); i++) {
      JSONObject jsonPath = jsonPathArray.getJSONObject(i);
      PathComponent component = new PathComponent(jsonPath);
      path.add(component);
    }
    String pathType =
        mapping.optString(Constants.EVENT_MAPPING_PATH_TYPE_KEY, Constants.PATH_TYPE_ABSOLUTE);
    JSONArray jsonParameterArray = mapping.optJSONArray("parameters");
    List<ParameterComponent> parameters = new ArrayList<>();
    if (null != jsonParameterArray) {
      for (int i = 0; i < jsonParameterArray.length(); i++) {
        JSONObject jsonParameter = jsonParameterArray.getJSONObject(i);
        ParameterComponent component = new ParameterComponent(jsonParameter);
        parameters.add(component);
      }
    }
    String componentId = mapping.optString("component_id");
    String activityName = mapping.optString("activity_name");

    return new EventBinding(
        eventName, method, type, appVersion, path, parameters, componentId, pathType, activityName);
  }

  public List<PathComponent> getViewPath() {
    return Collections.unmodifiableList(this.path);
  }

  public List<ParameterComponent> getViewParameters() {
    return Collections.unmodifiableList(this.parameters);
  }

  public String getEventName() {
    return this.eventName;
  }

  public ActionType getType() {
    return this.type;
  }

  public MappingMethod getMethod() {
    return this.method;
  }

  public String getAppVersion() {
    return this.appVersion;
  }

  public String getComponentId() {
    return this.componentId;
  }

  public String getPathType() {
    return this.pathType;
  }

  public String getActivityName() {
    return this.activityName;
  }

  public enum MappingMethod {
    MANUAL,
    INFERENCE,
  }

  public enum ActionType {
    CLICK,
    SELECTED,
    TEXT_CHANGED,
  }
}
