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

import java.util.Date;
import java.util.List;
import java.util.Set;

import android.content.Intent;

public class Session {

    public enum SessionState {
        CREATED(Category.CREATED_CATEGORY),
        CREATED_TOKEN_LOADED(Category.CREATED_CATEGORY),
        OPENED(Category.OPENED_CATEGORY),
        OPENED_TOKEN_EXTENDED(Category.OPENED_CATEGORY),
        CLOSED_LOGIN_FAILED(Category.CLOSED_CATEGORY),
        CLOSED(Category.CLOSED_CATEGORY);

        private final Category category;

        SessionState(Category category) {
            this.category = category;
        }

        public boolean getIsOpened() { return this.category == Category.OPENED_CATEGORY; }
        public boolean getIsClosed() { return this.category == Category.CLOSED_CATEGORY; }

        private enum Category { CREATED_CATEGORY, OPENED_CATEGORY, CLOSED_CATEGORY }
    }

    public enum LoginBehavior {
        SSO_WITH_FALLBACK,
        SSO_ONLY,
        SUPPRESS_SSO;
    }

    public interface StatusCallback {
        public void callback(Session session, SessionState status, Exception exception);
    }

    public Session() {
    }

    public Session(String[] permissions) {
    }

    public Session(
        List<String> permissions, String applicationId, String urlSchemeSuffix,
        TokenCache tokenCache) {
    }

    public final boolean getIsOpened() {
        return false;
    }

    public final SessionState getStatus() {
        return SessionState.CREATED;
    }

    public final String getApplicationId() {
        return null;
    }

    public final String getUrlSchemeSuffix() {
        return null;
    }

    public final String getAccessToken() {
        return null;
    }

    public final Date getExpirationDate() {
        return null;
    }

    public final List<String> getPermissions() {
        return null;
    }

    public final void open(StatusCallback callback) {
    }

    public final void open(StatusCallback callback, LoginBehavior behavior) {
    }

    public final void close() {
    }

    public final void closeAndClearTokenInformation() {
    }

    // TODO: where should this be?
    public final void authorizeCallback(int requestCode, int resultCode, Intent data) {
    }

    public final Set<String> getLoggingBehaviors() {
        return null;
    }

    public final void addLoggingBehavior(String behavior) {
    }

    public final void removeLoggingBehavior(String behavior) {
    }

    public final void clearLoggingBehaviors() {
    }

    @Override public String toString() {
        return null;
    }
}
