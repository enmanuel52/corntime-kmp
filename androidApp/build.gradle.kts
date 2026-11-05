plugins {
    alias(libs.plugins.corntime.android.application)
    alias(libs.plugins.corntime.koin)
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

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
