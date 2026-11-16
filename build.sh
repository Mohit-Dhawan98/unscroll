#!/bin/sh
# Builds the tiny release APK (signed with the debug key, fine for sideloading) and copies it to ./Unscroll.apk
set -e
# Defaults are the Homebrew locations on Apple Silicon; override by exporting these first.
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
cd "$(dirname "$0")/android"
./gradlew assembleRelease -q --console=plain
cp app/build/outputs/apk/release/app-release.apk ../Unscroll.apk
echo "built: $(cd .. && pwd)/Unscroll.apk"
