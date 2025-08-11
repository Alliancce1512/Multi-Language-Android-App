## Shelly Task (Android)

An Android showcase app that mixes modern Kotlin/Jetpack Compose with legacy Java components. It includes:
- Gallery (Compose + Kotlin, Unsplash API)
- Bluetooth LE scanner (Kotlin, View-based UI)
- Web content screen (Java, WebView)


### Setup Instructions

Prerequisites:
- Latest Android Studio (with Android Gradle Plugin support for this project)
- Android SDK 24+ (minSdk 24), compile/target SDK as configured in the project
- A physical device or emulator with Internet access (BLE feature requires a physical device with BLE)

Clone and open in Android Studio:
1. File → Open → select the project root.
2. Let Gradle sync complete.
3. Set your Unsplash API key (see “API Key” below), then Run the `app` configuration.

Build from command line (or just start the build via Android Studio):
```bash
cd Shelly-Task
./gradlew clean build
./gradlew :app:installDebug
# Optionally launch the main activity on a connected device/emulator
adb shell am start -n com.shellytask.app/.MainActivity
```


### API Key (Unsplash)

The app expects an Unsplash Access Key to authenticate API requests.

Current location:
- `app/build.gradle.kts` → inside `android { defaultConfig { ... } }` there is:
  ```kotlin
  buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"ADD_ACCESS_KEY_HERE\"")
  ```

How to set your key (quick start):
1. Obtain an Access Key from Unsplash Developers (`https://unsplash.com/developers`).
2. Replace `ADD_ACCESS_KEY_HERE` with your actual key, preserving quotes, for example:
   ```kotlin
   buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"YOUR_UNSPLASH_ACCESS_KEY\"")
   ```
3. Sync Gradle and run the app. The key will be available at `BuildConfig.UNSPLASH_ACCESS_KEY`.

Recommended (keep secrets out of VCS):
- Instead of hardcoding, store the key in `local.properties` (not committed) and read it in `build.gradle.kts`. This is not implemented by default in this repo but can be added as an improvement (see “Known Limitations/Improvements”).


### Design Decisions

- Kotlin + Jetpack Compose for the Gallery feature:
  - Modern declarative UI with `GalleryActivity` and `GalleryViewModel` managing state via Kotlin Flows.
  - Data layer uses `UnsplashRepository` with Retrofit/OkHttp + Gson for networking and mapping.
  - Coil handles image loading, placeholders, and error states efficiently.

- Retrofit + OkHttp:
  - `UnsplashApi` injects the `Authorization` header using `BuildConfig.UNSPLASH_ACCESS_KEY` via an OkHttp interceptor.
  - Logging interceptor at BASIC level for concise network logs during development.

- Mix of Java and Kotlin to reflect real-world migration:
  - Legacy Java screens (`web` and `legacy` packages) remain in Java to demonstrate interop and incremental modernization.
  - Newer features (Gallery, Bluetooth) are in Kotlin. Compose is used where it fits best (Gallery), while the BLE screen uses a View-based UI for practical BLE widget patterns.
  - `MainActivity.java` serves as the entry point linking all features, illustrating seamless Java↔Kotlin coexistence.

- Bluetooth LE (Kotlin, View-based UI):
  - Uses `BleClient`/`BleUtils` and runtime permission handling across API levels.
  - UI is XML-based (`activity_bluetooth.xml`) to keep parity with many existing BLE samples and to highlight interop with Compose parts elsewhere.

- Web content (Java):
  - `WebContentActivity` loads local assets (`app/src/main/assets`) into a `WebView`, enabling JavaScript and DOM storage.


### Known Limitations/Improvements

- Unsplash API key handling:
  - Currently set directly in `app/build.gradle.kts`. Consider reading from `local.properties` or environment variables and never committing the key.

- Networking and data:
  - No offline caching; network state is not persisted across sessions.
  - Error handling is basic; could surface specific error messages and retry actions.
  - Paging is naive (load-more). Consider using the Paging 3 library.

- UI/UX:
  - Gallery lacks search and filtering.
  - No theming beyond Material 3 defaults; dark-mode and accessibility polish could be improved.

- Navigation:
  - Currently there is a mix of multiple activities, explicit Intent navigation, and Compose-driven screens throughout the project.
  - Complete overhaul of the app navigation is required. It will have the task to centralise routes and back-stack handling/management.
  - A single-activity architecture will be implemented by utilising the Jetpack Navigation and Fragments.

- Bluetooth:
  - Scanning and simple connect flow only; limited GATT interaction in the UI.
  - Requires location permissions on some devices/versions; no background scanning.

- Testing:
  - Minimal unit/instrumentation tests. Add coverage for ViewModel, repository, and BLE utilities.

- Security:
  - Keys present in `BuildConfig` are visible to clients. For production, proxy API calls through a backend or apply stronger key handling strategies.


### Project Structure (high-level)

- `app/src/main/java/com/shellytask/app/gallery` — Compose-based gallery, ViewModel, repository, API models
- `app/src/main/java/com/shellytask/app/bluetooth` — BLE scanner activity and helpers (Kotlin + XML UI)
- `app/src/main/java/com/shellytask/app/web` — WebView screen (Java) loading `assets/about.html`
- `app/src/main/java/com/shellytask/app/legacy` — Legacy Java components maintained for interop demonstration
- `app/src/main/res/layout` — XML layouts for non-Compose screens
- `app/src/main/assets` — Static web content


### Build Tooling

- Gradle Version Catalog manages dependency versions in `gradle/libs.versions.toml`.
- Android Gradle Plugin and Kotlin versions are declared via the catalog.
- `compileSdk`, `targetSdk`, `minSdk` and Java/Kotlin targets are set in `app/build.gradle.kts`.


