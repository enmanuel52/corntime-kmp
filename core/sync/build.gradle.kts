plugins {
    alias(libs.plugins.corntime.kmp.library)
    alias(libs.plugins.corntime.koin)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.koin.android)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
        androidHostTest.dependencies {
            implementation(libs.androidx.work.testing)
            implementation(libs.robolectric)
            implementation(libs.junit4)
            implementation(libs.junit.vintage.engine)
        }
    }
}
