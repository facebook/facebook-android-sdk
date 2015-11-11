/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.LinearLayout;

public class FragmentTestCase<T extends FragmentTestCase.TestFragmentActivity<?>> extends FacebookActivityTestCase<T> {
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
                try {
                    fragment = createFragment();
                } catch (InstantiationException e) {
                    return;
                } catch (IllegalAccessException e) {
                    return;
                }
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

        protected T createFragment() throws InstantiationException, IllegalAccessException {
            return fragmentClass.newInstance();
        }

        T getFragment() {
            @SuppressWarnings("unchecked")
            T fragment = (T) getSupportFragmentManager().findFragmentById(fragmentId);
            return fragment;
        }
    }
}
