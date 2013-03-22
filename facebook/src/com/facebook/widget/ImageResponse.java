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

package com.facebook.widget;

import android.graphics.Bitmap;

class ImageResponse {

    private ImageRequest request;
    private Exception error;
    private boolean isCachedRedirect;
    private Bitmap bitmap;

    ImageResponse(ImageRequest request, Exception error, boolean isCachedRedirect, Bitmap bitmap) {
        this.request = request;
        this.error = error;
        this.bitmap = bitmap;
        this.isCachedRedirect = isCachedRedirect;
    }

    ImageRequest getRequest() {
        return request;
    }

    Exception getError() {
        return error;
    }

    Bitmap getBitmap() {
        return bitmap;
    }

    boolean isCachedRedirect() {
        return isCachedRedirect;
    }
}
