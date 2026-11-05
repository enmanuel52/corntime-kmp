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
            export(projects.feature.actor.domain)
            export(projects.feature.actor.presentation)
            export(projects.feature.auth.domain)
            export(projects.core.sync)
            export(projects.feature.favorites.presentation)
            export(projects.feature.watchlists.domain)
            export(projects.feature.watchlists.presentation)
            export(projects.feature.auth.presentation)
            export(projects.feature.settings.presentation)
            export(projects.feature.movies.domain)
            export(projects.feature.movies.presentation)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.presentation)
            api(projects.feature.actor.domain)
            api(projects.feature.actor.presentation)
            api(projects.feature.auth.domain)
            api(projects.feature.favorites.presentation)
            api(projects.feature.watchlists.domain)
            api(projects.feature.watchlists.presentation)
            api(projects.feature.auth.presentation)
            api(projects.feature.settings.presentation)
            api(projects.feature.movies.domain)
            api(projects.feature.movies.presentation)
            implementation(projects.core.data)
            implementation(projects.core.datastore)
            api(projects.core.sync)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel)
        }
        // Fakes for iOS UI-test launches only; Android never links them (and their JUnit) into the app.
        iosMain.dependencies {
            implementation(projects.core.testing)
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
