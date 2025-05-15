import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("signing")
//    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.32.0"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.accompanist.permissions)

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
        androidMain.dependencies {
            implementation(libs.androidx.fragment.ktx)
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}

android {
    namespace = "org.kmp.shots.k.sensor"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    val tag: String? = System.getenv("GITHUB_REF")?.split("/")?.lastOrNull()

    coordinates(
        groupId = libs.versions.groupId.get(),
        artifactId = libs.versions.artifactId.get(),
        version = tag ?: "1.0.0-SNAPSHOT"
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
    val keyId = findProperty("signingInMemoryKeyId")?.toString()
    val key = findProperty("signingInMemoryKey")?.toString()
    val keyPassword = findProperty("signingInMemoryKeyPassword")?.toString()
    val mavenCentralUser = findProperty("mavenCentralUsername")?.toString()
    val mavenCentralPassword = findProperty("mavenCentralPassword")?.toString()



    if (keyId == null) println("Missing property: signing.keyId")
    if (key == null) println("Missing property: signing.key")
    if (keyPassword == null) println("Missing property: signing.password")
    if (mavenCentralUser == null) println("Missing property: mavenCentral.user")
    if (mavenCentralPassword == null) println("Missing property: mavenCentral.password")

    useInMemoryPgpKeys(
        keyId,
        key,
        keyPassword
    )
}
