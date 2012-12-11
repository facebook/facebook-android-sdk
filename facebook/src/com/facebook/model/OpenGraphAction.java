/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.model;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Provides a strongly-typed representation of an Open Graph Action.
 * For more documentation of OG Actions, see: https://developers.facebook.com/docs/opengraph/actions/
 *
 * Note that this interface is intended to be used with GraphObject.Factory
 * and not implemented directly.
 */
public interface OpenGraphAction extends GraphObject {
    /**
     * Gets the ID of the action.
     * @return the ID
     */
    public String getId();

    /**
     * Sets the ID of the action.
     * @param id the ID
     */
    public void setId(String id);

    /**
     * Gets the start time of the action.
     * @return the start time
     */
    public Date getStartTime();

    /**
     * Sets the start time of the action.
     * @param startTime the start time
     */
    public void setStartTime(Date startTime);

    /**
     * Gets the end time of the action.
     * @return the end time
     */
    public Date getEndTime();

    /**
     * Sets the end time of the action.
     * @param endTime the end time
     */
    public void setEndTime(Date endTime);

    /**
     * Gets the time the action was published, if any.
     * @return the publish time
     */
    public Date getPublishTime();

    /**
     * Sets the time the action was published.
     * @param publishTime the publish time
     */
    public void setPublishTime(Date publishTime);

    /**
     * Gets the time the action was created.
     * @return the creation time
     */
    public Date getCreatedTime();

    /**
     * Sets the time the action was created.
     * @param createdTime the creation time
     */
    public void setCreatedTime(Date createdTime);

    /**
     * Gets the time the action expires at.
     * @return the expiration time
     */
    public Date getExpiresTime();

    /**
     * Sets the time the action expires at.
     * @param expiresTime the expiration time
     */
    public void setExpiresTime(Date expiresTime);

    /**
     * Gets the unique string which will be passed to the OG Action owner's website
     * when a user clicks through this action on Facebook.
     * @return the ref string
     */
    public String getRef();

    /**
     * Sets the unique string which will be passed to the OG Action owner's website
     * when a user clicks through this action on Facebook.
     * @param ref the ref string
     */
    public void setRef(String ref);

    /**
     * Gets the message assoicated with the action.
     * @return the message
     */
    public String getMessage();

    /**
     * Sets the message associated with the action.
     * @param message the message
     */
    public void setMessage(String message);

    /**
     * Gets the place where the action took place.
     * @return the place
     */
    public GraphPlace getPlace();

    /**
     * Sets the place where the action took place.
     * @param place the place
     */
    public void setPlace(GraphPlace place);

    /**
     * Gets the list of profiles that were tagged in the action.
     * @return the profiles that were tagged in the action
     */
    public List<GraphObject> getTags();

    /**
     * Sets the list of profiles that were tagged in the action.
     * @param tags the profiles that were tagged in the action
     */
    public void setTags(List<? extends GraphObject> tags);

    /**
     * Gets the images that were associated with the action.
     * @return the images
     */
    public List<JSONObject> getImage();

    /**
     * Sets the images that were associated with the action.
     * @param image the images
     */
    public void setImage(List<JSONObject> image);

    /**
     * Gets the from-user associated with the action.
     * @return the user
     */
    public GraphUser getFrom();

    /**
     * Sets the from-user associated with the action.
     * @param from the from-user
     */
    public void setFrom(GraphUser from);

    /**
     * Gets the 'likes' that have been performed on this action.
     * @return the likes
     */
    public JSONObject getLikes();

    /**
     * Sets the 'likes' that have been performed on this action.
     * @param likes the likes
     */
    public void setLikes(JSONObject likes);

    /**
     * Gets the application that created this action.
     * @return the application
     */
    public GraphObject getApplication();

    /**
     * Sets the application that created this action.
     * @param application the application
     */
    public void setApplication(GraphObject application);

    /**
     * Gets the comments that have been made on this action.
     * @return the comments
     */
    public JSONObject getComments();

    /**
     * Sets the comments that have been made on this action.
     * @param comments the comments
     */
    public void setComments(JSONObject comments);
}
