# Social Radar — Architecture

## 1. Runtime model

The Android application owns the user interface, local database, search orchestration and Telegram client lifecycle. There is no remote application backend.

Network access is limited to the external public sources queried by the local engine. "Local server" means an on-device service/component, not a publicly exposed HTTP server.

## 2. Telegram integration

Preferred integration: official TDLib Java/JNI interface, built for Android ABIs supported by the app.

Authorization state machine:
- WaitTdlibParameters
- WaitEncryptionKey
- WaitPhoneNumber
- WaitCode
- WaitPassword (when Telegram 2-step verification is enabled)
- Ready
- LoggingOut / Closed

The app must:
- obtain api_id and api_hash locally from the user/developer configuration;
- store the TDLib database in app-private storage;
- use a high-entropy local database encryption key;
- protect app-owned secrets with Android Keystore;
- never log phone numbers, login codes, passwords, API hashes or session material;
- expose explicit logout and local-session deletion;
- handle rate limits and flood-wait responses without aggressive retries.

TDLib is not included in the initial scaffold. Its official documentation describes the Java/JNI interface and requires a native Android build. Do not substitute an unverified Maven artifact.

## 3. Search pipeline

Query -> query expansion -> source adapters -> normalization -> deduplication -> relevance scoring -> verification -> Room/SQLite persistence -> UI.

Each source adapter is isolated. One unavailable source must not fail the entire search.

## 4. Search boundaries

In scope:
- publicly discoverable Telegram groups and supergroups;
- public channels;
- publicly linked discussion groups;
- public invite links and references discoverable from allowed public sources;
- public metadata available through authorized Telegram client methods.

Out of scope:
- private chat or private group scraping;
- extracting member lists or personal profiles;
- mass joining, messaging or account automation;
- bypassing Telegram access controls or rate limits.

A Telegram user session does not provide a universal global search API. Search coverage depends on the actual discovery sources and Telegram's access rules. Search results must include provenance and verification timestamps.

## 5. Local persistence

Initial entities:
- Community
- SearchSession
- SearchQuery
- SourceEvidence
- SavedCommunity
- VerificationRecord

Unknown fields remain null. A missing member count or activity signal must not be fabricated.

## 6. Background work

Use Android WorkManager for deferrable refresh tasks. Use a foreground service only for a user-initiated long-running search where required by current Android background execution rules. Show a persistent notification and provide pause/cancel controls.

## 7. Initial scaffold status

The repository currently contains the Compose UI shell and build configuration only. TDLib, authentication, search adapters, database, export, Gradle wrapper and APK verification are pending.
