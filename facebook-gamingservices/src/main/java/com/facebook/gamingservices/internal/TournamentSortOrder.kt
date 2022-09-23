/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.internal

// The sort order for how scores are ranked in the tournament.
enum class TournamentSortOrder(val rawValue: String) {
  LowerIsBetter("LOWER_IS_BETTER"),
  HigherIsBetter("HIGHER_IS_BETTER");

  override fun toString(): String {
    return rawValue
  }
}
