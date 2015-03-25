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

package com.facebook.internal;

import com.facebook.FacebookException;

import java.util.Iterator;

/**
 * com.facebook.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class CollectionMapper {
    public static <T> void iterate(final Collection<T> collection,
                                   final ValueMapper valueMapper,
                                   final OnMapperCompleteListener onMapperCompleteListener) {
        final Mutable<Boolean> didReturnError = new Mutable<Boolean>(false);
        final Mutable<Integer> pendingJobCount = new Mutable<Integer>(1);
        final OnMapperCompleteListener jobCompleteListener = new OnMapperCompleteListener() {
            @Override
            public void onComplete() {
                if (didReturnError.value) {
                    return;
                }
                if (--pendingJobCount.value == 0) {
                    onMapperCompleteListener.onComplete();
                }
            }

            @Override
            public void onError(FacebookException exception) {
                if (didReturnError.value) {
                    return;
                }
                didReturnError.value = true;
                onMapperCompleteListener.onError(exception);
            }
        };
        final Iterator<T> keyIterator = collection.keyIterator();
        while (keyIterator.hasNext()) {
            final T key = keyIterator.next();
            final Object value = collection.get(key);
            final OnMapValueCompleteListener onMapValueCompleteListener =
                    new OnMapValueCompleteListener() {
                        @Override
                        public void onComplete(Object mappedValue) {
                            collection.set(key, mappedValue, jobCompleteListener);
                            jobCompleteListener.onComplete();
                        }

                        @Override
                        public void onError(FacebookException exception) {
                            jobCompleteListener.onError(exception);
                        }
                    };
            pendingJobCount.value++;
            valueMapper.mapValue(value, onMapValueCompleteListener);
        }
        jobCompleteListener.onComplete();
    }

    public static interface OnErrorListener {
        public void onError(FacebookException exception);
    }

    public static interface OnMapperCompleteListener extends OnErrorListener {
        public void onComplete();
    }

    public static interface OnMapValueCompleteListener extends OnErrorListener {
        public void onComplete(Object mappedValue);
    }

    public static interface ValueMapper {
        public void mapValue(Object value, OnMapValueCompleteListener onMapValueCompleteListener);
    }

    public static interface Collection<T> {
        public Iterator<T> keyIterator();
        public Object get(T key);
        public void set(T key, Object value, OnErrorListener onErrorListener);
    }

    private CollectionMapper() {}
}
