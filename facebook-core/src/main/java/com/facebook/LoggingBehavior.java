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

package com.facebook;

/**
 * Specifies different categories of logging messages that can be generated.
 *
 * @see FacebookSdk#addLoggingBehavior(LoggingBehavior)
 */
public enum LoggingBehavior {
    /**
     * Indicates that HTTP requests and a summary of responses should be logged.
     */
    REQUESTS,
    /**
     * Indicates that access tokens should be logged as part of the request logging; normally they
     * are not.
     */
    INCLUDE_ACCESS_TOKENS,
    /**
     * Indicates that the entire raw HTTP response for each request should be logged.
     */
    INCLUDE_RAW_RESPONSES,
    /**
     * Indicates that cache operations should be logged.
     */
    CACHE,
    /**
     * Indicates the App Events-related operations should be logged.
     */
    APP_EVENTS,
    /**
     * Indicates that likely developer errors should be logged.  (This is set by default in
     * LoggingBehavior.)
     */
    DEVELOPER_ERRORS,

    /**
     * Log debug warnings from API response, e.g. when friends fields requested, but user_friends
     * permission isn't granted.
     */
    GRAPH_API_DEBUG_WARNING,

    /**
     * Log warnings from API response, e.g. when requested feature will be deprecated in next
     * version of API. Info is the lowest level of severity, using it will result in logging all
     * GRAPH_API_DEBUG levels.
     */
    GRAPH_API_DEBUG_INFO
    ;
}
