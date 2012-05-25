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

import java.util.Collection;
import java.util.Set;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.facebook.GraphUser;
import com.facebook.Session;

// ISSUE: Consider extending LinearLayout or other layout instead.
public class FriendPickerView extends View {
    public FriendPickerView(Context context) {
        super(context);
    }

    public FriendPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FriendPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public final boolean getAllowMultipleSelection() {
        return true;
    }

    public final void setAllowMultipleSelection(boolean allowMultipleSelection) {
    }

    public final Set<String> getFieldsForRequest() {
		return null;
    }

    public final boolean getItemPicturesEnabled() {
        return true;
    }

    public final void setItemPicturesEnabled(boolean itemPicturesEnabled) {
    }

    public final Collection<GraphUser> getSelection() {
    	return null;
    }

    public final Session getSession() {
        return null;
    }

    public final void setSession(Session session) {
    }

    public final String getUserId() {
        return null;
    }

    public final void setUserId(String userId) {
    }

    @Override public String toString() {
        return null;
    }
}
