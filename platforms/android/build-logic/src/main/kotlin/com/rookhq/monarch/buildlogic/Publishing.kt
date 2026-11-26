package com.rookhq.monarch.buildlogic

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.Platform
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure

/** Maven group of every Monarch for Android artifact. */
const val MONARCH_GROUP: String = "com.rookhq.monarch"

/** Name of the local verification repository; the publish task is `publishAllPublicationsToLocalRepository`. */
const val LOCAL_REPOSITORY_NAME: String = "local"

/** Sets the coordinates every module shares. The version comes from the catalog. */
fun Project.configureMonarchCoordinates() {
    group = MONARCH_GROUP
    version = libs.version("monarch")
}

/**
 * Gradle property that selects the javadoc jar: `dokka` builds it from the Dokka
 * HTML (the release workflow sets it); anything else, or unset, publishes an
 * empty javadoc jar, so `verifyMonarch` does not run Dokka on every build.
 */
const val JAVADOC_PROPERTY: String = "monarch.javadoc"

/** Gradle property of the in-memory OpenPGP key (ORG_GRADLE_PROJECT_signingInMemoryKey in CI). */
private const val SIGNING_KEY_PROPERTY = "signingInMemoryKey"

/** The javadoc jar of a publication, chosen by [JAVADOC_PROPERTY]. */
fun Project.monarchJavadocJar(): JavadocJar =
    if (providers.gradleProperty(JAVADOC_PROPERTY).orNull == "dokka") {
        JavadocJar.Dokka("dokkaGeneratePublicationHtml")
    } else {
        JavadocJar.Empty()
    }

/**
 * Publishing of one library module, through the vanniktech maven-publish base plugin:
 *
 * - the publication `maven` of [platform], with sources and javadoc jars;
 * - Maven Central through the Central Portal (`publishToMavenCentral`,
 *   `publishAndReleaseToMavenCentral`). Credentials are the Gradle properties
 *   `mavenCentralUsername` and `mavenCentralPassword`, passed by the release
 *   workflow from the secrets of the `maven-central` environment;
 * - signing of every publication with an in-memory key, only when the
 *   property `signingInMemoryKey` (and `signingInMemoryKeyPassword`) is set.
 *   Without a key nothing is signed, so local publishing works on any machine;
 * - the local verification repository ([configureLocalRepository]).
 */
fun Project.configureMonarchPublishing(platform: Platform) {
    extensions.configure<MavenPublishBaseExtension> {
        configure(platform)
        publishToMavenCentral(automaticRelease = true)
        if (providers.gradleProperty(SIGNING_KEY_PROPERTY).isPresent) {
            signAllPublications()
        }
    }
    extensions.configure<PublishingExtension> { configureLocalRepository(this) }
}

/**
 * Adds the repository every build publishes to for verification: a directory
 * under the root project's `build/`. The `verifyMonarch` gate publishes there
 * and nowhere else.
 */
fun Project.configureLocalRepository(publishing: PublishingExtension) {
    publishing.repositories.maven {
        name = LOCAL_REPOSITORY_NAME
        url = uri(rootProject.layout.buildDirectory.dir("repo"))
    }
}

/** POM metadata shared by all modules. */
fun Project.configureMonarchPom(displayName: String, summary: String) {
    extensions.configure<MavenPublishBaseExtension> {
        pom {
            name.set(displayName)
            description.set(summary)
            url.set("https://github.com/RookHQ/monarch")
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("rookhq")
                    name.set("Rook")
                }
            }
            scm {
                url.set("https://github.com/RookHQ/monarch")
                connection.set("scm:git:https://github.com/RookHQ/monarch.git")
            }
        }
    }
}
