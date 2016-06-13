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
import java.util.LinkedList;
import java.util.List;

/**
 * com.facebook.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class CollectionMapper {
    public static <T> void iterate(final Collection<T> collection,
                                   final ValueMapper valueMapper,
                                   final OnMapperCompleteListener onMapperCompleteListener) {
        final Mutable<Boolean> didReturnError = new Mutable<>(false);
        final Mutable<Integer> pendingJobCount = new Mutable<>(1);
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

        Iterator<T> keyIterator = collection.keyIterator();
        List<T> keys = new LinkedList<>();
        while (keyIterator.hasNext()) {
            keys.add(keyIterator.next());
        }

        for (final T key : keys) {
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

    public interface OnErrorListener {
        void onError(FacebookException exception);
    }

    public interface OnMapperCompleteListener extends OnErrorListener {
        void onComplete();
    }

    public interface OnMapValueCompleteListener extends OnErrorListener {
        void onComplete(Object mappedValue);
    }

    public interface ValueMapper {
        void mapValue(Object value, OnMapValueCompleteListener onMapValueCompleteListener);
    }

    public interface Collection<T> {
        Iterator<T> keyIterator();
        Object get(T key);
        void set(T key, Object value, OnErrorListener onErrorListener);
    }

    private CollectionMapper() {}
}
