[![CI](https://github.com/softartdev/ktLAN/actions/workflows/ci.yml/badge.svg)](https://github.com/softartdev/ktLAN/actions/workflows/ci.yml)
[![Build & Deploy CI/CD](https://github.com/softartdev/ktLAN/actions/workflows/gh-pages.yml/badge.svg)](https://github.com/softartdev/ktLAN/actions/workflows/gh-pages.yml)

# ktLAN

A modern, cross-platform LAN (Local Area Network) communication application built with Kotlin Multiplatform. ktLAN enables seamless peer-to-peer communication across multiple platforms including Android, iOS, Web, Desktop, and Server environments.

## Features

- **Cross-Platform Support**: Native applications for Android, iOS, Desktop, and Web
- **Real-time Communication**: WebRTC-powered peer-to-peer messaging and file sharing
- **QR Code Integration**: Easy device discovery and connection via QR code scanning
- **Network Discovery**: Automatic detection of devices on the same LAN
- **Modern UI**: Built with Compose Multiplatform for consistent, native-like experience
- **Server Component**: Optional server for enhanced functionality and centralized features

## Technology Stack

- **Kotlin Multiplatform**: Shared business logic across all platforms
- **Compose Multiplatform**: Modern declarative UI framework
- **WebRTC**: Real-time communication capabilities
- **Ktor**: Server-side framework for backend services
- **Kotlin/Wasm**: Web deployment with WebAssembly

This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop, Server.

The **wasmJS** target can be checked online at [https://softartdev.github.io/ktLAN/](https://softartdev.github.io/ktLAN/) powered by GitHub Pages.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that's common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple's CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you're sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* `/server` is for the Ktor server application.

* `/shared` is for the code that will be shared between all targets in the project.
  The most important subfolder is `commonMain`. If preferred, you can add code to the platform-specific folders here too.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.