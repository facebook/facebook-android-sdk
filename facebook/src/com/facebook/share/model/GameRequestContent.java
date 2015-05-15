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

import java.util.ArrayList;

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
    private final String to;
    private final String title;
    private final String data;

    private final ActionType actionType;
    private final String objectId;
    private final Filters filters;
    private final ArrayList<String> suggestions;

    private GameRequestContent(final Builder builder) {
        this.message = builder.message;
        this.to = builder.to;
        this.title = builder.title;
        this.data = builder.data;
        this.actionType = builder.actionType;
        this.objectId = builder.objectId;
        this.filters = builder.filters;
        this.suggestions = builder.suggestions;
    }

    GameRequestContent(final Parcel in) {
        this.message = in.readString();
        this.to = in.readString();
        this.title = in.readString();
        this.data = in.readString();
        this.actionType = ActionType.valueOf(in.readString());
        this.objectId = in.readString();
        this.filters = Filters.valueOf(in.readString());
        this.suggestions = new ArrayList<>();
        in.readStringList(this.suggestions);
    }

    /**
     * Gets the message that users receiving the request will see.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the user ID or user name the request will be sent to.
     */
    public String getTo() {
        return to;
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
    public ArrayList<String> getSuggestions() {
        return this.suggestions;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(final Parcel out, final int flags) {
        out.writeString(this.message);
        out.writeString(this.to);
        out.writeString(this.title);
        out.writeString(this.data);
        out.writeString(this.getActionType().toString());
        out.writeString(this.getObjectId());
        out.writeString(this.getFilters().toString());
        out.writeStringList(this.getSuggestions());
    }

    /**
     * Builder class for a concrete instance of GameRequestContent
     */
    public static class Builder
            implements ShareModelBuilder<GameRequestContent, Builder> {
        private String message;
        private String to;
        private String data;
        private String title;
        private ActionType actionType;
        private String objectId;
        private Filters filters;
        private ArrayList<String> suggestions;

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
         * @param to the id or user name to send the request to
         * @return the builder
         */
        public Builder setTo(final String to) {
            this.to = to;
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
        public Builder setSuggestions(ArrayList<String> suggestions) {
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
                    .setTo(content.getTo())
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
                    (GameRequestContent)parcel.readParcelable(
                            GameRequestContent.class.getClassLoader()));
        }
    }
}
