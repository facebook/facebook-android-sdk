This open source Java library allows you to integrate Facebook into your Android application.

Except as otherwise noted, the Facebook Connect Android SDK is licensed under the Apache Licence, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.html)

Alpha Status
============

This is an _alpha_ release. In order to guide the development of the library and allow you to freely inspect and use the source, we have open sourced the library. The underlying APIs are generally stable, however we may make changes to the library in response to developer feedback.

Getting Started
===============

The SDK is lightweight and has no external dependencies. Getting started is quick and easy.

Install necessary packages
--------------------------

* Follow the (http://developer.android.com/sdk/index.html)[Android SDK Getting Started Guide].

* Pull this repository from github

     git clone git@github.com:facebook/android-sdk.git

* Import the Facebook SDK project into your Eclipse workspace. 
  * Open the __File__ menu, click on __Import...__ and choose __Existing project into workspace__ under the General group. 
  * Select the __facebook__ subdirectory from within the git repository. 
  * You should see an entry for __FacebookSDK__ listed under __Projects__. Click __Finish__.

* To ensure Eclipse can build the project, you will have to define the ANDROID_DIR build path variable. 
  * Right click on the project, select __Build Path->Configure Build Path...__.
  * In the __Java Build Path__ panel, select the __Libraries__ tab, and click __Add Variable..._.
  * In the popup, click on __Configure Variables...__ and then __New...__
  * In the 'name' field enter __ANDROID_JAR__ and in the 'path' field click on __File...__ and select the android.jar file from the Android SDK directory on your local machine.

__NOTE: If you run into trouble, add the android.jar file directly to the project's build path.__

The SDK is now configured and ready to go.

Run the sample application
--------------------------

To test the SDK, you should run the simple sample application included.

* Import the sample application project into your Eclipse workspace.
  * Import as above, but choose the __examples/simple__ subdirectory from within the git repository.
  * You should see an entry for FacebookSDK-example.

Create your own application
---------------------------

* Create a Facebook Application: http://www.facebook.com/developers/createapp.php
* Check out the mobile documentation: http://developers.facebook.com/docs/guides/mobile/

Usage
=====

With the Android SDK, you can do three main things:

* Authorize users to your application
* Make API requests
* Display a Facebook dialog

Authentication and Authorization
-----

Authorization and login use the same method. By default, if you pass no ''permissions'' parameter, then you will get access to the user's general information.
This includes their name, profile picture, list of friends and other general information. For more information, see http://developers.facebook.com/docs/authentication/.

If you pass in extra permissions in the permissions parameter, then you will see it.

This SDK uses the (http://tools.ietf.org/html/draft-ietf-oauth-v2)["user-agent"] flow from OAuth 2.0 for authentication.

To authorize a user, the simplest usage is:

     facebook = new Facebook();
     facebook.authorize(context, applicationId, new LoginDialogListener());

See the sample applications for more specific code samples.

When the user wants to stop using Facebook integration with your application, you can call the logout method to clear all application state.

Accessing the Graph API
-----------------------

The (http://developers.facebook.com/docs/api)[Facebook Graph API] presents a simple, consistent view of the Facebook social graph, uniformly representing objects in the graph (e.g., people, photos, events, and fan pages) and the connections between them (e.g., friend relationships, shared content, and photo tags).

You can access the Graph API by passing the Graph Path to the ''request'' method. For example, to access information about the logged in user, call

    request('/me')               // get information about the currently logged in user
    request('/platform/posts')   // get the posts made by the "platform" page
    request('/me/friends')       // get the logged-in user's friends

The request call is synchronous, meaning it will block your thread. If you want to make it non-blocking, you can initialize it in a separate thread. For example:

    new Thread() {
      @Override public void run() {
         String resp = request(graphPath, parameters, httpMethod);
	 handleResponse(resp);
      }
    }.start();

The (http://developers.facebook.com/docs/reference/rest/)[Old REST API] is also supported. To access the older methods, pass in the named parameters and method name as a dictionary Bundle.
See the docblock for the request method for more details.

Publishing to the user's Wall
------------------------------

Facebook allows user interaction for various purposes, including:

* Publish to the user's stream
* Share a story with a friend

To do this, use the ''dialog'' method.

Error Handling
--------------

For synchronous methods (request), errors are thrown by exception. For the asynchronous methods (dialog, authorize), errors are passed to the onException method of the listener class.
