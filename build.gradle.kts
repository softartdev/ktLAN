import java.util.Properties

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
}

fun isPodRunnable(podFile: File): Boolean {
    if (!podFile.isFile || !podFile.canExecute()) {
        return false
    }
    val firstLine = podFile.bufferedReader().use { it.readLine() ?: "" }
    if (!firstLine.startsWith("#!")) {
        return true
    }
    val interpreter = firstLine.removePrefix("#!").trim().split(" ").firstOrNull() ?: return false
    return File(interpreter).exists()
}

fun podExecutableFromPath(): File? {
    val pathEnv = providers.environmentVariable("PATH").orNull ?: return null
    return pathEnv
        .split(File.pathSeparatorChar)
        .asSequence()
        .filter { it.isNotBlank() }
        .map { File(it, "pod") }
        .firstOrNull(::isPodRunnable)
}

fun localPropertyValue(name: String): String? {
    val localPropsFile = rootDir.resolve("local.properties")
    if (!localPropsFile.isFile) {
        return null
    }
    val properties = Properties()
    localPropsFile.inputStream().use(properties::load)
    return properties.getProperty(name)?.takeIf { it.isNotBlank() }
}

val podPropertyName = "kotlin.apple.cocoapods.bin"
val podOverridePath = providers.gradleProperty(podPropertyName).orNull ?: localPropertyValue(podPropertyName)
val podWrapper = rootDir.resolve("gradle/pod")
if (podOverridePath.isNullOrBlank() && podWrapper.isFile) {
    allprojects {
        extensions.extraProperties[podPropertyName] = podWrapper.absolutePath
    }
}
val podExecutable = when {
    !podOverridePath.isNullOrBlank() -> File(podOverridePath).takeIf(::isPodRunnable)
    else -> podExecutableFromPath()
}

if (podExecutable == null) {
    logger.lifecycle(
        "CocoaPods executable was not found or is not runnable. " +
            "Set $podPropertyName to a valid pod binary to enable CocoaPods tasks."
    )
}

subprojects {
    val enableCocoapods = podExecutable != null
    tasks.matching { it.name.startsWith("podInstall") }.configureEach {
        enabled = enableCocoapods
    }
}
