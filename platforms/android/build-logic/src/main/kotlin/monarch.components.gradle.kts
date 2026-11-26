import java.time.Duration
import com.android.build.api.variant.HostTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.rookhq.monarch.buildlogic.inventory.ComponentSelection
import com.rookhq.monarch.buildlogic.inventory.GenerateComponentTestSources
import com.rookhq.monarch.buildlogic.library
import com.rookhq.monarch.buildlogic.libs
import io.gitlab.arturbosch.detekt.Detekt

// The component library: generated test sources and the fast per-component loop.
plugins {
    id("monarch.android.library")
    // Screenshot tests of the fixtures on the JVM. Nothing is recorded or compared unless a
    // roborazzi task (recordRoborazziDebug, verifyRoborazziDebug) or -Proborazzi.test.* asks for it.
    id("io.github.takahirom.roborazzi")
}

dependencies {
    // Material icons for the inventory examples (compiled by the tests); the library itself bundles no icons.
    "testImplementation"(libs.library("compose-material-icons-extended"))
    "testImplementation"(libs.library("roborazzi"))
    "testImplementation"(libs.library("roborazzi-compose"))
    "testImplementation"(libs.library("roborazzi-junit-rule"))
}

// Screenshot gate (WS-S): ComponentScreenshotTest renders every component and compares it with the baselines in
// src/test/screenshots. It runs inside
// testDebugUnitTest, so verifyMonarch includes it.
//   ./gradlew verifyScreenshots      only the screenshot tests, compared (the default mode)
//   ./gradlew recordScreenshots      only the screenshot tests, writing the baselines
//   -Pmonarch.screenshots=off        skip them in a larger run
//   -Pmonarch.screenshots.ids=a,b    only those components
val screenshotTasks = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
val screenshotOnly = screenshotTasks.any { it == "recordScreenshots" || it == "verifyScreenshots" }
// Docs previews (PreviewCaptureTest): only when recordPreviews is requested. -Pmonarch.previews.dir=<folder> (default
// build/outputs/previews), -Pmonarch.previews.ids=a,b.
val previewsOnly = "recordPreviews" in screenshotTasks
val screenshotMode = when {
    "recordScreenshots" in screenshotTasks -> "record"
    else -> providers.gradleProperty("monarch.screenshots").orNull ?: "verify"
}
val screenshotBaselines = layout.projectDirectory.dir("src/test/screenshots")
tasks.withType<Test>().configureEach {
    // Dates and times are written in the zone of the machine otherwise; the screenshots must not depend on it.
    systemProperty("user.timezone", providers.gradleProperty("monarch.timezone").orNull ?: "UTC")
    systemProperty("monarch.screenshots", screenshotMode)
    systemProperty("monarch.screenshots.dir", screenshotBaselines.asFile.absolutePath)
    systemProperty("monarch.screenshots.out", layout.buildDirectory.dir("outputs/screenshots").get().asFile.absolutePath)
    providers.gradleProperty("monarch.screenshots.ids").orNull?.let { systemProperty("monarch.screenshots.ids", it) }
    // The baselines are inputs: a changed image reruns the test. Recording writes them, so it never skips.
    inputs.files(fileTree(screenshotBaselines)).withPropertyName("screenshotBaselines")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    if (screenshotMode == "record") {
        outputs.upToDateWhen { false }
        outputs.doNotCacheIf("recording screenshots") { true }
    }
    // The screenshot run is long: about 1,400 images, each of several fixtures.
    timeout.set(Duration.ofMinutes(90))
    if (screenshotOnly) filter { includeTestsMatching("*ScreenshotTest") }
    if (previewsOnly) {
        filter { includeTestsMatching("*PreviewCaptureTest") }
        val previewDir = providers.gradleProperty("monarch.previews.dir").orNull
            ?: layout.buildDirectory.dir("outputs/previews").get().asFile.absolutePath
        systemProperty("monarch.previews.dir", previewDir)
        providers.gradleProperty("monarch.previews.ids").orNull?.let { systemProperty("monarch.previews.ids", it) }
        outputs.upToDateWhen { false }
        outputs.doNotCacheIf("recording previews") { true }
    }
}

// Compiled examples and the fixture registry, generated into build/ and
// compiled with the unit tests. Nothing generated is in the source tree.
val generateComponentTestSources = tasks.register<GenerateComponentTestSources>("generateComponentTestSources") {
    group = "monarch"
    description = "Writes the compiled examples of inventory/*.json and the fixture registry."
    monarchRoot.set(rootProject.layout.projectDirectory)
    sources.from(
        rootProject.fileTree("inventory") { include("*.json") },
        fileTree("src/test/kotlin") { include("**/*.kt") },
        fileTree("src/main/kotlin") { include("**/*.kt") },
    )
    outputDir.set(layout.buildDirectory.dir("generated/monarch/test/kotlin"))
}

extensions.configure<LibraryAndroidComponentsExtension> {
    onVariants { variant ->
        variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.sources?.kotlin?.addGeneratedSourceDirectory(
            generateComponentTestSources,
            GenerateComponentTestSources::outputDir,
        )
    }
}

// -Pcomponent=<name>: run only that component's tests, its matrix fixtures,
// its examples and its previews, and analyse only its files.
val selection = ComponentSelection.from(project)
if (selection != null) {
    tasks.withType<Test>().configureEach {
        systemProperty("monarch.component.ids", selection.ids.joinToString(","))
        systemProperty("monarch.component.names", selection.typeNames.joinToString(","))
        filter {
            selection.testClasses.forEach { includeTestsMatching("*.$it") }
            includeTestsMatching("*.ComponentMatrixTest")
            includeTestsMatching("*.TallComponentMatrixTest")
            includeTestsMatching("*.InventoryExamplesTest")
            includeTestsMatching("*.ComponentPreviewsTest")
            includeTestsMatching("*.SourceRulesTest")
            includeTestsMatching("*.IconSourceRulesTest")
        }
    }

    tasks.register<Detekt>("detektComponent") {
        description = "Runs detekt on the files of the component named by -Pcomponent."
        setSource(files(selection.files))
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        parallel = false
        reports {
            html.required.set(false)
            xml.required.set(false)
            txt.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
        }
    }
}
