import org.gradle.api.Plugin
import org.gradle.api.Project
import util.kotlinMultiplatform
import util.libs
import util.library

/** Data layer: KMP + Koin + kotlinx.serialization + Ktor client. */
class KmpDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("corntime.kmp.library")
            pluginManager.apply("corntime.koin")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            kotlinMultiplatform {
                sourceSets.commonMain.dependencies {
                    implementation(libs.library("kotlinx-serialization-json"))
                    implementation(libs.library("ktor-client-core"))
                    implementation(libs.library("ktor-client-content-negotiation"))
                    implementation(libs.library("ktor-serialization-kotlinx-json"))
                }
                sourceSets.commonTest.dependencies {
                    implementation(libs.library("ktor-client-mock"))
                }
            }
        }
    }
}
