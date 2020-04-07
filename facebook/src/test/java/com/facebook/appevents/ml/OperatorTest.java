/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.ml;

import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class OperatorTest {

    @Test
    public void testReshape() {
        MTensor x = new MTensor(new int[]{2,3,4});
        Whitebox.setInternalState(x, "data", new float[]{
                1,  3,  5,  7,
                5,  7,  9,  11,
                9,  11, 13, 15,

                13, 15, 17, 19,
                17, 19, 21, 23,
                21, 23, 25, 27,
        });

        x.reshape(new int[] {2,2,4});
        float[] expected_data = new float[]{
                1,  3,  5,  7,
                5,  7,  9,  11,

                9,  11, 13, 15,
                13, 15, 17, 19,
        };
        assertArrayEquals(x.getData(), expected_data, (float) 0.0001);
        assertEquals(Whitebox.getInternalState(x, "capacity"), 16);

        x.reshape(new int[] {2,3,4});
        expected_data = new float[]{
                1,  3,  5,  7,
                5,  7,  9,  11,
                9,  11, 13, 15,

                13, 15, 17, 19,
                0,  0,  0,  0,
                0,  0,  0,  0,
        };
        assertArrayEquals(x.getData(), expected_data, (float) 0.0001);
        assertEquals(Whitebox.getInternalState(x, "capacity"), 24);
    }

    @Test
    public void testAddmv() {
        MTensor x = new MTensor(new int[]{2, 3, 4});
        Whitebox.setInternalState(x, "data", new float[]{
                0, 1, 2, 3,
                4, 5, 6, 7,
                8, 9, 10, 11,

                12, 13, 14, 15,
                16, 17, 18, 19,
                20, 21, 22, 23,
        });

        MTensor bias = new MTensor(new int[]{4});
        Whitebox.setInternalState(bias, "data", new float[]{
                1, 2, 3, 4,
        });

        Operator.addmv(x, bias);
        float[] expected_data = new float[]{
                1,  3,  5,  7,
                5,  7,  9,  11,
                9,  11, 13, 15,

                13, 15, 17, 19,
                17, 19, 21, 23,
                21, 23, 25, 27,
        };
        assertArrayEquals(x.getData(), expected_data, (float) 0.0001);
    }

    @Test
    public void testMul() {
        MTensor x = new MTensor(new int[]{2,4});
        Whitebox.setInternalState(x, "data", new float[]{
                0, 1, 2, 3,
                4, 5, 6, 7,
        });

        MTensor weight = new MTensor(new int[]{4,2});
        Whitebox.setInternalState(weight, "data", new float[]{
                 1, 0,
                 0, 2,
                -1, 0,
                 0, 2,
        });

        MTensor y = Operator.mul(x, weight);
        float[] expected_data = new float[]{
                -2, 8,
                -2, 24,
        };
        assertArrayEquals(y.getData(), expected_data, (float) 0.0001);
    }

    @Test
    public void testRelu() {
        MTensor x = new MTensor(new int[]{2,4});
        Whitebox.setInternalState(x, "data", new float[]{
                 0, -1,  2, -3,
                -4,  5, -6,  7,
        });

        Operator.relu(x);
        float[] expected_data = new float[]{
                0,  0,  2,  0,
                0,  5,  0,  7,
        };
        assertArrayEquals(x.getData(), expected_data, (float) 0.0001);
    }

    @Test
    public void testFlatten() {
        MTensor x = new MTensor(new int[]{2,3,4});
        Whitebox.setInternalState(x, "data", new float[]{
                0, 1, 2, 3,
                4, 5, 6, 7,
                8, 9, 10, 11,

                12, 13, 14, 15,
                16, 17, 18, 19,
                20, 21, 22, 23,
        });

        Operator.flatten(x, 1);
        int[] expected_shape = new int[]{2,12};
        float[] expected_data = new float[]{
                0,  1,  2,  3,  4,  5,  6,  7,  8,  9,  10, 11,
                12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
        };
        assertArrayEquals(x.getData(), expected_data, (float) 0.0001);
        assertArrayEquals((int[]) Whitebox.getInternalState(x, "shape"), expected_shape);
    }

    @Test
    public void testConcatenate() {
        MTensor x1 = new MTensor(new int[]{2,4});
        Whitebox.setInternalState(x1, "data", new float[]{
                0, 1, 2, 3,
                4, 5, 6, 7,
        });

        MTensor x2 = new MTensor(new int[]{2,3});
        Whitebox.setInternalState(x2, "data", new float[]{
                16, 17, 18,
                20, 21, 22,
        });

        MTensor x3 = new MTensor(new int[]{2,2});
        Whitebox.setInternalState(x3, "data", new float[]{
                4, 5,
                6, 7,
        });

        MTensor y = Operator.concatenate(new MTensor[]{x1, x2, x3});
        int[] expected_shape = new int[]{2,9};
        float[] expected_data = new float[]{
                0, 1, 2, 3, 16, 17, 18, 4, 5,
                4, 5, 6, 7, 20, 21, 22, 6, 7,
        };
        assertArrayEquals(y.getData(), expected_data, (float) 0.0001);
        assertArrayEquals((int[]) Whitebox.getInternalState(y, "shape"), expected_shape);
    }

    @Test
    public void testSoftmax() {
        MTensor x = new MTensor(new int[]{2,4});
        Whitebox.setInternalState(x, "data", new float[]{
                0, 1, 2, 3,
                4, 5, 6, 7,
        });

        Operator.softmax(x);
        float[] expected_data = new float[]{
                0.03205860f, 0.08714432f, 0.23688284f, 0.6439143f,
                0.03205860f, 0.08714432f, 0.23688284f, 0.6439143f,
        };
        assertArrayEquals(x.getData(), expected_data, (float) 0.00000001);
    }

    @Test
    public void testDense() {
        MTensor x = new MTensor(new int[]{2,4});
        Whitebox.setInternalState(x, "data", new float[]{
                0, 1, 2, 3,
                4, 5, 6, 7,
        });

        MTensor weight = new MTensor(new int[]{4,2});
        Whitebox.setInternalState(weight, "data", new float[]{
                1, 0,
                0, 2,
                -1, 0,
                0, 2,
        });

        MTensor bias = new MTensor(new int[]{2});
        Whitebox.setInternalState(bias, "data", new float[]{
                1, -1,
        });

        MTensor y = Operator.dense(x, weight, bias);
        float[] expected_data = new float[]{
                -1, 7,
                -1, 23,
        };
        assertArrayEquals(y.getData(), expected_data, (float) 0.0001);
    }

    @Test
    public void testTranspose2D() {
        MTensor input = new MTensor(new int[]{3,4});
        Whitebox.setInternalState(input, "data", new float[]{
                0, 1, 2, 3,
                4, 5, 6, 7,
                8, 9, 10, 11,
        });

        MTensor output = Operator.transpose2D(input);
        float[] expected_data = new float[]{
                0, 4, 8,
                1, 5, 9,
                2, 6, 10,
                3, 7, 11,
        };
        assertArrayEquals(output.getData(), expected_data, (float) 0.0001);
    }

    @Test
    public void testTranspose3D() {
        MTensor input = new MTensor(new int[]{2,3,4});
        Whitebox.setInternalState(input, "data", new float[]{
                0, 1, 2, 3,
                4, 5, 6, 7,
                8, 9, 10, 11,

                12, 13, 14, 15,
                16, 17, 18, 19,
                20, 21, 22, 23
        });

        MTensor output = Operator.transpose3D(input);
        float[] expected_data = new float[]{
                0, 12,
                4, 16,
                8, 20,

                1, 13,
                5, 17,
                9, 21,

                2, 14,
                6, 18,
                10, 22,

                3, 15,
                7, 19,
                11, 23,
        };
        assertArrayEquals(output.getData(), expected_data, (float) 0.0001);
    }

    @Test
    public void testConv1D() {
        MTensor input = new MTensor(new int[]{1,5,3});
        Whitebox.setInternalState(input, "data", new float[]{
                1, 2, 3,
                4, 5, 6,
                9, 8, 7,
                5, 8, 1,
                5, 3, 0,
        });

        MTensor weight = new MTensor(new int[]{3,3,4});
        Whitebox.setInternalState(weight, "data", new float[]{
                -1,   3,   0,   1,
                 5,  -7,   5,   7,
                -9,   9,   2,   3,

                 2,   4,   5,   6,
                 6,   8,   9,   4,
                 10, -10,  5,   6,

                 1,   0,   5,   6,
                 2,   5,   9,   4,
                 9,   10,  5,   6,
        });

        MTensor output = Operator.conv1D(input, weight);
        float[] expected_data = new float[]{
                168, 122, 263, 232,
                133, 111, 291, 253,
                47,  123, 208, 196,
        };
        assertArrayEquals(output.getData(), expected_data, (float) 0.0001);
    }

    @Test
    public void testMaxPool1D() {
        MTensor input = new MTensor(new int[]{2,2,3});
        Whitebox.setInternalState(input, "data", new float[]{
                -1,   2,   3,
                 4,  -5,   6,

                 7,  -8,   9,
                -10,  11,  12,
        });

        MTensor output = Operator.maxPool1D(input, 2);
        float[] expected_data = new float[]{
                 4,   2,   6,
                 7,   11,  12
        };
        assertArrayEquals(output.getData(), expected_data, (float) 0.0001);
    }
}
