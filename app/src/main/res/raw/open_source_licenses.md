# Open-Source Licenses

This list covers the **direct** runtime dependencies and vendored components used by NetAigis OTP, as declared in the project build files and `COPYING` / `COPYING.icons`. Transitive AndroidX libraries pulled in by those dependencies are generally also under Apache License 2.0.

## Android Open Source Project

Android platform APIs, SDK components, and Action Bar icons used according to the Creative Commons 2.5 Attribution License (see `COPYING.icons`).

License: Apache License 2.0 / CC BY 2.5 (icons)
Website: https://source.android.com

## Material Design Icons

About-screen outline icons (`ic_outline_*_24.xml`) are Material Design Icons / Material Symbols by Google.

License: Apache License 2.0
Website: https://github.com/google/material-design-icons

## AndroidX AppCompat / Core / Activity

Dependencies: androidx.appcompat:appcompat, androidx.core:core-ktx, androidx.activity:activity-ktx

License: Apache License 2.0
Website: https://developer.android.com/jetpack/androidx

## Material Components for Android

Dependency: com.google.android.material:material

License: Apache License 2.0
Website: https://github.com/material-components/material-components-android

## Kotlin Coroutines

Dependencies: org.jetbrains.kotlinx:kotlinx-coroutines-core, kotlinx-coroutines-android

License: Apache License 2.0
Website: https://github.com/Kotlin/kotlinx.coroutines

## CameraX

Dependencies: androidx.camera:camera-core, camera-camera2, camera-lifecycle, camera-view

License: Apache License 2.0
Website: https://developer.android.com/training/camerax

## AndroidX Biometric

Dependency: androidx.biometric:biometric

License: Apache License 2.0
Website: https://developer.android.com/jetpack/androidx/releases/biometric

## Room

Dependencies: androidx.room:room-runtime, room-ktx (compiler via annotation processing)

License: Apache License 2.0
Website: https://developer.android.com/training/data-storage/room

## Hilt / Dagger

Dependencies: com.google.dagger:hilt-android and related compiler artifacts

License: Apache License 2.0
Website: https://dagger.dev/hilt/

## ZXing

Dependency: com.google.zxing:core

License: Apache License 2.0
Website: https://github.com/zxing/zxing

## Gson

Dependency: com.google.code.gson:gson

License: Apache License 2.0
Website: https://github.com/google/gson

## Glide

Dependency: com.github.bumptech.glide:glide

License: BSD, MIT, and Apache License 2.0 (see project NOTICE)
Website: https://github.com/bumptech/glide

## Guava

Dependency: com.google.guava:guava (android artifact)

License: Apache License 2.0
Website: https://github.com/google/guava

## Desugar JDK Libs

Dependency: com.android.tools:desugar_jdk_libs

License: Apache License 2.0 / GPL-2.0 with Classpath Exception (see artifact licenses)
Website: https://github.com/google/desugar_jdk_libs

## TextDrawable (vendored module)

Module `:text-drawable`, based on Amulya Khare TextDrawable.

License: Apache License 2.0 (upstream)
Website: https://github.com/amulyakhare/TextDrawable

## Google Authenticator Base32String (vendored)

File: `token-data/.../com/google/android/apps/authenticator/Base32String.java`
Copyright 2009 Google Inc.

License: Apache License 2.0

## FreeOTP / FreeOTP+ heritage

This application is derived from FreeOTP and FreeOTP+.

License: Apache License 2.0
Website: https://github.com/helloworld1/FreeOTPPlus

## andOTP token images

Token thumbnail drawables `token_image_*.xml` in the `:token-images` library are from andOTP.
Copyright © 2017-2020 Jakob Nixdorf. Copyright © 2015 Bruno Bierbaumer.

License: MIT License
Website: https://github.com/andOTP/andOTP

## Additional token image attributions

- `token_image_forgejo.xml`: adapted from the Forgejo logo by Caesar Schinas, CC BY-SA 4.0
- `token_image_group_office.xml`: adapted from the GroupOffice logo by Intermesh

See `COPYING.icons` in the repository for details.
