/*
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

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {
  private ShareActionProvider mShareActionProvider;
  private ShareFragment mShareFragment;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);
    FragmentManager fm = getSupportFragmentManager();
    mShareFragment = (ShareFragment) fm.findFragmentById(R.id.sharefragment);
    mShareFragment.setOnShareContentChangeListener(
        new ShareFragment.OnShareContentChangedListener() {
          @Override
          public void onShareContentChanged(String content) {
            setShareUrl(content);
          }
        });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate menu resource file.
    getMenuInflater().inflate(R.menu.share_menu, menu);

    // Locate MenuItem with ShareActionProvider
    MenuItem item = menu.findItem(R.id.menu_item_share);

    // Fetch and store ShareActionProvider
    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
    setShareUrl(mShareFragment.getCurrentShareContent());

    // Return true to display menu
    return true;
  }

  private void setShareUrl(String shareUrl) {
    // When using androids share built into the ActionBar app attribution will not be
    // present when sharing to facebook and app events will not be logged.
    if (mShareActionProvider != null) {
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType("text/plain");
      shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
      mShareActionProvider.setShareIntent(shareIntent);
    }
  }

  @Override
  protected void onSaveInstanceState(final Bundle outState) {
    // Don't save any state
    super.onSaveInstanceState(new Bundle());
  }
}
