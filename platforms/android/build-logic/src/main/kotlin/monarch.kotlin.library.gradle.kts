import com.rookhq.monarch.buildlogic.configureMonarchCoordinates
import com.rookhq.monarch.buildlogic.configureMonarchPublishing
import com.rookhq.monarch.buildlogic.failFastOnHungTests
import com.rookhq.monarch.buildlogic.intVersion
import com.rookhq.monarch.buildlogic.library
import com.rookhq.monarch.buildlogic.libs
import com.rookhq.monarch.buildlogic.monarchJavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import com.rookhq.monarch.buildlogic.version
import java.time.Duration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// A published pure Kotlin (JVM) library: no Android, no Compose.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("monarch.detekt")
    // API reference (HTML) and the javadoc jar of the publication.
    id("org.jetbrains.dokka")
    // Publications, signing and Maven Central (Central Portal). See Publishing.kt.
    id("com.vanniktech.maven.publish.base")
}

configureMonarchCoordinates()

val bytecodeTarget = libs.version("jvmTarget")

kotlin {
    explicitApi()
    jvmToolchain(libs.intVersion("jdk"))
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(bytecodeTarget))
        freeCompilerArgs.add("-Xjdk-release=$bytecodeTarget")
        allWarningsAsErrors.set(true)
    }
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(15))
    failFastOnHungTests()
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = bytecodeTarget
    targetCompatibility = bytecodeTarget
}

dependencies {
    testImplementation(libs.library("junit"))
    testImplementation(libs.library("kotlin-test-junit"))
}

// API reference. Dokka runs in its own JVM with a fixed heap, so it never grows the Gradle daemon.
dokka {
    moduleName.set(project.name)
    dokkaGeneratorIsolation.set(ProcessIsolation { maxHeapSize.set("2g") })
}

// The java component, with sources and javadoc jars, as the publication `maven`.
configureMonarchPublishing(KotlinJvm(javadocJar = monarchJavadocJar(), sourcesJar = SourcesJar.Sources()))

// mergeCheck runs both in one build: the dump is written first, then checked.
tasks.named("apiCheck") { mustRunAfter("apiDump") }
