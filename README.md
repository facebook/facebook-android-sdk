This open source Java library allows you to integrate Facebook into your Android application. Except as otherwise noted, the Facebook Android SDK is licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html)

Getting Started
===============

See our [Android SDK Getting Started Guide](http://developers.facebook.com/docs/guides/mobile#android)

Sample Applications
===============

This library includes two sample applications to guide you in development.

* __simple__: A bare-bones app that demonstrates authorization, making API calls, and invoking a dialog.

* __stream__: This slightly beefier application lets you view your news feed.

To install a sample application into Eclipse (3.5):

* Create the sample application in your workspace:
2. Select __File__ -> __New__ -> __Project__, choose __Android Project__, and then click __Next__.
  3. Select "Create project from existing source".
  4. Choose either __examples/simple__ or __examples/stream__. You should see the project properties populated.
  5. Click Finish to continue.

* Build the project: from the Project menu, select "Build Project".

* Run the application: from the Run menu, select "Run Configurations...".  Under Android Application, you can create a new run configuration: give it a name and select the simple Example project; use the default activity Launch Action.  See http://developer.android.com/guide/developing/eclipse-adt.html#RunConfig for more details.


Testing
===============

Here are some tips to help test your application:

* You will need to have the Facebook application in your test environment. The SDK includes a developer release of the Facebook application that can be side-loaded for testing purposes. On an actual device, you can just download the latest version of the app from the Android Market, but on the emulator you will have to install it yourself:

      adb install FBAndroid.apk

* Use a signed build. You can sign with a debug key, but make sure that the key you used to sign matches the __Key Hash__ field in the Facebook developer settings.

* Make sure to test both with and without the Facebook application. The SDK will fall back to a Webview if the Facebook app is not installed.

* You can use this [guide to developing on a device](http://developer.android.com/guide/developing/device.html).

Debugging
==========

Here's a few common errors and their solutions.

* __Build error: "missing gen files".__

  This should go away when you rebuild the project. If you have trouble, try running __Clean...__ from the __Project__ menu.

* __Error: "invalid_key"__

  This error means that the Facebook server doesn't recognize your Android key hash. Make sure that you correctly generated and copy/pasted your key hash into the Facebook developer settings console (http://www.facebook.com/developers/apps.php), and make sure that your application has been signed with the same key you registered with Facebook.

* __Dialog won't load or shows a blank screen.__

  This can be tricky to debug. If the logs don't give an indication of what's wrong, I suggest installing tcpdump on your device and getting a trace. Tutorial: http://www.vbsteven.be/blog/android-debugging-inspectin-network-traffic-with-tcpdump/

  If you still can't tell what's going on, then file an issue and please include the HTTP trace.

* __I can't upload photos with photos.upload.__

  Make sure the Bundle value for the photo parameter is a byte array.

Report Issues/Bugs
===============
[http://bugs.developers.facebook.net/enter_bug.cgi?product=SDKs](http://bugs.developers.facebook.net/enter_bug.cgi?product=SDKs)
