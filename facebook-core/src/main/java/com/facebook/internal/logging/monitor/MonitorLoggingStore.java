/**
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

package com.facebook.internal.logging.monitor;

import android.content.Context;

import com.facebook.FacebookSdk;
import com.facebook.internal.Utility;
import com.facebook.internal.logging.ExternalLog;
import com.facebook.internal.logging.LoggingStore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MonitorLoggingStore will read/write logs from/to the disk.
 * When the app is stopped or crashed, the logs in the LoggingCache will be written into the disk.
 *
 * Considering the data we collect are not such important so far, we will discard the logs which
 * are send back to our server not successfully including the error of non-network, in order to
 * reduce the usage of storage.
 *
 * We will fetch the logs in the disk and send them to the server once Monitor is enabled.
 */
public class MonitorLoggingStore implements LoggingStore {
    private static MonitorLoggingStore monitorLoggingStore;
    public static final String PERSISTED_LOGS_FILENAME = "facebooksdk.monitoring.persistedlogs";

    private MonitorLoggingStore() {
    }

    public synchronized static MonitorLoggingStore getInstance() {
        if (monitorLoggingStore == null) {
            monitorLoggingStore = new MonitorLoggingStore();
        }
        return monitorLoggingStore;
    }

    /**
     * The file will be deleted no matter if the logs are fetched from the file successfully,
     * to avoid that we re-send the same logs
     * @return logs fetched from the disk
     */
    @Override
    public List<ExternalLog> readAndClearStore() {
        List<ExternalLog> logs = new ArrayList<>();
        Context context = FacebookSdk.getApplicationContext();
        ObjectInputStream ois = null;
        try {
            InputStream is = context.openFileInput(PERSISTED_LOGS_FILENAME);
            ois = new ObjectInputStream(new BufferedInputStream(is));
            logs = (List<ExternalLog>) ois.readObject();
        } catch (Exception e) {
            // swallow Exception to avoid user's app to crash
        } finally {
            Utility.closeQuietly(ois);

            try {
                context.getFileStreamPath(PERSISTED_LOGS_FILENAME).delete();
            } catch (Exception e) {
                // swallow Exception to avoid user's app to crash
            }
        }
        return logs;
    }

    /**
     * We will delete the file if there is any exception thrown.
     * @param logs List of Externallog which should be written into disk
     */
    @Override
    public void saveLogsToDisk(List<ExternalLog> logs) {
        ObjectOutputStream oos = null;
        Context context = FacebookSdk.getApplicationContext();

        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(
                            context.openFileOutput(PERSISTED_LOGS_FILENAME, 0)));
            oos.writeObject(logs);
        } catch (Exception e) {
            // delete the file anyway, since saving to the disk failed
            try {
                context.getFileStreamPath(PERSISTED_LOGS_FILENAME).delete();
            } catch (Exception innerException) {
                // swallow Exception to avoid user's app to crash
            }
        } finally {
            Utility.closeQuietly(oos);
        }
    }
}
