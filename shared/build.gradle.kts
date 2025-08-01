import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinCocoapods)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.network)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.napier)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.stream.webrtc.android)
        }
        jvmMain.dependencies {
            implementation(libs.webrtc.java)
            implementation(dependencies.variantOf(libs.webrtc.java) { classifier("windows-x86_64") })
            implementation(dependencies.variantOf(libs.webrtc.java) { classifier("macos-aarch64") })
            implementation(dependencies.variantOf(libs.webrtc.java) { classifier("linux-x86_64") })
            implementation(dependencies.variantOf(libs.webrtc.java) { classifier("linux-aarch64") })
            implementation(dependencies.variantOf(libs.webrtc.java) { classifier("linux-aarch32") })
            implementation(libs.json)
        }
    }
    cocoapods {
        name = "SharedCocoaPod"
        version = "1.0"
        summary = "Shared library for the Kotlin/Native module"
        homepage = "https://github.com/softartdev/ktLAN"
        framework {
            baseName = "SharedFramework"
            isStatic = false
        }
        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
        ios.deploymentTarget = "13.0"
    }
}

android {
    namespace = "com.softartdev.ktlan.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
