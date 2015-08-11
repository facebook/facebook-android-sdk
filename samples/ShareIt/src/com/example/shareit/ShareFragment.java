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

package com.example.shareit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.CallbackManager;
import com.facebook.login.widget.LoginButton;
import com.facebook.share.widget.LikeView;

import java.util.ArrayList;

public class ShareFragment extends Fragment {
    private CallbackManager mCallbackManager;
    private OnShareContentChangedListener mShareContentChangedListener;
    private ViewPager mViewPage;

    // The resource to url mapping
    private static final ArrayList<Pair<Integer, String>> IMAGE_IDS =
            new ArrayList<Pair<Integer, String>>() {{
                add(new Pair<>(R.drawable.goofy, "http://shareitexampleapp.parseapp.com/goofy/"));
                add(new Pair<>(R.drawable.liking, "http://shareitexampleapp.parseapp.com/liking/"));
                add(new Pair<>(R.drawable.viking, "http://shareitexampleapp.parseapp.com/viking/"));
            }};

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mCallbackManager = CallbackManager.Factory.create();

        View view = inflater.inflate(R.layout.share_it_view, container, false);

        LoginButton loginButton = (LoginButton) view.findViewById(R.id.login_button);
        loginButton.setFragment(this);
        loginButton.setReadPermissions("public_profile");

        setupViewPage(view);
        LikeView pageLike = (LikeView) view.findViewById(R.id.like_page);
        pageLike.setFragment(this);
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void setOnShareContentChangeListener(OnShareContentChangedListener listener) {
        mShareContentChangedListener = listener;
    }

    public String getCurrentShareContent() {
        return IMAGE_IDS.get(mViewPage.getCurrentItem()).second;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            ShareImageFragment imageFragment = new ShareImageFragment();
            imageFragment.setImage(IMAGE_IDS.get(position).first);
            return imageFragment;
        }

        @Override
        public int getCount() {
            return IMAGE_IDS.size();
        }
    }

    private void setupViewPage(View view) {
        mViewPage = (ViewPager) view.findViewById(R.id.pager);
        PagerAdapter adapter = new ScreenSlidePagerAdapter(getFragmentManager());
        mViewPage.setAdapter(adapter);

        final PageSelector pageSelector = (PageSelector) view.findViewById(R.id.page_selector);
        pageSelector.setImageCount(IMAGE_IDS.size());

        final LikeView photoLike = (LikeView) view.findViewById(R.id.like_photo);
        photoLike.setFragment(this);

        mViewPage.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(
                    int position,
                    float positionOffset,
                    int positionOffsetPixels) {
                pageSelector.setPosition(position);
                String shareContent = IMAGE_IDS.get(position).second;
                photoLike.setObjectIdAndType(
                        shareContent,
                        LikeView.ObjectType.OPEN_GRAPH);
                mShareContentChangedListener.onShareContentChanged(shareContent);
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public interface OnShareContentChangedListener {
        void onShareContentChanged(String content);
    }
}
