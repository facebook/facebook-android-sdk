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

package com.facebook.iconicus;

import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Abstraction for a game board
 */
public class GameBoard {
    public static final int BOARD_ROWS = 9; // 9x9 board
    public static final int GROUP_ROWS = 3;
    public static final int BOARD_SIZE = BOARD_ROWS * BOARD_ROWS; // 9x9 board
    public static final int EMPTY_PIECE = 0;
    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 9;

    private static final Uri SHARE_URI = Uri.parse("https://fb.me/1570399853210604");
    private static final String DATA_KEY = "data";
    private static final String LOCKED_KEY = "locked";

    private static final int [] SEED_GRID = {
            1,2,3,4,5,6,7,8,9,
            4,5,6,7,8,9,1,2,3,
            7,8,9,1,2,3,4,5,6,
            2,3,4,5,6,7,8,9,1,
            5,6,7,8,9,1,2,3,4,
            8,9,1,2,3,4,5,6,7,
            3,4,5,6,7,8,9,1,2,
            6,7,8,9,1,2,3,4,5,
            9,1,2,3,4,5,6,7,8
    };

    private int[] board = new int[BOARD_SIZE];
    private boolean[] lockedPositions = new boolean[BOARD_SIZE];

    private GameBoard(int[] board, boolean[] lockedPositions) {
        if (board.length != this.board.length
                || lockedPositions.length != this.lockedPositions.length) {
            throw new IllegalArgumentException("boards are not the same size");
        }
        System.arraycopy(board, 0, this.board, 0, board.length);
        System.arraycopy(lockedPositions, 0, this.lockedPositions, 0, lockedPositions.length);
    }

    private GameBoard(int[] board) {
        if (board.length != this.board.length) {
            throw new IllegalArgumentException("boards are not the same size");
        }
        for (int i = 0; i < BOARD_SIZE; i++) {
            this.board[i] = board[i];
            this.lockedPositions[i] = (this.board[i] != EMPTY_PIECE);
        }
    }

    /**
     * Generates a new valid board.
     * @param openPositions the number of open positions to leave on the board.
     * @return a new valid board.
     */
    public static GameBoard generateBoard(final int openPositions) {
        Random random = new Random(System.currentTimeMillis());
        int [] board = new int[BOARD_SIZE];
        System.arraycopy(SEED_GRID, 0, board, 0, BOARD_SIZE);

        for (int i = 0; i < 9; i++) {
            shuffleGrid(random, board);
        }

        List<Integer> remainingPositions = new ArrayList<>(BOARD_SIZE);
        for (int i = 0; i < BOARD_SIZE; i++) {
            remainingPositions.add(i);
        }

        for (int i = 0; i < openPositions; i++) {
            removeOpenPosition(random, board, remainingPositions);
        }

        return new GameBoard(board);
    }

    /**
     * Returns a GameBoard from a Uri.
     * @param uri the uri that was shared.
     * @return a board from the Uri.
     */
    public static GameBoard fromUri(final Uri uri) {
        String data = uri.getQueryParameter(DATA_KEY);
        if (data != null) {
            int [] newBoard = decodeBoard(data);
            String locked = uri.getQueryParameter(LOCKED_KEY);
            boolean [] lockedArr;

            if (locked != null) {
                lockedArr = decodeLockedPositions(locked);
            } else {
                // if there's no explicit locked param, then treat every position
                // passed in as being locked.
                lockedArr = new boolean[BOARD_SIZE];
                for (int i = 0; i < lockedArr.length; i++) {
                    if (newBoard[i] != EMPTY_PIECE) {
                        lockedArr[i] = true;
                    }
                }
            }
            return new GameBoard(newBoard, lockedArr);
        }
        return null;
    }

    /**
     * Clears the existing board of all pieces that weren't locked in place.
     */
    public void clearBoard() {
        for (int i = 0; i < board.length; i++) {
            if (!lockedPositions[i]) {
                board[i] = EMPTY_PIECE;
            }
        }
    }

    /**
     * Determines whether the current position is locked or not.
     * @param position the position of the piece on the board.
     * @return whether it's locked or not.
     */
    public boolean isLocked(final int position) {
        return lockedPositions[position];
    }

    /**
     * Sets the value for the position on the board. Returns true if the value is successfully set,
     * false otherwise (e.g. the current position is locked and can't be modified).
     * @param value the value to set.
     * @param position the position of the piece on the board.
     * @return true if value is successfully set.
     */
    public boolean setValue(final int value, final int position) {
        if (!isLocked(position)
                && ((value >= MIN_VALUE && value <= MAX_VALUE) || value == EMPTY_PIECE)) {
            board[position] = value;
            return true;
        }
        return false;
    }

