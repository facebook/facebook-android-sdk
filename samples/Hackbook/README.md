This 'Hackbook for Android' app includes Single Sign On implementation (SSO), sample API calls and Graph API Explorer and is targeted towards android developers who want to make their apps social using Facebook Social Graph. The Code provided here is to showcase how to implement the SSO and make the API calls. If you have any questiosn or comments related to this sample app, please post them here - http://facebook.stackoverflow.com/questions/tagged/hackbook-for-android

Getting Started
===============

See Android tutorial - https://developers.facebook.com/docs/mobile/android/build/

Configuring the app
===============

1. Launch Eclipse
2. Ensure you have installed the Android plugin.
3. Create Facebook SDK Project - follow the Step-1 instructions in the tutorial
4. Create the Hackbook Project :
   4.1 Select __File__ -> __New__ -> __Project__, choose __Android Project__, and then click __Next__.
   4.2 Select "Create project from existing source".
   4.3. Choose  examples/Hackbook. You should see the project properties populated.
   4.4. Click Finish to continue.
5. Add reference to the Facebook SDK - follow the Step-3 instructions in the tutorial
6. Create a Facebook app if you don't have one already and add the app id in the Hackbook.java->APP_ID

Build the app
===============

7. And you are done and ready to compile the project:
   7.1 From the Project menu, select "Build Project".

Run the app
===============

8. Hopefully project would compile fine. Next run the app on the emulator or on the phone (See http://developer.android.com/guide/developing/eclipse-adt.html#RunConfig for more details.)
   8.1 If you plan to run on emulator, ensure you have created an Android Virtual Device (AVD):
		8.1.1 Go to Window -> Android SDK and AVD Manager -> Click New
		8.1.2 Provide a Name (AVD 2.3 e.g.) and choose the Target (Android 2.3 if available).
		8.1.3 Click 'Create AVD' at the bottom and that should create an AVD which you can run the app on described  next.

   8.2 Go to Run->Run Configurations->Android Application->create a new run configuration by clicking the icon with + button on it.
   8.3 Name it 'Hackbook'
   8.4 Under the Project, Browse and choose Hackbook
   8.5 Go to Target tab -> Choose manual if you wish to run on the phone, else choose Automatic and select an AVD created in step 8.1
   8.6 Click Run and your 'Hackbook for Android' app should be up and running.


Installing the Facebook app
===============

You will need to have the Facebook application on the handset or the emulator to test the Single Sign On. The SDK includes a developer release of the Facebook application that can be side-loaded for testing purposes. On an actual device, you can just download the latest version of the app from the Android Market, but on the emulator you will have to install it yourself:

      adb install FBAndroid.apk

What's in there
===============

Note: The source tags are provided through out the code base to facilitate easy search for the relevant code. Do a project-wide search for the source tags to get straight to the relevant code. Refer below for source tags for each feature.

1. Login button - This uses SSO to authorize the app. Clicking on Login should activate SSO (if the app is installed) or show OAuth dialog. When authorizing, no permissions are requested and the app will get basic permission by default.

Source Tag - login_tag

- Hackbook.java - this layout the login button and initialize it. Since this is also the calling acitivty, this overrides the onActivityResult() method.
- LoginButton.java - this calls the mFb.authorize(mActivity, mPermissions, mActivityCode, new LoginDialogListener()) which authorizes the app via SSO or OAuth.
- SessionStore.java - stores the access token and access expiry time for future app launch. This is important that you save the access token, else user will need to authorize your app each time they launch it which is annoying and user is likely to churn out.
- SessionEvents.java - Authorization state tracker, calls the listener on login/logout success or error.
------------------------

2. Update Status - this allows user to update his status by calling the 'feed' dialog. More info on feed dialog - https://developers.facebook.com/docs/reference/dialogs/feed/

Source Tag - update_status_tag, view_post_tag, delete_post_tag

- Hackbook.java - Case 0: update status by calling the 'feed' dialog.
- UpdateStatusResultDialog.java - shows the object-id returned in the dialog response. You can view or delete the post here.
------------------------

3. App Requests - this allows to send app requests to user's friends by calling the 'apprequests' dialog. More info - https://developers.facebook.com/docs/reference/dialogs/requests/

Source Tag - app_requests_tag

- Hackbook.java - Case 1: send the app requests by calling the the 'apprequests' dialog.
------------------------

4. Get Friends - Get user's friends via Graph API or FQL Query. User must be logged-in to use this. Also post on a friend's wall by clicking on his name in the list.

Source Tag - get_friends_tag, friend_wall_tag

- Hackbook.java - Case 2:  Use Graph API 'me/friends' which returns friends sorted by UID, currently it's not possible to sort any other way in the Graph API. Use the FQL Query to sort by name - select name, current_location, uid, pic_square from user where uid in (select uid2 from friend where uid1=me()) order by name
- FriendsList.java - displays the friends profile pic and names as returned by the api. Also post on friend's wall by clicking on the friend.
- FriendsModel.java - run async tasks to fetch the profile picture limited to 15 tasks at any given time.

5. Upload Photo - Upload a photo either from the media gallery or from a remote server. You require 'photo_upload' to upload photos on user profile.

Source Tag - upload_photo, view_photo_tag, tag_photo_tag

- Hackbook.java - Case 3: Photo is uploaded by posting byte-array or url to me/photos endpoint. Media Gallery is launched by invoking the MediaStore.Images.Media.EXTERNAL_CONTENT_URI intent and overriding the OnActivityResult() to get the picture from the media gallery. Photo from remote server is uploaded by simply providing the image url in the 'url' param in the graph.facebook.com/me/photos endpoint.
- UploadPhotoResultDialog.java - shows the object-id returned after uploaded the photo. You can view or tag the photo here.
------------------------


6. Place Check-in - Fetch user's current location or use Times Square as the current location and get nearby places which user can check-in at.

Source Tag - fetch_places_tag, check_in_tag

- Hackbook.java - Case 4: Ask to fetch current location or use Times Square as the current location.
- Places.java - Get user's current location and fetch the nearby places by calling the graph.facebook.com/search?type=places&distance=1000&center=lat,lon. Check-in user at a place by calling the graph.facebook.com/me/checkins&place=<place_id_>&message=<message>&coordinates={"latitude": <lat>, "longitude:": <lon>}
------------------------

7. Run FQL Query - Type and run any FQL Query and see the results.

Source Tag - fql_query_tag

- FQLQuery.java - Layout the FQL Query Dialog and run the query and show the results.
------------------------

8. Graph API Explorer - Explore user's social graph, see his and friends' connections and get new permissions. This is similar to the Graph Explorer dev tool - http://developers.facebook.com/tools/explorer/. The ObjectIDs in the API response are linkified and can be clicked to fetch object specific data.
  - Click the 'x' button to clear the textfield
  - Click the green up arrow button in the textfield to get the 'me' object data.
  - Click on 'Get Permissions' to get new permissions including user's, his friends or extended permissions.
  - Click on 'Fields/Connections' to see current object's fields and connections.
  - On the Fields & Connections dialog, in the Fields tab, select the fields to view or in the Connections tab, click the connection to view it's content.

Source Tag - graph_explorer

- Hackbook.java - Case 5: Launch the GraphExplorer intent
- GraphExplorer.java - Layout and execute the graph explorer
- IntentUriHandler.java - Handle the fbgraphex: schema generated while linkifying the Object IDs in the graph explorer output



Report Issues/Bugs
===============
Please report issues here - http://facebook.stackoverflow.com/questions/tagged/hackbook-for-android
