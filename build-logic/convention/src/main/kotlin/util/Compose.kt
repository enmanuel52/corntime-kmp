package util

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Applies the Compose compiler. In KMP modules it only targets Android: iOS uses SwiftUI,
 * so shared ViewModels/state stay free of any Compose runtime dependency.
 */
internal fun Project.configureComposeCompiler(androidOnly: Boolean) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        if (androidOnly) {
            targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
        }
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("compose_stability.conf")
        )
    }
}
