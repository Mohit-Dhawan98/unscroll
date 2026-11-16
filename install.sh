#!/bin/sh
# Installs ./Unscroll.apk on the connected phone over USB (adb install bypasses Android's "restricted settings" block on accessibility).
cd "$(dirname "$0")"
adb devices
adb install -r Unscroll.apk && adb shell am start -n com.maverick.unscroll/.MainActivity
