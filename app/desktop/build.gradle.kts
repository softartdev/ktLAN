import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(projects.app.shared)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(compose.desktop.currentOs)
        }

        val desktopTest by getting
        desktopTest.dependencies {
            implementation(project.dependencies.enforcedPlatform(libs.koin.bom))
            implementation(projects.app.shared)
            implementation(projects.shared)
            implementation(libs.compose.runtime)
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.runtime.testing)
            implementation(libs.koin.core)
            implementation(libs.kermit)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.softartdev.ktlan.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.softartdev.ktlan"
            packageVersion = "1.0.0"
        }
    }
}
