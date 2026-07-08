import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    alias(libs.plugins.kmmbridge) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.vanniktechPublish) apply false
}

subprojects {
    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        val props = project.providers
        val artifactIdProp = props.gradleProperty("POM_ARTIFACT_ID").orNull
        val snapshotVersion = props.gradleProperty("PROJECT_VERSION_NAME").get()
        val computedVersion = System.getenv("GITHUB_REF")
            ?.takeIf { it.startsWith("refs/tags/") }
            ?.substringAfterLast("/")
            ?: snapshotVersion

        project.group = props.gradleProperty("PROJECT_GROUP").get()
        project.version = computedVersion

        extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {

            publishToMavenCentral()
            signAllPublications()

            artifactIdProp?.let{
                coordinates(project.group.toString(), artifactIdProp, project.version.toString())
            }

            pom {
                name.set(props.gradleProperty("POM_NAME"))
                description.set(props.gradleProperty("POM_DESCRIPTION"))
                url.set(props.gradleProperty("POM_URL"))
                licenses {
                    license {
                        name.set(props.gradleProperty("POM_LICENSE_NAME"))
                        url.set(props.gradleProperty("POM_LICENSE_URL"))
                    }
                }
                developers {
                    developer {
                        id.set(props.gradleProperty("POM_DEVELOPER_ID"))
                        name.set(props.gradleProperty("POM_DEVELOPER_NAME"))
                        email.set(props.gradleProperty("POM_DEVELOPER_EMAIL"))
                    }
                }
                scm {
                    connection.set(props.gradleProperty("SCM_CONNECTION"))
                    developerConnection.set(props.gradleProperty("SCM_DEVELOPER_CONNECTION"))
                    url.set(props.gradleProperty("POM_URL"))
                }
            }
        }

        extensions.configure<SigningExtension>("signing") {
            val keyId = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyId")
            val key = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
            val keyPassword = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword")

            if (keyId != null && key != null && keyPassword != null) {
                useInMemoryPgpKeys(keyId, key, keyPassword)
            }
        }
    }
}
