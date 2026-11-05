import org.gradle.api.Plugin
import org.gradle.api.Project
import util.kotlinMultiplatform
import util.libs
import util.library

/** Koin Annotations processed by the Koin Compiler Plugin (no KSP). */
class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.insert-koin.compiler.plugin")
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                kotlinMultiplatform {
                    sourceSets.commonMain.dependencies {
                        implementation(libs.library("koin-core"))
                        implementation(libs.library("koin-annotations"))
                    }
                    sourceSets.commonTest.dependencies {
                        implementation(libs.library("koin-test"))
                    }
                }
            }
            pluginManager.withPlugin("com.android.application") {
                dependencies.add("implementation", libs.library("koin-core"))
                dependencies.add("implementation", libs.library("koin-annotations"))
                dependencies.add("implementation", libs.library("koin-android"))
            }
        }
    }
}
