plugins {
    alias(libs.plugins.corntime.android.application)
    alias(libs.plugins.corntime.koin)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.enmanuelbergling.corntime"

    defaultConfig {
        applicationId = "com.enmanuelbergling.corntime_kmp"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.core.designSystem)
    implementation(projects.feature.auth.presentation)
    implementation(projects.feature.settings.presentation)
    implementation(projects.feature.movies.presentation)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertk)
}
