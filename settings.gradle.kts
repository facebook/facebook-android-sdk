/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// Facebook SDK
include(":facebook-testutil")

include(":facebook-core")

include(":facebook-bolts")

include(
    ":facebook-common",
    ":facebook-login",
    ":facebook-share",
    ":facebook-applinks",
    ":facebook-messenger")

include(":facebook-livestreaming") // @fb-only

include(":facebook-beta") // @fb-only

include(":facebook-gamingservices")

include(":facebook")

// Samples
include(":samples:HelloFacebookSample")

include(":samples:Iconicus")

include(":samples:LoginSample") // @fb-only

include(":samples:Scrumptious")

include(":samples:FBLoginSample")

include(":samples:KotlinSampleApp")

if (file("internal/internal-settings.gradle").exists()) {
  apply("internal/internal-settings.gradle")
}

if (file("local.gradle").exists()) {
  apply("local.gradle")
}

project(":facebook-beta").projectDir = File("internal/facebook-beta") // @fb-only
