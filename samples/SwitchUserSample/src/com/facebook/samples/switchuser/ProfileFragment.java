package com.facebook.samples.switchuser;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.TextView;
import com.facebook.GraphUser;
import com.facebook.ProfilePictureView;

public class ProfileFragment extends Fragment {

    public static final String TAG = "ProfileFragment";

    private TextView userNameView;
    private ProfilePictureView profilePictureView;
    private OnOptionsItemSelectedListener onOptionsItemSelectedListener;

    private GraphUser pendingUpdateForUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_profile, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = false;
        OnOptionsItemSelectedListener listener = onOptionsItemSelectedListener;
        if (listener != null) {
            handled = listener.onOptionsItemSelected(item);
        }

        if (!handled) {
            handled = super.onOptionsItemSelected(item);
        }

        return handled;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, parent, false);

        userNameView = (TextView)v.findViewById(R.id.profileUserName);
        profilePictureView = (ProfilePictureView)v.findViewById(R.id.profilePic);

        if (pendingUpdateForUser != null) {
            updateViewForUser(pendingUpdateForUser);
            pendingUpdateForUser = null;
        }

        return v;
    }

    public void setOnOptionsItemSelectedListener(OnOptionsItemSelectedListener listener) {
        this.onOptionsItemSelectedListener = listener;
    }

    public void updateViewForUser(GraphUser user) {
        if (userNameView == null || profilePictureView == null) {
            // Fragment not yet added to the view. So let's store which user was intended
            // for display.
            pendingUpdateForUser = user;
            return;
        }

        if (user == null) {
            profilePictureView.setUserId(null);
            userNameView.setText(getString(R.string.greeting_no_user));
        } else {
            profilePictureView.setUserId(user.getId());
            userNameView.setText(
                    String.format(getString(R.string.greeting_format), user.getFirstName()));
        }
    }

    public interface OnOptionsItemSelectedListener {
        boolean onOptionsItemSelected(MenuItem item);
    }
}
