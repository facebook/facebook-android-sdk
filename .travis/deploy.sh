cp secring.gpg facebook-core/
./gradlew uploadArchives -p facebook-core -PossrhUsername=${NEXUS_USERNAME} -PossrhPassword=${NEXUS_PASSWORD} -Psigning.keyId=${GPG_KEY_ID} -Psigning.password=${GPG_KEY_PASSPHRASE} -Psigning.secretKeyRingFile=secring.gpg
rm facebook-core/secring.gpg
