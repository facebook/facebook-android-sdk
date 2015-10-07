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

package com.facebook.share.model;

import android.os.Parcel;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Describes the content that will be displayed by the GameRequestDialog
 */
public final class GameRequestContent implements ShareModel {
    public enum ActionType {
        SEND,
        ASKFOR,
        TURN,
    }

    public enum Filters {
        APP_USERS,
        APP_NON_USERS,
    }

    private final String message;
    private final List<String> recipients;
    private final String title;
    private final String data;

    private final ActionType actionType;
    private final String objectId;
    private final Filters filters;
    private final List<String> suggestions;

    private GameRequestContent(final Builder builder) {
        this.message = builder.message;
        this.recipients = builder.recipients;
        this.title = builder.title;
        this.data = builder.data;
        this.actionType = builder.actionType;
        this.objectId = builder.objectId;
        this.filters = builder.filters;
        this.suggestions = builder.suggestions;
    }

    GameRequestContent(final Parcel in) {
        this.message = in.readString();
        this.recipients = in.createStringArrayList();
        this.title = in.readString();
        this.data = in.readString();
        this.actionType = (ActionType) in.readSerializable();
        this.objectId = in.readString();
        this.filters = (Filters) in.readSerializable();
        this.suggestions = in.createStringArrayList();
        in.readStringList(this.suggestions);
    }

    /**
     * Gets the message that users receiving the request will see.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the user IDs or user names the request will be sent to.
     *
     * @deprecated Replaced by {@link #getRecipients()}
     * */
    public String getTo() {
        return this.getRecipients() != null ? TextUtils.join(",", this.getRecipients()) : null;
    }

    /**
     * Gets the user IDs or user names the request will be sent to.
     */
    public List<String> getRecipients() {
        return recipients;
    }

    /**
     * Gets the optional title for the dialog
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets optional data which can be used for tracking
     */
    public String getData() {
        return data;
    }

    /**
     * Gets the action type
     */
    public ActionType getActionType() {
        return this.actionType;
    }

    /**
     * Gets the open graph id of the object that action type will be performed on
     */
    public String getObjectId() {
        return this.objectId;
    }

    /**
     * Get the filters
     */
    public Filters getFilters() {
        return this.filters;
    }

    /**
     * Gets a list of suggested user ids
     */
    public List<String> getSuggestions() {
        return this.suggestions;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        out.writeString(this.message);
        out.writeStringList(this.recipients);
        out.writeString(this.title);
        out.writeString(this.data);
        out.writeSerializable(this.actionType);
        out.writeString(this.objectId);
        out.writeSerializable(this.filters);
        out.writeStringList(this.suggestions);
    }

    @SuppressWarnings("unused")
    public static final Creator<GameRequestContent> CREATOR =
            new Creator<GameRequestContent>() {
                public GameRequestContent createFromParcel(final Parcel in) {
                    return new GameRequestContent(in);
                }

                public GameRequestContent[] newArray(final int size) {
                    return new GameRequestContent[size];
                }
            };

    /**
     * Builder class for a concrete instance of GameRequestContent
     */
    public static class Builder
            implements ShareModelBuilder<GameRequestContent, Builder> {
        private String message;
        private List<String> recipients;
        private String data;
        private String title;
        private ActionType actionType;
        private String objectId;
        private Filters filters;
        private List<String> suggestions;

        /**
         * Sets the message users receiving the request will see. The maximum length
         * is 60 characters.
         *
         * @param message the message
         * @return the builder
         */
        public Builder setMessage(final String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the user ID or user name the request will be sent to. If this is not
         * specified, a friend selector will be displayed and the user can select up
         * to 50 friends.
         *
         * @deprecated Replaced by {@link #setRecipients(List)}
         * @param to the id or user name to send the request to
         * @return the builder
         */
        public Builder setTo(final String to) {
            if (to != null) {
                String[] recipientsArray = to.split(",");
                this.recipients = Arrays.asList(recipientsArray);
            }

            return this;
        }

        /**
         * An array of user IDs, usernames or invite tokens of people to send request.
         * If this is not specified, a friend selector will be displayed and the user
         * can select up to 50 friends.
         *
         * This is equivalent to the "to" parameter when using the web game request dialog.
         *
         * @param recipients the list of user ids to send the request to
         * @return the builder
         */
        public Builder setRecipients(List<String> recipients) {
            this.recipients = recipients;
            return this;
        }

        /**
         * Sets optional data which can be used for tracking; maximum length is 255
         * characters.
         *
         * @param data the data
         * @return the builder
         */
        public Builder setData(final String data) {
            this.data = data;
            return this;
        }

        /**
         * Sets an optional title for the dialog; maximum length is 50 characters.
         *
         * @param title the title
         * @return the builder
         */
        public Builder setTitle(final String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the action type for this request
         */
        public Builder setActionType(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        /**
         * Sets the open graph id of the object that action type will be performed on
         * Only valid (and required) for ActionTypes SEND, ASKFOR
         */
        public Builder setObjectId(String objectId) {
            this.objectId = objectId;
            return this;
        }

        /**
         * Sets the filters for everybody/app users/non app users
         */
        public Builder setFilters(Filters filters) {
            this.filters = filters;
            return this;
        }

        /**
         * Sets a list of user ids suggested as request receivers
         */
        public Builder setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
            return this;
        }

        @Override
        public GameRequestContent build() {
            return new GameRequestContent(this);
        }

        @Override
        public Builder readFrom(final GameRequestContent content) {
            if (content == null) {
                return this;
            }
            return this
                    .setMessage(content.getMessage())
                    .setRecipients(content.getRecipients())
                    .setTitle(content.getTitle())
                    .setData(content.getData())
                    .setActionType(content.getActionType())
                    .setObjectId(content.getObjectId())
                    .setFilters(content.getFilters())
                    .setSuggestions(content.getSuggestions());
        }

        @Override
        public Builder readFrom(final Parcel parcel) {
            return this.readFrom(
                    (GameRequestContent) parcel.readParcelable(
                            GameRequestContent.class.getClassLoader()));
        }
    }
}
