Facebook SDK for Android
========================
[![Run testsuite with gradle](https://github.com/facebook/facebook-android-sdk/actions/workflows/verifybuild.yml/badge.svg)](https://github.com/facebook/facebook-android-sdk/actions/workflows/verifybuild.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.facebook.android/facebook-android-sdk/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.facebook.android/facebook-android-sdk)

This library allows you to integrate Facebook into your Android app.

Learn more about the provided samples, documentation, integrating the SDK into your app, accessing source code, and more at https://developers.facebook.com/docs/android

:wave: The SDK team is eager to learn from you! Fill out [this survey](https://facebook.co1.qualtrics.com/jfe/form/SV_2hJ13Imkq1YF9Sm?TrackID=GitHub) to tell us what’s most important to you and how we can improve.

TRY IT OUT
----------
1. Check-out the tutorials available online at https://developers.facebook.com/docs/android/getting-started
2. Start coding! Visit https://developers.facebook.com/docs/android/ for tutorials and reference documentation.

FEATURES
--------
* [Login](https://developers.facebook.com/docs/facebook-login)
* [Sharing](https://developers.facebook.com/docs/sharing)
* [Messenger](https://developers.facebook.com/docs/messenger-expressions)
* [App Links](https://developers.facebook.com/docs/applinks)
* [Analytics](https://developers.facebook.com/docs/analytics)
* [Graph API](https://developers.facebook.com/docs/android/graph)
* [Marketing](https://developers.facebook.com/docs/app-events/marketing-kit)

STRUCTURE
---------
The SDK is separated into modules with the following structure.

    +----------------------------------------------------+
    |                                                    |
    | Facebook-android-sdk                               |
    |                                                    |
    +----------------------------------------------------+
    +----------+ +----------+ +------------+ +-----------+
    |          | |          | |            | |           |
    | Facebook | | Facebook | | Facebook   | | Facebook  |
    | -Login : | | -Share   | | -Messenger | | -Applinks |
    |          | |          | |            | |           |
    +----------+ +----------+ |            | |           |
    +-----------------------+ |            | |           |
    |                       | |            | |           |
    | Facebook-Common       | |            | |           |
    |                       | |            | |           |
    +-----------------------+ +------------+ +-----------+
    +----------------------------------------------------+
    |                                                    |
    | Facebook-Core                                      |
    |                                                    |
    +----------------------------------------------------+

USAGE
-----
Facebook SDKs are broken up into separate modules as shown above. To ensure the most optimized use of
space only install the modules that you intend to use. To get started, see the Installation section below.

Any Facebook SDK initialization must occur only in the main process of the app. Use of Facebook SDK in processes other than the main process is not supported and will likely cause problems.


INSTALLATION
------------
Facebook SDKs are published to Maven as independent modules. To utilize a feature listed above
include the appropriate dependency (or dependencies) listed below in your `app/build.gradle` file.
```gradle
dependencies {
    // Facebook Core only (Analytics)
    implementation 'com.facebook.android:facebook-core:latest.release'

    // Facebook Login only
    implementation 'com.facebook.android:facebook-login:latest.release'

    // Facebook Share only
    implementation 'com.facebook.android:facebook-share:latest.release'

    // Facebook Messenger only
    implementation 'com.facebook.android:facebook-messenger:latest.release'

    // Facebook App Links only
    implementation 'com.facebook.android:facebook-applinks:latest.release'
    
    // Facebook Marketing only
    implementation 'com.facebook.android:facebook-marketing:latest.release'

    // Facebook Android SDK (everything)
    implementation 'com.facebook.android:facebook-android-sdk:latest.release'
}
```

You may also need to add the following to your project/build.gradle file.
```gradle
buildscript {
    repositories {
        mavenCentral()
    }
}
```

GIVE FEEDBACK
-------------
Please report bugs or issues to https://developers.facebook.com/bugs/

You can also visit our [Facebook Developer Community Forum](https://developers.facebook.com/community/),
join the [Facebook Developers Group on Facebook](https://www.facebook.com/groups/fbdevelopers/),
ask questions on [Stack Overflow](http://facebook.stackoverflow.com),
or open an issue in this repository.

SECURITY
--------
See the [SECURITY POLICY](SECURITY.md) for more info on our bug bounty program.

CONTRIBUTING
-------------
We are able to accept contributions to the Facebook SDK for Android. To contribute please do the following.
- Follow the instructions in the [CONTRIBUTING.md](https://github.com/facebook/facebook-android-sdk/blob/main/CONTRIBUTING.md).
- Submit your pull request to the [main](https://github.com/facebook/facebook-android-sdk/tree/main) branch. This allows us to merge your change into our internal main and then push out the change in the next release.

LICENSE
-------
Except as otherwise noted, the Facebook SDK for Android is licensed under the Facebook Platform License (https://github.com/facebook/facebook-android-sdk/blob/main/LICENSE.txt).

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.

DEVELOPER TERMS
---------------

- By enabling Facebook integrations, including through this SDK, you can share information with Facebook, including information about people’s use of your app. Facebook will use information received in accordance with our Data Use Policy (https://www.facebook.com/about/privacy/), including to provide you with insights about the effectiveness of your ads and the use of your app.  These integrations also enable us and our partners to serve ads on and off Facebook.

- You may limit your sharing of information with us by updating the Insights control in the developer tool (https://developers.facebook.com/apps/[app_id]/settings/advanced).

- If you use a Facebook integration, including to share information with us, you agree and confirm that you have provided appropriate and sufficiently prominent notice to and obtained the appropriate consent from your users regarding such collection, use, and disclosure (including, at a minimum, through your privacy policy). You further agree that you will not share information with us about children under the age of 13.

- You agree to comply with all applicable laws and regulations and also agree to our Terms (https://www.facebook.com/policies/), including our Platform Policies (https://developers.facebook.com/policy/) and Advertising Guidelines, as applicable (https://www.facebook.com/ad_guidelines.php).

By using the Facebook SDK for Android you agree to these terms.
