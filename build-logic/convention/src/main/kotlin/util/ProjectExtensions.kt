package util

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalArgumentException("Missing library '$alias' in libs.versions.toml") }

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).orElseThrow { IllegalArgumentException("Missing version '$alias' in libs.versions.toml") }
        .requiredVersion

internal const val BASE_PACKAGE = "com.enmanuelbergling.corntime"

/**
 * Derives a unique Android namespace from the Gradle path, e.g.
 * `:feature:movies:presentation` -> `com.enmanuelbergling.corntime.feature.movies.presentation`.
 */
internal val Project.derivedNamespace: String
    get() = BASE_PACKAGE + path.split(":")
        .filter { it.isNotBlank() }
        .joinToString(separator = ".", prefix = ".") { it.replace("-", "").lowercase() }
