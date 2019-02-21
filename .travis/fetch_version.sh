# Copyright 2015-present Facebook. All Rights Reserved.


VERSION_CLASS=facebook-core/src/main/java/com/facebook/FacebookSdkVersion.java
CURRENT_VERSION=$(sed -n 's/.*BUILD = "\(.*\)\";/\1/p' $VERSION_CLASS)
