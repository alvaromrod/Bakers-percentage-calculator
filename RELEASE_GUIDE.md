# Release Guide

This guide explains how to build a signed App Bundle (`.aab`) for the Google Play Store.

## 1. Generate a Keystore (One time only)
If you don't have a keystore yet, generate one using keytool (included with JDK) or Android Studio.

**Using Android Studio:**
1.  Go to **Build > Generate Signed Bundle / APK**.
2.  Select **Android App Bundle**.
3.  Click **Create new...** under Key store path.
4.  Fill in the details and save the `.jks` file (e.g., `release-key.jks`) in the root project folder (or keep it safe elsewhere).

## 2. Create `keystore.properties`
Create a file named `keystore.properties` in the **root project directory** (same level as `gradlew`).
**DO NOT commit this file to Git!**

Add your keystore details:
```properties
storeFile=release-key.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```
*Note: If `storeFile` is not in the app module folder, provide the full path or relative path from the app module.*

## 3. Build the Release Bundle
Run the following command in the terminal:

```bash
./gradlew bundleRelease
```

## 4. Locate the Artifact
Once the build finishes, your signed App Bundle will be located at:
`app/build/outputs/bundle/release/app-release.aab`

## 5. Upload to Play Console
1.  Go to the [Google Play Console](https://play.google.com/console).
2.  Create a new release.
3.  Upload the `app-release.aab` file.
