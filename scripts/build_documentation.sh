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

# This script builds the API documentation from source-level comments.

# Utility functions.
function die() {
  echo ""
  echo "FATAL: $*" >&2
  exit 1
}

# Make sure javadoc is installed.
hash javadoc >/dev/null || die 'Javadoc is not installed!'

# The directory containing this script
# We need to go there and use pwd so these are all absolute paths
pushd "$(dirname $BASH_SOURCE[0])" > /dev/null
FB_SDK_SCRIPT=$(pwd)
popd >/dev/null

# The root directory where the Facebook SDK for android is cloned
FB_SDK_ROOT=$(dirname "$FB_SDK_SCRIPT")

# Path to java source file under each kit folder
PATH_TO_SRC='src/main/java'

# mkdir -p $FB_SDK_ROOT/out
mkdir -p $FB_SDK_ROOT/out/docs

# The tmp directory where source files can processed
DOCS_TMP_DIR=out/docs/tmp

# Source folders for Facebook
FB_SRC_FOLDERS=(
  'facebook'
  'facebook-core'
  'facebook-common'
  'facebook-login'
  'facebook-share'
  'facebook-places'
  'facebook-messenger'
  'facebook-applinks'
  'facebook-gamingservices'
)

  echo ""
  echo "Preparing source files for docs generation"
  echo ""

# 'Flattens' Facebook sources into a single folder.
FB_SRC_DEST=$FB_SDK_ROOT/$DOCS_TMP_DIR/facebook/$PATH_TO_SRC
mkdir -p $FB_SRC_DEST

for (( i = 0; i < ${#FB_SRC_FOLDERS[@]}; i++ ))
do
  FB_SRC=${FB_SRC_FOLDERS[$i]}

  FB_SRC_FOLDER=$FB_SDK_ROOT/$FB_SRC/$PATH_TO_SRC

  cp -rn $FB_SRC_FOLDER/ $FB_SRC_DEST
done

# Source folders of each kit
KIT_SRC_FOLDERS=(
  $DOCS_TMP_DIR/facebook
  'accountkit/accountkitsdk'
  'ads/AdsApi'
)

# Output doc folders for each kit
KIT_DOC_FOLDERS=(
  'facebook'
  'accountkit'
  'audiencenetwork'
)

for (( i = 0; i < ${#KIT_SRC_FOLDERS[@]}; i++ ))
do
  KIT_SRC=${KIT_SRC_FOLDERS[$i]}
  KIT_DOC=${KIT_DOC_FOLDERS[$i]}

  SRC_FOLDER=$FB_SDK_ROOT/$KIT_SRC/$PATH_TO_SRC 
  DOC_FOLDER=$FB_SDK_ROOT/out/docs/$KIT_DOC
  LOG_FILE=$FB_SDK_ROOT/out/docs/$KIT_DOC.log

  # Find all the facebook packages except internal ones
  # Then use javadoc to generate docs for those packages
  grep --recursive --no-filename -Po '(?<=package )com\.facebook.*?(?=;)' $SRC_FOLDER | sort | uniq | grep -v internal \
    | xargs javadoc -quiet -d $DOC_FOLDER -sourcepath $SRC_FOLDER &> $LOG_FILE

  cd $FB_SDK_ROOT/out/docs
  zip --quiet --recurse-paths $DOC_FOLDER.zip $KIT_DOC

  echo ""
  echo "Generated docs in $DOC_FOLDER"
  echo "... zip as $DOC_FOLDER.zip"
  echo "... see log file at $LOG_FILE"
  echo ""
done

# Clean up tmp directory
rm -rf ${FB_SDK_ROOT:?}/$DOCS_TMP_DIR

