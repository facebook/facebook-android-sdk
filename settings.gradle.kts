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

// Facebook SDK
include(":facebook-testutil")
include(":facebook-core")
include(":facebook-bolts")
include(":facebook-common", ":facebook-login", ":facebook-share", ":facebook-applinks", ":facebook-messenger")
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

if (file("internal/internal-settings.gradle").exists()) {
    apply("internal/internal-settings.gradle")
}

if (file("local.gradle").exists()) {
    apply("local.gradle")
}
project(":facebook-beta").projectDir = File("internal/facebook-beta") // @fb-only
