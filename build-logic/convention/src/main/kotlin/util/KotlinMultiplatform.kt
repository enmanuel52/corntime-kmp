package util

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.kotlinMultiplatform(block: KotlinMultiplatformExtension.() -> Unit) =
    extensions.configure<KotlinMultiplatformExtension>(block)

internal fun KotlinMultiplatformExtension.androidLibraryTarget(
    block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit,
) {
    val extensions = (this as ExtensionAware).extensions
    val target = extensions.findByName("android") ?: extensions.getByName("androidLibrary")
    (target as KotlinMultiplatformAndroidLibraryTarget).block()
}

/**
 * Shared KMP setup: Android (AGP KMP library) + iOS device/simulator targets,
 * JVM 17 and the common test stack (kotlin.test, coroutines-test, Turbine, AssertK; JUnit5 on the JVM host).
 */
internal fun Project.configureKotlinMultiplatform() {
    kotlinMultiplatform {
        androidLibraryTarget {
            namespace = derivedNamespace
            compileSdk = libs.version("android-compileSdk").toInt()
            minSdk = libs.version("android-minSdk").toInt()
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
            withHostTest {
                isIncludeAndroidResources = true
            }
        }

        iosArm64()
        iosSimulatorArm64()

        compilerOptions {
            freeCompilerArgs.addAll("-Xexpect-actual-classes")
        }

        sourceSets.apply {
            commonMain.dependencies {
                implementation(libs.library("kotlinx-coroutines-core"))
            }
            commonTest.dependencies {
                implementation(libs.library("kotlin-test"))
                implementation(libs.library("kotlinx-coroutines-test"))
                implementation(libs.library("turbine"))
                implementation(libs.library("assertk"))
            }
            getByName("androidHostTest").dependencies {
                implementation(libs.library("kotlin-test-junit5"))
                implementation(project.dependencies.platform(libs.library("junit-bom")))
                implementation(libs.library("junit-jupiter"))
                runtimeOnly(libs.library("junit-platform-launcher"))
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
