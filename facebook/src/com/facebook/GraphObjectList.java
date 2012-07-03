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

import java.util.List;

import org.json.JSONArray;

public interface GraphObjectList<T> extends List<T> {
    // cast method is only supported if T extends GraphObject
    public <U extends GraphObject> GraphObjectList<U> castToListOf(Class<U> graphObjectClass);
    public JSONArray getInnerJSONArray();
}
