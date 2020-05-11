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

package com.example.rps;

public class RpsGameUtils {
  public enum RpsState {
    INIT,
    PLAYING,
    RESULT
  };

  public enum RpsResult {
    WIN(R.string.win, R.string.result_won),
    LOSS(R.string.loss, R.string.result_lost),
    TIE(R.string.tie, R.string.result_tied),
    INVALID(0, 0);

    private int id;
    private int resultId;

    private RpsResult(int stringId, int resultStringId) {
      id = stringId;
      resultId = resultStringId;
    }

    public int getStringId() {
      return id;
    }

    public int getResultStringId() {
      return resultId;
    }
  };

  public static final int ROCK = 0;
  public static final int PAPER = 1;
  public static final int SCISSORS = 2;
  public static final int INVALID_CHOICE = -1;
  public static final int SHOOT = 100;
  public static final int[] DRAWABLES_HUMAN = {
    R.drawable.left_rock, R.drawable.left_paper, R.drawable.left_scissors
  };
  public static final int[] DRAWABLES_COMPUTER = {
    R.drawable.right_rock, R.drawable.right_paper, R.drawable.right_scissors
  };
  public static final int[] STRINGS_TITLES = {R.string.rock, R.string.paper, R.string.scissors};
  public static final RpsResult[][] RESULTS = {
    {RpsResult.TIE, RpsResult.LOSS, RpsResult.WIN},
    {RpsResult.WIN, RpsResult.TIE, RpsResult.LOSS},
    {RpsResult.LOSS, RpsResult.WIN, RpsResult.TIE}
  };
}
