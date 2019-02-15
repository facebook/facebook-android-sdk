# Copyright 2015-present Facebook. All Rights Reserved.

# shellcheck disable=SC2086
# shellcheck disable=SC2006

VERSION_CLASS=facebook-core/src/main/java/com/facebook/FacebookSdkVersion.java

VERSION=$(sed -n 's/.*BUILD = "\(.*\)\";/\1/p' $VERSION_CLASS)

GIT_TAG=$(git describe --abbrev=0 --tags | cut -d'-' -f 3)
echo $VERSION
echo $GIT_TAG

if [ "$VERSION" == "$GIT_TAG" ]; then
  echo 'versions are updated, no need to add new tag.'
else
  echo 'versions are not updated, start to add new tag.'

  git config --global user.email "builds@travis-ci.com"
  git config --global user.name "Travis CI"
  UPDATE_TAG="sdk-version-$VERSION-test"
  echo $UPDATE_TAG
  git tag $UPDATE_TAG -a -m "test version"
  git push --quiet https://$api_key@github.com/facebook/facebook-android-sdk $UPDATE_TAG
fi
