#!/bin/sh

# Example signing helper for the free release flavor.
# Requires a local keystore; do not commit keystore.properties or .jks files.

APK_DIR="app/build/outputs/apk/free/release"
UNSIGNED_APK_FILE_NAME="app-free-release-unsigned.apk"
SIGNED_APK_FILE_NAME="NetAigisOTP-free-release.apk"

jarsigner -verbose -digestalg SHA1 -sigalg MD5withRSA -keystore ~/Documents/keys/liberty-android-release.keystore "$APK_DIR/$UNSIGNED_APK_FILE_NAME" liberty-android-key
zipalign -v 4 "$APK_DIR/$UNSIGNED_APK_FILE_NAME" "$APK_DIR/$SIGNED_APK_FILE_NAME"
