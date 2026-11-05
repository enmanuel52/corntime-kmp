plugins {
    // Loaded once here to avoid loading the plugins multiple times in each subproject's classloader.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.koinCompiler) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.roborazzi) apply false
}
