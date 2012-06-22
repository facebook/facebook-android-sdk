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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//TODO: docs
public class Settings {
    private static final HashSet<LoggingBehaviors> loggingBehaviors = new HashSet<LoggingBehaviors>();

    public static final Set<LoggingBehaviors> getLoggingBehaviors() {
        synchronized (loggingBehaviors) {
            return Collections.unmodifiableSet(new HashSet<LoggingBehaviors>(loggingBehaviors));
        }
    }

    public static final void addLoggingBehavior(LoggingBehaviors behavior) {
        synchronized (loggingBehaviors) {
            loggingBehaviors.add(behavior);
        }
    }

    public static final void removeLoggingBehavior(LoggingBehaviors behavior) {
        synchronized (loggingBehaviors) {
            loggingBehaviors.remove(behavior);
        }
    }

    public static final void clearLoggingBehaviors() {
        synchronized (loggingBehaviors) {
            loggingBehaviors.clear();
        }
    }
    
    public static final boolean isLoggingBehaviorEnabled(LoggingBehaviors behavior) {
        synchronized (loggingBehaviors) {
            return loggingBehaviors.contains(behavior);
        }
    }
}
