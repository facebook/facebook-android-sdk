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

package com.facebook.internal.logging.monitor;

/**
 *  MonitorEvent contains a name which should be consistent to the attribute at endpoint.
 *  The integer samplingRate indicates how often we process and send the log of a particular
 *  MonitorEvent type.
 *  For example, if samplingRate equals 5, we will send the log every 5 logs
 *  which have the same MonitorEvent type.
 */
public enum MonitorEvent {
    FB_CORE_STARTUP("fb_core_startup", 1);

    private String name;
    private int samplingRate;

    MonitorEvent(String name, int samplingRate) {
        this.name = name;
        this.samplingRate = samplingRate;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
