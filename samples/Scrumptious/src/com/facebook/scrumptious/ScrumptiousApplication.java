package com.facebook.scrumptious;

import android.app.Application;
import com.facebook.GraphPlace;
import com.facebook.GraphUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Use a custom Application class to pass state data between Activities.
 */
public class ScrumptiousApplication extends Application {

    private List<GraphUser> selectedUsers;
    private GraphPlace selectedPlace;

    public List<GraphUser> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(Collection<GraphUser> users) {
        if (users != null) {
            // TODO: change FriendPicker to return a List instead of a Set
            //       so we don't have to do this conversion.
            selectedUsers = new ArrayList<GraphUser>(users);
        } else {
            selectedUsers = null;
        }
    }

    public GraphPlace getSelectedPlace() {
        return selectedPlace;
    }

    public void setSelectedPlace(GraphPlace place) {
        this.selectedPlace = place;
    }
}
