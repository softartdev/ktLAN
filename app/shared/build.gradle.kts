import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinCocoapods)
}

kotlin {
    android {
        namespace = "com.softartdev.ktlan.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        androidResources.enable = true
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets.forEach {
        it.dependencies {
            implementation(project.dependencies.enforcedPlatform(libs.kotlinx.coroutines.bom))
            implementation(project.dependencies.enforcedPlatform(libs.ktor.bom))
            implementation(project.dependencies.enforcedPlatform(libs.koin.bom))
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.material.theme.prefs)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.kermit)
            implementation(libs.kermit.koin)
            implementation(libs.qrose)
            implementation(libs.kscan)
            implementation(libs.camera.compose.permission)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.ui.tooling)
        }
    }

    cocoapods {
        name = "AppSharedCocoaPod"
        version = "1.0"
        summary = "Shared Compose UI module"
        homepage = "https://github.com/softartdev/ktLAN"
        ios.deploymentTarget = "13.0"
        pod("WebRTC-SDK", version = libs.versions.webrtc.ios.get(), moduleName = "WebRTC", linkOnly = true)
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}
