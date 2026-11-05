import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.enmanuelbergling.corntime.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.koin.compiler.gradle.plugin)
    compileOnly(libs.roborazzi.gradle.plugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "corntime.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "corntime.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("kmpLibrary") {
            id = "corntime.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpDomain") {
            id = "corntime.kmp.domain"
            implementationClass = "KmpDomainConventionPlugin"
        }
        register("kmpData") {
            id = "corntime.kmp.data"
            implementationClass = "KmpDataConventionPlugin"
        }
        register("kmpPresentation") {
            id = "corntime.kmp.presentation"
            implementationClass = "KmpPresentationConventionPlugin"
        }
        register("koin") {
            id = "corntime.koin"
            implementationClass = "KoinConventionPlugin"
        }
    }
}
