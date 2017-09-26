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

package com.facebook.places.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Describes the parameters used to create a place info request
 * with {@link com.facebook.places.PlaceManager}.
 */
public final class PlaceInfoRequestParams {

    private final String placeId;
    private final Set<String> fields = new HashSet<>();

    private PlaceInfoRequestParams(Builder b) {
        placeId = b.placeId;
        fields.addAll(b.fields);
    }

    /**
     * Gets the ID of the place to be queried.
     * @return the ID of the place to be queried.
     */
    public String getPlaceId() {
        return placeId;
    }

    /**
     * Gets the list of fields to be returned in the response.
     * @return the list of fields to be returned in the response.
     */
    public Set<String> getFields() {
        return fields;
    }

    /**
     * Describes the builder to create a {@link PlaceInfoRequestParams}.
     */
    public static class Builder {

        private String placeId;
        private final Set<String> fields = new HashSet<>();

        /**
         * Sets the ID of the place to be queried.
         * @param placeId the ID of the place to be queried.
         * @return the builder.
         */
        public Builder setPlaceId(String placeId) {
            this.placeId = placeId;
            return this;
        }

        /**
         * Add a field to be returned in the response data. See {@link PlaceFields} for a list of
         * known fields. Refer to the online Places Graph documentation for an up to date list of
         * fields.
         * @param field The field to be added to the list of fields to be returned.
         * @return The builder.
         */
        public Builder addField(String field) {
            fields.add(field);
            return this;
        }

        /**
         * Add a list of fields to be returned in the response data. See {@link PlaceFields}
         * for a list of known fields. Refer to the online Places Graph documentation for an
         * up to date list of fields.
         * @param fields The list of fields to be returned in the response data.
         * @return The builder.
         */
        public Builder addFields(String[] fields) {
            for (String field : fields) {
                this.fields.add(field);
            }
            return this;
        }

        /**
         * Returns the {@link PlaceInfoRequestParams}.
         * @return the {@link PlaceInfoRequestParams}.
         */
        public PlaceInfoRequestParams build() {
            return new PlaceInfoRequestParams(this);
        }
    }
}
