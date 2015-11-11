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

package com.facebook;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.facebook.internal.ImageRequest;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class represents a basic Facebook profile.
 */
public final class Profile implements Parcelable {
    private static final String ID_KEY = "id";
    private static final String FIRST_NAME_KEY = "first_name";
    private static final String MIDDLE_NAME_KEY = "middle_name";
    private static final String LAST_NAME_KEY = "last_name";
    private static final String NAME_KEY = "name";
    private static final String LINK_URI_KEY = "link_uri";

    private final String id;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String name;
    private final Uri linkUri;

    /**
     * Getter for the profile that is currently logged in to the application.
     * @return The profile that is currently logged in to the application.
     */
    public static Profile getCurrentProfile()
    {
        return ProfileManager.getInstance().getCurrentProfile();
    }

    /**
     * Setter for the profile that is currently logged in to the application. If the access token is
     * invalidated, the current profile will not be updated. It's only updated when there is an
     * explicit logout, login or when permissions change via the
     * {@link com.facebook.login.LoginManager}.
     * @param profile The profile that is currently logged in to the application.
     */
    public static void setCurrentProfile(Profile profile) {
        ProfileManager.getInstance().setCurrentProfile(profile);
    }

    /**
     * Fetches and sets the current profile from the current access token.
     * <p/>
     * This should only be called from the UI thread.
     */
    public static void fetchProfileForCurrentAccessToken() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null) {
            Profile.setCurrentProfile(null);
            return;
        }

        Utility.getGraphMeRequestWithCacheAsync(accessToken.getToken(),
                new Utility.GraphMeRequestWithCacheCallback() {
                    @Override
                    public void onSuccess(JSONObject userInfo) {
                        String id = userInfo.optString("id");
                        if (id == null) {
                            return;
                        }
                        String link = userInfo.optString("link");
                        Profile profile = new Profile(
                                id,
                                userInfo.optString("first_name"),
                                userInfo.optString("middle_name"),
                                userInfo.optString("last_name"),
                                userInfo.optString("name"),
                                link != null ? Uri.parse(link) : null
                        );
                        Profile.setCurrentProfile(profile);
                    }

                    @Override
                    public void onFailure(FacebookException error) {
                        return;
                    }
                });
    }

    /**
     * Contructor.
     * @param id         The id of the profile.
     * @param firstName  The first name of the profile. Can be null.
     * @param middleName The middle name of the profile. Can be null.
     * @param lastName   The last name of the profile. Can be null.
     * @param name       The name of the profile. Can be null.
     * @param linkUri    The link for this profile. Can be null.
     */
    public Profile(
            final String id,
            @Nullable
            final String firstName,
            @Nullable
            final String middleName,
            @Nullable
            final String lastName,
            @Nullable
            final String name,
            @Nullable
            final Uri linkUri) {
        Validate.notNullOrEmpty(id, "id");

        this.id = id;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.name = name;
        this.linkUri = linkUri;
    }

    /**
     * Getter for the Uri of the profile picture.
     *
     * @param width  The desired width for the profile picture.
     * @param height The desired height for the profile picture.
     * @return The Uri of the profile picture.
     */
    public Uri getProfilePictureUri(
            int width,
            int height) {
        return ImageRequest.getProfilePictureUri(this.id, width, height);
    }

    /**
     * Getter for the id of the profile.
     * @return id of the profile.
     */
    public String getId() {
        return id;
    }

    /**
     * Getter for the first name of the profile.
     * @return the first name of the profile.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Getter for the middle name of the profile.
     * @return the middle name of the profile.
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Getter for the last name of the profile.
     * @return the last name of the profile.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Getter for the name of the profile.
     * @return the name of the profile.
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the link of the profile.
     * @return the link of the profile.
     */
    public Uri getLinkUri() {
        return linkUri;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Profile)) {
            return false;
        }

        Profile o = (Profile) other;

        return id.equals(o.id) &&
                firstName == null ? o.firstName == null : firstName.equals(o.firstName) &&
                middleName == null ? o.middleName == null : middleName.equals(o.middleName) &&
                lastName == null ? o.lastName == null : lastName.equals(o.lastName) &&
                name == null ? o.name == null : name.equals(o.name) &&
                linkUri == null ? o.linkUri == null : linkUri.equals(o.linkUri);
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 31 + id.hashCode();
        if (firstName != null) {
            result = result * 31 + firstName.hashCode();
        }
        if (middleName != null) {
            result = result * 31 + middleName.hashCode();
        }
        if (lastName != null) {
            result = result * 31 + lastName.hashCode();
        }
        if (name != null) {
            result = result * 31 + name.hashCode();
        }
        if (linkUri != null) {
            result = result * 31 + linkUri.hashCode();
        }

        return result;
    }

    JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(ID_KEY, id);
            jsonObject.put(FIRST_NAME_KEY, firstName);
            jsonObject.put(MIDDLE_NAME_KEY, middleName);
            jsonObject.put(LAST_NAME_KEY, lastName);
            jsonObject.put(NAME_KEY, name);
            if (linkUri != null) {
                jsonObject.put(LINK_URI_KEY, linkUri.toString());
            }
        } catch (JSONException object) {
            jsonObject = null;
        }
        return jsonObject;
    }

    Profile(JSONObject jsonObject) {
        id = jsonObject.optString(ID_KEY, null);
        firstName = jsonObject.optString(FIRST_NAME_KEY, null);
        middleName = jsonObject.optString(MIDDLE_NAME_KEY, null);
        lastName = jsonObject.optString(LAST_NAME_KEY, null);
        name = jsonObject.optString(NAME_KEY, null);
        String linkUriString = jsonObject.optString(LINK_URI_KEY, null);
        linkUri = linkUriString == null ? null : Uri.parse(linkUriString);
    }

    private Profile(Parcel source) {
        id = source.readString();
        firstName = source.readString();
        middleName = source.readString();
        lastName = source.readString();
        name = source.readString();
        String linkUriString = source.readString();
        linkUri = linkUriString == null ? null : Uri.parse(linkUriString);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(firstName);
        dest.writeString(middleName);
        dest.writeString(lastName);
        dest.writeString(name);
        dest.writeString(linkUri == null ? null : linkUri.toString());
    }

    public static final Parcelable.Creator<Profile> CREATOR = new Parcelable.Creator() {

        @Override
        public Profile createFromParcel(Parcel source) {
            return new Profile(source);
        }

        @Override
        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };
}