    /**
     * Gets the integer value of the piece on the board.
     * @param position the position of the piece on the board.
     * @return the integer value (0-9) or -1 if there's no current value.
     */
    public int getValue(final int position) {
        if (position < 0 || position >= BOARD_SIZE) {
            return EMPTY_PIECE;
        }
        return board[position];
    }

    /**
     * Gets the string value of the piece on the board.
     * @param position the position of the piece on the board.
     * @return the String value ("0"-"9") or the empty string "" if there's no current value.
     */
    public String getValueAsString(final int position) {
        int value = getValue(position);
        if (value < MIN_VALUE || value > MAX_VALUE) {
            return "";
        }
        return "" + value;
    }

    /**
     * Determines whether the current position is empty or not.
     * @param position the position of the piece.
     * @return true if the current position is empty.
     */
    public boolean isEmpty(final int position) {
        return board[position] == EMPTY_PIECE;
    }

    /**
     * Determines whether the value in the current position is valid or not.
     * @param position the position of the piece.
     * @return true if the current position is valid (empty pieces are by default valid).
     */
    public boolean isValid(final int position) {
        if (isEmpty(position)) {
            return true;
        }
        return validateRow(position) && validateColumn(position) && validateGroup(position);
    }

    /**
     * Converts the current board into a Uri.
     * @return a Uri that represents the current board.
     */
    public Uri toUri() {
        Uri.Builder shareUri = SHARE_URI.buildUpon();
        shareUri.appendQueryParameter(DATA_KEY, encodeBoard());
        shareUri.appendQueryParameter(LOCKED_KEY, encodeLockedPositions());
        return shareUri.build();
    }

