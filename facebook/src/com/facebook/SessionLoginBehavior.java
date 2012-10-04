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

/**
 * Specifies the behaviors to try during
 * {@link Session#openForRead(com.facebook.Session.OpenRequest) openForRead},
 * {@link Session#openForPublish(com.facebook.Session.OpenRequest) openForPublish},
 * {@link Session#reauthorizeForRead(com.facebook.Session.ReauthorizeRequest) reauthorizeForRead}, or
 * {@link Session#reauthorizeForPublish(com.facebook.Session.ReauthorizeRequest) reauthorizeForPublish}.
 */
public enum SessionLoginBehavior {
    /**
     * Specifies that Session should attempt Single Sign On (SSO), and if that
     * does not work fall back to dialog auth. This is the default behavior.
     */
    SSO_WITH_FALLBACK,

    /**
     * Specifies that Session should only attempt SSO. If SSO fails, then the
     * open or reauthorize call fails.
     */
    SSO_ONLY,

    /**
     * Specifies that SSO should not be attempted, and to only use dialog auth.
     */
    SUPPRESS_SSO;
}
