plugins {
    alias(libs.plugins.corntime.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(libs.kotlin.test)
            api(libs.kotlinx.coroutines.test)
            api(libs.turbine)
            api(libs.assertk)
        }
        androidMain.dependencies {
            // kotlin.test annotations (@BeforeTest/@AfterTest) on the JVM come from the JUnit 5 adapter.
            api(libs.kotlin.test.junit5)
        }
    }
}
