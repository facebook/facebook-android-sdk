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

package com.facebook.share;

/**
 * The common interface for components that initiate sharing.
 * @see com.facebook.share.widget.ShareDialog
 * @see com.facebook.share.widget.MessageDialog
 */
public interface Sharer {
    /**
     * Specifies whether the sharer should fail if it finds an error with the share content.
     * If false, the share dialog will still be displayed without the data that was mis-configured.
     * For example, an invalid placeID specified on the shareContent would produce a data error.
     * @return A Boolean value.
     */
    public boolean getShouldFailOnDataError();

    /**
     * Specifies whether the sharer should fail if it finds an error with the share content.
     * If false, the share dialog will still be displayed without the data that was mis-configured.
     * For example, an invalid placeID specified on the shareContent would produce a data error.
     *
     * @param shouldFailOnDataError whether the dialog should fail if it finds an error.
     */
    public void setShouldFailOnDataError(boolean shouldFailOnDataError);

    /**
     * Helper object for handling the result from a share dialog or share operation
     */
    public static class Result {
        final String postId;

        /**
         * Constructor.
         * @param postId the resulting post id.
         */
        public Result(String postId) {
            this.postId = postId;
        }

        /**
         * Returns the post id, if available.
         * @return the post id.
         */
        public String getPostId() {
            return postId;
        }
    }
}
