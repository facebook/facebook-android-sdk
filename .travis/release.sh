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

# shellcheck disable=SC2148
# shellcheck disable=SC2086
# shellcheck disable=SC2154
# shellcheck disable=SC2006
# shellcheck disable=SC2034

main() {
  VERSION_CLASS=facebook-core/src/main/java/com/facebook/FacebookSdkVersion.java

  local command_type="$1"
  shift

  case "$command_type" in
    "tag-current-version") tag_current_version "$@" ;;
    "deploy-to-maven") deploy_to_maven "$@" ;;
  esac
}

tag_current_version() {
  VERSION=$(sed -n 's/.*BUILD = "\(.*\)\";/\1/p' $VERSION_CLASS)
  GIT_RAW_TAG=$(git describe --abbrev=0 --tags)

  if [[ $GIT_RAW_TAG == sdk-version* ]]; then
    # like `sdk-version-4.40.0`
    GIT_TAG=$(echo $GIT_RAW_TAG | cut -d'-' -f3-)
  else
    # like `v4.40.0`
    GIT_TAG=${GIT_RAW_TAG:1}
  fi

  if [ "$VERSION" == "$GIT_TAG" ]; then
    echo 'versions are updated, no need to add new tag.'
  else
    echo 'versions are not updated, start to add new tag.'
    if ! is_valid_semver "$VERSION"; then
      echo "This version isn't a valid semantic versioning"
       exit 1
    fi

    UPDATE_TAG="sdk-version-$VERSION"

    git tag $UPDATE_TAG -a -m "test version" || die "get error when add new tag"
    if [ "$1" == "--push" ]; then
      echo 'push tag...'
      git push origin $UPDATE_TAG
    fi
  fi
}

# deploy new release
deploy_to_maven() {
  openssl aes-256-cbc -K $encrypted_e83d0815cd6c_key -iv $encrypted_e83d0815cd6c_iv -in secring.gpg.enc -out secring.gpg -d

  id1=`git log -n 1 --pretty=format:%H -- $VERSION_CLASS`
  id2=`git log -n 1 --pretty=format:%H`


  FB_SRC_FOLDERS=(
    'facebook-core'
    'facebook-common'
    'facebook-login'
    'facebook-share'
    'facebook-places'
    'facebook-messenger'
    'facebook-applinks'
     # 'facebook-marketing'
    'facebook'
  )
  for (( i = 0; i < ${#FB_SRC_FOLDERS[@]}; i++ ))
  do
    FOLDER=${FB_SRC_FOLDERS[$i]}
    echo "Publishing $FOLDER SDK to maven central";
    cp secring.gpg $FOLDER/
    ./gradlew uploadArchives -p $FOLDER -PossrhUsername=${NEXUS_USERNAME} -PossrhPassword=${NEXUS_PASSWORD} -Psigning.keyId=${GPG_KEY_ID} -Psigning.password=${GPG_KEY_PASSPHRASE} -Psigning.secretKeyRingFile=secring.gpg || die "Failed to publish $FOLDER SDK to maven central"
    rm $FOLDER/secring.gpg
  done
  rm secring.gpg
}

# Proper Semantic Version
is_valid_semver() {
  if ! [[ "$1" =~ ^([0-9]{1}|[1-9][0-9]+)\.([0-9]{1}|[1-9][0-9]+)\.([0-9]{1}|[1-9][0-9]+)($|[-+][0-9A-Za-z+.-]+$) ]]; then
    false
    return
  fi
}

die() {
  echo ""
  echo "FATAL: $*" >&2
  exit 1
}

main "$@"
