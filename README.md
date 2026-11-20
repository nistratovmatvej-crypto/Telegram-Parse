# Social Radar

Android app for discovering public Telegram groups and channels through TDLib (MTProto).

**Current version:** 0.1.0-alpha  
**Status:** TDLib authorization and public-chat discovery are implemented in the current source; CI build is pending for the latest commit.

## What works in the source

- TDLib native Android dependency and Kotlin Flow wrapper.
- API ID / API Hash configuration.
- Telegram login flow: phone number, login code, 2FA password, email verification and new-account registration state.
- Search via Telegram's `SearchPublicChats` endpoint, expanded query variants, deduplication and relevance sorting.
- Result cards resolve chat titles and public usernames; tapping a username opens the community in Telegram.
- Local query history in the UI session.
- Only public groups/channels are searched; the app does not scrape private chats, message contents, members or perform mass actions.

## Search limits

Telegram's public-chat discovery searches community names and usernames, not messages or post contents. Search requests shorter than five characters are skipped. Telegram's API controls how many public-chat results are returned per query, so this is not an exhaustive index of all Telegram communities. Query expansion is deterministic (RU/EN variants), not an AI model.

## Telegram setup

1. In Telegram's official developer portal at https://my.telegram.org/apps, create an app and obtain its **API ID** and **API Hash**.
2. In Social Radar, open **Settings → Connect / configure** and enter those values.
3. Complete the Telegram login in the app: phone, Telegram code, and 2FA/email if requested.
4. Return to Search and enter a query of at least 5 characters.

Use your own Telegram account and do not share login codes or 2FA passwords. Credentials are currently stored in local app preferences; this version does not provide encrypted credential storage. TDLib's local database encryption key is not configured by the current wrapper.

## Build

- Android Studio / Android SDK Platform 36
- JDK 17
- Gradle 8.13

GitHub Actions builds `app:assembleDebug` and uploads the debug APK as an artifact. Device login and live Telegram searches have not been verified from CI.

## Project structure

- `app/` — Android app
- `app/src/main/java/app/socialradar/TelegramSession.kt` — TDLib auth and public search
- `app/src/main/java/app/socialradar/search/` — query expansion and search orchestration
- `docs/ARCHITECTURE.md` — architecture notes

## Scope & privacy

Social Radar is a public-community discovery client. It does not provide private-chat scraping, member harvesting, mass joining or unsolicited messaging.
