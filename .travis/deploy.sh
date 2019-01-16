# Copyright 2015-present Facebook. All Rights Reserved.

# shellcheck disable=SC2086
# shellcheck disable=SC2006

VERSION_CLASS=facebook-core/src/main/java/com/facebook/FacebookSdkVersion.java
id1=`git log -n 1 --pretty=format:%H -- $VERSION_CLASS`
id2=`git log -n 1 --pretty=format:%H`

if [ "$id1" == "$id2" ]; then
  FB_SRC_FOLDERS=(
    'facebook-core'
    'facebook-common'
    'facebook-login'
    'facebook-share'
    'facebook-places'
    'facebook-messenger'
    'facebook-applinks'
    'facebook-marketing'
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

else
  echo 'No version update for this commit.';
fi
