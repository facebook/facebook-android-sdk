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

package com.facebook.samples.messenger.send;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.widget.Toolbar;
import com.facebook.messenger.MessengerThreadParams;
import com.facebook.messenger.MessengerUtils;
import com.facebook.messenger.ShareToMessengerParams;

/** Main Activity for sample. */
public class MainActivity extends Activity {

  // This is the request code that the SDK uses for startActivityForResult. See the code below
  // that references it. Messenger currently doesn't return any data back to the calling
  // application.
  private static final int REQUEST_CODE_SHARE_TO_MESSENGER = 1;

  private Toolbar mToolbar;
  private View mMessengerButton;
  private MessengerThreadParams mThreadParams;
  private boolean mPicking;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main_activity);
    mToolbar = (Toolbar) findViewById(R.id.toolbar);
    mMessengerButton = findViewById(R.id.messenger_send_button);

    mToolbar.setTitle(R.string.app_name);

    // If we received Intent.ACTION_PICK from Messenger, we were launched from a composer shortcut
    // or the reply flow.
    Intent intent = getIntent();
    if (Intent.ACTION_PICK.equals(intent.getAction())) {
      mThreadParams = MessengerUtils.getMessengerThreadParamsForIntent(intent);
      mPicking = true;

      // Note, if mThreadParams is non-null, it means the activity was launched from Messenger.
      // It will contain the metadata associated with the original content, if there was content.
    }

    mMessengerButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onMessengerButtonClicked();
          }
        });
  }

  private void onMessengerButtonClicked() {
    // The URI can reference a file://, content://, or android.resource. Here we use
    // android.resource for sample purposes.
    Uri uri =
        Uri.parse("android.resource://com.facebook.samples.messenger.send/" + R.drawable.tree);

    // Create the parameters for what we want to send to Messenger.
    ShareToMessengerParams shareToMessengerParams =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .setMetaData("{ \"image\" : \"tree\" }")
            .build();

    if (mPicking) {
      // If we were launched from Messenger, we call MessengerUtils.finishShareToMessenger to return
      // the content to Messenger.
      MessengerUtils.finishShareToMessenger(this, shareToMessengerParams);
    } else {
      // Otherwise, we were launched directly (for example, user clicked the launcher icon). We
      // initiate the broadcast flow in Messenger. If Messenger is not installed or Messenger needs
      // to be upgraded, this will direct the user to the play store.
      MessengerUtils.shareToMessenger(
          this, REQUEST_CODE_SHARE_TO_MESSENGER, shareToMessengerParams);
    }
  }
}
