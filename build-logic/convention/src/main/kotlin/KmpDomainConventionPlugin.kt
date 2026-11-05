import org.gradle.api.Plugin
import org.gradle.api.Project
import util.kotlinMultiplatform

/** Domain layer: pure KMP (no Android APIs), Koin annotations for use cases. Depends on :core:domain only. */
class KmpDomainConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("corntime.kmp.library")
            pluginManager.apply("corntime.koin")

            if (path != ":core:domain") {
                kotlinMultiplatform {
                    sourceSets.commonMain.dependencies {
                        api(project(":core:domain"))
                    }
                }
            }
        }
    }
}