    private String encodeBoard() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            builder.append(board[i]);
        }
        return builder.toString();
    }

    private static int[] decodeBoard(String input) {
        int [] newBoard = new int[BOARD_SIZE];
        Arrays.fill(newBoard, EMPTY_PIECE);
        if (input.length() == BOARD_SIZE) {
            for (int i = 0; i < input.length(); i++) {
                newBoard[i] = Integer.parseInt(input.substring(i, i + 1));
            }
        }
        return newBoard;
    }

    private String encodeLockedPositions() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lockedPositions.length; i++) {
            builder.append(lockedPositions[i] ? 1 : 0);;;
        }
        return builder.toString();
    }

    private static boolean[] decodeLockedPositions(String input) {
        boolean [] locked = new boolean[BOARD_SIZE];
        Arrays.fill(locked, false);
        if (input.length() == BOARD_SIZE) {
            for (int i = 0; i < input.length(); i++) {
                locked[i] = (input.charAt(i) == '1');
            }
        }
        return locked;
    }

    private boolean validateRow(final int position) {
        int startPos = (position / BOARD_ROWS) * BOARD_ROWS;
        for (int i = 0; i < BOARD_ROWS; i++) {
            if (!checkIsValid(position, startPos + i)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateColumn(final int position) {
        int startPos = position % BOARD_ROWS;
        for (int i = 0; i < BOARD_ROWS; i++) {
            if (!checkIsValid(position, startPos + (i * BOARD_ROWS))) {
                return false;
            }
        }
        return true;
    }

    private boolean validateGroup(final int position) {
        int row = position / BOARD_ROWS;
        int column = position % BOARD_ROWS;

        int group = (row / GROUP_ROWS) * GROUP_ROWS + (column / GROUP_ROWS);
        int startRow = (group / GROUP_ROWS) * GROUP_ROWS;
        int startColumn = (group % GROUP_ROWS) * GROUP_ROWS;

        for (int i = 0; i < GROUP_ROWS; i++) {
            for (int j = 0; j < GROUP_ROWS; j++) {
                if (!checkIsValid(position, (startRow + i) * 9 + startColumn + j)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkIsValid(final int position1, final int position2) {
        return (position1 == position2) || (board[position1] != board[position2]);
    }

    private static void shuffleGrid(final Random random, final int[] board) {
        switch (random.nextInt(5)) {
            case 0:
                shuffleRow(random, board);
                break;
            case 1:
                shuffleRowGroup(random, board);
                break;
            case 2:
                shuffleColumn(random, board);
                break;
            case 3:
                shuffleColumnGroup(random, board);
                break;
            case 4:
                transpose(board);
                break;
            default:
                break;
        }
    }

    private static void shuffleRow(final Random random, final int[] board) {
        // Swap two random rows. Note that it's only safe to shuffle rows within each group of 3
        // i.e. row 1 can only be shuffled with either row 2 or 3.
        int group = random.nextInt(GROUP_ROWS);
        int row1 = random.nextInt(GROUP_ROWS);
        int row2 = randomOther(random, row1, GROUP_ROWS);

        int realRow1 = group * GROUP_ROWS + row1;
        int realRow2 = group * GROUP_ROWS + row2;

        Range range1 = new Range(realRow1 * BOARD_ROWS, BOARD_ROWS);
        Range range2 = new Range(realRow2 * BOARD_ROWS, BOARD_ROWS);
        swap(board, range1, range2);
    }

    private static void shuffleRowGroup(final Random random, final int[] board) {
        // Swap two groups of rows. i.e. swap rows 123 with rows 789
        int group1 = random.nextInt(GROUP_ROWS);
        int group2 = randomOther(random, group1, GROUP_ROWS);

        Range range1 = new Range(group1 * GROUP_ROWS * BOARD_ROWS, GROUP_ROWS * BOARD_ROWS);
        Range range2 = new Range(group2 * GROUP_ROWS * BOARD_ROWS, GROUP_ROWS * BOARD_ROWS);
        swap(board, range1, range2);
    }

    private static void shuffleColumn(final Random random, final int[] board) {
        // Swap two random columns. Note that just like with rows, it's only safe to shuffle columns
        // within each group of 3
        int group = random.nextInt(GROUP_ROWS);
        int col1 = random.nextInt(GROUP_ROWS);
        int col2 = randomOther(random, col1, GROUP_ROWS);

        int realCol1 = group * GROUP_ROWS + col1;
        int realCol2 = group * GROUP_ROWS + col2;

        swapColumn(board, realCol1, realCol2);
    }

    private static void shuffleColumnGroup(final Random random, final int[] board) {
        // Swap two groups of columns. i.e. swap columns 123 with columns 789
        int group1 = random.nextInt(GROUP_ROWS);
        int group2 = randomOther(random, group1, GROUP_ROWS);

        for (int i = 0; i < GROUP_ROWS; i++) {
            int realCol1 = group1 * GROUP_ROWS + i;
            int realCol2 = group2 * GROUP_ROWS + i;
            swapColumn(board, realCol1, realCol2);
        }
    }

    private static void transpose(final int[] board) {
        for (int row = 0; row < BOARD_ROWS; ++row) {
            for (int col = row + 1; col < BOARD_ROWS; ++col) {
                int index1 = (row * BOARD_ROWS) + col;
                int index2 = (col * BOARD_ROWS) + row;
                swapPosition(board, index1, index2);
            }
        }
    }

    private static int randomOther(final Random random, final int currentValue, final int space) {
        return ((currentValue % space) + (random.nextInt(space - 1) + 1)) % space;
    }

    private static void swap(final int[] board, final Range range1, final Range range2) {
        if (range1.getSize() != range2.getSize()) {
            return;
        }
        int[] range2Copy = Arrays.copyOfRange(board, range2.getStart(), range2.getEnd());
        System.arraycopy(board, range1.getStart(), board, range2.getStart(), range1.getSize());
        System.arraycopy(range2Copy, 0, board, range1.getStart(), range1.getSize());
    }

    private static void swapColumn(final int[] board, final int col1, final int col2) {
        for (int i = 0; i < BOARD_ROWS; i++) {
            swapPosition(board, (i * BOARD_ROWS) + col1, (i * BOARD_ROWS) + col2);
        }
    }

    private static void swapPosition(final int[] board, final int pos1, final int pos2) {
        int val2 = board[pos2];
        board[pos2] = board[pos1];
        board[pos1] = val2;
    }

    private static void removeOpenPosition(
            final Random random,
            final int[] board,
            final List<Integer> remainingPositions) {
        int index = random.nextInt(remainingPositions.size());
        int position = remainingPositions.remove(index);
        board[position] = EMPTY_PIECE;
    }

    private static class Range {
        private int start;
        private int size;
        private int end;

        public Range(final int start, final int size) {
            this.start = start;
            this.size = size;
            this.end = start + size;
        }

        public int getStart() {
            return start;
        }

        public int getSize() {
            return size;
        }

        public int getEnd() {
            return end;
        }
    }
}
