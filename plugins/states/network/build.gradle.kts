@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.vanniktechPublish)
}

kotlin {
    android {
        namespace = "com.ksensor.plugins.states.network"
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    val xcfName = "networkStateKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    jvm()

    wasmJs {
        browser()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        commonMain {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(projects.core)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.core)
            }
        }
    }
}
