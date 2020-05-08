/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 * <p>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 * <p>
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.internal.logging;

import java.io.Serializable;

/**
 * Log event contains event name and category.
 * Event name indicates which event/method is tracked, sampling rate is event-basis.
 * Category indicates which category the log belongs to, we can add validator for each category if
 * needed.
 */
public class LogEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String eventName;
    private LogCategory logCategory;

    public LogEvent(String eventName, LogCategory logCategory) {
        this.eventName = eventName;
        this.logCategory = logCategory;
    }

    public String getEventName() {
        return this.eventName;
    }

    public LogCategory getLogCategory() {
        return logCategory;
    }

    public String upperCaseEventName() {
        this.eventName = eventName.toUpperCase();
        return this.eventName;
    }
}
