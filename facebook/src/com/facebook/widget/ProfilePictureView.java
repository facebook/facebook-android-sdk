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

package com.facebook.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ProfilePictureView extends View {
    public ProfilePictureView(Context context) {
        super(context);
    }

    public ProfilePictureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProfilePictureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public final PictureSize getPictureSize() {
        return PictureSize.SQUARE;
    }

    public final void setPictureSize(PictureSize pictureSize) {
    }

    public final String getUserId() {
        return null;
    }

    public final void setUserId(String userId) {
    }

    @Override public String toString() {
        return null;
    }

    public enum PictureSize { SQUARE, SMALL, NORMAL, LARGE }
}
