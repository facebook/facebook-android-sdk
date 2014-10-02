/**
 * Copyright 2010-present Facebook.
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

package com.facebook.internal;


import android.content.Context;
import android.os.Bundle;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for Android. Use of
 * any of the classes in this package is unsupported, and they may be modified or removed without warning at
 * any time.
 */

/**
 * This class executes service calls to fetch like-state of objects from the Facebook Application, if available.
 */
final class LikeStatusClient extends PlatformServiceClient {
    private String objectId;

    LikeStatusClient(Context context, String applicationId, String objectId) {
        super(context, NativeProtocol.MESSAGE_GET_LIKE_STATUS_REQUEST, NativeProtocol.MESSAGE_GET_LIKE_STATUS_REPLY,
                NativeProtocol.PROTOCOL_VERSION_20141001, applicationId);

        this.objectId = objectId;
    }

    @Override
    protected void populateRequestBundle(Bundle data) {
        // Only thing we need to pass in is the object id.
        data.putString(NativeProtocol.EXTRA_OBJECT_ID, objectId);
    }
}

