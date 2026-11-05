import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties

plugins {
    alias(libs.plugins.corntime.kmp.data)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            implementation(libs.ktor.client.logging)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}

val secrets = Properties().apply {
    val file = rootProject.file("secret.properties")
    if (file.exists()) file.inputStream().use(::load)
}

buildkonfig {
    packageName = "com.enmanuelbergling.corntime.core.data"
    objectName = "Secrets"
    defaultConfigs {
        buildConfigField(STRING, "API_KEY", secrets.getProperty("API_KEY").orEmpty(), const = true)
    }
}
