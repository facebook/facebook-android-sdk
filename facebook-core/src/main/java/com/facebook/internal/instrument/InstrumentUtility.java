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

package com.facebook.internal.instrument;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.facebook.GraphRequest;
import com.facebook.FacebookSdk;
import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class InstrumentUtility {

    public static final String CRASH_REPORT_PREFIX = "crash_log_";
    public static final String ERROR_REPORT_PREFIX = "error_log_";

    private static final String FBSDK_PREFIX = "com.facebook";
    private static final String INSTRUMENT_DIR = "instrument";

    /**
     * Get the cause of the raised exception.
     *
     * @param e The Throwable containing the exception that was raised
     * @return The String containing the cause of the raised exception
     */
    @Nullable
    public static String getCause(Throwable e) {
        if (e == null) {
            return null;
        }
        if (e.getCause() == null) {
            return e.toString();
        }
        return e.getCause().toString();
    }

    /**
     * Get the iterated call stack traces of the raised exception.
     *
     * @param e The Throwable containing the exception that was raised
     * @return The String containing the stack traces of the raised exception
     */
    @Nullable
    public static String getStackTrace(Throwable e) {
        if (e == null) {
            return null;
        }

        // Iterate on causes recursively
        JSONArray array = new JSONArray();
        Throwable previous = null; // Prevent infinite loops
        for (Throwable t = e; t != null && t != previous; t = t.getCause()) {
            for (final StackTraceElement element : t.getStackTrace()) {
                array.put(element.toString());
            }
            previous = t;
        }
        return array.toString();
    }

    /**
     * Check whether a Throwable is related to Facebook SDK by looking at iterated
     * stack traces and return true if one of the traces has prefix "com.facebook".
     *
     * @param e The Throwable containing the exception that was raised
     * @return Whether the raised exception is related to Facebook SDK
     */
    public static boolean isSDKRelatedException(@Nullable  Throwable e) {
        if (e == null) {
            return false;
        }

        // Iterate on causes recursively
        Throwable previous = null; // Prevent infinite loops
        for (Throwable t = e; t != null && t != previous; t = t.getCause()) {
            for (final StackTraceElement element : t.getStackTrace()) {
                if (element.getClassName().startsWith(FBSDK_PREFIX)) {
                    return true;
                }
            }
            previous = t;
        }
        return false;
    }

    /**
     * Get the list of crash report files from instrument report directory defined in
     * {@link InstrumentUtility#getInstrumentReportDir()} method.
     *
     * Note that the function should be called after FacebookSdk is initialized. Otherwise,
     * exception FacebookSdkNotInitializedException will be thrown.
     *
     * @return  The list of crash report files
     */
    public static File[] listCrashReportFiles() {
        final File reportDir = getInstrumentReportDir();
        if (reportDir == null) {
            return new File[]{};
        }

        File[] reports = reportDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(String.format("^%s[0-9]+.json$", CRASH_REPORT_PREFIX));
            }
        });
        return (null != reports ? reports : new File[]{});
    }

    /**
     * Read the content from the file which is denoted by filename and the directory is the
     * instrument report directory defined in {@link InstrumentUtility#getInstrumentReportDir()}
     * method.
     *
     * Note that the function should be called after FacebookSdk is initialized. Otherwise,
     * exception FacebookSdkNotInitializedException will be thrown.
     */
    @Nullable
    public static JSONObject readFile(@Nullable String filename, boolean deleteOnException) {
        final File reportDir = getInstrumentReportDir();
        if (reportDir == null || filename == null) {
            return null;
        }

        final File file = new File(reportDir, filename);
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
            String content = Utility.readStreamToString(inputStream);
            return new JSONObject(content);
        } catch (Exception e) {
            if (deleteOnException) {
                deleteFile(filename);
            }
        }
        return null;
    }

    /**
     * Write the content to the file which is denoted by filename and the file will be put in
     * instrument report directory defined in {@link InstrumentUtility#getInstrumentReportDir()}
     * method.
     *
     * Note that the function should be called after FacebookSdk is initialized. Otherwise,
     * exception FacebookSdkNotInitializedException will be thrown.
     */
    public static void writeFile(@Nullable String filename, @Nullable String content) {
        final File reportDir = getInstrumentReportDir();
        if (reportDir == null || filename == null || content == null) {
            return;
        }

        final File file = new File(reportDir, filename);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (Exception e) {
            /* no op */
        }
    }

    /**
     * Deletes the cache file under instrument report directory. If the instrument report
     * directory exists and the file exists under the directory, the file will be deleted.
     *
     * Note that the function should be called after FacebookSdk is initialized. Otherwise,
     * exception FacebookSdkNotInitializedException will be thrown.
     *
     * @return  Whether the file is successfully deleted
     */
    public static boolean deleteFile(@Nullable String filename) {
        final File reportDir = getInstrumentReportDir();
        if (reportDir == null || filename == null) {
            return false;
        }
        File file = new File(reportDir, filename);
        return file.delete();
    }

    /**
     * Create Graph Request for Instrument reports and send the reports to Facebook.
     */
    public static void sendReports(String key, JSONArray reports, GraphRequest.Callback callback) {
        if (reports.length() == 0) {
            return;
        }

        final JSONObject params = new JSONObject();
        try {
            params.put(key, reports.toString());
        } catch (JSONException e) {
            return;
        }

        final GraphRequest request = GraphRequest.newPostRequest(null, String.format("%s" +
                "/instruments", FacebookSdk.getApplicationId()), params, callback);
        request.executeAsync();
    }

    /**
     * Get the instrument directory for report if the directory exists. If the directory doesn't
     * exist, will attempt to create the directory. Note that, the instrument directory is under
     * cache directory of the Application.
     *
     * Note that the function should be called after FacebookSdk is initialized. Otherwise,
     * exception FacebookSdkNotInitializedException will be thrown.
     *
     * @return  The instrument cache directory if and only if the directory exists or it's
     *          successfully created, otherwise return null.
     */
    @Nullable
    public static File getInstrumentReportDir() {
        final File cacheDir = FacebookSdk.getApplicationContext().getCacheDir();
        final File dir = new File(cacheDir, INSTRUMENT_DIR);
        if (dir.exists() || dir.mkdirs()) {
            return dir;
        }
        return null;
    }
}
