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

import com.facebook.GraphUser;
import com.facebook.Session;

import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;

// ISSUE: Consider extending LinearLayout or other layout instead.
public class PlacePickerView extends View {
    public PlacePickerView(Context context) {
        super(context);
    }

    public PlacePickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlacePickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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

    public final Location getLocation() {
        return null;
    }

    public final void setLocation(Location location) {
    }

    public final int getRadiusInMeters() {
        return 0;
    }

    public final void setRadiusInMeters(int radiusInMeters) {
    }

    public final String getSearchText() {
        return null;
    }

    public final void setSearchText(String searchText) {
    }

    @Override public String toString() {
        return null;
    }
}
