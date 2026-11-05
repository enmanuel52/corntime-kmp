import org.gradle.api.Plugin
import org.gradle.api.Project
import util.configureKotlinMultiplatform

/** Base Kotlin Multiplatform library: Android + iOS targets, JVM 17, common test stack. */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            configureKotlinMultiplatform()
        }
    }
}
