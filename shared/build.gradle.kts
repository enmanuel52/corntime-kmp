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
            export(projects.core.domain)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
        }
    }
}

skie {
    features {
        // Sealed classes/interfaces -> exhaustive Swift enums, Flows -> AsyncSequence, suspend -> async.
        enableSwiftUIObservingPreview = true
    }
}
