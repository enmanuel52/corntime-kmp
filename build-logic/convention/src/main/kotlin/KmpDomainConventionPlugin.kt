import org.gradle.api.Plugin
import org.gradle.api.Project

/** Domain layer: pure KMP (no Android APIs), Koin annotations for use cases. */
class KmpDomainConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("corntime.kmp.library")
            pluginManager.apply("corntime.koin")
        }
    }
}
