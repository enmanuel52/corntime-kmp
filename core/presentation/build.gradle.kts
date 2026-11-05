import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.corntime.kmp.library)
    alias(libs.plugins.composeCompiler)
}

composeCompiler {
    // iOS renders with SwiftUI: only the Android target needs the Compose compiler.
    targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
}

kotlin {
    android {
        androidResources {
            enable = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}
