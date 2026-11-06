# Open-Source-Lizenzen

Diese Liste umfasst die **direkten** Runtime-Abhängigkeiten und eingebetteten Komponenten von NetAigis OTP laut Build-Dateien sowie `COPYING` / `COPYING.icons`. Transitive AndroidX-Bibliotheken sind in der Regel ebenfalls unter Apache License 2.0 lizenziert.

## Android Open Source Project

Android-Plattform-APIs, SDK-Komponenten und Action-Bar-Icons gemäß Creative Commons 2.5 Attribution (siehe `COPYING.icons`).

Lizenz: Apache License 2.0 / CC BY 2.5 (Icons)
Website: https://source.android.com

## Material Design Icons

About-Icons (`ic_outline_*_24.xml`) sind Material Design Icons / Material Symbols von Google.

Lizenz: Apache License 2.0
Website: https://github.com/google/material-design-icons

## AndroidX AppCompat / Core / Activity

Abhängigkeiten: androidx.appcompat:appcompat, androidx.core:core-ktx, androidx.activity:activity-ktx

Lizenz: Apache License 2.0
Website: https://developer.android.com/jetpack/androidx

## Material Components for Android

Abhängigkeit: com.google.android.material:material

Lizenz: Apache License 2.0
Website: https://github.com/material-components/material-components-android

## Kotlin Coroutines

Abhängigkeiten: org.jetbrains.kotlinx:kotlinx-coroutines-core, kotlinx-coroutines-android

Lizenz: Apache License 2.0
Website: https://github.com/Kotlin/kotlinx.coroutines

## CameraX

Abhängigkeiten: androidx.camera:camera-core, camera-camera2, camera-lifecycle, camera-view

Lizenz: Apache License 2.0
Website: https://developer.android.com/training/camerax

## AndroidX Biometric

Abhängigkeit: androidx.biometric:biometric

Lizenz: Apache License 2.0
Website: https://developer.android.com/jetpack/androidx/releases/biometric

## Room

Abhängigkeiten: androidx.room:room-runtime, room-ktx (Compiler über Annotation Processing)

Lizenz: Apache License 2.0
Website: https://developer.android.com/training/data-storage/room

## Hilt / Dagger

Abhängigkeiten: com.google.dagger:hilt-android und zugehörige Compiler-Artefakte

Lizenz: Apache License 2.0
Website: https://dagger.dev/hilt/

## ZXing

Abhängigkeit: com.google.zxing:core

Lizenz: Apache License 2.0
Website: https://github.com/zxing/zxing

## Gson

Abhängigkeit: com.google.code.gson:gson

Lizenz: Apache License 2.0
Website: https://github.com/google/gson

## Glide

Abhängigkeit: com.github.bumptech.glide:glide

Lizenz: BSD, MIT und Apache License 2.0 (siehe NOTICE des Projekts)
Website: https://github.com/bumptech/glide

## Guava

Abhängigkeit: com.google.guava:guava (Android-Artefakt)

Lizenz: Apache License 2.0
Website: https://github.com/google/guava

## Desugar JDK Libs

Abhängigkeit: com.android.tools:desugar_jdk_libs

Lizenz: Apache License 2.0 / GPL-2.0 with Classpath Exception (siehe Artefakt-Lizenzen)
Website: https://github.com/google/desugar_jdk_libs

## TextDrawable (eingebettetes Modul)

Modul `:text-drawable`, basierend auf Amulya Khare TextDrawable.

Lizenz: Apache License 2.0 (Upstream)
Website: https://github.com/amulyakhare/TextDrawable

## Google Authenticator Base32String (eingebettet)

Datei: `token-data/.../com/google/android/apps/authenticator/Base32String.java`
Copyright 2009 Google Inc.

Lizenz: Apache License 2.0

## FreeOTP / FreeOTP+-Herkunft

Diese Anwendung basiert auf FreeOTP und FreeOTP+.

Lizenz: Apache License 2.0
Website: https://github.com/helloworld1/FreeOTPPlus

## andOTP Token-Bilder

Token-Vorschaubilder `token_image_*.xml` in der Bibliothek `:token-images` stammen von andOTP.
Copyright © 2017-2020 Jakob Nixdorf. Copyright © 2015 Bruno Bierbaumer.

Lizenz: MIT License
Website: https://github.com/andOTP/andOTP

## Weitere Token-Bild-Hinweise

- `token_image_forgejo.xml`: adaptiert vom Forgejo-Logo von Caesar Schinas, CC BY-SA 4.0
- `token_image_group_office.xml`: adaptiert vom GroupOffice-Logo von Intermesh

Details stehen in `COPYING.icons` im Repository.
