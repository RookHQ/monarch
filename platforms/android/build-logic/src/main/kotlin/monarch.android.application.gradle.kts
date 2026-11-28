import com.rookhq.monarch.buildlogic.failFastOnHungTests
import com.rookhq.monarch.buildlogic.intVersion
import com.rookhq.monarch.buildlogic.library
import com.rookhq.monarch.buildlogic.libs
import com.rookhq.monarch.buildlogic.version
import java.time.Duration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

// An Android application written with Jetpack Compose. Not published.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("monarch.detekt")
}

val bytecodeTarget = libs.version("jvmTarget")

android {
    compileSdk = libs.intVersion("compileSdk")

    defaultConfig {
        minSdk = libs.intVersion("minSdk")
        targetSdk = libs.intVersion("targetSdk")
        versionCode = 1
        versionName = libs.version("monarch")
    }

    buildTypes {
        release {
            // Minified, so that the library is also built the way a shipping app consumes it.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // No release key exists in this repository. The debug key makes the release build installable.
            signingConfig = signingConfigs.getByName("debug")
        }
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
                test.systemProperty("robolectric.graphicsMode", "NATIVE")
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

// See monarch.android.library: the component has to exist, the tests run once.
tasks.matching { it.name == "testReleaseUnitTest" }.configureEach { enabled = false }

extensions.configure<KotlinAndroidProjectExtension> {
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
    "testImplementation"(platform(libs.library("compose-bom")))
    "testImplementation"(libs.library("compose-ui-test-junit4"))
    "debugImplementation"(platform(libs.library("compose-bom")))
    "debugImplementation"(libs.library("compose-ui-test-manifest"))
    "debugImplementation"(libs.library("compose-ui-tooling"))
}
