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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.*;
import com.facebook.android.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;

class GraphObjectAdapter<T extends GraphObject> extends BaseAdapter implements SectionIndexer {
    private final int DISPLAY_SECTIONS_THRESHOLD = 1;
    private final int HEADER_VIEW_TYPE = 0;
    private final int GRAPH_OBJECT_VIEW_TYPE = 1;
    private final String ID = "id";
    private final String NAME = "name";
    private final String PICTURE = "picture";
    private final String DATA = "data";
    private final String URL = "url";

    private final LayoutInflater inflater;
    private List<String> sectionKeys = new ArrayList<String>();
    private Map<String, ArrayList<T>> graphObjectsBySection = new HashMap<String, ArrayList<T>>();
    private Map<String, T> allGraphObjects = new HashMap<String, T>();
    private boolean displaySections;
    private Set<String> selectedGraphObjectIds = new HashSet<String>();
    private List<String> sortFields;
    private String groupByField;
    private boolean showProfilePicture;

    // TODO temporary cache
    private Map<URL, Bitmap> imageCache = new HashMap<java.net.URL, Bitmap>();

    public GraphObjectAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public List<String> getSortFields() {
        return sortFields;
    }

    public void setSortFields(List<String> sortFields) {
        this.sortFields = sortFields;
    }

    public String getGroupByField() {
        return groupByField;
    }

    public void setGroupByField(String groupByField) {
        this.groupByField = groupByField;
    }

    public boolean getShowProfilePicture() {
        return showProfilePicture;
    }

    public void setShowProfilePicture(boolean showProfilePicture) {
        this.showProfilePicture = showProfilePicture;
    }

    public void add(Collection<T> graphObjects) {
        if (graphObjects.size() == 0) {
            return;
        }

        for (T graphObject : graphObjects) {
            allGraphObjects.put(getIdOfGraphObject(graphObject), graphObject);
        }
        rebuildSections();

        notifyDataSetChanged();
    }

    public void clear() {
        allGraphObjects = new HashMap<String, T>();
        selectedGraphObjectIds = new HashSet<String>();
        rebuildSections();

        notifyDataSetChanged();
    }

    protected String getSectionKeyOfGraphObject(T graphObject) {
        String result = null;

        if (groupByField != null) {
            result = (String) graphObject.get(groupByField);
            if (result != null && result.length() > 0) {
                result = result.substring(0, 1).toUpperCase();
            }
        }

        return (result != null) ? result : "";
    }

    protected boolean filterIncludesItem(T graphObject) {
        // TODO implement searching/filtering logic
        return true;
    }

    protected CharSequence getTitleOfGraphObject(T graphObject) {
        return (String) graphObject.get(NAME);
    }

    protected String getIdOfGraphObject(T graphObject) {
        if (graphObject.containsKey(ID)) {
            Object obj = graphObject.get(ID);
            if (obj instanceof String) {
                return (String) obj;
            }
        }
        throw new FacebookException("Received an object without an ID.");
    }

