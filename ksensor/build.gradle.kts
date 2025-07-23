import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }


    iosX64()
    iosArm64()
    iosSimulatorArm64()


    sourceSets {
        androidMain.dependencies {
            implementation(libs.accompanist.permissions)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.startup)

        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.core)
            implementation(libs.androidx.rules)
            implementation(libs.androidx.espresso.core)
        }
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
            }
        }
    }
}

android {
    namespace = "org.kmp.shots.k.sensor"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}


mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    val tag: String? = System.getenv("GITHUB_REF")?.split("/")?.lastOrNull()
    coordinates(
        groupId = libs.versions.groupId.get(),
        artifactId = libs.versions.artifactId.get(),
        version = tag ?: "1.3.4-SNAPSHOT"
    )

    pom {
        name = "KSensor"
        description = "A KMP library that provides Sensors info for both Android and iOS"
        url = "https://github.com/shadmanadman/KSensor"
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "shadmanadman"
                name = "Shadman Adman"
                email = "adman.shadman@gmail.com"
            }
        }
        scm {
            connection = "scm:git:https://github.com/shadmanadman/KSensor"
            developerConnection = "scm:git:github.com/shadmanadman/KSensor.git"
            url = "https://github.com/shadmanadman/KSensor"
        }
    }
}

signing {
    val keyId = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId")
    val key = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
    val keyPassword = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")

    useInMemoryPgpKeys(
        keyId,
        key,
        keyPassword
    )
}
