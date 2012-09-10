package com.facebook.scrumptious;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.facebook.Session;

import java.util.Arrays;

public class SplashFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.splash, container, false);
        Button loginButton = (Button) view.findViewById(R.id.splash_login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Session session = Session.getActiveSession();
                if (session != null && !session.getIsOpened() && !session.getState().getIsClosed()) {
                    session.open(getActivity(), null);
                } else {
                    String[] permissions = getResources().getStringArray(R.array.permissions);
                    if (permissions != null && permissions.length > 0) {
                        Session.sessionOpen(getActivity(), null, Arrays.asList(permissions), null);
                    } else {
                        Session.sessionOpen(getActivity(), null);
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

}
