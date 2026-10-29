#!/bin/sh
set -eu
# Builds only the diagnostic instrumentation APK; it does not install anything.
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
: "${JAVA_HOME:?Set JAVA_HOME to a JDK directory}"
test_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
test_out=${TV_TEST_OUTPUT:-/tmp/tv-reorder-device-tests}
test_tools="$ANDROID_HOME/build-tools/34.0.0"
test_android="$ANDROID_HOME/platforms/android-34/android.jar"
mkdir -p "$test_out/classes"
"$JAVA_HOME/bin/javac" -source 8 -target 8 -cp "$test_android" -d "$test_out/classes" "$test_dir/SmokeRunner.java"
"$test_tools/d8" --min-api 29 --lib "$test_android" --output "$test_out" "$test_out"/classes/com/tvreorder/devicetests/*.class
"$test_tools/aapt2" link -o "$test_out/unsigned.apk" --manifest "$test_dir/AndroidManifest.xml" -I "$test_android"
(cd "$test_out" && zip -j unsigned.apk classes.dex)
"$test_tools/zipalign" -f 4 "$test_out/unsigned.apk" "$test_out/aligned.apk"
"$test_tools/apksigner" sign --ks "${TV_TEST_KEYSTORE:-$HOME/.android/debug.keystore}" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$test_out/tests.apk" "$test_out/aligned.apk"
