plugins {
    alias(libs.plugins.corntime.kmp.presentation)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.feature.watchlists.domain)
        }
        androidMain.dependencies {
            // movies | TV shows segments of the watchlist
            implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
