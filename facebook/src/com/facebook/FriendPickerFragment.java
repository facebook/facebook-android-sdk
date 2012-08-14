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

package com.facebook;

import android.os.Bundle;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FriendPickerFragment extends GraphObjectListFragment<GraphUser> {
    private static final String CACHE_IDENTITY = "FriendPickerFragment";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String PICTURE = "picture";

    private String userId;

    public FriendPickerFragment() {
        super(CACHE_IDENTITY, GraphUser.class);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    protected Request getRequestForLoadData() {
        String[] sortFields = new String[]{NAME};
        String groupByField = NAME;

        workerFragment.getAdapter().setSortFields(Arrays.asList(sortFields));
        workerFragment.getAdapter().setGroupByField(NAME);

        String userToFetch = (userId != null) ? userId : "me";
        return createRequest(userToFetch, sortFields, groupByField, session);
    }

    private static Request createRequest(String userID, String[] sortFields, String groupByField, Session session) {
        Request request = Request.newGraphPathRequest(session, userID + "/friends", null);

        Set<String> fields = new HashSet<String>();
        // TODO get user requested fields
        String[] requiredFields = new String[]{
                ID,
                NAME,
                PICTURE
        };
        fields.addAll(Arrays.asList(requiredFields));
        fields.addAll(Arrays.asList(sortFields));
        fields.add(groupByField);

        Bundle parameters = new Bundle();
        parameters.putString("fields", TextUtils.join(",", fields));
        request.setParameters(parameters);

        return request;
    }
}
