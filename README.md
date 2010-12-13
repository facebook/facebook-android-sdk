This open source Java library allows you to integrate Facebook into your Android application. Except as otherwise noted, the Facebook Android SDK is licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html)

Getting Started
===============

The SDK is lightweight and has no external dependencies. Getting started is easy.

Setup your environment
--------------------------

1. Pull the repository from GitHub:

    git clone git://github.com/facebook/facebook-android-sdk.git

2. If you have not already done so, follow the (http://developer.android.com/sdk/index.html)[Android SDK Getting Started Guide]. You will need the device emulator and debugging tools.

3. The Facebook Android SDK works fine in any Android development environment. To build in Eclipse:

  * Create a new project for the Facebook SDK in your Eclipse workspace. 
  * Select __File__ -> __New__ -> __Project__, choose __Android Project__ (inside the Android folder), and then click __Next__.
  * Select "Create project from existing source".
  * Select the __facebook__ subdirectory from within the git repository. You should see the project properties populated (you might want to change the project name to something like "FacebookSDK").
  * Click Finish to continue.

The Facebook SDK is now configured and ready to go.  

Sample Applications
--------------------

This library includes two sample applications to guide you in development.

* __simple__: A bare-bones app that demonstrates authorization, making API calls, and invoking a dialog.

![simple](http://sphotos.ak.fbcdn.net/hphotos-ak-snc4/hs935.snc4/74854_172094179474325_109700069047070_612899_4026782_n.jpg)

* __stream__: This slightly beefier application lets you view your news feed.

![stream](http://sphotos.ak.fbcdn.net/hphotos-ak-snc4/hs827.snc4/68752_172094172807659_109700069047070_612898_4403693_n.jpg)

To install a sample application into Eclipse (3.5):

* Create the sample application in your workspace:
2. Select __File__ -> __New__ -> __Project__, choose __Android Project__, and then click __Next__.
  3. Select "Create project from existing source".
  4. Choose either __examples/simple__ or __examples/stream__. You should see the project properties populated.
  5. Click Finish to continue.

* Build the project: from the Project menu, select "Build Project".

* Run the application: from the Run menu, select "Run Configurations...".  Under Android Application, you can create a new run configuration: give it a name and select the simple Example project; use the default activity Launch Action.  See http://developer.android.com/guide/developing/eclipse-adt.html#RunConfig for more details.

Integrate with an existing application
-----------

The easiest way to get started is to copy/hack up the sample applications (that's what they are there for). However, if you want to just integrate the Facebook SDK with an existing application (or create a new one from scratch), then you should:

* Add a dependency on the Facebook Android SDK library on your application:
  1. Select __File__ -> __Properties__. Open the __Android__ section within the Properties dialog.
  2. In the bottom __Library__ section, click __Add...__ and select the Facebook SDK project.
  3. Any issues? Check [Android documentation](http://developer.android.com/guide/developing/eclipse-adt.html#libraryProject)

* Ensure that your application has network access (android.permission.INTERNET) in the Android manifest:

	<code><uses-permission android:name="android.permission.INTERNET"></uses-permission></code>

* Register your application with Facebook:
  1. Create a new Facebook application: http://www.facebook.com/developers/createapp.php . If you already have a canvas or web application, you can use the same application ID.
  2. Set your application's name and picture. This is what users will see when they authorize your application.

Set up single sign-on
-----------

Optionally, you can make your login system more seamless by incorporating single sign-on.

* Register your application's Android key hash. This is used by Facebook to ensure that another app can't impersonate your app when talking to the Facebook Android app.

  1. Generate the key hash: <pre><code>keytool -exportcert -alias [alias] -keystore [keystore]
  | openssl sha1 -binary
  | openssl base64</code></pre>

  2. In the Facebook developer settings, go to the __Mobile and Devices__ tab.
  3. In the __Android__ section, enter the key hash in the __Key Hash__ field.

![keyhash](http://sphotos.ak.fbcdn.net/hphotos-ak-snc4/hs992.snc4/76543_172095306140879_109700069047070_612908_7263236_n.jpg)

* Insert a call to the authorizeCallback() method at the top of your Activity's onActivityResult() function. (If onActivityResult doesn't already exist, then create it)

      @Override
      public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebook.authorizeCallback(requestCode, resultCode, data);
        // ... anything else your app does onActivityResult ...
      }

Testing
-------

Here are some tips to help test your application:

* You will need to have the Facebook application in your test environment. The SDK includes a developer release of the Facebook application that can be side-loaded for testing purposes. On an actual device, you can just download the latest version of the app from the Android Market, but on the emulator you will have to install it yourself:

      adb install FBAndroid.apk

* Use a signed build. You can sign with a debug key, but make sure that the key you used to sign matches the __Key Hash__ field in the Facebook developer settings.

* Make sure to test both with and without the Facebook application. The SDK will fall back to a Webview if the Facebook app is not installed.

* You can use this [guide to developing on a device](http://developer.android.com/guide/developing/device.html).

Usage
=====

Begin by instantiating the Facebook object:

    facebook = new Facebook(applicationId);

The Facebook object lets you do three major things:

* __Authentication and Authorization__: prompt users to log in to facebook and grant permissions to your application.
* __Making API Calls__: fetch user profile data (such as name and profile pic), as well as info about a user's friends.
* __Display a dialog__: interact with user via a WebView. You primarily use this to publish to a user's feed without requiring upfront permissions.

Authentication and Authorization
--------------------------------

#### Making the authorize request

To login the current user, call the authorize() method. By default, your application can read the user's basic information, which includes their name, profile picture, list of friends, and other information that they have made public.

    facebook.authorize(context, new AuthorizeListener());

Private user information is protected by [a set of granular permissions](http://developers.facebook.com/docs/authentication/permissions). If you want to access private information, use the authorize() method to request permission:

    facebook.authorize(context, 
                       String[] {"offline_access","user_photos"},
                       new AuthorizeListener())

You should use one of the buttons provided in the images/buttons/ directory to direct the user to login.

#### Login process

If the user has installed and is logged into the latest Facebook application on their device, then they will be directed to the Facebook app to grant permissions. If the user is not logged in, then they will need to do that first. If the Facebook application is not installed at all, then the Facebook Android SDK will gracefully fall back to a WebView-based flow that requires username/password.

#### Handle the authorize response

Your application handles the response with the __onComplete__ method of a __DialogListener__ object.

    class AuthorizeListener implements DialogListener {
      public void onComplete(Bundle values) {
       //  Handle a successful login
      }
    }

Check out the sample listeners for more details on the DialogListener interface.

### Logging out

When the user wants to stop using Facebook integration with your application, you can call the logout method to clear all application state and invalidate the current OAuth token.

     facebook.logout(context);

Making API calls
-----------------------

#### Graph API

The [Facebook Graph API](http://developers.facebook.com/docs/api) presents a simple, consistent view of the Facebook social graph, uniformly representing objects in the graph (e.g., people, photos, events, and fan pages) and the connections between them (e.g., friend relationships, shared content, and photo tags).

You can access the Graph API by passing the Graph Path to the ''request'' method. For example, to access information about the logged in user, call

    facebook.request("me");               // get information about the currently logged in user
    facebook.request("platform/posts");   // get the posts made by the "platform" page
    facebook.request("me/friends");       // get the logged-in user's friends

Because the request call is synchronous (meaning it will block the calling thread), it should not be called from the main (UI) thread in Android. To make it non-blocking, you can make the request in a separate or background thread. For example:

    new Thread() {
      @Override public void run() {
         String resp = request("me");
	 handleResponse(resp);
      }
    }.start();

See the AsyncFacebookRunner class and sample application for examples of making asynchronous requests.

#### Response format 

The server response is a JSON string.  The SDK provides a Util.parseJson() method to convert this to a JSONObject, whose fields and values can be inspected and accessed.  The sample implementation checks for a variety of error conditions and raises JSON or Facebook exceptions if the content is invalid or includes an error generated by the server.  Advanced applications may wish to provide their own parsing and error handling. 

#### Old REST API

The [Old REST API](http://developers.facebook.com/docs/reference/rest/) is also supported. To access the older methods, pass in the named parameters and method name as a dictionary Bundle.

    Bundle parameters = new Bundle();
    parameters.putString("method", "auth.expireSession");
    String response = request(parameters);

See the comments on the __request__ method for more details.

<a name="dialogs"/>
Display a Dialog
----------------------

This SDK provides a method for popping up a Facebook dialog for user interaction. This is useful if you want to publish to a user's feed without requesting a bunch of permissions first.

To invoke a dialog:

    facebook.dialog(context, 
                    "feed", 
                    new SampleDialogListener()); 


Error Handling
--------------

For synchronous methods (request), errors are thrown by exception. For the asynchronous methods (dialog, authorize), errors are passed to the onException methods of the listener callback interface.

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
