VERSION_CLASS=facebook-core/src/main/java/com/facebook/FacebookSdkVersion.java
id1=`git log -n 1 --pretty=format:%H -- $VERSION_CLASS`
id2=`git show -n 1 --pretty=format:%H`
if [ "$id1" == "$id2" ];
then
  echo 'Start update to new version.'
  cp secring.gpg facebook-core/
  ./gradlew uploadArchives -p facebook-core -PossrhUsername=${NEXUS_USERNAME} -PossrhPassword=${NEXUS_PASSWORD} -Psigning.keyId=${GPG_KEY_ID} -Psigning.password=${GPG_KEY_PASSPHRASE} -Psigning.secretKeyRingFile=secring.gpg
  rm facebook-core/secring.gpg
else
  echo 'No version update for this commit.'
fi
