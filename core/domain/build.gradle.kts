plugins {
    alias(libs.plugins.corntime.kmp.domain)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.datetime)
    }
}
