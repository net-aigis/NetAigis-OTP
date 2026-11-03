# NetAigis OTP

Android OTP authenticator based on a FreeOTP+ fork, rebranded for NetAigis.

## Packages

- Free: `de.wittmer.app.otp`
- Pro: `de.wittmer.app.otp.pro`

## Build

```bash
./gradlew :app:assembleFreeDebug
./gradlew :app:assembleProDebug
```

APKs:

```text
app/build/outputs/apk/free/debug/app-free-debug.apk
app/build/outputs/apk/pro/debug/app-pro-debug.apk
```

## Requirements

* Android 6.0+ (minSdk 23)

## Heritage

Originally forked from [FreeOTP+](https://github.com/helloworld1/FreeOTPPlus) / FreeOTP (Red Hat).
See `COPYING` for license details.
