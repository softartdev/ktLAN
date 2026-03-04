# Repository Guidelines

For comprehensive documentation, see:
- [README.md](README.md) - Project overview and build entry points
- [CONTRIBUTING.md](CONTRIBUTING.md) - Code style, architecture, testing guidance
- [.github/workflows](.github/workflows) - CI (ci.yml) and CD (gh-pages.yml)

## Quick Reference

### Project Structure and Module Organization
- App UI Shared: `app/shared` (KMP Compose UI: `commonMain`, `androidMain`, `desktopMain`, `iosMain`, `wasmJsMain`)
- Android App: `app/android` (Android application target)
- Desktop App: `app/desktop` (desktop launcher/distribution target)
- iOS Kit: `app/ios-kit` (Compose iOS framework + CocoaPods target)
- Web App: `app/web` (wasmJs launcher target)
- Shared Logic: `shared` (domain, data, common utilities)
- Server: `server` (Ktor backend)
- iOS App Shell: `app/iosApp` (Xcode project via CocoaPods)
- Tooling: `gradle/libs.versions.toml` (versions), `gradle/` (wrapper)
- WebRTC: `io.ktor:ktor-client-webrtc` (desktop uses a JVM adapter backed by `dev.onvoid.webrtc:webrtc-java`)

### Build, Test, and Development Commands
- Build all (CI parity): `./gradlew build`
- Run tests: `./gradlew test`
- Android app: `./gradlew :app:android:assembleDebug` (APK) or `:app:android:installDebug`
- Desktop app: `./gradlew :app:desktop:desktopRun`
- Web app (dev): `./gradlew :app:web:wasmJsBrowserDevelopmentRun`
- Web app (prod build for Pages): `./gradlew :app:web:wasmJsBrowserDistribution`
- Server: `./gradlew :server:run`
- iOS: `cd app/iosApp && pod install` then open `app/iosApp/iosApp.xcworkspace` in Xcode
  - Regenerate CocoaPods artifacts if needed: `./gradlew :app:ios-kit:podInstall`

## AI Agent Workflow for Verifying Changes
After making changes, AI agents must perform the following checks sequentially. If any step fails, fix the changes and re-run all checks until they succeed.
1. Unit tests: `./gradlew test`
2. Web production build (CD parity): `./gradlew :app:web:wasmJsBrowserDistribution`
3. Full build (CI parity): `./gradlew build`

## Coding Style and Naming Conventions
- Kotlin official style (`kotlin.code.style=official`), 4-space indentation, organized imports.
- Prefer `val` over `var`; add explicit types when they improve readability.
- Classes/objects: `PascalCase`; functions/vars: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Compose: prefer state hoisting; keep @Composable functions small and previewable.

## Testing Guidelines
- Source sets: `commonTest`, `desktopTest`, `iosTest`, `androidTest` (when present).
- Unit tests: `./gradlew test`
- Desktop UI tests: `./gradlew :app:desktop:desktopTest`
- Web UI tests: `./gradlew :app:web:wasmJsBrowserTest` (requires a Chrome/Chromium binary)
- iOS UI tests: `./gradlew :app:shared:iosSimulatorArm64Test` (requires iOS simulator)

## Commit and Pull Request Guidelines
- Follow the checklist and template in `CONTRIBUTING.md`.
- Update `README.md` or other docs when behavior or workflows change.
- For UI changes, include screenshots/GIFs in the PR description when possible.

## Security and Configuration Tips
- Keep `local.properties` and local signing credentials out of version control.
- Do not commit secrets; use environment variables or CI secrets instead.

## Additional Resources
- `.github/workflows/ci.yml` - CI build (`./gradlew build`)
- `.github/workflows/gh-pages.yml` - Web deployment (`:app:web:wasmJsBrowserDistribution`)
