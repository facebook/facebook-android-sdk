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
 * Describes the parameters to create a place search request
 * with {@link com.facebook.places.PlaceManager}.
 */
public final class PlaceSearchRequestParams {

    private final int distance;
    private final int limit;
    private final String searchText;
    private final Set<String> categories = new HashSet<>();
    private final Set<String> fields = new HashSet<>();

    private PlaceSearchRequestParams(Builder b) {
        distance = b.distance;
        limit = b.limit;
        searchText = b.searchText;
        categories.addAll(b.categories);
        fields.addAll(b.fields);
    }

    /**
     * Gets the maximum distance (in meters) from the location specified.
     * This can be used only in conjunction with a location.
     *
     * @return The maximum distance in meters from the location specified.
     */
    public int getDistance() {
        return distance;
    }

    /**
     * Sets the maximum number of results to be returned.
     *
     * @return The maximum number of results to return.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Gets the name of the place to search for.
     *
     * @@return The name of the place to search for.
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Gets the categories used to filter the place search results.
     * @return The categories used to filter the place search results.
     */
    public Set<String> getCategories() {
        return categories;
    }

    /**
     * Gets the list of fields to be returned in the response.
     * @return The list of fields to be returned in the response.
     */
    public Set<String> getFields() {
        return fields;
    }

    /**
     * Describes the builder to create a {@link PlaceSearchRequestParams}
     */
    public static class Builder {

        private int distance;
        private int limit;
        private String searchText;
        private final Set<String> categories = new HashSet<>();
        private final Set<String> fields = new HashSet<>();

        /**
         * Sets the maximum search radius in meters. If {@code PlaceManager.newPlaceSearchRequest()}
         * is used, then this parameter defines the maximum search radius around the current device
         * location. If {@code PlaceManager.newPlaceSearchRequestForLocation()} is used, then this
         * parameter defines the maximum search radius around the specified location.
         *
         * @param distance The maximum distance in meters.
         * @return The builder.
         */
        public Builder setDistance(int distance) {
            this.distance = distance;
            return this;
        }

        /**
         * Sets the maximum number of places to be returned.
         *
         * @param limit The maximum number of places to return.
         * @return The builder.
         */
        public Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the name of the place to search for.
         * If this parameter is not specified, then you must supply a location,
         * and the response will contain places that are near the given location.
         *
         * @param searchText The name of the place to search for.
         * @return The builder.
         */
        public Builder setSearchText(String searchText) {
            this.searchText = searchText;
            return this;
        }

        /**
         * Add a place category to restrict the search results. Refer to the online Places Graph
         * documentation to see the list of supported categories.
         * @param category The name of the place category to add as a filter.
         * @return The builder.
         */
        public Builder addCategory(String category) {
            categories.add(category);
            return this;
        }

        /**
         * Add a place information field to the list of fields to be returned.
         * Refer to {@link PlaceFields} for a list of fields. Refer to the online Places Graph
         * documentation for the current list of supported fields.
         *
         * @param field The field to be returned in the place search response.
         * @return The builder.
         */
        public Builder addField(String field) {
            fields.add(field);
            return this;
        }

        /**
         * Returns the {@link PlaceSearchRequestParams}.
         * @return the {@link PlaceSearchRequestParams}.
         */
        public PlaceSearchRequestParams build() {
            return new PlaceSearchRequestParams(this);
        }
    }
}
