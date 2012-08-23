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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.TouchUtils;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.facebook.sdk.tests.R;

public class FriendPickerFragmentTests extends FacebookActivityTestCase<FriendPickerFragmentTests.TestActivity> {
    public FriendPickerFragmentTests() {
        super(TestActivity.class);
    }

    @LargeTest
    public void testActivityTestCaseSetUpProperly() {
        TestActivity activity = getActivity();
        assertNotNull("activity should be launched successfully", activity);

        FriendPickerFragment fragment = activity.getFragment();
        assertNotNull("fragment should be instantiated", fragment);

        View fragmentView = fragment.getView();
        assertNotNull("fragment should have view", fragmentView);
    }

    @LargeTest
    public void testFriendsLoad() throws Throwable {
        TestActivity activity = getActivity();
        assertNotNull(activity);

        final FriendPickerFragment fragment = activity.getFragment();
        assertNotNull(fragment);

        // Ensure our test user has at least one friend.
        final TestSession session1 = openTestSessionWithSharedUser();
        TestSession session2 = openTestSessionWithSharedUser(SECOND_TEST_USER_TAG);
        makeTestUsersFriends(session1, session2);

        // Trigger a data load (on the UI thread).
        final TestBlocker blocker = getTestBlocker();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.setSession(session1);
                fragment.loadData();
                fragment.setOnDataChangedListener(new GraphObjectListFragment.OnDataChangedListener() {
                    @Override
                    public void onDataChanged() {
                        blocker.signal();
                    }
                });
            }
        });
        // Wait for the data to load and the UI to update.
        blocker.waitForSignals(1);
        getInstrumentation().waitForIdleSync();

        // Click on the first item in the list view.
        ListView listView = (ListView) fragment.getView().findViewById(R.id.listView);
        assertNotNull(listView);
        View firstChild = listView.getChildAt(0);
        assertNotNull(firstChild);

        // Assert our state before we touch anything.
        CheckBox checkBox = (CheckBox)listView.findViewById(R.id.picker_checkbox);
        assertNotNull(checkBox);
        assertFalse(checkBox.isChecked());
        assertEquals(0, fragment.getSelectedGraphObjects().size());

        TouchUtils.clickView(this, firstChild);

        // We should have a selection (it might not be the user we made a friend up above, if the
        // test user has more than one friend).
        assertEquals(1, fragment.getSelectedGraphObjects().size());

        // And the checkbox should be checked.
        assertTrue(checkBox.isChecked());

        // Touch the item again. We should go back to no selection.
        TouchUtils.clickView(this, firstChild);
        assertEquals(0, fragment.getSelectedGraphObjects().size());
        assertFalse(checkBox.isChecked());
    }

    public static class TestFragmentActivity<T extends Fragment> extends FragmentActivity{
        public static final int FRAGMENT_ID = 0xFACE;
        private Class<T> fragmentClass;

        protected TestFragmentActivity(Class<T> fragmentClass) {
            this.fragmentClass = fragmentClass;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(createUI());
        }

        private View createUI() {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.FILL_PARENT));
            layout.setId(FRAGMENT_ID);
            {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                try {
                    transaction.add(FRAGMENT_ID, fragmentClass.newInstance());
                } catch (IllegalAccessException e) {
                    fail("could not add fragment");
                } catch (InstantiationException e) {
                    fail("could not add fragment");
                }
                transaction.commit();

            }
            return layout;
        }

        T getFragment() {
            return (T)getSupportFragmentManager().findFragmentById(FRAGMENT_ID);
        }
    }

    public static class TestActivity extends TestFragmentActivity<FriendPickerFragment> {
        public TestActivity() {
            super(FriendPickerFragment.class);
        }
    }
}
