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
 * Describes the fields available when searching for a place, or when querying a
 * place's' information with {@link com.facebook.places.PlaceManager}. Refer to
 * the online Places Graph documentation to get the complete list of supported
 * place fields.
 */
public interface PlaceFields {

    /**
     * Information about the Place.
     */
    String ABOUT = "about";

    /**
     * AppLinks to the Place on various devices.
     */
    String APP_LINKS = "app_links";

    /**
     * The Place's categories.
     */
    String CATEGORY_LIST = "category_list";

    /**
     * The number of checkins at this Place.
     */
    String CHECKINS = "checkins";

    /**
     * To be used on the current_place request. Indicates the confidence level of the
     * current place result.
     */
    String CONFIDENCE_LEVEL = "confidence_level";

    /**
     * OpenGraphContext. The social context for this Place, including friends who were at
     * this place, or who liked to its page. This field requires authentication with a user
     * token. An error will be thrown if this field is requested using a client token.
     */
    String CONTEXT = "context";

    /**
     * CoverPhoto. Information about the cover image.
     */
    String COVER = "cover";

    /**
     * The description of the Place.
     */
    String DESCRIPTION = "description";

    /**
     * Engagement. The social sentence and like count information for this place.
     * This is the same information used for the Like button.
     */
    String ENGAGEMENT = "engagement";

    /**
     * Specifies a single range of open hours per day.
     * Each day can have two different hours ranges.
     * The keys in the map are in the form of {day}_{number}_{status}.
     * {day} should be the first 3 characters of the day of the week,
     * {number} should be either 1 or 2 to allow for the two different hours ranges per day.
     * {status} should be either open or close, to delineate the start or end of a time range.
     * An example would be mon_1_open with value 17:00 and mon_1_close with value 21:15
     * which would represent a single opening range of 5 PM to 9:15 PM on Mondays.
     */
    String HOURS = "hours";

    /**
     * The unique node ID of the place on the graph api.
     */
    String ID = "id";

    /**
     * Indicates whether this place is always open.
     */
    String IS_ALWAYS_OPEN = "is_always_open";

    /**
     * Indicates whether this place is permanently closed.
     */
    String IS_PERMANENTLY_CLOSED = "is_permanently_closed";

    /**
     * Pages with a large number of followers can be manually verified by Facebook as having
     * an authentic identity. This field indicates whether the page is verified by this process.
     */
    String IS_VERIFIED = "is_verified";

    /**
     * The place's web URL.
     */
    String LINK = "link";

    /**
     * Location information about the Place. E.g., latitude and longitude, and street address.
     */
    String LOCATION = "location";

    /**
     * The categories that this place matched.
     * To be used on the search request if the categories parameter is specified.
     */
    String MATCHED_CATEGORIES = "matched_categories";

    /**
     * The name of the place.
     */
    String NAME = "name";

    /**
     * Overall page rating based on a rating survey from users, on a scale of 1-5.
     * This value is normalized, and is not guaranteed to be a strict average of user ratings.
     */
    String OVERALL_STAR_RATING = "overall_star_rating";

    /**
     * PageParking. Parking information about the Place.
     */
    String PARKING = "parking";

    /**
     * The available payment options.
     */
    String PAYMENT_OPTIONS = "payment_options";

    /**
     * The place's phone number.
     */
    String PHONE = "phone";

    /**
     * Profile photos posted by the place's Page.
     */
    String PHOTOS_PROFILE = "photos";

    /**
     * Photos on the place's Page tagged by other Pages.
     */
    String PHOTOS_TAGGED = "photos.type(tagged)";

    /**
     * Photos uploaded by the place's Page.
     */
    String PHOTOS_UPLOADED = "photos.type(uploaded)";

    /**
     * The current profile picture of the place's Page.
     */
    String PICTURE = "picture";

    /**
     * Price range of the business. Applicable to Restaurants or Nightlife.
     * Can be one of $ (0-10), $$ (10-30), $$$ (30-50), $$$$ (50+), or Unspecified.
     */
    String PRICE_RANGE = "price_range";

    /**
     * Number of ratings for the place.
     */
    String RATING_COUNT = "rating_count";

    /**
     * Restaurant services. Example: delivery, takeout.
     */
    String RESTAURANT_SERVICES = "restaurant_services";

    /**
     * PageRestaurantSpecialties. The restaurant's specialties. Applicable to Restaurants.
     */
    String RESTAURANT_SPECIALTIES = "restaurant_specialties";

    /**
     * The address, in a single line of text.
     */
    String SINGLE_LINE_ADDRESS = "single_line_address";

    /**
     * The URL of the place's website.
     */
    String WEBSITE = "website";

    /**
     * Workflows.
     */
    String WORKFLOWS = "workflows";
}
