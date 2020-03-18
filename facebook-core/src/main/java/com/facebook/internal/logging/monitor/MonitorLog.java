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

import android.os.Build;
import android.support.annotation.Nullable;

import com.facebook.FacebookSdk;
import com.facebook.internal.logging.ExternalLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_DEVICE_MODEL;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_DEVICE_OS_VERSION;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_EVENT_NAME;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_SAMPLE_APP_INFO;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_TIME_SPENT;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_TIME_START;

/**
 *  MonitorLog will be sent to the server via our Monitor.
 *  MonitorLog must have an event to indicate the specific tracked feature/function.
 *  New MonitorEvent type should be added in MonitorEvent class.
 */
public class MonitorLog implements ExternalLog {

    private MonitorEvent event;
    private long timeStart;
    private int timeSpent;
    private String sampleAppInformation;
    private String deviceOSVersion;
    private String deviceModel;

    // Lazily initialized hashcode.
    private int hashCode;

    private static final int INVALID_TIME = -1;
    private static final String SAMPLE_APP_FBLOGINSAMPLE = "com.facebook.fbloginsample";
    private static final String SAMPLE_APP_HELLOFACEBOOK = "com.example.hellofacebook";
    private static final String SAMPLE_APP_MESSENGERSENDSAMPLE = "com.facebook.samples.messenger.send";
    private static final String SAMPLE_APP_RPSSAMPLE = "com.example.rps";
    private static final String SAMPLE_APP_SHAREIT = "com.example.shareit";
    private static final String SAMPLE_APP_SWITCHUSERSAMPLE = "com.example.switchuser";

    private static Set<String> sampleAppSet;

    static {
        sampleAppSet = new HashSet<>();
        sampleAppSet.add(SAMPLE_APP_FBLOGINSAMPLE);
        sampleAppSet.add(SAMPLE_APP_HELLOFACEBOOK);
        sampleAppSet.add(SAMPLE_APP_MESSENGERSENDSAMPLE);
        sampleAppSet.add(SAMPLE_APP_RPSSAMPLE);
        sampleAppSet.add(SAMPLE_APP_SHAREIT);
        sampleAppSet.add(SAMPLE_APP_SWITCHUSERSAMPLE);
    }

    public MonitorLog(LogBuilder logBuilder) {
        this.deviceOSVersion = Build.VERSION.RELEASE;
        this.deviceModel = Build.MODEL;
        this.event = logBuilder.event;
        this.timeStart = logBuilder.timeStart;
        this.timeSpent = logBuilder.timeSpent;
        String packageName = FacebookSdk.getApplicationContext().getPackageName();
        if (packageName != null && isSampleApp(packageName)) {
            sampleAppInformation = packageName;
        }
    }

    public String getDeviceOSVersion() {
        return this.deviceOSVersion;
    }

    public String getDeviceModel() {
        return this.deviceModel;
    }

    public MonitorEvent getEvent() {
        return this.event;
    }

    public long getTimeStart() {
        return this.timeStart;
    }

    public int getTimeSpent() {
        return this.timeSpent;
    }

    public String getSampleAppInformation() {
        return this.sampleAppInformation;
    }

    public static class LogBuilder {
        private MonitorEvent event;
        private Long timeStart;
        private Integer timeSpent;

        public LogBuilder(MonitorEvent event) {
            this.event = event;
        }

        public LogBuilder timeStart(long timeStart) {
            this.timeStart = timeStart;
            return this;
        }

        public LogBuilder timeSpent(int timeSpent) {
            this.timeSpent = timeSpent;
            return this;
        }

        public MonitorLog build() {
            MonitorLog monitorLog = new MonitorLog(this);
            validateMonitorLog(monitorLog);
            return monitorLog;
        }

        private void validateMonitorLog(MonitorLog monitorLog) {
            // if the time is a negative value, we will set it to -1 to indicate it's an invalid value
            if (timeSpent < 0) {
                monitorLog.timeSpent = INVALID_TIME;
            }

            if (timeStart < 0) {
                monitorLog.timeStart = INVALID_TIME;
            }
        }
    }

    private static boolean isSampleApp(String packageName) {
        return sampleAppSet.contains(packageName);
    }

    @Override
    public String toString() {
        String format = ": %s";
        return String.format(
                PARAM_DEVICE_OS_VERSION + format + ", "
                        + PARAM_DEVICE_MODEL + format + ", "
                        + PARAM_EVENT_NAME + format + ", "
                        + PARAM_SAMPLE_APP_INFO + format + ", "
                        + PARAM_TIME_START + format + ", "
                        + PARAM_TIME_SPENT + format,
                deviceOSVersion,
                deviceModel,
                event,
                sampleAppInformation,
                timeStart,
                timeSpent);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 31 * result + (deviceOSVersion != null ? deviceOSVersion.hashCode() : 0);
            result = 31 * result + (deviceModel != null ? deviceModel.hashCode() : 0);
            result = 31 * result + (event != null ? event.hashCode() : 0);
            result = 31 * result + (sampleAppInformation != null ? sampleAppInformation.hashCode() : 0);
            result = 31 * result + (int) (timeStart ^ (timeStart >>> 32));
            result = 31 * result + (timeSpent ^ (timeSpent >>> 32));
            hashCode = result;
        }
        return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MonitorLog other = (MonitorLog) obj;
        return deviceOSVersion.equals(other.deviceOSVersion)
                && deviceModel.equals(other.deviceModel)
                && event == other.event
                && timeStart == other.timeStart
                && timeSpent == other.timeSpent
                && ((sampleAppInformation == null && other.sampleAppInformation == null)
                || (sampleAppInformation.equals(other.sampleAppInformation)));
    }

    @Override
    public JSONObject convertToJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put(PARAM_DEVICE_OS_VERSION, deviceOSVersion);
            object.put(PARAM_DEVICE_MODEL, deviceModel);
            object.put(PARAM_EVENT_NAME, event.getName());

            if (timeStart != 0) {
                object.put(PARAM_TIME_START, timeStart);
            }
            if (timeSpent != 0) {
                object.put(PARAM_TIME_SPENT, timeSpent);
            }
            if (sampleAppInformation != null) {
                object.put(PARAM_SAMPLE_APP_INFO, sampleAppInformation);
            }

            return object;
        } catch (JSONException e) {
            /* no op */
        }
        return null;
    }
}
