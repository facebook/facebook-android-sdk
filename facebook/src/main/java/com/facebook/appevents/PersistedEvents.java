/**
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

package com.facebook.appevents;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

class PersistedEvents implements Serializable {
    private static final long serialVersionUID = 2016_06_29_001L;

    private HashMap<AccessTokenAppIdPair, List<AppEvent>> events = new HashMap<>();

    public PersistedEvents() {
    }

    public PersistedEvents(HashMap<AccessTokenAppIdPair, List<AppEvent>> appEventMap) {
        events.putAll(appEventMap);
    }

    public Set<AccessTokenAppIdPair> keySet() {
        return events.keySet();
    }

    public List<AppEvent> get(AccessTokenAppIdPair accessTokenAppIdPair) {
        return events.get(accessTokenAppIdPair);
    }

    public boolean containsKey(AccessTokenAppIdPair accessTokenAppIdPair) {
        return events.containsKey(accessTokenAppIdPair);
    }

    public void addEvents(AccessTokenAppIdPair accessTokenAppIdPair, List<AppEvent> appEvents) {
        if (!events.containsKey(accessTokenAppIdPair)) {
            events.put(accessTokenAppIdPair, appEvents);
            return;
        }

        events.get(accessTokenAppIdPair).addAll(appEvents);
    }

    static class SerializationProxyV1 implements Serializable {
        private static final long serialVersionUID = 2016_06_29_001L;;
        private final HashMap<AccessTokenAppIdPair, List<AppEvent>> proxyEvents;

        private SerializationProxyV1(HashMap<AccessTokenAppIdPair, List<AppEvent>> events) {
            this.proxyEvents = events;
        }

        private Object readResolve() {
            return new PersistedEvents(proxyEvents);
        }
    }

    private Object writeReplace() {
        return new SerializationProxyV1(events);
    }
}
