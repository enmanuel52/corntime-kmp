plugins {
    alias(libs.plugins.corntime.kmp.presentation)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.feature.tvshows.domain)
        }
        androidMain.dependencies {
            // list/detail scenes for the seasons -> episodes -> episode flow on wide windows
            api(libs.androidx.navigation3.ui)
            api(libs.androidx.material3.adaptive.navigation3)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
