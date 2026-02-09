plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktechPublish)
    id("signing")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "sensor"
            isStatic = true
        }
        iosTarget.compilations.getByName("main") {
            val nskeyvalueobserving by cinterops.creating
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.accompanist.permissions)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle)
            implementation(libs.androidx.startup)

        }

        androidInstrumentedTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.core)
            implementation(libs.androidx.rules)
            implementation(libs.androidx.espresso.core)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = providers.gradleProperty("ANDROID_NAMESPACE").get()
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.android.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}