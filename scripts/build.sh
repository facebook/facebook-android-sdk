#!/bin/bash
#
# Copyright (c) Meta Platforms, Inc. and affiliates.
# All rights reserved.
#
# This source code is licensed under the license found in the
# LICENSE file in the root directory of this source tree.
#
# Build SDK modules by short name.
# Usage:
#   ./scripts/build.sh           # all modules
#   ./scripts/build.sh core      # facebook-core only
#   ./scripts/build.sh login     # facebook-login only

set -e
cd "$(dirname "$0")/.."

MODULE="$1"

if [ -z "$MODULE" ]; then
    echo "Building all modules..."
    ./gradlew assemble
    exit $?
fi

# Map short names to Gradle module paths
case "$MODULE" in
    # SDK library modules
    core)             GRADLE_MODULE=":facebook-core" ;;
    common)           GRADLE_MODULE=":facebook-common" ;;
    login)            GRADLE_MODULE=":facebook-login" ;;
    share)            GRADLE_MODULE=":facebook-share" ;;
    applinks)         GRADLE_MODULE=":facebook-applinks" ;;
    messenger)        GRADLE_MODULE=":facebook-messenger" ;;
    gamingservices)    GRADLE_MODULE=":facebook-gamingservices" ;;
    gaming)           GRADLE_MODULE=":facebook-gamingservices" ;;
    bolts)            GRADLE_MODULE=":facebook-bolts" ;;
    marketing)        GRADLE_MODULE=":facebook-marketing" ;;
    testutil)         GRADLE_MODULE=":facebook-testutil" ;;
    all|facebook)     GRADLE_MODULE=":facebook" ;;
    # Sample / test apps
    hackbook|hb4a)    GRADLE_MODULE=":internal:testing:hb4a" ;;
    hello)            GRADLE_MODULE=":samples:HelloFacebookSample" ;;
    coffeeshop)       GRADLE_MODULE=":internal:testing:CoffeeShop" ;;
    fblogin)          GRADLE_MODULE=":samples:FBLoginSample" ;;
    kotlinsample)     GRADLE_MODULE=":samples:KotlinSampleApp" ;;
    *)
        echo "Unknown module: $MODULE"
        echo "SDK: core, common, login, share, applinks, messenger, gamingservices, bolts, marketing, testutil, all"
        echo "Apps: hackbook, hello, coffeeshop, fblogin, kotlinsample"
        exit 1
        ;;
esac

echo "Building $GRADLE_MODULE..."
./gradlew "${GRADLE_MODULE}:assemble"
