plugins {
    alias(libs.plugins.corntime.kmp.library)
}

// Fakes and fake data. They are also linked into the iOS framework for UI-test launches, so this
// module must not drag test-only libraries (Turbine, AssertK) into the app: the convention plugin
// already gives every module those in its own tests.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlin.test.junit5)
        }
    }
}
