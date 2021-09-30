#!/bin/bash
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

# This script builds the API documentation from source-level comments.

pushd "$(dirname ${BASH_SOURCE[0]})" > /dev/null || exit 1
FB_SDK_SCRIPT=$(pwd)
popd >/dev/null || exit 1
FB_SDK_ROOT=$(dirname "$FB_SDK_SCRIPT")

DOC_OUT_FOLDER="$FB_SDK_ROOT/out/docs"
mkdir -p "$DOC_OUT_FOLDER"

cd "$FB_SDK_ROOT" || exit 1
./gradlew facebook:dokkaJavadoc
cd "$FB_SDK_ROOT/facebook/build/dokka/" || exit 1
cp -r javadoc "$DOC_OUT_FOLDER/facebook"
cd "$DOC_OUT_FOLDER" || exit 1
zip --quiet --recurse-paths facebook.zip facebook

echo ""
echo "Generated docs in $DOC_OUT_FOLDER/facebook"
echo "... zipped as $DOC_OUT_FOLDER/facebook.zip"
echo ""
