rootProject.name = "KSensorLib"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
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
    }
}

include(":core")

include(":plugins:sensors:motion")
include(":plugins:sensors:environment")
include(":plugins:sensors:positioning")
include(":plugins:sensors:interaction")
include(":plugins:states:lifecycle")
include(":plugins:states:network")
include(":plugins:states:system")
include(":plugins:states:bluetooth")

//include(":bundles:motion")
//include(":bundles:environment")
//include(":bundles:positioning")
//include(":bundles:interaction")
//include(":bundles:network")
//include(":bundles:system")
//include(":bundles:bluetooth")
//include(":bundles:lifecycle")
//include(":bundles:all")

include(":sample")
include(":doc")
