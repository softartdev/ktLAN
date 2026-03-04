[![CI](https://github.com/softartdev/ktLAN/actions/workflows/ci.yml/badge.svg)](https://github.com/softartdev/ktLAN/actions/workflows/ci.yml)
[![Build & Deploy CI/CD](https://github.com/softartdev/ktLAN/actions/workflows/gh-pages.yml/badge.svg)](https://github.com/softartdev/ktLAN/actions/workflows/gh-pages.yml)

# ktLAN

A modern, cross-platform LAN (Local Area Network) communication application built with Kotlin Multiplatform. ktLAN enables seamless peer-to-peer communication across multiple platforms including Android, iOS, Web, Desktop, and Server environments.

The **wasmJS** target can be checked online at [https://softartdev.github.io/ktLAN/](https://softartdev.github.io/ktLAN/) powered by GitHub Pages.

## Features

- **Cross-Platform Support**: Native applications for Android, iOS, Desktop, and Web
- **Real-time Communication**: Ktor WebRTC client-powered peer-to-peer messaging and file sharing
- **QR Code Integration**: Easy device discovery and connection via QR code scanning
- **Network Discovery**: Automatic detection of devices on the same LAN
- **Modern UI**: Built with Compose Multiplatform for consistent, native-like experience
- **Server Component**: Optional server for enhanced functionality and centralized features

## Developer Guide

For detailed info about technology stack, directory structure, building and running the project, please refer to [AGENTS.md](AGENTS.md).

### App Modules

- `:app:shared` - shared Compose UI and platform `expect/actual` implementations
- `:app:android` - Android application target
- `:app:desktop` - desktop launcher/distribution target
- `:app:ios-kit` - iOS framework/CocoaPods integration target
- `:app:web` - WebAssembly (wasmJs) launcher target

### WebRTC Stack

- `io.ktor:ktor-client-webrtc` is the unified WebRTC API across Android, iOS, Desktop, and Web.
- Desktop uses a JVM engine adapter backed by `dev.onvoid.webrtc:webrtc-java` to bridge into Ktor's WebRTC abstractions.

## Learn More

* [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
* [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
* [Kotlin/Wasm](https://kotl.in/wasm/)

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
