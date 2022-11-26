/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share

/**
 * The common interface for components that initiate sharing.
 *
 * @see com.facebook.share.widget.ShareDialog
 *
 * @see com.facebook.share.widget.MessageDialog
 */
interface Sharer {
  /**
   * Specifies whether the sharer should fail if it finds an error with the share content. If false,
   * the share dialog will still be displayed without the data that was mis-configured. For example,
   * an invalid placeID specified on the shareContent would produce a data error.
   *
   * @return A Boolean value.
   */
  fun getShouldFailOnDataError(): Boolean

  /**
   * Specifies whether the sharer should fail if it finds an error with the share content. If false,
   * the share dialog will still be displayed without the data that was mis-configured. For example,
   * an invalid placeID specified on the shareContent would produce a data error.
   *
   * @param shouldFailOnDataError whether the dialog should fail if it finds an error.
   */
  fun setShouldFailOnDataError(shouldFailOnDataError: Boolean)

  /** Helper object for handling the result from a share dialog or share operation */
  class Result
  /**
   * Constructor.
   *
   * @param postId the resulting post id.
   */
  (
      /**
       * Returns the post id, if available.
       *
       * @return the post id.
       */
      val postId: String?
  )
}
