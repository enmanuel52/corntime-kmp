plugins {
    alias(libs.plugins.corntime.kmp.presentation)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.feature.movies.domain)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
