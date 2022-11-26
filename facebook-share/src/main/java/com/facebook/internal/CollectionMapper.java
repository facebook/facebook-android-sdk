/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal;

import com.facebook.FacebookException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public class CollectionMapper {
  public static <T> void iterate(
      final Collection<T> collection,
      final ValueMapper valueMapper,
      final OnMapperCompleteListener onMapperCompleteListener) {
    final Mutable<Boolean> didReturnError = new Mutable<Boolean>(false);
    final Mutable<Integer> pendingJobCount = new Mutable<Integer>(1);
    final OnMapperCompleteListener jobCompleteListener =
        new OnMapperCompleteListener() {
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
