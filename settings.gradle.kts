rootProject.name = "Corntimekmp"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://jitpack.io") {
            mavenContent { includeGroup("com.github.skydoves") }
        }
    }
}

include(":androidApp")
include(":shared")

include(":core:domain")
include(":core:data")
include(":core:testing")
include(":core:datastore")
include(":core:sync")
include(":core:presentation")
include(":core:design-system")

include(":feature:actor:domain")
include(":feature:actor:presentation")
include(":feature:auth:domain")
include(":feature:auth:presentation")
include(":feature:settings:presentation")
include(":feature:favorites:presentation")
include(":feature:watchlists:domain")
include(":feature:watchlists:presentation")
include(":feature:movies:domain")
include(":feature:movies:presentation")