    protected URL getPictureUrlOfGraphObject(T graphObject) {
        String url = null;
        Object o = graphObject.get(PICTURE);
        if (o instanceof String) {
            url = (String) o;
        } else {
            // TODO this
        }

        if (url != null) {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
            }
        }
        return null;
    }

    protected View getSectionHeaderView(String sectionHeader, View convertView, ViewGroup parent) {
        TextView result = (TextView) convertView;

        if (result == null) {
            result = (TextView) inflater.inflate(R.layout.list_section_header, null);
        }

        result.setText(sectionHeader);

        return result;
    }

    protected View getGraphObjectView(T graphObject, View convertView, ViewGroup parent) {
        View result = (View) convertView;

        if (result == null) {
            result = createGraphObjectView(convertView, parent);
        }

        populateGraphObjectView(result, graphObject);
        return result;
    }

    protected boolean showCheckbox() {
        return true;
    }

    protected View createGraphObjectView(View convertView, ViewGroup parent) {
        View result = inflater.inflate(R.layout.profile_picker_list_row, null);

        ViewStub checkboxStub = (ViewStub) result.findViewById(R.id.checkbox_stub);
        if (!showCheckbox()) {
            checkboxStub.setVisibility(View.GONE);
        } else {
            CheckBox checkBox = (CheckBox) checkboxStub.inflate();
            checkboxStub.setVisibility(View.VISIBLE);
        }

        ViewStub profilePicStub = (ViewStub) result.findViewById(R.id.profile_pic_stub);
        if (!getShowProfilePicture()) {
            profilePicStub.setVisibility(View.GONE);
        } else {
            ImageView imageView = (ImageView) profilePicStub.inflate();
            imageView.setVisibility(View.VISIBLE);
        }

        return result;
    }

    protected void populateGraphObjectView(View view, T graphObject) {
        String id = getIdOfGraphObject(graphObject);
        view.setTag(id);

        CharSequence title = getTitleOfGraphObject(graphObject);
        TextView nameView = (TextView) view.findViewById(R.id.profile_name);
        nameView.setText(title, TextView.BufferType.SPANNABLE);

        if (showCheckbox()) {
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.picker_checkbox);
            checkBox.setChecked(selectedGraphObjectIds.contains(id));
        }

        if (getShowProfilePicture()) {
            URL pictureURL = getPictureUrlOfGraphObject(graphObject);
            ImageView profilePic = (ImageView) view.findViewById(R.id.profile_image);
            // Do we already have the right picture? If so, leave it alone.
            if (!pictureURL.equals(profilePic.getTag())) {
                if (imageCache.containsKey(pictureURL)) {
                    profilePic.setImageBitmap(imageCache.get(pictureURL));
                    profilePic.setTag(pictureURL);
                } else {
                    // Track the ID we're fetching a picture for.
                    profilePic.setTag(id);
                    profilePic.setImageResource(R.drawable.no_profile_pic);

                    if (pictureURL != null) {
                        try {
                            ProfilePictureDownloadTask task = new ProfilePictureDownloadTask();
                            task.execute(new ProfilePictureRequest(id, pictureURL, profilePic));
                        } catch (RejectedExecutionException exception) {
                            // TODO retry?
                        }
                    }
                }
            }
        }
    }

    private void rebuildSections() {
        sectionKeys = new ArrayList<String>();
        graphObjectsBySection = new HashMap<String, ArrayList<T>>();

        int objectsAdded = 0;
        for (T graphObject : allGraphObjects.values()) {
            if (!filterIncludesItem(graphObject)) {
                continue;
            }

            objectsAdded++;

            String sectionKeyOfItem = getSectionKeyOfGraphObject(graphObject);
            if (!graphObjectsBySection.containsKey(sectionKeyOfItem)) {
                sectionKeys.add(sectionKeyOfItem);
                graphObjectsBySection.put(sectionKeyOfItem, new ArrayList<T>());
            }
            List<T> section = graphObjectsBySection.get(sectionKeyOfItem);
            section.add(graphObject);
        }

        if (sortFields != null) {
            final Collator collator = Collator.getInstance();
            for (List<T> section : graphObjectsBySection.values()) {
                Collections.sort(section, new Comparator<GraphObject>() {
                    // TODO pull this out into Utility method?
                    @Override
                    public int compare(GraphObject a, GraphObject b) {
                        return Utility.compareGraphObjects(a, b, sortFields, collator);
                    }
                });
            }
        }

        Collections.sort(sectionKeys, Collator.getInstance());

        displaySections = sectionKeys.size() > 1 && objectsAdded > DISPLAY_SECTIONS_THRESHOLD;
    }

    Pair<String, T> getSectionAndItem(int position) {
        if (sectionKeys.size() == 0) {
            return null;
        }
        String sectionKey = null;
        T graphObject = null;

        if (!displaySections) {
            sectionKey = sectionKeys.get(0);
            graphObject = graphObjectsBySection.get(sectionKey).get(position);
        } else {
            // Count through the sections; the "0" position in each section is the header. We decrement
            // position each time we skip forward a certain number of elements, including the header.
            for (String key : sectionKeys) {
                // Decrement if we skip over the header
                if (position-- == 0) {
                    sectionKey = key;
                    break;
                }

                List<T> section = graphObjectsBySection.get(key);
                if (position < section.size()) {
                    // The position is somewhere in this section. Get the corresponding graph object.
                    sectionKey = key;
                    graphObject = section.get(position);
                    break;
                }
                // Decrement by as many items as we skipped over
                position -= section.size();
            }
        }
        if (sectionKey != null) {
            // Note: graphObject will be null if this represents a section header.
            return new Pair<String, T>(sectionKey, graphObject);
        } else {
            return null;
        }
    }

    int getPosition(String sectionKey, T graphObject) {
        int position = 0;
        boolean found = false;

        // First find the section key and increment position one for each header we will render;
        // increment by the size of each section prior to the one we want.
        for (String key : sectionKeys) {
            if (displaySections) {
                position++;
            }
            if (key.equals(sectionKey)) {
                found = true;
                break;
            } else {
                position += graphObjectsBySection.get(key).size();
            }
        }

        if (!found) {
            return -1;
        } else if (graphObject == null) {
            // null represents the header for a section; we counted this header in position earlier,
            // so subtract it back out.
            return position - (displaySections ? 1 : 0);
        }

        // Now find index of this item within that section.
        for (T t : graphObjectsBySection.get(sectionKey)) {
            if (GraphObjectWrapper.hasSameId(t, graphObject)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    @Override
    public int getCount() {
        if (sectionKeys.size() == 0) {
            return 0;
        }

        // If we are not displaying sections, we don't display a header; otherwise, we have one header per item in
        // addition to the actual items.
        int count = (displaySections) ? sectionKeys.size() : 0;
        for (List<T> section : graphObjectsBySection.values()) {
            count += section.size();
        }
        return count;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return displaySections;
    }

    @Override
    public boolean isEnabled(int position) {
        Pair<String, T> pair = getSectionAndItem(position);
        return pair.second != null;
    }

    @Override
    public Object getItem(int position) {
        Pair<String, T> pair = getSectionAndItem(position);
        return (pair != null) ? pair.second : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        Pair<String, T> pair = getSectionAndItem(position);
        return (pair.second != null) ? GRAPH_OBJECT_VIEW_TYPE : HEADER_VIEW_TYPE;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Pair<String, T> pair = getSectionAndItem(position);

        if (pair.second == null) {
            return getSectionHeaderView(pair.first, convertView, parent);
        } else {
            return getGraphObjectView(pair.second, convertView, parent);
        }
    }

    @Override
    public Object[] getSections() {
        if (displaySections) {
            return sectionKeys.toArray();
        } else {
            return new Object[0];
        }
    }

    @Override
    public int getPositionForSection(int section) {
        if (displaySections) {
            section = Math.max(0, Math.min(section, sectionKeys.size() - 1));
            if (section < sectionKeys.size()) {
                return getPosition(sectionKeys.get(section), null);
            }
        }
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        Pair<String, T> pair = getSectionAndItem(position);
        if (pair != null) {
            return Math.max(0, Math.min(sectionKeys.indexOf(pair.first), sectionKeys.size() - 1));
        }
        return 0;
    }


    public void toggleSelection(T graphObject, View view) {
        String id = getIdOfGraphObject(graphObject);

        // TODO selection modes
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.picker_checkbox);
        if (selectedGraphObjectIds.contains(id)) {
            // Un-select the graph object
            selectedGraphObjectIds.remove(id);
            checkBox.setChecked(false);
        } else {
            // Select the graph object
            selectedGraphObjectIds.add(id);
            checkBox.setChecked(true);
        }
    }

    public Set<T> getSelectedGraphObjects() {
        Set<T> result = new HashSet<T>(selectedGraphObjectIds.size());
        for (String id : selectedGraphObjectIds) {
            T graphObject = allGraphObjects.get(id);
            if (graphObject != null) {
                result.add(graphObject);
            }
        }
        return result;
    }

    private class ProfilePictureRequest {
        public final String graphObjectId;
        public final URL profilePictureUrl;
        public final ImageView profilePictureView;

        public ProfilePictureRequest(String graphObjectId, URL profilePictureUrl, ImageView profilePictureView) {
            this.graphObjectId = graphObjectId;
            this.profilePictureUrl = profilePictureUrl;
            this.profilePictureView = profilePictureView;
        }
    }

    private class ProfilePictureResult {
        public final ProfilePictureRequest request;
        public final Bitmap picture;

        private ProfilePictureResult(ProfilePictureRequest request, Bitmap picture) {
            this.request = request;
            this.picture = picture;
        }
    }

    private class ProfilePictureDownloadTask extends AsyncTask<ProfilePictureRequest, Void, List<ProfilePictureResult>> {
        @Override
        protected List<ProfilePictureResult> doInBackground(ProfilePictureRequest... params) {
            List<ProfilePictureResult> results = new ArrayList<ProfilePictureResult>();
            for (ProfilePictureRequest request : params) {
                try {
                    URLConnection connection = request.profilePictureUrl.openConnection();
                    InputStream stream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    results.add(new ProfilePictureResult(request, bitmap));
                    // TODO cache
                } catch (IOException e) {
                }
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<ProfilePictureResult> profilePictureResults) {
            super.onPostExecute(profilePictureResults);

            for (ProfilePictureResult result : profilePictureResults) {
                imageCache.put(result.request.profilePictureUrl, result.picture);

                Object tag = result.request.profilePictureView.getTag();
                if (result.request.graphObjectId.equals(tag)) {
                    result.request.profilePictureView.setImageBitmap(result.picture);
                    result.request.profilePictureView.setTag(result.request.profilePictureUrl);
                }
            }
        }
    }
}
