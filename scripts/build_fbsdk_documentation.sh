#!/bin/bash
#
# Copyright (c) Meta Platforms, Inc. and affiliates.
# All rights reserved.
#
# This source code is licensed under the license found in the
# LICENSE file in the root directory of this source tree.

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
