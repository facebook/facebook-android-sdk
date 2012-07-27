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

import android.content.Context;
import android.os.Bundle;

//TODO: docs
public class SharedPreferencesTokenCache extends TokenCache {
    public SharedPreferencesTokenCache(Context applicationContext) {
    }

    public SharedPreferencesTokenCache(Context applicationContext, String cacheKey) {
    }

    public Bundle load() {
        return null;
    }

    public void save(Bundle bundle) {
    }

    public void clear() {
    }

    @Override public String toString() {
        return null;
    }
}
