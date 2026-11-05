plugins {
    alias(libs.plugins.corntime.kmp.library)
    alias(libs.plugins.corntime.koin)
    alias(libs.plugins.skie)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Everything SwiftUI touches must be exported from the umbrella framework.
            export(projects.core.domain)
            export(projects.core.presentation)
            export(projects.feature.movies.domain)
            export(projects.feature.movies.presentation)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.presentation)
            api(projects.feature.movies.domain)
            api(projects.feature.movies.presentation)
            implementation(projects.core.data)
            implementation(projects.core.datastore)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
        }
    }
}

skie {
    features {
        // Sealed classes/interfaces -> exhaustive Swift enums, Flows -> AsyncSequence, suspend -> async.
        enableSwiftUIObservingPreview = true
    }
}
