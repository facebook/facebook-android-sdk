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

package com.facebook.messenger;

import java.util.List;

/**
 * Parameters describing the Intent that Messenger sent to the app. Returned by
 * {@link MessengerUtils#getMessengerThreadParamsForIntent}.
 */
public class MessengerThreadParams {

  /**
   * The origin of the flow that user originated from.
   */
  public enum Origin {
    /**
     * The user clicked on a reply link in Messenger to a particular message.
     */
    REPLY_FLOW,

    /**
     * The user clicked an app shortcut in Messenger.
     */
    COMPOSE_FLOW,

    /**
     * The user came from a flow that was not known at the time this code was written.
     */
    UNKNOWN
  }

  /**
   * The origin of the flow that the user originated from.
   */
  public final Origin origin;

  /**
   * A token representing the thread the user originated from. This is an opaque value that is not
   * meant for the app to consume. It exists to complete the flow back to Mesenger.
   */
  public final String threadToken;

  /**
   * Metadata that originated from content the app originally set when it sent the request to
   * Messenger.
   */
  public final String metadata;

  /**
   * The list of participants in the thread represented as App-scoped User IDs. This may not
   * always be set and will only ever be set for apps that include Facebook login. When set, it
   * will only include the participants in the thread that have logged into the app. See
   * <a href="https://developers.facebook.com/docs/apps/upgrading">docs</a> for more info.
   */
  public final List<String> participants;

  public MessengerThreadParams(
      Origin origin,
      String threadToken,
      String metadata,
      List<String> participants) {
    this.threadToken = threadToken;
    this.metadata = metadata;
    this.participants = participants;
    this.origin = origin;
  }
}
