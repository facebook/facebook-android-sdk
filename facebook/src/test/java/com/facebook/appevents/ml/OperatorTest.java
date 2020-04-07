package com.facebook.appevents.ml;

import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertArrayEquals;

public class OperatorTest {

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
