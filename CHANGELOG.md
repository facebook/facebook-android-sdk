# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Removed
- Removed deprecated `ShareOpenGraphAction`, `ShareOpenGraphContent`, `ShareOpenGraphValueContainer` and `ShareOpenGraphObject` classes.

## [13.2.0]
### Added
- Added `LoginManager.createLogInActivityResultContract()` and `FacebookDialog.createActivityResultContractForShowingDialog()` methods to expose the activity result contracts.
- Support for CloudBridge for AppEvents. This feature is available for selected partners now.

### Fixed
- The client token is now mandatory. SDK initialization will have a check on it and throw out an exception if the client token is not available.

## [13.1.0]
### Added
- All classes from the deprecated [Bolts](https://github.com/BoltsFramework/Bolts-Android) library are moved to an independent `facebook-bolts` kit. Now all kits depend on it to keep the back-compatibility, but we will gradually remove the dependency.
### Fixed
- `AppEventsLogger.setUserID()` allows to receive null value to clear the user ID.
- Fixed the crashes if WebView is not available.

### Deprecated
- Deprecate `ShareOpenGraphAction`, `ShareOpenGraphContent`, `ShareOpenGraphValueContainer` and `ShareOpenGraphObject` since Open Graph stories will only render as plan link shares. See [Open Graph Stories document](https://developers.facebook.com/docs/sharing/opengraph) for more details.

## [13.0.0]

### Added
- Set Java source and target compatibility to 1.8 (Java 8). All apps that integrate with Facebook Android SDK should also set source and compatibility to 1.8 or above.
- GMS AD_ID Permission is added to the SDK by default for requesting the advertising ID. You can follow [this guide](https://support.google.com/googleplay/android-developer/answer/6048248) to remove the permission request.

### Removed
- Removed the support for tokenless requests. Now all Graph API requests must be associated with an access token and the app needs to have a client token to construct the access token without a logged in user. Please follow [this document](https://developers.facebook.com/docs/android/getting-started/#add-app_id) to add the client token into `AndroidManifest.xml`.
- Removed deprecated LikeView and various related classes (LikeButton, LikeContent, LikeDialog etc)
- Removed deprecated Device Sharing classes (DeviceShareButton, DeviceShareDialog, DeviceShareDialogFragment)
- Removed deprecated Messenger Sharing classes (ShareMessengerMediaTemplateContent, ShareMessengerGenericTemplateContent etc)
- Removed deprecated Fblite Login
- Removed deprecated PerformanceGuardian
- Removed end-to-end dumpsys utility for testing

### Fixed
- Fixed the bug of handling sharing result if the post id is not available.

## [12.3.0]

### Added
- Avoid tokenless requests if the client token is presented. Tokenless request support will be removed in v13.0.

### Deprecated
- Fblite Login is deprecated.
- Internal utility methods on collections are deprecated and will be replaced by `kotlin.collections`.

### Fixed
- Fixed reauthorization of data access.
- Add fields parameters for login GET requests to remove warning messages.

## [12.2.0]

### Added
- Added parsing entry intent to look for gaming payload and tournament data for user coming from FB surface. Gaming tournament feature is available for selected partners now.
- Added data processing options to crash/error report.

### Fixed
- On Android 12 and above, AppEvents will send attribution ID and anonymous ID if GMS is supported and isTrackingLimited is OFF. On Android 11 and below, nothing is changed. This feature will be turned on after Google Play Store starts the enforcement of the new policy.

## [12.1.0]

### Fixed
- Fixed a bug that causes multiple login intents to launch when the user has previously authorized the app and is logging in with the `openid` scope.

### Added
- Adds Tournaments for native games, this feature is only available for select partners.

## [12.0.1]

### Added
- Added AuthenticationToken Cache to enhance OIDC experience. NOTE: This feature is currently in beta testing and not broadly available to developers

## [12.0.0]

### Changed
- `com.facebook.bolts.AppLinkResolver` will only receive nonnull parameters and return nonnull values. `com.facebook.bolts.Capture` is removed.
- Removed deprecated functions in `com.facebook.appevents.AppEventsLogger`
- Removed deprecated function `ViewIndexer.sendToServerUnity(String)`
- Login and share kits now fully supports AndroidX Activity Result APIs. No need to override the `onActivityResult()` if you're passing activity/fragment from `androidx.activity` v1.2.0+ or `androidx.fragment` v1.3.0+ as the context to `LoginButton` and `ShareButton`.

### Added
- Added the ability to perform login based on OpenID Connect standard. NOTE: This feature is currently in beta testing and not broadly available to developers

## [11.3.0]

### Added
- Add Custom Updates for Cloud Games
### Fixed
- Fixed Login Connect support in CCT and Webview

## [11.2.0]

### Added
- Access token and profile management for Instagram users, support for querying graph.instagram.com, and Instagram login dialogs. NOTE: These changes support a feature currently in alpha testing and not broadly available to developers.

### Fixed
- Fixed the crash when calling `AppEventsLoggerImpl.augmentWebView(WebView, Context)`.

## [11.1.1]

### Fixed
- Fixed the bug in `GraphRequest` when passing an attachment through POST method.
- Fixed the race condition in cloud game.
- Fixed the bug of accessing WindowManager from a non-visual context on Android 11.

## [11.1.0]

### Added
- Validating the package checksum's providers when requesting it on Android S+.
- Application Not Response (ANR) detector for improving the SDK quality. It will only collect ANR that caused by Facebook SDK and can be turned off by disabling automatically logged events. More details: https://developers.facebook.com/docs/app-events/getting-started-app-events-android/#auto-events

## [11.0.0]

### Added
- Released `user_messenger_contact` permission to enable Login Connect with Messenger. This new feature allows people to opt in to being contacted by a business on Messenger following the FB Login flow. Learn more at https://developers.facebook.com/docs/facebook-login/login-connect
- Support Android S+ checksum API for computing the package checksum if it’s available.
- Expanded support for auto-logging of in-app purchases to support apps that use V2 and/or V3 of the Google Play Billing Library


### Changed
- The default Graph API version is updated to v11.0
- Removed deprecated signing key hashes for validating signatures
- In `AccessToken` , `token`, `applicationId`, `userId` and `permissions` fields became nonnull.
- The constructors of `GraphResponse` now require a nonnull `GraphRequest` object. If the constructor is called with a `JSONArray` or an error, the array/error should also be nonnull.
- In `GraphRequest`, The parameter of `Callback.onComplete()` is declared nonnull. The parameters of `GraphJSONArrayCallback` and `GraphJSONObjectCallback` are declared nullable.

### Fixed
- Fixed NPE on calling  `AppEventsLogger.activateApp(Context context)`
- Removed the dependencies on deprecated `kotlin-android-extension` library

### Deprecated
- `GraphRequest#setSkipClientToken()`


## [9.1.1]

## Changed
- Supported refetching Gate Keeper values after switching app IDs.
- Fixed POM validation error in staging phase.
- Fixed IAP enable logic

## [9.1.0]

## Added
- GameRequests has a new UI for Gaming Domain Apps.
- Added a GamingPayload class that can retrieve Payloads sent from our messenger XMAs.

## [9.0.0]

### Changed

- The default Graph API version is updated to v9.0

## [8.2.0]

## Added

- Added SDK on-device signal processing capabilities, but feature is not yet active

## [8.1.0]

## Changed

- Mitigations for switching to Facebook app for logging in, due to Android 11 package visibility change

## [8.0.0]

**Note: The current version uses Graph API v8.0. To use the latest Graph API version, please specific that version in your GraphRequest call.**

## Added

- Added Performance Guardian to improve the performance of Suggested Events and Codeless
- Added ReferralManager for integrating with web Referral dialog

## Changed

- Updated tokenless profile picture API call

## Fixed

- Fixed callback issues for GamingImageUploader

## Deprecated

- Deprecated GameRequestDialog
- Deprecated DeviceShareDialog

## [7.1.0]

## Added

- Introduce DataProcessingOptions

### Deprecated

- Remove UserProperties API

## [7.0.1]
- AAM improvement
- Corrected the report type of Crash Reports
- Handle RejectedExecutionException in ViewIndexer.schedule()
- Fixed the exception in getRootView
- Fixed model cache issue

## [7.0.0]

### Added
- Android X, to use v7 please upgrade to Android X. Big thanks to @sunyal for helping with the migration.

### Deprecated
- Marketing kit is deprecated

## [6.5.1]

### Fixed
- Fixed AppLinkData issue: [issue 761](https://github.com/facebook/facebook-android-sdk/issues/761)
- Fixed timestamp issue of Model Delivery

## [6.5.0]

## Added
- More usecase for Integrity is supported.

### Fixed
- Fixed bugs for suggested events

## [6.4.0]

## Added

- FBSDKMessageDialog now accepts FBSDKSharePhotoContent.

### Fixed
- Fixed crash in Codeless

## [6.3.0]

### Added
- Support new event type for suggested events

### Fixed
- Fixed an issue in for suggested events

## [6.2.0]

### Added
- Support for Gaming Video Uploads
- Allow Gaming Image Uploader to accept a callback
- [Messenger Sharing](https://developers.facebook.com/docs/messenger-platform/changelog/#20200304)

## [6.1.0]

### Added
- New SDK component: Gaming Services

### Deprecated
- Places Kit

## [6.0.0]
### Changed
- Graph API call upgrade to [v6.0](https://developers.facebook.com/docs/graph-api/changelog/version6.0)

## [5.15.2]
### Fixed
- Attempts to fix #665

## [5.15.1]

### Added
- FB Login improvements

## [5.15.0]

### Added
- Install Referrer uses new API
- Deprecates messenger sharing. See: https://developers.facebook.com/docs/sharing/messenger for more details on why the deprecation is occurring and how you can update your application to account for this change
- Chrome Custom Tabs for FB Login improvements

## [5.13.0]

### Added
- Parameter deactivation

## [5.12.1]

### Fixed
- Fixed a corner case

## [5.12.0]

### Changed
- Updated suggested events

## [5.11.2]

## [5.11.1]

### Fixed
- Fix java.lang.SecurityException in AccessTokenManager: [issue 627](https://github.com/facebook/facebook-android-sdk/issues/627)
- Fix Google Api Error because of values-fb: [issue 614](https://github.com/facebook/facebook-android-sdk/issues/614)
- Minor fixes

## [5.11.0]

### Added
- Launch event suggestions

### Fixed
- Fix NPE on AccessTokenAppIdPair

## [5.9.0]

### Changed

- API call upgrade to v5.0

## [5.8.0]

### Added

- Launch automatic advanced matching: https://www.facebook.com/business/help/2445860982357574

## [5.5.2]

### Fixed

- Change keepnames to keep in proguard-rules for IAP
- Fix memory leak in Codeless
- Fix issue of listing crash report files

## [5.5.1]

### Fixed

- Crash in FetchedAppGateKeepersManager

## [5.5.0]

- Various bug fixes

## [5.4.0]

### Changed

- Add handling for crash and error to make SDK more stable

## [5.2.0]

### Changed

- API call upgrade to v4.0

## [5.1.1]

### Fixed

- Various bug fixes


## [5.1.0]

### Added

- Auto log Subscribe and StartTrial going through GooglePlay store when the developer enables it in Facebook Developer setting page

## [5.0.2]

### Fixed

- Fix in-app purchase auto-logging issue which was introduced in 5.0.1

## [5.0.1]

### Added

- Support campaign attribution for Audience Network

### Fixed

- Fixed a crash that caused by absence of Google Play Store services

## [5.0.0]

### Added
- support manual SDK initialization

### Changed
- extend coverage of AutoLogAppEventsEnabled flag to all internal analytics events

### Removed

- Deprecate several activateApp and deactivateApp functions in AppEventsLogger.java

## [4.41.0]

### Removed

- Deprecated classes: FacebookUninstallTracker

### Fixed

- Various bug fixes

## [4.40.0]

### Fixed

- Various bug fixes

## [4.39.0]

### Other

- Facebook Developer Docs: [Changelog v4.x](https://developers.facebook.com/docs/android/change-log-4x)

<!-- Links -->

[Unreleased]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-6.0.0...HEAD
[13.2.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-13.1.0...sdk-version-13.2.0
[13.1.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-13.0.0...sdk-version-13.1.0
[13.0.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-12.3.0...sdk-version-13.0.0
[12.3.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-12.2.0...sdk-version-12.3.0
[12.2.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-12.1.0...sdk-version-12.2.0
[12.1.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-12.0.1...sdk-version-12.1.0
[12.0.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-12.0.0...sdk-version-12.0.1
[12.0.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-11.3.0...sdk-version-12.0.0
[11.3.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-11.2.0...sdk-version-11.3.0
[11.2.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-11.1.1...sdk-version-11.2.0
[11.1.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-11.1.0...sdk-version-11.1.1
[11.1.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-11.0.0...sdk-version-11.1.0
[11.0.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-9.1.1...sdk-version-11.0.0
[9.1.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-9.1.0...sdk-version-9.1.1
[9.1.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-9.0.0...sdk-version-9.1.0
[9.0.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-8.2.0...sdk-version-9.0.0
[8.2.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-8.1.0...sdk-version-8.2.0
[8.1.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-8.0.0...sdk-version-8.1.0
[8.0.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-7.1.0...sdk-version-8.0.0
[7.1.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-7.0.1...sdk-version-7.1.0
[7.0.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-7.0.0...sdk-version-7.0.1
[7.0.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-6.5.1...sdk-version-7.0.0
[6.5.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-6.5.0...sdk-version-6.5.1
[6.5.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-6.4.0...sdk-version-6.5.0
[6.4.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-6.3.0...sdk-version-6.4.0
[6.3.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-6.2.0...sdk-version-6.3.0
[6.2.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-6.1.0...sdk-version-6.2.0
[6.1.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-6.0.0...sdk-version-6.1.0
[6.0.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.15.2...sdk-version-6.0.0
[5.15.2]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.15.1...sdk-version-5.15.2
[5.15.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.15.0...sdk-version-5.15.1
[5.15.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.13.0...sdk-version-5.15.0
[5.13.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.12.1...sdk-version-5.13.0
[5.12.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.12.0...sdk-version-5.12.1
[5.12.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.11.2...sdk-version-5.12.0
[5.11.2]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.11.1...sdk-version-5.11.2
[5.11.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.11.0...sdk-version-5.11.1
[5.11.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.9.0...sdk-version-5.11.0
[5.9.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.8.0...sdk-version-5.9.0
[5.8.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.5.2...sdk-version-5.8.0
[5.5.2]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.5.1...sdk-version-5.5.2
[5.5.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.5.0...sdk-version-5.5.1
[5.5.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.4.0...sdk-version-5.5.0
[5.4.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.2.0...sdk-version-5.4.0
[5.2.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.1.0...sdk-version-5.2.0
[5.1.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.0.2...sdk-version-5.1.0
[5.0.2]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.0.1...sdk-version-5.0.2
[5.0.1]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.0.0...sdk-version-5.0.1
[5.0.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-4.41.0...sdk-version-5.0.0
[4.41.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-4.40.0...sdk-version-4.41.0
[4.40.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-4.39.0...sdk-version-4.40.0
