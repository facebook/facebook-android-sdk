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

package com.facebook.share.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Set;

/**
 * Provides an abstract class to contain Open Graph values.
 *
 * Use {@link ShareOpenGraphValueContainer.Builder} to create instances
 */
public abstract class ShareOpenGraphValueContainer
        <P extends ShareOpenGraphValueContainer, E extends ShareOpenGraphValueContainer.Builder>
        implements ShareModel {
    private final Bundle bundle;

    protected ShareOpenGraphValueContainer(
            final Builder<P, E> builder) {
        super();
        this.bundle = (Bundle)builder.bundle.clone();
    }

    ShareOpenGraphValueContainer(final Parcel in) {
        this.bundle = in.readBundle(Builder.class.getClassLoader());
    }

    /**
     * Gets a value out of the object.
     * @param key The key for the value.
     * @return The boolean value.
     */
    @Nullable
    public Object get(String key) {
        return this.bundle.get(key);
    }

    /**
     * Gets a boolean value out of the object.
     * @param key The key for the value.
     * @param defaultValue The value to return if no value is found for the specified key.
     * @return The boolean value.
     */
    public boolean getBoolean(final String key, final boolean defaultValue) {
        return this.bundle.getBoolean(key, defaultValue);
    }

    /**
     * Gets an array of boolean values out of the object.
     * @param key The key for the value.
     * @return The boolean values.
     */
    @Nullable
    public boolean[] getBooleanArray(final String key) {
        return this.bundle.getBooleanArray(key);
    }

    /**
     * Gets a double value out of the object.
     * @param key The key for the value.
     * @param defaultValue The value to return if no value is found for the specified key.
     * @return The double value.
     */
    public double getDouble(final String key, final double defaultValue) {
        return this.bundle.getDouble(key, defaultValue);
    }

    /**
     * Gets an array of double values out of the object.
     * @param key The key for the value.
     * @return The double values.
     */
    @Nullable
    public double[] getDoubleArray(final String key) {
        return this.bundle.getDoubleArray(key);
    }

    /**
     * Gets an int value out of the object.
     * @param key The key for the value.
     * @param defaultValue The value to return if no value is found for the specified key.
     * @return The int value.
     */
    public int getInt(final String key, final int defaultValue) {
        return this.bundle.getInt(key, defaultValue);
    }

    /**
     * Gets an array of int values out of the object.
     * @param key The key for the value.
     * @return The int values.
     */
    @Nullable
    public int[] getIntArray(final String key) {
        return this.bundle.getIntArray(key);
    }

    /**
     * Gets an long value out of the object.
     * @param key The key for the value.
     * @param defaultValue The value to return if no value is found for the specified key.
     * @return The long value.
     */
    public long getLong(final String key, final long defaultValue) {
        return this.bundle.getLong(key, defaultValue);
    }

    /**
     * Gets an array of long values out of the object.
     * @param key The key for the value.
     * @return The long values.
     */
    @Nullable
    public long[] getLongArray(final String key) {
        return this.bundle.getLongArray(key);
    }

    /**
     * Gets an object value out of the object.
     * @param key The key for the value.
     * @return The object value.
     */
    public ShareOpenGraphObject getObject(final String key) {
        final Object value = this.bundle.get(key);
        return (value instanceof ShareOpenGraphObject ? (ShareOpenGraphObject)value : null);
    }

    /**
     * Gets an array of object values out of the object.
     * @param key The key for the value.
     * @return The object values.
     */
    @Nullable
    public ArrayList<ShareOpenGraphObject> getObjectArrayList(final String key) {
        final ArrayList<Parcelable> items = this.bundle.getParcelableArrayList(key);
        if (items == null) {
            return null;
        }
        final ArrayList<ShareOpenGraphObject> list = new ArrayList<ShareOpenGraphObject>();
        for (Parcelable item : items) {
            if (item instanceof ShareOpenGraphObject) {
                list.add((ShareOpenGraphObject)item);
            }
        }
        return list;
    }

    /**
     * Gets a photo value out of the object.
     * @param key The key for the value.
     * @return The photo value.
     */
    @Nullable
    public SharePhoto getPhoto(final String key) {
        final Object value = this.bundle.getParcelable(key);
        return (value instanceof SharePhoto ? (SharePhoto)value : null);
    }

    /**
     * Gets an array of photo values out of the object.
     * @param key The key for the value.
     * @return The photo values.
     */
    @Nullable
    public ArrayList<SharePhoto> getPhotoArrayList(final String key) {
        final ArrayList<Parcelable> items = this.bundle.getParcelableArrayList(key);
        if (items == null) {
            return null;
        }
        final ArrayList<SharePhoto> list = new ArrayList<SharePhoto>();
        for (Parcelable item : items) {
            if (item instanceof SharePhoto) {
                list.add((SharePhoto)item);
            }
        }
        return list;
    }

    /**
     * Gets a string value out of the object.
     * @param key The key for the value.
     * @return The string value.
     */
    @Nullable
    public String getString(final String key) {
        return this.bundle.getString(key);
    }

    /**
     * Gets an array of string values out of the object.
     * @param key The key for the value.
     * @return The string values.
     */
    @Nullable
    public ArrayList<String> getStringArrayList(final String key) {
        return this.bundle.getStringArrayList(key);
    }

    /**
     * Returns the values in the container packaged in a bundle.
     * @return A bundle with the values.
     */
    public Bundle getBundle() {
        return (Bundle)this.bundle.clone();
    }

    /**
     * Returns a set of the keys contained in this object.
     * @return A set of the keys.
     */
    public Set<String> keySet() {
        return this.bundle.keySet();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        out.writeBundle(this.bundle);
    }

    /**
     * Abstract builder for the {@link com.facebook.share.model.ShareOpenGraphValueContainer} class.
     */
    public abstract static class Builder
            <P extends ShareOpenGraphValueContainer, E extends Builder>
            implements ShareModelBuilder<P, E> {
        private Bundle bundle = new Bundle();

        /**
         * Sets a boolean value in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putBoolean(final String key, final boolean value) {
            this.bundle.putBoolean(key, value);
            return (E)this;
        }

        /**
         * Sets an array of boolean values in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putBooleanArray(final String key, @Nullable final boolean[] value) {
            this.bundle.putBooleanArray(key, value);
            return (E)this;
        }

        /**
         * Sets a double value in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putDouble(final String key, final double value) {
            this.bundle.putDouble(key, value);
            return (E)this;
        }

        /**
         * Sets an array of double values in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putDoubleArray(final String key, @Nullable final double[] value) {
            this.bundle.putDoubleArray(key, value);
            return (E)this;
        }

        /**
         * Sets an int value in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putInt(final String key, final int value) {
            this.bundle.putInt(key, value);
            return (E)this;
        }

        /**
         * Sets an array of int values in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putIntArray(final String key, @Nullable final int[] value) {
            this.bundle.putIntArray(key, value);
            return (E)this;
        }

        /**
         * Sets a long value in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putLong(final String key, final long value) {
            this.bundle.putLong(key, value);
            return (E)this;
        }

        /**
         * Sets an array of long values in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putLongArray(final String key, @Nullable final long[] value) {
            this.bundle.putLongArray(key, value);
            return (E)this;
        }

        /**
         * Sets an object value in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putObject(final String key, @Nullable final ShareOpenGraphObject value) {
            this.bundle.putParcelable(key, value);
            return (E)this;
        }

        /**
         * Sets an array of object values in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putObjectArrayList(
                final String key,
                @Nullable final ArrayList<ShareOpenGraphObject> value) {
            this.bundle.putParcelableArrayList(key, value);
            return (E)this;
        }

        /**
         * Sets a photo value in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putPhoto(final String key, @Nullable final SharePhoto value) {
            this.bundle.putParcelable(key, value);
            return (E)this;
        }

        /**
         * Sets an array of photo values in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putPhotoArrayList(final String key, @Nullable final ArrayList<SharePhoto> value) {
            this.bundle.putParcelableArrayList(key, value);
            return (E) this;
        }

        /**
         * Sets a string value in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putString(final String key, @Nullable final String value) {
            this.bundle.putString(key, value);
            return (E) this;
        }

        /**
         * Sets an array of string values in the object.
         * @param key The key for the value.
         * @param value The value.
         * @return The builder.
         */
        public E putStringArrayList(final String key, @Nullable final ArrayList<String> value) {
            this.bundle.putStringArrayList(key, value);
            return (E) this;
        }

        @Override
        public E readFrom(final P model) {
            if (model != null) {
                this.bundle.putAll(model.getBundle());
            }
            return (E)this;
        }
    }
}
