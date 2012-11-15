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

package com.facebook.scrumptious;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.facebook.*;
import com.facebook.model.*;
import com.facebook.widget.ProfilePictureView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment that represents the main selection screen for Scrumptious.
 */
public class SelectionFragment extends Fragment {

    private static final String TAG = "SelectionFragment";
    private static final String POST_ACTION_PATH = "me/fb_sample_scrumps:eat";
    private static final String PENDING_ANNOUNCE_KEY = "pendingAnnounce";

    private static final int REAUTH_ACTIVITY_CODE = 100;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private Button announceButton;
    private ListView listView;
    private ProgressDialog progressDialog;
    private List<BaseListElement> listElements;
    private ProfilePictureView profilePictureView;
    private TextView userNameView;
    private boolean pendingAnnounce;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.selection, container, false);

        profilePictureView = (ProfilePictureView) view.findViewById(R.id.selection_profile_pic);
        profilePictureView.setCropped(true);
        userNameView = (TextView) view.findViewById(R.id.selection_user_name);
        announceButton = (Button) view.findViewById(R.id.announce_button);
        listView = (ListView) view.findViewById(R.id.selection_list);

        announceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleAnnounce();
            }
        });
        init(savedInstanceState);

        final Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {
                    if (session == Session.getActiveSession()) {
                        if (user != null) {
                            profilePictureView.setProfileId(user.getId());
                            userNameView.setText(user.getName());
                        }
                    }
                }
            });
            Request.executeBatchAsync(request);
        }
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REAUTH_ACTIVITY_CODE) {
            Session.getActiveSession().onActivityResult(getActivity(), requestCode, resultCode, data);
        } else if (resultCode == Activity.RESULT_OK && requestCode >= 0 && requestCode < listElements.size()) {
            listElements.get(requestCode).onActivityResult(data);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        for (BaseListElement listElement : listElements) {
            listElement.onSaveInstanceState(bundle);
        }
        bundle.putBoolean(PENDING_ANNOUNCE_KEY, pendingAnnounce);
    }

    /**
     * Notifies that the session token has been updated.
     */
    public void tokenUpdated() {
        if (pendingAnnounce) {
            handleAnnounce();
        }
    }

    /**
     * Resets the view to the initial defaults.
     */
    private void init(Bundle savedInstanceState) {
        announceButton.setEnabled(false);

        listElements = new ArrayList<BaseListElement>();

        listElements.add(new EatListElement(0));
        listElements.add(new LocationListElement(1));
        listElements.add(new PeopleListElement(2));

        if (savedInstanceState != null) {
            for (BaseListElement listElement : listElements) {
                listElement.restoreState(savedInstanceState);
            }
            pendingAnnounce = savedInstanceState.getBoolean(PENDING_ANNOUNCE_KEY, false);
        }

        listView.setAdapter(new ActionListAdapter(getActivity(), R.id.selection_list, listElements));
    }

    private void handleAnnounce() {
        pendingAnnounce = false;
        Session session = Session.getActiveSession();

        if (session == null || !session.isOpened()) {
            return;
        }

        List<String> permissions = session.getPermissions();
        if (!permissions.containsAll(PERMISSIONS)) {
            pendingAnnounce = true;
            Session.ReauthorizeRequest reauthRequest = new Session.ReauthorizeRequest(this, PERMISSIONS).
                    setRequestCode(REAUTH_ACTIVITY_CODE);
            session.reauthorizeForPublish(reauthRequest);
            return;
        }

        // Show a progress dialog because sometimes the requests can take a while.
        progressDialog = ProgressDialog.show(getActivity(), "",
                getActivity().getResources().getString(R.string.progress_dialog_text), true);

        // Run this in a background thread since some of the populate methods may take
        // a non-trivial amount of time.
        AsyncTask<Void, Void, Response> task = new AsyncTask<Void, Void, Response>() {

            @Override
            protected Response doInBackground(Void... voids) {
                EatAction eatAction = GraphObject.Factory.create(EatAction.class);
                for (BaseListElement element : listElements) {
                    element.populateOGAction(eatAction);
                }
                Request request = new Request(Session.getActiveSession(),
                        POST_ACTION_PATH, null, HttpMethod.POST);
                request.setGraphObject(eatAction);
                return request.executeAndWait();
            }

            @Override
            protected void onPostExecute(Response response) {
                onPostActionResponse(response);
             }
        };

        task.execute();
    }

    private void onPostActionResponse(Response response) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        if (getActivity() == null) {
            // if the user removes the app from the website, then a request will
            // have caused the session to close (since the token is no longer valid),
            // which means the splash fragment will be shown rather than this one,
            // causing activity to be null. If the activity is null, then we cannot
            // show any dialogs, so we return.
            return;
        }

        PostResponse postResponse = response.getGraphObjectAs(PostResponse.class);
        String title = null;
        String buttonText = null;
        String dialogBody = null;

        if (postResponse != null && postResponse.getId() != null) {
            title = getString(R.string.result_dialog_title);
            buttonText = getString(R.string.result_dialog_button_text);
            dialogBody = String.format(getString(R.string.result_dialog_text), postResponse.getId());
        } else {
            title = getString(R.string.error_dialog_title);
            buttonText = getString(R.string.error_dialog_button_text);
            FacebookRequestError requestError = response.getError();
            if (requestError != null) {
                dialogBody = requestError.getErrorMessage();
            }
            if (dialogBody == null) {
                dialogBody = getString(R.string.error_dialog_default_text);
            }
        }

        if (dialogBody != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setPositiveButton(buttonText, null)
                   .setTitle(title)
                   .setMessage(dialogBody)
                   .show();
        }
        init(null);
    }

    private void startPickerActivity(Uri data, int requestCode) {
        Intent intent = new Intent();
        intent.setData(data);
        intent.setClass(getActivity(), PickerActivity.class);
        startActivityForResult(intent, requestCode);
    }

    /**
     * Interface representing the Meal Open Graph object.
     */
    private interface MealGraphObject extends GraphObject {
        public String getUrl();
        public void setUrl(String url);

        public String getId();
        public void setId(String id);
    }

    /**
     * Interface representing the Eat action.
     */
    private interface EatAction extends OpenGraphAction {
        public MealGraphObject getMeal();
        public void setMeal(MealGraphObject meal);
    }

    /**
     * Used to inspect the response from posting an action
     */
    private interface PostResponse extends GraphObject {
        String getId();
    }

    private class EatListElement extends BaseListElement {

        private static final String FOOD_KEY = "food";
        private static final String FOOD_URL_KEY = "food_url";

        private final String[] foodChoices;
        private final String[] foodUrls;
        private String foodChoiceUrl = null;
        private String foodChoice = null;

        public EatListElement(int requestCode) {
            super(getActivity().getResources().getDrawable(R.drawable.action_eating),
                  getActivity().getResources().getString(R.string.action_eating),
                  getActivity().getResources().getString(R.string.action_eating_default),
                  requestCode);
            foodChoices = getActivity().getResources().getStringArray(R.array.food_types);
            foodUrls = getActivity().getResources().getStringArray(R.array.food_og_urls);
        }

        @Override
        protected View.OnClickListener getOnClickListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMealOptions();
                }
            };
        }

        @Override
        protected void populateOGAction(OpenGraphAction action) {
            if (foodChoiceUrl != null) {
                EatAction eatAction = action.cast(EatAction.class);
                MealGraphObject meal = GraphObject.Factory.create(MealGraphObject.class);
                meal.setUrl(foodChoiceUrl);
                eatAction.setMeal(meal);
            }
        }

        @Override
        protected void onSaveInstanceState(Bundle bundle) {
            if (foodChoice != null && foodChoiceUrl != null) {
                bundle.putString(FOOD_KEY, foodChoice);
                bundle.putString(FOOD_URL_KEY, foodChoiceUrl);
            }
        }

        @Override
        protected boolean restoreState(Bundle savedState) {
            String food = savedState.getString(FOOD_KEY);
            String foodUrl = savedState.getString(FOOD_URL_KEY);
            if (food != null && foodUrl != null) {
                foodChoice = food;
                foodChoiceUrl = foodUrl;
                setFoodText();
                return true;
            }
            return false;
        }

        private void showMealOptions() {
            String title = getActivity().getResources().getString(R.string.select_meal);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(title).
                    setCancelable(true).
                    setItems(foodChoices, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            foodChoice = foodChoices[i];
                            foodChoiceUrl = foodUrls[i];
                            setFoodText();
                            notifyDataChanged();
                        }
                    });
            builder.show();
        }

        private void setFoodText() {
            if (foodChoice != null && foodChoiceUrl != null) {
                setText2(foodChoice);
                announceButton.setEnabled(true);
            } else {
                setText2(getActivity().getResources().getString(R.string.action_eating_default));
                announceButton.setEnabled(false);
            }
        }
    }

    private class PeopleListElement extends BaseListElement {

        private static final String FRIENDS_KEY = "friends";

        private List<GraphUser> selectedUsers;

        public PeopleListElement(int requestCode) {
            super(getActivity().getResources().getDrawable(R.drawable.action_people),
                  getActivity().getResources().getString(R.string.action_people),
                  getActivity().getResources().getString(R.string.action_people_default),
                  requestCode);
        }

        @Override
        protected View.OnClickListener getOnClickListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startPickerActivity(PickerActivity.FRIEND_PICKER, getRequestCode());
                }
            };
        }

        @Override
        protected void onActivityResult(Intent data) {
            selectedUsers = ((ScrumptiousApplication) getActivity().getApplication()).getSelectedUsers();
            setUsersText();
            notifyDataChanged();
        }

        @Override
        protected void populateOGAction(OpenGraphAction action) {
            if (selectedUsers != null) {
                action.setTags(selectedUsers);
            }
        }

        @Override
        protected void onSaveInstanceState(Bundle bundle) {
            if (selectedUsers != null) {
                bundle.putByteArray(FRIENDS_KEY, getByteArray(selectedUsers));
            }
        }

        @Override
        protected boolean restoreState(Bundle savedState) {
            byte[] bytes = savedState.getByteArray(FRIENDS_KEY);
            if (bytes != null) {
                selectedUsers = restoreByteArray(bytes);
                setUsersText();
                return true;
            }
            return false;
        }

        private void setUsersText() {
            String text = null;
            if (selectedUsers != null) {
                if (selectedUsers.size() == 1) {
                    text = String.format(getResources().getString(R.string.single_user_selected),
                            selectedUsers.get(0).getName());
                } else if (selectedUsers.size() == 2) {
                    text = String.format(getResources().getString(R.string.two_users_selected),
                            selectedUsers.get(0).getName(), selectedUsers.get(1).getName());
                } else if (selectedUsers.size() > 2) {
                    text = String.format(getResources().getString(R.string.multiple_users_selected),
                            selectedUsers.get(0).getName(), (selectedUsers.size() - 1));
                }
            }
            if (text == null) {
                text = getResources().getString(R.string.action_people_default);
            }
            setText2(text);
        }

        private byte[] getByteArray(List<GraphUser> users) {
            // convert the list of GraphUsers to a list of String where each element is
            // the JSON representation of the GraphUser so it can be stored in a Bundle
            List<String> usersAsString = new ArrayList<String>(users.size());

            for (GraphUser user : users) {
                usersAsString.add(user.getInnerJSONObject().toString());
            }
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                new ObjectOutputStream(outputStream).writeObject(usersAsString);
                return outputStream.toByteArray();
            } catch (IOException e) {
                Log.e(TAG, "Unable to serialize users.", e);
            }
            return null;
        }

        private List<GraphUser> restoreByteArray(byte[] bytes) {
            try {
                @SuppressWarnings("unchecked")
                List<String> usersAsString =
                        (List<String>) (new ObjectInputStream(new ByteArrayInputStream(bytes))).readObject();
                if (usersAsString != null) {
                    List<GraphUser> users = new ArrayList<GraphUser>(usersAsString.size());
                    for (String user : usersAsString) {
                        GraphUser graphUser = GraphObject.Factory
                                .create(new JSONObject(user), GraphUser.class);
                        users.add(graphUser);
                    }
                    return users;
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Unable to deserialize users.", e);
            } catch (IOException e) {
                Log.e(TAG, "Unable to deserialize users.", e);
            } catch (JSONException e) {
                Log.e(TAG, "Unable to deserialize users.", e);
            }
            return null;
        }
    }

    private class LocationListElement extends BaseListElement {

        private static final String PLACE_KEY = "place";

        private GraphPlace selectedPlace = null;

        public LocationListElement(int requestCode) {
            super(getActivity().getResources().getDrawable(R.drawable.action_location),
                  getActivity().getResources().getString(R.string.action_location),
                  getActivity().getResources().getString(R.string.action_location_default),
                  requestCode);
        }

        @Override
        protected View.OnClickListener getOnClickListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startPickerActivity(PickerActivity.PLACE_PICKER, getRequestCode());
                }
            };
        }

        @Override
        protected void onActivityResult(Intent data) {
            selectedPlace = ((ScrumptiousApplication) getActivity().getApplication()).getSelectedPlace();
            setPlaceText();
            notifyDataChanged();
        }

        @Override
        protected void populateOGAction(OpenGraphAction action) {
            if (selectedPlace != null) {
                action.setPlace(selectedPlace);
            }
        }

        @Override
        protected void onSaveInstanceState(Bundle bundle) {
            if (selectedPlace != null) {
                bundle.putString(PLACE_KEY, selectedPlace.getInnerJSONObject().toString());
            }
        }

        @Override
        protected boolean restoreState(Bundle savedState) {
            String place = savedState.getString(PLACE_KEY);
            if (place != null) {
                try {
                    selectedPlace = GraphObject.Factory
                            .create(new JSONObject(place), GraphPlace.class);
                    setPlaceText();
                    return true;
                } catch (JSONException e) {
                    Log.e(TAG, "Unable to deserialize place.", e);
                }
            }
            return false;
        }

        private void setPlaceText() {
            String text = null;
            if (selectedPlace != null) {
                text = selectedPlace.getName();
            }
            if (text == null) {
                text = getResources().getString(R.string.action_location_default);
            }
            setText2(text);
        }

    }

    private class ActionListAdapter extends ArrayAdapter<BaseListElement> {
        private List<BaseListElement> listElements;

        public ActionListAdapter(Context context, int resourceId, List<BaseListElement> listElements) {
            super(context, resourceId, listElements);
            this.listElements = listElements;
            for (int i = 0; i < listElements.size(); i++) {
                listElements.get(i).setAdapter(this);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater =
                        (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.listitem, null);
            }

            BaseListElement listElement = listElements.get(position);
            if (listElement != null) {
                view.setOnClickListener(listElement.getOnClickListener());
                ImageView icon = (ImageView) view.findViewById(R.id.icon);
                TextView text1 = (TextView) view.findViewById(R.id.text1);
                TextView text2 = (TextView) view.findViewById(R.id.text2);
                if (icon != null) {
                    icon.setImageDrawable(listElement.getIcon());
                }
                if (text1 != null) {
                    text1.setText(listElement.getText1());
                }
                if (text2 != null) {
                    text2.setText(listElement.getText2());
                }
            }
            return view;
        }

    }
}
