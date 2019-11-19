# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [5.11.1] - 2019-11-18

### Fixed
- Minor fixes

## [5.11.0] - 2019-11-14

### Added
- Launch event suggestions

### Fixed
- Fix NPE on AccessTokenAppIdPair

## [5.9.0] - 2019-10-28

### Changed

- API call upgrade to v5.0

## [5.8.0] - 2019-10-08

### Added

- Launch automatic advanced matching: https://www.facebook.com/business/help/2445860982357574

## [5.5.2] - 2019-10-04

### Fixed

- Change keepnames to keep in proguard-rules for IAP
- Fix memory leak in Codeless
- Fix issue of listing crash report files

## [5.5.1] - 2019-09-05

### Fixed

- Crash in FetchedAppGateKeepersManager

## [5.5.0] - 2019-08-30

- Various bug fixes

## [5.4.0] - 2019-08-15

### Changed

- Add handling for crash and error to make SDK more stable

## [5.2.0] - 2019-07-29

### Changed

- API call upgrade to v4.0

## [5.1.1] - 2019-07-21

### Fixed

- Various bug fixes


## [5.1.0] - 2019-06-21

### Added

- Auto log Subscribe and StartTrial going through GooglePlay store when the developer enables it in Facebook Developer setting page

## [5.0.2] - 2019-06-07

### Fixed

- Fix in-app purchase auto-logging issue which was introduced in 5.0.1

## [5.0.1] - 2019-05-16

### Added

- Support campaign attribution for Audience Network

### Fixed

- Fixed a crash that caused by absence of Google Play Store services

## [5.0.0] - 2019-04-30

### Added
- support manual SDK initialization

### Changed
- extend coverage of AutoLogAppEventsEnabled flag to all internal analytics events

### Removed

- Deprecate several activateApp and deactivateApp functions in AppEventsLogger.java

## [4.41.0] - 2019-03-08

### Removed

- Deprecated classes: FacebookUninstallTracker

### Fixed

- Various bug fixes

## [4.40.0] - 2019-01-17

### Fixed

- Various bug fixes

## [4.39.0] - 2018-12-03

### Other

- Facebook Developer Docs: [Changelog v4.x](https://developers.facebook.com/docs/android/change-log-4x)

<!-- Links -->

[Unreleased]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-5.4.0...HEAD
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
[4.39.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-4.0.0...sdk-version-4.39.0


