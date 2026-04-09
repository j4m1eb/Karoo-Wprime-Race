#!/bin/bash
# Builds a debug APK and installs it directly onto a connected Karoo via USB.
# Prerequisites:
#   1. Karoo plugged in via USB
#   2. USB Debugging enabled on Karoo (Settings → Developer Options → USB Debugging)

set -e

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$PATH:/Users/jamiebishop/Library/Android/sdk/platform-tools"

echo "Checking for connected device..."
adb devices

echo ""
echo "Building debug APK..."
./gradlew assembleDebug

echo ""
echo "Installing on device..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo ""
echo "Done! Extension installed. Restart the Karoo app or reboot if fields don't appear."
