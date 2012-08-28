package com.facebook;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.LinearLayout;

public class FragmentTestCase<T extends FragmentTestCase.TestFragmentActivity> extends FacebookActivityTestCase<T> {
    public FragmentTestCase(Class<T> activityClass) {
        super(activityClass);
    }

    protected T getTestActivity() {
        return (T) getActivity();
    }

    public static class TestFragmentActivity<T extends Fragment> extends FragmentActivity {
        public static final int FRAGMENT_ID = 0xFACE;

        private Class<T> fragmentClass;
        private int fragmentId;

        protected TestFragmentActivity(Class<T> fragmentClass) {
            this.fragmentClass = fragmentClass;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getAutoCreateUI()) {
                setContentToFragment(null);
            }
        }

        protected boolean getAutoCreateUI() {
            return true;
        }

        void setContentToFragment(T fragment) {
            if (fragment == null) {
                fragment = createFragment();
            }

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.FILL_PARENT));
            layout.setId(FRAGMENT_ID);

            getSupportFragmentManager().beginTransaction()
                    .add(FRAGMENT_ID, fragment)
                    .commit();

            fragmentId = FRAGMENT_ID;

            setContentView(layout);
        }

        void setContentToLayout(int i, int fragmentId) {
            this.fragmentId = fragmentId;
            setContentView(i);
        }

        T createFragment() {
            try {
                return fragmentClass.newInstance();
            } catch (IllegalAccessException e) {
                fail("could not create fragment");
            } catch (InstantiationException e) {
                fail("could not create fragment");
            }
            return null;
        }

        T getFragment() {
            return (T) getSupportFragmentManager().findFragmentById(fragmentId);
        }
    }
}
