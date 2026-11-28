import com.rookhq.monarch.buildlogic.configureMonarchCoordinates
import com.rookhq.monarch.buildlogic.configureMonarchPublishing
import com.rookhq.monarch.buildlogic.failFastOnHungTests
import com.rookhq.monarch.buildlogic.intVersion
import com.rookhq.monarch.buildlogic.library
import com.rookhq.monarch.buildlogic.libs
import com.rookhq.monarch.buildlogic.monarchJavadocJar
import com.rookhq.monarch.buildlogic.version
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SourcesJar
import java.time.Duration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// A published Android library written with Jetpack Compose.
// Kotlin comes from the Android Gradle plugin's built-in Kotlin support.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("monarch.detekt")
    // API reference (HTML) and the javadoc jar of the release publication.
    id("org.jetbrains.dokka")
    // Publications, signing and Maven Central (Central Portal). See Publishing.kt.
    id("com.vanniktech.maven.publish.base")
}

configureMonarchCoordinates()

val bytecodeTarget = libs.version("jvmTarget")

android {
    compileSdk = libs.intVersion("compileSdk")

    defaultConfig {
        minSdk = libs.intVersion("minSdk")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(bytecodeTarget)
        targetCompatibility = JavaVersion.toVersion(bytecodeTarget)
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                // Robolectric draws with the real Android graphics code, so layout and text are measured.
                test.systemProperty("robolectric.graphicsMode", "NATIVE")
                test.systemProperty("monarch.rootDir", rootProject.projectDir.absolutePath)
                test.maxHeapSize = "1g"
                // A hung UI test fails the build instead of blocking it: one test may run 5 minutes
                // (the watchdog names the tests in flight), the whole task 15.
                test.timeout.set(Duration.ofMinutes(15))
                test.failFastOnHungTests()
            }
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = false
    }
}

// The release unit test component has to exist (see gradle.properties), but
// running the same JVM tests against it adds nothing.
tasks.matching { it.name == "testReleaseUnitTest" }.configureEach { enabled = false }

extensions.configure<KotlinAndroidProjectExtension> {
    explicitApi()
    jvmToolchain(libs.intVersion("jdk"))
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(bytecodeTarget))
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    "testImplementation"(libs.library("junit"))
    "testImplementation"(libs.library("kotlin-test-junit"))
    "testImplementation"(libs.library("robolectric"))
    "testImplementation"(libs.library("androidx-test-core"))
    "testImplementation"(libs.library("androidx-test-espresso-core"))
    "testImplementation"(libs.library("androidx-activity-compose"))
    "testImplementation"(platform(libs.library("compose-bom")))
    "testImplementation"(libs.library("compose-ui-test-junit4"))
    "debugImplementation"(platform(libs.library("compose-bom")))
    "debugImplementation"(libs.library("compose-ui-test-manifest"))
    "debugImplementation"(libs.library("compose-ui-tooling"))
}

// API reference. Dokka runs in its own JVM with a fixed heap, so it never grows the Gradle daemon.
dokka {
    moduleName.set(project.name)
    dokkaGeneratorIsolation.set(ProcessIsolation { maxHeapSize.set("2g") })
}

// The release variant, with sources and javadoc jars, as the publication `maven`.
configureMonarchPublishing(
    AndroidSingleVariantLibrary(javadocJar = monarchJavadocJar(), sourcesJar = SourcesJar.Sources(), variant = "release"),
)

// Public API check. binary-compatibility-validator registers its tasks for
// Kotlin JVM projects, but does not know the Android Gradle plugin's built-in
// Kotlin, so the same three tasks are wired here from its task types. The
// release variant is checked, because that is what gets published.
val apiClasses = "compileReleaseKotlin"
val generatedApi = layout.buildDirectory.file("api/${project.name}.api")
val committedApiDir = layout.projectDirectory.dir("api")

val apiBuild = tasks.register<KotlinApiBuildTask>("apiBuild") {
    description = "Builds the Kotlin API dump of the release variant."
    dependsOn(apiClasses)
    inputClassesDirs.from(provider { tasks.named<KotlinCompile>(apiClasses).get().destinationDirectory })
    outputApiFile.set(generatedApi)
    runtimeClasspath.from(configurations.named("bcv-rt-jvm-cp-resolver"))
}

val apiCheck = tasks.register<KotlinApiCompareTask>("apiCheck") {
    group = "verification"
    description = "Checks the public API against the committed dump in api/."
    projectApiFile.set(committedApiDir.file("${project.name}.api"))
    generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
}

tasks.register<Copy>("apiDump") {
    group = "other"
    description = "Writes the public API of the current code to api/."
    from(apiBuild.flatMap { it.outputApiFile })
    into(committedApiDir)
}

tasks.named("check") { dependsOn(apiCheck) }

// mergeCheck runs both in one build: the dump is written first, then checked.
apiCheck.configure { mustRunAfter("apiDump") }
