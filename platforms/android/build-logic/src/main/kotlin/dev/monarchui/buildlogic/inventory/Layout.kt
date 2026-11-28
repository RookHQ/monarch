package dev.monarchui.buildlogic.inventory

import java.io.File

/** Where the per-component files of Monarch for Android live, relative to `platforms/android`. */
class MonarchLayout(val root: File) {
    val inventory = File(root, "inventory")
    val schema = File(inventory, "schema.json")
    val componentsJson = File(root, "components.json")
    val composeMain = File(root, "monarch-compose/src/main/kotlin/dev/monarchui")
    val composeDebug = File(root, "monarch-compose/src/debug/kotlin/dev/monarchui")
    val composeTest = File(root, "monarch-compose/src/test/kotlin")
    val composeRes = File(root, "monarch-compose/src/main/res/values")
    val sampleSections = File(root, "sample/src/main/kotlin/dev/monarchui/sample/sections")
    val parity = File(root.parentFile, "parity.json")

    /** The cross-platform scenario fixtures, written by scripts/native/fixtures.mjs. */
    val parityFixtures = File(root, "monarch-compose/src/test/kotlin/dev/monarchui/fixtures/ParityFixtures.kt")

    /** Path of [file] from the root, for messages. */
    fun relative(file: File): String = file.relativeTo(root).invariantSeparatorsPath
}

/** The mapping from a web family to a Kotlin package segment and a folder of the sample. */
object Families {
    /** Family of entries that have no web id: the theme and the icon. */
    const val FOUNDATIONS = "foundations"

    private val titles = mapOf(
        "actions" to "Actions", "billing" to "Billing", "charts" to "Charts",
        "collaboration" to "Collaboration", "datadisplay" to "Data display", "datagrid" to "Data grid",
        "feedback" to "Feedback", "files" to "Files", "forms" to "Forms", "foundations" to "Foundations",
        "geo" to "Geo", "identity" to "Identity", "intelligence" to "Intelligence",
        "interaction" to "Interaction", "layout" to "Layout", "navigation" to "Navigation",
        "overlays" to "Overlays", "scheduling" to "Scheduling", "search" to "Search",
        "technical" to "Technical", "typography" to "Typography", "workflow" to "Workflow",
    )

    /** `data-display` gives `datadisplay`; `crm` is filed under `workflow`. */
    fun packageOf(family: String): String = if (family == "crm") "workflow" else family.replace("-", "")

    /** The display title of a family package, for the catalogue. `null` when the folder is not a family. */
    fun titleOf(familyPackage: String): String? = titles[familyPackage]

    /** Every family package, for messages. */
    val packages: Set<String> get() = titles.keys
}

/** `progress-bar` gives `ProgressBar`. */
fun pascal(id: String): String = id.split('-', ' ').joinToString("") { it.replaceFirstChar(Char::uppercase) }

/** `MonarchSwitchRow` gives `switchrow`: the package segment and folder of a component. */
fun componentPackage(typeName: String): String = typeName.removePrefix("Monarch").lowercase()

/** Every `.kt` file under [directory], sorted, so generated output is deterministic. */
fun kotlinFiles(directory: File): List<File> =
    if (!directory.isDirectory) {
        emptyList()
    } else {
        directory.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.invariantSeparatorsPath }.toList()
    }
