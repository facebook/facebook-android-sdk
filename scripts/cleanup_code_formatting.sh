#!/bin/sh
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

# This script runs a few shell commands for additional code formatting cleanup

# cd up one level if run from the scripts dir
CURRENT_DIR_NAME="${PWD##*/}"
if [ "$CURRENT_DIR_NAME" = "scripts" ]; then
  cd ..
fi

# Convert functions that return a single expression to single-expression functions
# https://kotlinlang.org/docs/functions.html#single-expression-functions
find . -name '*.kt' -print0 | xargs -0 perl -0777 -pi -e 's/(fun.*) {\n\s*return ([\w\.]+)\n\s*}/\1 = \2/g'

# Ensure a blank line above the package statement
find . -name '*.kt' -print0 | xargs -0 perl -0777 -pi -e 's/( \*\/)\n(package)/\1\n\n\2/g'
