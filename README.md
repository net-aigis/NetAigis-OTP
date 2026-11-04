# NetAigis OTP

Android OTP authenticator based on a FreeOTP+ fork, rebranded for NetAigis.

## Packages

- Free: `de.wittmer.app.otp.free`
- Pro: `de.wittmer.app.otp` (default flavor)

Features are currently the same for Free and Pro.

## Build

```bash
./gradlew :app:assembleFreeDebug
./gradlew :app:assembleProDebug
```

APKs:

```text
app/build/outputs/apk/free/debug/otp-free-debug.apk
app/build/outputs/apk/pro/debug/otp-debug.apk
```

## Requirements

* Android 6.0+ (minSdk 23)

## Screenshots

<p>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/001.png" alt="Token list" width="180" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/002.png" alt="OTP code shown" width="180" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/003.png" alt="Category filter" width="180" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/004.png" alt="Edit token" width="180" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/005.png" alt="Add token" width="180" />
</p>

## Build Dependencies

* Android SDK (minSDK 23)
* Android Studio

## Import Notes

### When importing from JSON.

* Both issuerExt and label are required, if missing the entry will be silently ignored
* secret is required and needs to be an array/list of unsigned char integer values
* tokenOrder is required, any tokens NOT listed will be silently ignored

## Heritage

Originally forked from [FreeOTP+](https://github.com/helloworld1/FreeOTPPlus) / FreeOTP (Red Hat).
See `COPYING` for license details.
