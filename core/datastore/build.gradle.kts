plugins {
    alias(libs.plugins.corntime.kmp.library)
    alias(libs.plugins.corntime.koin)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
