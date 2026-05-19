@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinCocoapods)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.app.shared)
        }
    }

    cocoapods {
        name = "ComposeAppCocoaPod"
        version = "1.0"
        summary = "Compose app for the Kotlin/Native module"
        homepage = "https://github.com/softartdev/ktLAN"
        framework {
            baseName = "ComposeAppFramework"
            binaryOption("bundleId", "com.softartdev.ktlan.compose.app.framework")
        }
        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
        ios.deploymentTarget = "14.0"
        pod("WebRTC-SDK", version = libs.versions.webrtc.ios.get(), moduleName = "WebRTC", linkOnly = true)
        podfile = project.file("../iosApp/Podfile")
    }
}
