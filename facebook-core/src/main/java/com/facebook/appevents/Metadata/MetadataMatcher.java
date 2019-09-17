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

package com.facebook.appevents.Metadata;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import com.facebook.appevents.codeless.internal.ViewHierarchy;

import java.util.ArrayList;
import java.util.List;

final class MetadataMatcher {
    private static final String TAG = MetadataMatcher.class.getCanonicalName();
    private static final int MAX_TEXT_LENGTH = 100;
    private static final int MAX_INDICATOR_LENGTH = 100;

    static boolean match(MatcherInput input, MetadataRule rule) {
        return matchValue(input.text, rule.getValRule()) &&
                matchIndicator(input.indicators, rule.getKeyRules());
    }

    private static List<String> getIndicators(View view) {
        List<String> indicators = new ArrayList<>();
        // Hint
        indicators.add(ViewHierarchy.getHintOfView(view));
        // tag
        Object tag = view.getTag();
        if (tag != null) {
            indicators.add(tag.toString());
        }
        // description
        CharSequence description = view.getContentDescription();
        if (description != null) {
            indicators.add(description.toString());
        }
        // resource id name
        try {
            if (view.getId() != -1) {
                // resource name format: {package_name}:id/{id_name}
                String resourceName = view.getResources().getResourceName(view.getId());
                String[] splitted = resourceName.split("/");
                if (splitted.length == 2) {
                    indicators.add(splitted[1]);
                }
            }
        } catch (Resources.NotFoundException _e) {/*no op*/}

        List<String> validIndicators = new ArrayList<>();
        for (String indicator : indicators) {
            if (!indicator.isEmpty() && indicator.length() <= MAX_INDICATOR_LENGTH) {
                validIndicators.add(indicator.toLowerCase());
            }
        }
        return validIndicators;
    }

    private static boolean matchIndicator(List<String> indicators, List<String> keys) {
        for (String indicator : indicators) {
            for (String key : keys) {
                if (indicator.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchValue(String text, String rule) {
        return text.matches(rule);
    }

    static class MatcherInput {
        String text;
        boolean isValid = true;
        private List<String> indicators;

        MatcherInput(TextView view) {
            text = view.getText().toString().trim();
            if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH) {
                isValid = false;
                return;
            }
            indicators = getIndicators(view);
            if (indicators.isEmpty()) {
                isValid = false;
                return;
            }

            text = text.toLowerCase();
        }
    }
}
