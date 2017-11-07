/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
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
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.plugin.ui;

import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;

import javax.swing.*;

public class FacebookCoreCodeForm extends CodeFormContainer {

    private JPanel mainPanel;
    private ScrollablePanel purchaseCodePanel;
    private ScrollablePanel appEventsLoggerPanel;
    private ScrollablePanel logEventPanel;
    private ScrollablePanel bundleCodePanel;

    FacebookCoreCodeForm() {
        setCodeForm("logger.logPurchase(BigDecimal.valueOf(4.32), Currency.getInstance(\"USD\"));",
                purchaseCodePanel);

        setCodeForm("AppEventsLogger logger = AppEventsLogger.newLogger(this);",
                appEventsLoggerPanel);

        setCodeForm("logger.logEvent(AppEventsConstants.EVENT_NAME_{XXXXX});\n" +
                        "\nlogger.logEvent(\"battledAnOrc\");",
                logEventPanel);

        setCodeForm("Bundle parameters = new Bundle();\n" +
                "parameters.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, \"USD\");\n" +
                "parameters.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, \"product\");\n" +
                "parameters.putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, \"HDFU-8452\");\n" +
                "\n" +
                "logger.logEvent(AppEventsConstants.EVENT_NAME_ADDED_TO_CART,\n" +
                "                54.23,\n" +
                "                parameters);",
                bundleCodePanel);
    }

    @Override
    public JComponent getComponent() {
        return this.mainPanel;
    }
}
