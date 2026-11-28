package dev.monarchui.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** The `libs` version catalog (gradle/libs.versions.toml), for use inside convention plugins. */
val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** A version from the `[versions]` table. Fails the build if the key is missing. */
fun VersionCatalog.version(alias: String): String =
    findVersion(alias).orElseThrow { IllegalStateException("No version '$alias' in libs.versions.toml") }
        .requiredVersion

/** A version from the `[versions]` table as an integer (SDK levels, JDK versions). */
fun VersionCatalog.intVersion(alias: String): Int = version(alias).toInt()

/** A library from the `[libraries]` table. Fails the build if the alias is missing. */
fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalStateException("No library '$alias' in libs.versions.toml") }
