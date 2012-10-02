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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.facebook.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Fragment that represents the main selection screen for Scrumptious.
 */
public class SelectionFragment extends Fragment {

    private static final String POST_ACTION_PATH = "me/fb_sample_scrumps:eat";

    private static final int REAUTH_ACTIVITY_CODE = 100;

    // Suppressing this warning since ArrayList is serializable and this syntax below
    // is essentially treated like an anonymous class definition that is missing the
    // serialVersionUID field.
    @SuppressWarnings("serial")
    private static final List<String> PERMISSIONS = new ArrayList<String>() {{
        add("publish_actions");
    }};

    private Button announceButton;
    private ListView listView;
    private ProgressDialog progressDialog;
    private List<BaseListElement> listElements;
    private ProfilePictureView profilePictureView;
    private TextView userNameView;

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
        reset();

        final Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            Request request = Request.newMeRequest(session, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    if (session == Session.getActiveSession()) {
                        GraphUser user = response.getGraphObjectAs(GraphUser.class);
                        if (user != null) {
                            profilePictureView.setUserId(user.getId());
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
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REAUTH_ACTIVITY_CODE) {
                Session.getActiveSession().onActivityResult(getActivity(), requestCode, resultCode, data);
            } else if (requestCode >= 0 && requestCode < listElements.size()) {
                listElements.get(requestCode).onActivityResult(data);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    /**
     * Notifies that the session token has been updated.
     */
    public void tokenUpdated() {
        handleAnnounce();
    }

    /**
     * Resets the view to the initial defaults.
     */
    private void reset() {
        announceButton.setEnabled(false);

        listElements = new ArrayList<BaseListElement>();

        listElements.add(new EatListElement(0));
        listElements.add(new LocationListElement(1));
        listElements.add(new PeopleListElement(2));

        listView.setAdapter(new ActionListAdapter(getActivity(), R.id.selection_list, listElements));
    }

    private void handleAnnounce() {
        Session session = Session.getActiveSession();

        if (session == null || !session.isOpened()) {
            return;
        }

        List<String> permissions = session.getPermissions();
        if (!isSubsetOf(PERMISSIONS, permissions)) {
            Session.ReauthorizeRequest reauthRequest = new Session.ReauthorizeRequest(this, PERMISSIONS).
                    setRequestCode(REAUTH_ACTIVITY_CODE);
            session.reauthorizeForPublish(reauthRequest);
            return;
        }

        // Show a progress dialog because sometimes the requests can take a while.
        progressDialog = ProgressDialog.show(getActivity(), "",
                getActivity().getResources().getString(R.string.progress_dialog_text), true);

        // Run this in a background thread since if the user sets an image, we need to upload the
        // image first, and we don't want to block the main thread.
        AsyncTask<Void, Void, Response> task = new AsyncTask<Void, Void, Response>() {

            @Override
            protected Response doInBackground(Void... voids) {
                EatAction eatAction = GraphObjectWrapper.createGraphObject(EatAction.class);
                for (BaseListElement element : listElements) {
                    element.populateOGAction(eatAction);
                }
                Request request = new Request(Session.getActiveSession(),
                        POST_ACTION_PATH, null, Request.POST_METHOD);
                request.setGraphObject(eatAction);
                return request.execute();
            }

            @Override
            protected void onPostExecute(Response response) {
                onPostActionResponse(response);
             }
        };

        task.execute();
    }

    private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
        for (String string : subset) {
            if (!superset.contains(string)) {
                return false;
            }
        }
        return true;
    }

    private void onPostActionResponse(Response response) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        String id = getIdFromResponseOrShowError(response);
        if (id != null) {
            String dialogBody = String.format(getActivity().getResources().getString(R.string.result_dialog_text), id);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setPositiveButton(R.string.result_dialog_button_text, null).
                    setTitle(R.string.result_dialog_title).setMessage(dialogBody);
            builder.show();
        }
        reset();
    }

    private String getIdFromResponseOrShowError(Response response) {
        PostResponse postResponse = response.getGraphObjectAs(PostResponse.class);

        String id = null;
        PostResponse.Body body = null;
        if (postResponse != null) {
            id = postResponse.getId();
            body = postResponse.getBody();
        }

        String dialogBody = "";

        if (body != null && body.getError() != null) {
            dialogBody = body.getError().getMessage();
        } else if (response.getError() != null) {
            dialogBody = response.getError().getLocalizedMessage();
        } else if (id != null) {
            return id;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(R.string.error_dialog_button_text, null).
                setTitle(R.string.error_dialog_title).setMessage(dialogBody);
        builder.show();
        return null;
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
        Body getBody();

        String getId();

        interface Body extends GraphObject {
            Error getError();
        }

        interface Error extends GraphObject {
            String getMessage();
        }
    }

    private class EatListElement extends BaseListElement {

        private final String[] foodChoices;
        private final String[] foodUrls;
        private String foodChoiceUrl = null;

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
                MealGraphObject meal = GraphObjectWrapper.createGraphObject(MealGraphObject.class);
                meal.setUrl(foodChoiceUrl);
                eatAction.setMeal(meal);
            }
        }

        private void showMealOptions() {
            String title = getActivity().getResources().getString(R.string.select_meal);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(title).
                    setCancelable(true).
                    setItems(foodChoices, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String foodChoice = foodChoices[i];
                            EatListElement.this.setText2(foodChoice);
                            EatListElement.this.notifyDataChanged();
                            foodChoiceUrl = foodUrls[i];
                            announceButton.setEnabled(true);
                        }
                    });
            builder.show();

        }
    }

    private class PeopleListElement extends BaseListElement {

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
            this.notifyDataChanged();
        }

        @Override
        protected void populateOGAction(OpenGraphAction action) {
            if (selectedUsers != null) {
                action.setTags(selectedUsers);
            }
        }


    }

    private class LocationListElement extends BaseListElement {

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
            String text = null;
            if (selectedPlace != null) {
                text = selectedPlace.getName();
            }
            if (text == null) {
                text = getResources().getString(R.string.action_location_default);
            }
            setText2(text);
            this.notifyDataChanged();
        }

        @Override
        protected void populateOGAction(OpenGraphAction action) {
            if (selectedPlace != null) {
                action.setPlace(selectedPlace);
            }
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
