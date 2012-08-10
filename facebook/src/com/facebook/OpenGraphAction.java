/**
 * Copyright 2010 Facebook, Inc.
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

package com.facebook;

import java.util.List;

import org.json.JSONObject;

// ISSUE: What are Java guidelines around snapshots vs. live collections?
//        We have several List<T> below that might need to be array or
//        something else.
// ISSUE: References to other strongly typed GraphObjects that don't exist
//        need to be strongly typed as GraphObject now.
// ISSUE: Do we consider additions to this interface to be a breaking change?
//        We may need to doc something that tells people to only use these
//        with GraphObjectWrapper.
// TODO docs
public interface OpenGraphAction extends GraphObject {
    public String getId();
    public void setId(String id);

    public String getStartTime();
    public void setStartTime(String startTime);

    public String getEndTime();
    public void setEndTime(String endTime);

    public String getPublishTime();
    public void setPublishTime(String publishTime);

    public String getCreatedTime();
    public void setCreatedTime(String createdTime);

    public String getExpiresTime();
    public void setExpiresTime(String expiresTime);

    public String getRef();
    public void setRef(String ref);

    public String getUserMessage();
    public void setUserMessage(String userMessage);

    public GraphPlace getPlace();
    public void setPlace(GraphPlace place);

    public List<GraphObject> getTags();
    public void setTags(List<GraphObject> tags);

    public List<JSONObject> getImage();
    public void setImage(List<JSONObject> image);

    public GraphUser getFrom();
    public void setFrom(GraphUser from);

    public JSONObject getLikes();
    public void setLikes(JSONObject likes);

    public GraphObject getApplication();
    public void setApplication(GraphObject application);

    public JSONObject getComments();
    public void setComments(JSONObject comments);
}
