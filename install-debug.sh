#!/bin/bash
# Builds a debug APK and installs it directly onto a connected Karoo via USB.
# Prerequisites:
#   1. Karoo plugged in via USB
#   2. USB Debugging enabled on Karoo (Settings → Developer Options → USB Debugging)

set -e

if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
elif [ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
fi

if [ -d "$HOME/Library/Android/sdk" ]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
elif [ -d "/opt/homebrew/share/android-commandlinetools" ]; then
  export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
fi

export PATH="$PATH:$ANDROID_HOME/platform-tools"

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
