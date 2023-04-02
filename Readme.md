
Your account has been flagged.
Because of that, your profile is hidden from the public. If you believe this is a mistake, contact support to have your account status reviewed.
facebook
/
facebook-android-sdk
Public
Used to integrate Android apps with Facebook Platform.

developers.facebook.com/docs/android
License
 View license
 5.8k stars  3.7k forks
Code
Issues
60
Pull requests
2
Actions
Projects
Security
Insights
facebook/facebook-android-sdk
Latest commit
@KylinChang
@facebook-github-bot
KylinChang and facebook-github-bot
…
2 weeks ago
Git stats
Files
README.md
Facebook SDK for Android
Run testsuite with gradle Maven Central

This library allows you to integrate Facebook into your Android app.

Learn more about the provided samples, documentation, integrating the SDK into your app, accessing source code, and more at https://developers.facebook.com/docs/android

👋 The SDK team is eager to learn from you! Fill out this survey to tell us what’s most important to you and how we can improve.

TRY IT OUT
Check-out the tutorials available online at https://developers.facebook.com/docs/android/getting-started
Start coding! Visit https://developers.facebook.com/docs/android/ for tutorials and reference documentation.
FEATURES
Login
Sharing
Messenger
App Links
Analytics
Graph API
Marketing
STRUCTURE
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
Facebook SDKs are broken up into separate modules as shown above. To ensure the most optimized use of space only install the modules that you intend to use. To get started, see the Installation section below.

Any Facebook SDK initialization must occur only in the main process of the app. Use of Facebook SDK in processes other than the main process is not supported and will likely cause problems.

INSTALLATION
Facebook SDKs are published to Maven as independent modules. To utilize a feature listed above include the appropriate dependency (or dependencies) listed below in your app/build.gradle file.

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

    // Facebook Android SDK (everything)
    implementation 'com.facebook.android:facebook-android-sdk:latest.release'
}
You may also need to add the following to your project/build.gradle file.

buildscript {
    repositories {
        mavenCentral()
    }
}
GIVE FEEDBACK
Please report bugs or issues to https://developers.facebook.com/bugs/

You can also visit our Facebook Developer Community Forum, join the Facebook Developers Group on Facebook, ask questions on Stack Overflow, or open an issue in this repository.

SECURITY
See the SECURITY POLICY for more info on our bug bounty program.

CONTRIBUTING
We are able to accept contributions to the Facebook SDK for Android. To contribute please do the following.

Follow the instructions in the CONTRIBUTING.md.
Submit your pull request to the main branch. This allows us to merge your change into our internal main and then push out the change in the next release.
LICENSE
Except as otherwise noted, the Facebook SDK for Android is licensed under the Facebook Platform License (https://github.com/facebook/facebook-android-sdk/blob/main/LICENSE.txt).

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

DEVELOPER TERMS
By enabling Facebook integrations, including through this SDK, you can share information with Facebook, including information about people’s use of your app. Facebook will use information received in accordance with our Data Use Policy (https://www.facebook.com/about/privacy/), including to provide you with insights about the effectiveness of your ads and the use of your app. These integrations also enable us and our partners to serve ads on and off Facebook.

You may limit your sharing of information with us by updating the Insights control in the developer tool (https://developers.facebook.com/apps/[app_id]/settings/advanced).

If you use a Facebook integration, including to share information with us, you agree and confirm that you have provided appropriate and sufficiently prominent notice to and obtained the appropriate consent from your users regarding such collection, use, and disclosure (including, at a minimum, through your privacy policy). You further agree that you will not share information with us about children under the age of 13.

You agree to comply with all applicable laws and regulations and also agree to our Terms (https://www.facebook.com/policies/), including our Platform Policies (https://developers.facebook.com/policy/) and Advertising Guidelines, as applicable (https://www.facebook.com/ad_guidelines.php).

By using the Facebook SDK for Android you agree to these terms.
