/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.messenger

/**
 * Parameters describing the Intent that Messenger sent to the app.
 * Returned by [MessengerUtils.getMessengerThreadParamsForIntent].
 */
class MessengerThreadParams(
    /** The origin of the flow that the user originated from.  */
    val origin: Origin,
    /**
     * A token representing the thread the user originated from. This is an opaque value that is not
     * meant for the app to consume. It exists to complete the flow back to Messenger.
     */
    val threadToken: String,
    /**
     * Metadata that originated from content the app originally set when it sent the request to
     * Messenger.
     */
    val metadata: String,
    /**
     * The list of participants in the thread represented as App-scoped User IDs. This may not always
     * be set and will only ever be set for apps that include Facebook login. When set, it will only
     * include the participants in the thread that have logged into the app.
     * See [docs](https://developers.facebook.com/docs/apps/upgrading) for more info.
     */
    val participants: List<String>) {

  /** The origin of the flow that user originated from.  */
  enum class Origin {
    /** The user clicked on a reply link in Messenger to a particular message.  */
    REPLY_FLOW,

    /** The user clicked an app shortcut in Messenger.  */
    COMPOSE_FLOW,

    /** The user came from a flow that was not known at the time this code was written.  */
    UNKNOWN
  }
}
