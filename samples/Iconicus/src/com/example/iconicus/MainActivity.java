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

package com.example.iconicus;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.SendButton;
import com.facebook.share.widget.ShareButton;

public class MainActivity extends Activity {

    private GridView board;
    private GridView validNumbers;

    private GameController gameController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
        setContentView(R.layout.activity_main);

        gameController = new GameController(this, getIntent());

        board = (GridView) findViewById(R.id.board);

        validNumbers = (GridView) findViewById(R.id.valid_numbers);

        gameController.setBoardView(board);
        gameController.setSelectionView(validNumbers);

        Button newGame = (Button) findViewById(R.id.new_board);
        newGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameController.newGame();
            }
        });

        Button clearBoard = (Button) findViewById(R.id.clear_board);
        clearBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameController.clearBoard();
            }
        });

        final ShareButton share = (ShareButton) findViewById(R.id.share_button);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share.setShareContent(getLinkContent());
            }
        });

        final SendButton send = (SendButton) findViewById(R.id.send_button);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send.setShareContent(getLinkContent());
            }
        });

        Button copy = (Button) findViewById(R.id.copy_button);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newUri(
                        getContentResolver(), "Iconicus", gameController.getShareUri());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, R.string.link_copied, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ShareLinkContent getLinkContent() {
        return new ShareLinkContent.Builder()
                .setContentUrl(gameController.getShareUri())
                .setContentTitle(getString(R.string.share_title))
                .setContentDescription(getString(R.string.share_description))
                .build();
    }

}
