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

import android.net.Uri;

/**
 * Builder for {@link ShareToMessengerParams}.
 */
public class ShareToMessengerParamsBuilder {

  private final Uri mUri;
  private final String mMimeType;
  private String mMetaData;
  private Uri mExternalUri;

  ShareToMessengerParamsBuilder(Uri uri, String mimeType) {
    mUri = uri;
    mMimeType = mimeType;
  }

  /**
   * Gets the URI of the local image, video, or audio clip to send to messenger. Must be
   * a file://, content://, or res:// URI.
   *
   * @return the uri
   */
  public Uri getUri() {
    return mUri;
  }

  /**
   * Gets the mime type of the content. See {@link ShareToMessengerParams#VALID_MIME_TYPES} for
   * what mime types are supported.
   *
   * @return the mime type
   */
  public String getMimeType() {
    return mMimeType;
  }

  /**
   * Sets the metadata to attach to the content to shared. See
   * <a href="https://developers.facebook.com/docs/messenger/android">developer docs</a> for more
   * info.
   *
   * @param metaData the metadata to attach to the message
   * @return this builder
   */
  public ShareToMessengerParamsBuilder setMetaData(String metaData) {
    mMetaData = metaData;
    return this;
  }

  /**
   * Gets the metadata to attach to the content to shared. See
   * <a href="https://developers.facebook.com/docs/messenger/android">developer docs</a> for more
   * info.
   *
   * @return the metadata to attach to the message
   */
  public String getMetaData() {
    return mMetaData;
  }

  /**
   * Sets an external URI that Messenger can use to download the content on Facebook's servers
   * instead of requiring the Messenger application to upload the content. The content returned by
   * the this URI must be exactly the same as the content specified by {@link #getUri()}. If the
   * content is different, Messenger may fail to send the content. See
   * <a href="https://developers.facebook.com/docs/messenger/android">developer docs</a> for more
   * info.
   *
   * @param externalUri the external uri to set
   * @return this builder
   */
  public ShareToMessengerParamsBuilder setExternalUri(Uri externalUri) {
    mExternalUri = externalUri;
    return this;
  }

  /**
   * Gets an external URI that Messenger can use to download the content on Facebook's servers
   * instead of requiring the Messenger application to upload the content. The content returned by
   * the this URI must be exactly the same as the content specified by {@link #getUri()}. If the
   * content is different, Messenger may fail to send the content. See
   * <a href="https://developers.facebook.com/docs/messenger/android">developer docs</a> for more
   * info.
   *
   * @return the external URI
   */
  public Uri getExternalUri() {
    return mExternalUri;
  }

  /**
   * Builds the parameter object.
   *
   * @return the parameter object
   */
  public ShareToMessengerParams build() {
    return new ShareToMessengerParams(this);
  }
}
