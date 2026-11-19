# Social Radar

Android application for discovering public Telegram groups, channels, discussion groups and communities.

**Current version:** 0.1.0-alpha  
**Current milestone:** Android shell and product foundation

## Product constraints

- Android APK; local-first architecture.
- Search runs on-device. Internet is used only to query external public sources.
- Telegram account authorization is mandatory for the planned MTProto integration.
- TDLib is the preferred Telegram client library; it is not yet bundled in this initial scaffold.
- Only public entities and public metadata are in scope. No private-chat scraping, member harvesting, mass joining or unsolicited messaging.
- Telegram API credentials and user sessions must never be committed to source control.

## Project structure

- `app/` — Android application.
- `MainActivity.kt` — initial Compose navigation and screens.
- `docs/ARCHITECTURE.md` — implementation architecture and integration plan.

## Build prerequisites

- Android Studio with Android SDK Platform 36.
- JDK 17.
- Gradle 8.13+ (wrapper will be added in the build setup milestone).

Open the project in Android Studio and sync Gradle. This repository currently contains the initial source scaffold; a verified APK has not yet been produced.

## Telegram setup

The Telegram integration requires an application `api_id` and `api_hash` obtained by the account owner from Telegram's official developer portal. They must be supplied locally and must not be committed to Git. The TDLib native Android build and authorization flow are a separate milestone.

## Milestones

1. Android shell and navigation.
2. TDLib native integration and MTProto authorization.
3. Local search orchestration and source adapters.
4. Result normalization, deduplication and ranking.
5. Local persistence, export and background execution.
6. Device testing and APK verification.
