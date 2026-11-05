import org.gradle.api.Plugin
import org.gradle.api.Project
import util.androidLibraryTarget
import util.configureComposeCompiler
import util.kotlinMultiplatform
import util.libs
import util.library

/**
 * Presentation layer.
 * - commonMain: State / Action / Event / ViewModel shared by Android and iOS.
 * - androidMain: the original Jetpack Compose screens (Pop-Corns UI).
 */
class KmpPresentationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("corntime.kmp.library")
            pluginManager.apply("corntime.koin")
            configureComposeCompiler(androidOnly = true)

            kotlinMultiplatform {
                androidLibraryTarget {
                    androidResources {
                        enable = true
                    }
                }

                sourceSets.commonMain.dependencies {
                    api(libs.library("androidx-lifecycle-viewmodel"))
                    implementation(libs.library("koin-core-viewmodel"))
                    implementation(libs.library("kotlinx-collections-immutable"))
                    if (path != ":core:presentation") {
                        implementation(project(":core:presentation"))
                    }
                }
                sourceSets.androidMain.dependencies {
                    implementation(project.dependencies.platform(libs.library("androidx-compose-bom")))
                    implementation(libs.library("androidx-compose-ui"))
                    implementation(libs.library("androidx-compose-foundation"))
                    implementation(libs.library("androidx-compose-animation"))
                    implementation(libs.library("androidx-compose-material3"))
                    implementation(libs.library("androidx-compose-material-icons-extended"))
                    implementation(libs.library("androidx-compose-ui-tooling-preview"))
                    implementation(libs.library("androidx-lifecycle-runtime-compose"))
                    implementation(libs.library("androidx-lifecycle-viewmodel-compose"))
                    implementation(libs.library("androidx-navigation3-runtime"))
                    implementation(libs.library("koin-compose-viewmodel"))
                    implementation(libs.library("coil-compose"))
                    implementation(project(":core:design-system"))
                }
                sourceSets.getByName("androidHostTest").dependencies {
                    implementation(project.dependencies.platform(libs.library("androidx-compose-bom")))
                    implementation(libs.library("androidx-compose-ui-test-junit4"))
                    implementation(libs.library("androidx-compose-ui-test-manifest"))
                    implementation(libs.library("robolectric"))
                    implementation(libs.library("junit4"))
                    runtimeOnly(libs.library("junit-vintage-engine"))
                }
            }
        }
    }
}
