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

package com.facebook.appevents.ml;

public class MTensor {

    private float[] data;
    private int[] shape;
    private int capacity;

    public MTensor(int[] shape) {
        this.shape = shape;
        this.capacity = getCapacity(shape);
        this.data = new float[this.capacity];
    }

    public float[] getData() {
        return this.data;
    }

    public int getShape(int i) {
        return this.shape[i];
    }

    public void reshape(int[] shape) {
        this.shape = shape;
        int new_capacity = getCapacity(shape);
        float[] new_data = new float[new_capacity];
        System.arraycopy(this.data, 0, new_data, 0, Math.min(this.capacity, new_capacity));
        this.data = new_data;
        this.capacity = new_capacity;
    }

    public int getShapeSize() {
        return shape.length;
    }

    private static int getCapacity(int[] shape) {
        int capacity = 1;
        for (int i = 0; i < shape.length; i++) {
            capacity *= shape[i];
        }
        return capacity;
    }
}
