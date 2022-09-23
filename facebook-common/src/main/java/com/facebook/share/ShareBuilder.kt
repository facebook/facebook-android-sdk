/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share

/**
 * Interface for builders related to sharing.
 *
 * @param <M> The class of the object to be built.
 * @param <B> The concrete builder class.
 */
fun interface ShareBuilder<M, B : ShareBuilder<M, B>> {
  /**
   * Builds the object.
   *
   * @return The built object.
   */
  fun build(): M
}
