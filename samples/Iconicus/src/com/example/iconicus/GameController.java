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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import bolts.AppLinks;

public class GameController {

    private static final int NUM_OPEN_POSITIONS = 50;

    private static boolean iconsInitialized = false;
    private static Drawable[] validIcons = new Drawable[GameBoard.BOARD_ROWS + 1];

    private Context context;
    private GameBoard board;
    private GridView boardView;
    private GridView selectionView;
    private BoardAdapter boardAdapter;
    private SelectionAdapter selectionAdapter;
    private int selectedNum;
    private View selectedView;


    public GameController(final Context context, final Intent intent) {
        this.context = context;
        initializeIcons();
        board = handleDeepLink(context, intent);
        if (board == null) {
            board = GameBoard.generateBoard(NUM_OPEN_POSITIONS);
        }
        resetSelection();
    }

    public void setBoardView(final GridView boardView) {
        this.boardView = boardView;
        boardAdapter = new BoardAdapter();
        this.boardView.setAdapter(boardAdapter);
        this.boardView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if (selectedNum >= 0) {
                    if (board.isLocked(position)) {
                        Toast.makeText(context.getApplicationContext(),
                                R.string.position_locked,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        board.setValue(selectedNum, position);
                        dataSetChanged();
                    }
                } else {
                    Toast.makeText(context.getApplicationContext(),
                            R.string.nothing_selected,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void setSelectionView(final GridView selectionView) {
        this.selectionView = selectionView;
        selectionAdapter = new SelectionAdapter();
        this.selectionView.setAdapter(selectionAdapter);
        this.selectionView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if (selectedNum != position) {
                    resetSelection();
                    selectedNum = position;
                    v.setBackgroundResource(R.drawable.selected_icon_background);
                    selectedView = v;
                } else {
                    resetSelection();
                }
            }
        });
    }

    public void newGame() {
        board = GameBoard.generateBoard(NUM_OPEN_POSITIONS);
        dataSetChanged();
    }

    public void clearBoard() {
        board.clearBoard();
        dataSetChanged();
    }

    public Uri getShareUri() {
        return board.toUri();
    }

    private synchronized void initializeIcons() {
        if (iconsInitialized) {
            return;
        }
        validIcons[0] = null;
        validIcons[1] = context.getResources().getDrawable(R.drawable.tile1);
        validIcons[2] = context.getResources().getDrawable(R.drawable.tile2);
        validIcons[3] = context.getResources().getDrawable(R.drawable.tile3);
        validIcons[4] = context.getResources().getDrawable(R.drawable.tile4);
        validIcons[5] = context.getResources().getDrawable(R.drawable.tile5);
        validIcons[6] = context.getResources().getDrawable(R.drawable.tile6);
        validIcons[7] = context.getResources().getDrawable(R.drawable.tile7);
        validIcons[8] = context.getResources().getDrawable(R.drawable.tile8);
        validIcons[9] = context.getResources().getDrawable(R.drawable.tile9);
        iconsInitialized = true;
    }

    private GameBoard handleDeepLink(final Context context, final Intent intent) {
        Uri targetUri = AppLinks.getTargetUrlFromInboundIntent(context, intent);
        if (targetUri == null) {
            targetUri = intent.getData();
        }

        if (targetUri == null) {
            return null;
        }

        return GameBoard.fromUri(targetUri);
    }

    private void dataSetChanged() {
        if (boardAdapter != null) {
            boardAdapter.notifyDataSetChanged();
        }
    }

    private void resetSelection() {
        if (selectedView != null) {
            selectedView.setBackgroundResource(R.drawable.choice_icon_background);
        }
        selectedNum = GameBoard.EMPTY_PIECE;
        selectedView = null;
    }

    private void updateCell(final ImageView imageView, final int position) {
        imageView.setImageDrawable(validIcons[board.getValue(position)]);

        if (board.isLocked(position)) {
            imageView.setBackgroundResource(R.drawable.locked_icon_background);
        } else if (board.isEmpty(position)) {
            imageView.setBackgroundResource(R.drawable.default_icon_background);
        } else if (board.isValid(position)) {
            imageView.setBackgroundResource(R.drawable.valid_icon_background);
        } else {
            imageView.setBackgroundResource(R.drawable.invalid_icon_background);
        }
    }

    private class SelectionAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return GameBoard.BOARD_ROWS + 1;
        }

        @Override
        public Object getItem(int position) {
            return validIcons[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                return convertView;
            }

            ImageView view = (ImageView) View.inflate(context, R.layout.choice_cell, null);
            view.setImageDrawable(validIcons[position]);
            return view;
        }
    }

    private class BoardAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return GameBoard.BOARD_SIZE;
        }

        @Override
        public Object getItem(final int position) {
            return board.getValue(position);
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            if (convertView != null) {
                updateCell((ImageView) convertView, position);
                return convertView;
            }

            ImageView view = (ImageView) View.inflate(context, R.layout.grid_cell, null);

            updateCell(view, position);

            return view;
        }
    }
}
