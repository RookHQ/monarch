import dev.monarchui.buildlogic.LOCAL_REPOSITORY_NAME
import dev.monarchui.buildlogic.inventory.ComponentSelection
import dev.monarchui.buildlogic.inventory.GenerateInventory
import dev.monarchui.buildlogic.inventory.ParityReport

// Tasks of the root project: generation of the shared inventory file, the gates, and the
// combined API reference.
//
//   componentCheck -Pcomponent=<name>[,<name>...]   the fast loop for one or more components
//   verifyLocal                         what an engineer passes before handing over
//   mergeCheck                          what the lead runs on the merged tree
//   verifyMonarch                       the complete gate (includes the screenshot compare)
//   verifyScreenshots, recordScreenshots  the screenshot tests alone, compared or recorded

plugins {
    // dokkaGeneratePublicationHtml writes the API reference of both libraries to
    // build/dokka/html, which the docs site serves at /android/api/.
    id("org.jetbrains.dokka")
}

dependencies {
    dokka(project(":monarch-tokens"))
    dokka(project(":monarch-compose"))
    dokka(project(":monarch-fonts-poppins"))
}

dokka {
    moduleName.set("Monarch for Android")
    dokkaGeneratorIsolation.set(ProcessIsolation { maxHeapSize.set("2g") })
}

val perComponentFiles = files(
    fileTree("inventory") { include("*.json") },
    fileTree("monarch-compose/src") { include("**/*.kt", "main/res/values/*.xml") },
    fileTree("sample/src/main/kotlin") { include("**/sections/**/*.kt") },
)

val generateInventory = tasks.register<GenerateInventory>("generateInventory") {
    group = "monarch"
    description = "Validates inventory/*.json and the conventions, and writes components.json."
    monarchRoot.set(layout.projectDirectory)
    sources.from(perComponentFiles)
    check.set(false)
    outputs.upToDateWhen { false }
}

val checkInventory = tasks.register<GenerateInventory>("checkInventory") {
    group = "monarch"
    description = "Validates inventory/*.json and the conventions, and fails if components.json is stale."
    monarchRoot.set(layout.projectDirectory)
    sources.from(perComponentFiles)
    check.set(true)
    outputs.upToDateWhen { false }
    mustRunAfter(generateInventory)
}

val parityReport = tasks.register<ParityReport>("parityReport") {
    group = "monarch"
    description = "Compares the inventory with ../parity.json. Reports only."
    monarchRoot.set(layout.projectDirectory)
    sources.from(perComponentFiles)
    outputs.upToDateWhen { false }
    mustRunAfter(generateInventory)
}

// Gates that every mode runs.
val codeGates = listOf(
    // Static analysis. No baseline files.
    ":monarch-tokens:detekt",
    ":monarch-compose:detekt",
    ":monarch-fonts-poppins:detekt",
    ":sample:detekt",
    // Android lint, warnings as errors.
    ":monarch-compose:lintDebug",
    ":monarch-fonts-poppins:lintDebug",
    ":sample:lintDebug",
    // Unit and Compose UI tests on the JVM (Robolectric).
    ":monarch-tokens:test",
    ":monarch-compose:testDebugUnitTest",
    ":monarch-fonts-poppins:testDebugUnitTest",
    ":sample:testDebugUnitTest",
    // The catalogue app builds.
    ":sample:assembleDebug",
)

// Gates that only make sense on the merged tree.
val publish = "publishAllPublicationsTo${LOCAL_REPOSITORY_NAME.replaceFirstChar(Char::uppercase)}Repository"
val mergeGates = listOf(
    // Public API against the committed dumps in <module>/api/.
    ":monarch-tokens:apiCheck",
    ":monarch-compose:apiCheck",
    ":monarch-fonts-poppins:apiCheck",
    // The catalogue also builds minified, the way a shipping app consumes the library.
    ":sample:assembleRelease",
    // The libraries publish to build/repo with a complete POM.
    ":monarch-tokens:$publish",
    ":monarch-compose:$publish",
    ":monarch-fonts-poppins:$publish",
)

tasks.register("verifyLocal") {
    group = "monarch"
    description = "Everything to pass before handing a component over. Skips the API check and other merge-time gates."
    dependsOn(generateInventory)
    dependsOn(codeGates)
}

tasks.register("verifyMonarch") {
    group = "monarch"
    description = "The complete gate: generated files are current, tests, API check, detekt, lint, sample builds, publish."
    dependsOn(checkInventory)
    dependsOn(codeGates)
    dependsOn(mergeGates)
}

// Screenshot baselines (WS-S). The mode is read by monarch.components from the requested task names.
tasks.register("verifyScreenshots") {
    group = "monarch"
    description = "Renders every component and compares it with the baselines in monarch-compose/src/test/screenshots."
    dependsOn(":monarch-compose:testDebugUnitTest")
}

tasks.register("recordScreenshots") {
    group = "monarch"
    description = "Renders every component and writes the baselines. Run it only on the reference environment (Robolectric on this Mac)."
    dependsOn(":monarch-compose:testDebugUnitTest")
}

// Docs previews: one primary example per component (PreviewCaptureTest), then compressed in place.
tasks.register<Exec>("recordPreviews") {
    group = "monarch"
    description = "Renders the docs previews (-Pmonarch.previews.dir=<folder>, -Pmonarch.previews.ids=a,b) and compresses them."
    dependsOn(":monarch-compose:testDebugUnitTest")
    val dir = providers.gradleProperty("monarch.previews.dir").orNull
        ?: project(":monarch-compose").layout.buildDirectory.dir("outputs/previews").get().asFile.absolutePath
    commandLine("python3", rootProject.layout.projectDirectory.dir("../../scripts/native/compress-previews.py").asFile.path, dir)
}

tasks.register("mergeCheck") {
    group = "monarch"
    description = "For the merged tree: regenerates components.json and the API dumps, reports parity, then verifyMonarch."
    dependsOn(generateInventory, ":monarch-tokens:apiDump", ":monarch-compose:apiDump", ":monarch-fonts-poppins:apiDump", parityReport, "verifyMonarch")
}

// The fast loop. The selection is applied to the test and detekt tasks in monarch.components.
val selection = ComponentSelection.from(project)
tasks.register("componentCheck") {
    group = "monarch"
    description = "Checks components: -P${ComponentSelection.PROPERTY}=<type name, title or id>[,<another>...]."
    if (selection == null) {
        doFirst {
            throw GradleException("componentCheck needs -P${ComponentSelection.PROPERTY}=<type name, title or id>")
        }
    } else {
        dependsOn(
            generateInventory,
            ":monarch-compose:detektComponent",
            ":monarch-compose:testDebugUnitTest",
            ":sample:compileDebugKotlin",
        )
        doLast {
            logger.lifecycle(
                "componentCheck: ${selection.ids.joinToString()} " +
                    "(${selection.files.size} files; tests ${selection.testClasses.joinToString().ifEmpty { "none" }})",
            )
        }
    }
}
