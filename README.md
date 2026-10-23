# Quản lí quán cafe

A multi-client cafe operations system with an Android app, a Java desktop client, a Java HTTP API, and SQL Server database scripts.

## Project components

- `QuanLiQuanCafeAndroid/` — Native Android application built with Java, XML layouts, and Gradle.
- `QuanLiQuanCafeDesktop/` — Java desktop application for cafe administration.
- `QuanLiQuanCafeAPI/` — Java HTTP API used by the mobile client.
- `SQL/` — Database creation and migration scripts for SQL Server.

## Requirements

- JDK 11 or newer for the Java API and desktop client.
- Android Studio with the Android SDK for the Android application.
- Microsoft SQL Server configured with the schema in `SQL/`.
- The Java libraries referenced by each client in its `lib/` directory.

## Android build

Open `QuanLiQuanCafeAndroid/` in Android Studio, allow Gradle to sync, and run the `app` configuration. To build a debug APK from Windows:

```powershell
cd QuanLiQuanCafeAndroid
./gradlew.bat assembleDebug
```

The APK is generated under `app/build/outputs/apk/debug/` and is intentionally excluded from version control.

## API and desktop setup

1. Install and configure SQL Server.
2. Create the application database using the appropriate scripts in `SQL/`.
3. Configure database connection settings for your environment. Do not commit passwords or local credentials.
4. Configure the API base URL in the clients for the host where the API is running.
5. Use the IDE or the project's launch configuration to compile and start the API and desktop client with their required libraries.

Machine-specific settings, downloaded SDKs, build output, logs, and personal project documents are excluded from this repository.

## Google Play listing draft

The Vietnamese draft is maintained in `QuanLiQuanCafeAndroid/play-store/vi-VN/listing.md`. It is a preparation document and has not been published to Google Play.

## Security note

Use environment-specific credentials and rotate any credentials that may have been shared outside the development environment. Keep secrets out of source control, including in private repositories.