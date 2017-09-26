/*
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

package com.facebook;

import android.net.Uri;
import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.internal.NativeProtocol;
import com.facebook.share.internal.OpenGraphJSONUtility;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.model.SharePhoto;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class ShareGraphRequest {

    /**
     * Create an User Owned Open Graph object
     *
     * Use this method to create an open graph object, which can then be posted utilizing the same
     * GraphRequest methods as other GraphRequests.
     *
     * @param openGraphObject The open graph object to create. Only SharePhotos with the imageUrl
     *                        set are accepted through this helper method.
     * @return GraphRequest for creating the given openGraphObject
     * @throws FacebookException thrown in the case of a JSONException or in the case of invalid
     *                           format for SharePhoto (missing imageUrl)
     */

    public static GraphRequest createOpenGraphObject(final ShareOpenGraphObject openGraphObject)
            throws FacebookException {
        String type = openGraphObject.getString("type");
        if (type == null) {
            type = openGraphObject.getString("og:type");
        }

        if (type == null) {
            throw new FacebookException("Open graph object type cannot be null");
        }
        try {
            JSONObject stagedObject = (JSONObject) OpenGraphJSONUtility.toJSONValue(
                    openGraphObject,
                    new OpenGraphJSONUtility.PhotoJSONProcessor() {
                        @Override
                        public JSONObject toJSONObject(SharePhoto photo) {
                            Uri photoUri = photo.getImageUrl();
                            JSONObject photoJSONObject = new JSONObject();
                            try {
                                photoJSONObject.put(
                                        NativeProtocol.IMAGE_URL_KEY, photoUri.toString());
                            } catch (Exception e) {
                                throw new FacebookException("Unable to attach images", e);
                            }
                            return photoJSONObject;
                        }
                    });
            String ogType = type;
            Bundle parameters = new Bundle();
            parameters.putString("object", stagedObject.toString());

            String graphPath = String.format(
                    Locale.ROOT, "%s/%s",
                    "me",
                    "objects/" + ogType);
            return new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    graphPath,
                    parameters,
                    HttpMethod.POST);
        }
        catch(JSONException e){
            throw new FacebookException(e.getMessage());
        }
    }

}
