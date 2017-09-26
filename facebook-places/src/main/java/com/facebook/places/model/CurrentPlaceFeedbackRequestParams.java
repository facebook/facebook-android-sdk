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

/**
 * Describes the parameters used to create a current place feedback request
 * with {@link com.facebook.places.PlaceManager}
 */
public class CurrentPlaceFeedbackRequestParams {

    private final String tracking;
    private final String placeId;
    private final Boolean wasHere;

    private CurrentPlaceFeedbackRequestParams(Builder b) {
        tracking = b.tracking;
        placeId = b.placeId;
        wasHere = b.wasHere;
    }

    /**
     * Gets the tracking ID. The tracking value is returned in the response
     * of the current place request {@link com.facebook.places.PlaceManager}
     *
     * @return The tracking ID returned by the current place request.
     */
    public String getTracking() {
        return tracking;
    }

    /**
     * Gets the place ID associated with the feedback request.
     * @return The place ID associated with the feedback.
     */
    public String getPlaceId() {
        return placeId;
    }

    /**
     * Indicates whether the user was actually located at the place specified by
     * {@code getPlaceId()}.
     * Could return null if {@code wasHere} was not set on the {@link Builder}.
     *
     * @return {@code true} if the user was at the place specified by getPlaceId(),
     * {@false} if not,
     * {@null} if wasHere was not specified on {@link Builder}
     */
    public Boolean wasHere() {
        return wasHere;
    }

    /**
     * Describes the builder to create a {@link CurrentPlaceFeedbackRequestParams}
     */
    public static class Builder {

        private String tracking;
        private String placeId;
        private Boolean wasHere;

        /**
         * Indicates whether the user was actually located at the place specified by
         * {@code setPlaceId()}.
         * @param wasHere {@code true} if the user was at the place specified by
         * {@code setPlaceId()}
         * @return the builder
         */
        public Builder setWasHere(boolean wasHere) {
            this.wasHere = wasHere;
            return this;
        }

        /**
         * Sets the place ID for the feedback request.
         * @param placeId the place ID associated with the feedback.
         * @return the builder
         */
        public Builder setPlaceId(String placeId) {
            this.placeId = placeId;
            return this;
        }

        /**
         * Sets the tracking ID. The tracking ID value is returned in the response
         * of the current place request. See {@link com.facebook.places.PlaceManager}
         * for information on how to place a current place request.
         *
         * @param tracking The tracking ID returned by the current place request.
         * @return The builder
         */
        public Builder setTracking(String tracking) {
            this.tracking = tracking;
            return this;
        }

        /**
         * Returns the {@link CurrentPlaceFeedbackRequestParams}
         * @return the {@link CurrentPlaceFeedbackRequestParams}
         */
        public CurrentPlaceFeedbackRequestParams build() {
            return new CurrentPlaceFeedbackRequestParams(this);
        }
    }
}
