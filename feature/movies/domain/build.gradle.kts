plugins {
    alias(libs.plugins.corntime.kmp.domain)
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core.testing)
    }
}
