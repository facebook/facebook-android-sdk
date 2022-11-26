#!/bin/sh
#
# Copyright (c) Meta Platforms, Inc. and affiliates.
# All rights reserved.
#
# This source code is licensed under the license found in the
# LICENSE file in the root directory of this source tree.

# This script runs a few shell commands for additional code formatting cleanup

read -r -d '' LICENSE_STRING << EOM
/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */
EOM

read -r -d '' XML_LICENSE_STRING << EOM
<!--
    Copyright (c) Meta Platforms, Inc. and affiliates.
    All rights reserved.

    This source code is licensed under the license found in the
    LICENSE file in the root directory of this source tree.
-->
EOM

# cd up one level if run from the scripts dir
CURRENT_DIR_NAME="${PWD##*/}"
if [ "$CURRENT_DIR_NAME" = "scripts" ]; then
  cd ..
fi

# Ensure every java and kotlin file has the correct license header (also adds missing header)
git ls-files '*.java' '*.kt' | xargs perl -0777 -pi -e "s|.*?package|$LICENSE_STRING\n\npackage|s"

# Fix copyright statement for kts files
git ls-files '*.kts' | xargs perl -0777 -pi -e "s|.*?(import\|plugins)|$LICENSE_STRING\n\n\1|s"

# Fix copyright statement for xml files
git ls-files '*.xml' | xargs perl -0777 -pi -e "s|<!--.+?Copyright.+?-->\n+|$XML_LICENSE_STRING\n\n|s"
