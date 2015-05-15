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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameters used by {@link MessengerUtils} for sending the media to Messenger to share.
 * See <a href="https://developers.facebook.com/docs/messenger/android">developer docs</a> for
 * more info.
 */
public class ShareToMessengerParams {

  public static final Set<String> VALID_URI_SCHEMES;
  public static final Set<String> VALID_MIME_TYPES;
  public static final Set<String> VALID_EXTERNAL_URI_SCHEMES;

  static {
    Set<String> validMimeTypes = new HashSet<String>();
    validMimeTypes.add("image/*");
    validMimeTypes.add("image/jpeg");
    validMimeTypes.add("image/png");
    validMimeTypes.add("image/gif");
    validMimeTypes.add("image/webp");
    validMimeTypes.add("video/*");
    validMimeTypes.add("video/mp4");
    validMimeTypes.add("audio/*");
    validMimeTypes.add("audio/mpeg");
    VALID_MIME_TYPES = Collections.unmodifiableSet(validMimeTypes);

    Set<String> validUriSchemes = new HashSet<String>();
    validUriSchemes.add("content");
    validUriSchemes.add("android.resource");
    validUriSchemes.add("file");
    VALID_URI_SCHEMES = Collections.unmodifiableSet(validUriSchemes);

    Set<String> validExternalUriSchemes = new HashSet<String>();
    validExternalUriSchemes.add("http");
    validExternalUriSchemes.add("https");
    VALID_EXTERNAL_URI_SCHEMES = Collections.unmodifiableSet(validExternalUriSchemes);
  }

  /**
   * The URI of the local image, video, or audio clip to send to messenger. Must be
   * a file://, content://, or res:// URI.
   */
  public final Uri uri;

  /**
   * The mime type of the content. See {@link #VALID_MIME_TYPES} for what mime types are supported.
   */
  public final String mimeType;

  /**
   * The metadata to attach to the content to shared. See
   * <a href="https://developers.facebook.com/docs/messenger/android">developer docs</a> for more
   * info.
   */
  public final String metaData;

  /**
   * An external URI that Messenger can use to download the content on Facebook's servers
   * instead of requiring the Messenger application to upload the content. The content returned by
   * the this URI must be exactly the same as the content specified by {@link #uri}. If the
   * content is different, Messenger may fail to send the content. See
   * <a href="https://developers.facebook.com/docs/messenger/android">developer docs</a> for more
   * info.
   */
  public final Uri externalUri;

  ShareToMessengerParams(ShareToMessengerParamsBuilder builder) {
    uri = builder.getUri();
    mimeType = builder.getMimeType();
    metaData = builder.getMetaData();
    externalUri = builder.getExternalUri();

    if (uri == null) {
      throw new NullPointerException("Must provide non-null uri");
    }
    if (mimeType == null) {
      throw new NullPointerException("Must provide mimeType");
    }
    if (!VALID_URI_SCHEMES.contains(uri.getScheme())) {
      throw new IllegalArgumentException("Unsupported URI scheme: " + uri.getScheme());
    }
    if (!VALID_MIME_TYPES.contains(mimeType)) {
      throw new IllegalArgumentException("Unsupported mime-type: " + mimeType);
    }
    if (externalUri != null) {
      if (!VALID_EXTERNAL_URI_SCHEMES.contains(externalUri.getScheme())) {
        throw new IllegalArgumentException("Unsupported external uri scheme: " +
            externalUri.getScheme());
      }
    }
  }

  /**
   * Creates a new builder for creating a {@link ShareToMessengerParams} instance
   *
   * @param uri the uri of the local content. Must be a file://, content://, or res:// URI.
   * @param mimeType the mime-type of the content. See {@link #VALID_MIME_TYPES} for what mime
   *                 types are supported.
   * @return the builder instance
   */
  public static ShareToMessengerParamsBuilder newBuilder(Uri uri, String mimeType) {
    return new ShareToMessengerParamsBuilder(uri, mimeType);
  }
}
