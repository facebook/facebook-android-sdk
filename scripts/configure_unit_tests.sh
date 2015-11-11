#!/bin/sh
#
# Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
#
# You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
# copy, modify, and distribute this software in source code or binary form for use
# in connection with the web services and APIs provided by Facebook.
#
# As with any software that integrates with the Facebook platform, your use of
# this software is subject to the Facebook Developer Principles and Policies
# [http://developers.facebook.com/policy/]. This copyright notice shall be
# included in all copies or substantial portions of the software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
# FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
# IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# this script configures your Android simulator for unit tests
# Note: On Mac OS X, an easy way to generate a MACHINE_UNIQUE_USER_TAG is with the following:
#   system_profiler SPHardwareDataType | grep -i "Serial Number (system):" | awk '{print $4}'

cd $(dirname $0)/..
FB_SDK_ROOT=$(pwd)
FB_SDK_TESTS=$FB_SDK_ROOT/facebook/src/androidTest

if [ "$#" -lt 3 ]; then
      echo "Usage: $0 APP_ID APP_SECRET CLIENT_TOKEN [MACHINE_UNIQUE_USER_KEY]"
      echo "  APP_ID                   your unit-testing Facebook application's App ID"
      echo "  APP_SECRET               your unit-testing Facebook application's App Secret"
      echo "  CLIENT_TOKEN             your unit-testing Facebook application's client token"
      echo "  MACHINE_UNIQUE_USER_TAG  optional text used to ensure this machine will use its own set of test users rather than sharing"
      die 'Arguments do not conform to usage'
fi

function write_config_json {
    CONFIG_JSON_FILE="$FB_SDK_TESTS"/assets/config.json

    mkdir -p "$FB_SDK_TESTS"/assets

    # use heredoc syntax to output the json
    cat > "$CONFIG_JSON_FILE" \
<<DELIMIT
{"applicationId":"$1","applicationSecret":"$2","clientToken":"$3","machineUniqueUserTag":"$4"}
DELIMIT
# end heredoc

    echo "wrote unit test config file at $CONFIG_JSON_FILE"
}

write_config_json $1 $2 $3
