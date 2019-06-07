# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Log Subscribe/StartTrial events going through GooglePlay store, which was logged as fb_mobile_purchase

## [5.0.2] - 2019-06-07

### Fixed

- Fix in-app purchase auto-logging issue which was introduced in 5.0.1

## [5.0.1] - 2019-05-16

### Added

- Support campaign attribution for Audience Network

### Fixed

- Fixed a crash that caused by absence of Google Play Store services

## [5.0.0] - 2019-04-30

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

[Unreleased]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-4.40.0...HEAD
[4.40.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-4.39.0...sdk-version-4.40.0
[4.39.0]: https://github.com/facebook/facebook-android-sdk/compare/sdk-version-4.0.0...sdk-version-4.39.0
