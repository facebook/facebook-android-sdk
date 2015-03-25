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

package com.facebook.scrumptious;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.BaseAdapter;

import com.facebook.share.model.ShareOpenGraphAction;

import org.json.JSONObject;

/**
 * Base class for a list element in the Scrumptious main display, consisting of an
 * icon to the left, and a two line display to the right.
 */
public abstract class BaseListElement {

    private Drawable icon;
    private String text1;
    private String text2;
    private BaseAdapter adapter;
    private int requestCode;

    /**
     * Constructs a new list element.
     *
     * @param icon the drawable for the icon
     * @param text1 the first row of text
     * @param text2 the second row of text
     * @param requestCode the requestCode to start new Activities with
     */
    public BaseListElement(Drawable icon, String text1, String text2, int requestCode) {
        this.icon = icon;
        this.text1 = text1;
        this.text2 = text2;
        this.requestCode = requestCode;
    }

    /**
     * The Adapter associated with this list element (used for notifying that the
     * underlying dataset has changed).
     * @param adapter the adapter associated with this element
     */
    public void setAdapter(BaseAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Returns the icon.
     *
     * @return the icon
     */
    public Drawable getIcon() {
        return icon;
    }

    /**
     * Returns the first row of text.
     *
     * @return the first row of text
     */
    public String getText1() {
        return text1;
    }

    /**
     * Returns the second row of text.
     *
     * @return the second row of text
     */
    public String getText2() {
        return text2;
    }

    /**
     * Returns the requestCode for starting new Activities.
     *
     * @return the requestCode
     */
    public int getRequestCode() {
        return requestCode;
    }

    /**
     * Sets the first row of text.
     *
     * @param text1 text to set on the first row
     */
    public void setText1(String text1) {
        this.text1 = text1;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Sets the second row of text.
     *
     * @param text2 text to set on the second row
     */
    public void setText2(String text2) {
        this.text2 = text2;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Returns the OnClickListener associated with this list element. To be
     * overridden by the subclasses.
     *
     * @return the OnClickListener associated with this list element
     */
    protected abstract View.OnClickListener getOnClickListener();

    /**
     * Populate an OpenGraphAction with the results of this list element.
     *
     * @param actionBuilder the builder to populate with data
     */
    protected abstract void populateOpenGraphAction(ShareOpenGraphAction.Builder actionBuilder);

    /**
     * Callback if the OnClickListener happens to launch a new Activity.
     *
     * @param data the data associated with the result
     */
    protected void onActivityResult(Intent data) {}

    /**
     * Save the state of the current element.
     *
     * @param bundle the bundle to save to
     */
    protected void onSaveInstanceState(Bundle bundle) {}

    /**
     * Restore the state from the saved bundle. Returns true if the
     * state was restored.
     *
     * @param savedState the bundle to restore from
     * @return true if state was restored
     */
    protected boolean restoreState(Bundle savedState) {
        return false;
    }

    /**
     * Notifies the associated Adapter that the underlying data has changed,
     * and to re-layout the view.
     */
    protected void notifyDataChanged() {
        adapter.notifyDataSetChanged();
    }

}
