/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.model

import com.facebook.share.ShareBuilder

/**
 * Interface for builders related to sharing.
 *
 * @param <M> The model protocol to be built.
 * @param <B> The concrete builder class.
 */
interface ShareModelBuilder<M : ShareModel, B : ShareModelBuilder<M, B>> : ShareBuilder<M, B> {
  /**
   * Reads the values from a ShareModel into the builder.
   *
   * @param model The source ShareModel
   * @return The builder.
   */
  fun readFrom(model: M?): B
}
