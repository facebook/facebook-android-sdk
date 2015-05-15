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

package com.facebook.scrumptious.picker;

import android.database.CursorIndexOutOfBoundsException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class GraphObjectCursor {
    private int pos = -1;
    private boolean closed = false;
    private List<JSONObject> graphObjects = new ArrayList<JSONObject>();
    private boolean moreObjectsAvailable = false;

    GraphObjectCursor() {
    }

    GraphObjectCursor(GraphObjectCursor other) {
        pos = other.pos;
        closed = other.closed;
        graphObjects = new ArrayList<JSONObject>();
        graphObjects.addAll(other.graphObjects);

        // We do not copy observers.
    }

    public void addGraphObjects(JSONArray graphObjects) {
        for (int i = 0; i < graphObjects.length(); ++i) {
            this.graphObjects.add(graphObjects.optJSONObject(i));
        }
    }

    public boolean areMoreObjectsAvailable() {
        return moreObjectsAvailable;
    }

    public void setMoreObjectsAvailable(boolean moreObjectsAvailable) {
        this.moreObjectsAvailable = moreObjectsAvailable;
    }

    public int getCount() {
        return graphObjects.size();
    }

    public int getPosition() {
        return pos;
    }

    public boolean move(int offset) {
        return moveToPosition(pos + offset);
    }

    public boolean moveToPosition(int position) {
        final int count = getCount();
        if (position >= count) {
            pos = count;
            return false;
        }

        if (position < 0) {
            pos = -1;
            return false;
        }

        pos = position;
        return true;
    }

    public boolean moveToFirst() {
        return moveToPosition(0);
    }

    public boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }

    public boolean moveToNext() {
        return moveToPosition(pos + 1);
    }

    public boolean moveToPrevious() {
        return moveToPosition(pos - 1);
    }

    public boolean isFirst() {
        return (pos == 0) && (getCount() != 0);
    }

    public boolean isLast() {
        final int count = getCount();
        return (pos == (count - 1)) && (count != 0);
    }

    public boolean isBeforeFirst() {
        return (getCount() == 0) || (pos == -1);
    }

    public boolean isAfterLast() {
        final int count = getCount();
        return (count == 0) || (pos == count);
    }

    public JSONObject getGraphObject() {
        if (pos < 0) {
            throw new CursorIndexOutOfBoundsException("Before first object.");
        }
        if (pos >= graphObjects.size()) {
            throw new CursorIndexOutOfBoundsException("After last object.");
        }
        return graphObjects.get(pos);
    }

    public void close() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

}
