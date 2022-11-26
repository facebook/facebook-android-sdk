#!/bin/sh
#
# Copyright (c) Meta Platforms, Inc. and affiliates.
# All rights reserved.
#
# This source code is licensed under the license found in the
# LICENSE file in the root directory of this source tree.


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
