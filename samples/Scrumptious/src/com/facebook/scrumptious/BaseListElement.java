package com.facebook.scrumptious;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.BaseAdapter;
import com.facebook.OpenGraphAction;

/**
 * Base class for a list element in the Scrumptious main display, consisting of an
 * icon to the left, and a two line display to the right.
 */
public abstract class BaseListElement {

    private Drawable icon;
    private String text1;
    private String text2;
    private BaseAdapter adapter;

    /**
     * Constructs a new list element.
     *
     * @param icon the drawable for the icon
     * @param text1 the first row of text
     * @param text2 the second row of text
     */
    public BaseListElement(Drawable icon, String text1, String text2) {
        this.icon = icon;
        this.text1 = text1;
        this.text2 = text2;
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
     * @param action the action to populate with data
     */
    protected abstract void populateOGAction(OpenGraphAction action);

    /**
     * Callback if the OnClickListener happens to launch a new Activity.
     */
    protected void onActivityResult() {}

    /**
     * Notifies the associated Adapter that the underlying data has changed,
     * and to re-layout the view.
     */
    protected void notifyDataChanged() {
        adapter.notifyDataSetChanged();
    }

}
