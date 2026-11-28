package dev.monarchui.rules

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The inventory status and the compiler marking say the same thing (C-056):
 * an `experimental` entry's component carries `@ExperimentalMonarchApi` on
 * every public declaration of its name, an `available` one on none, and a
 * marked declaration belongs to an experimental entry. A pattern (ADR 0017,
 * `"layer": "patterns"`) is always experimental.
 */
class ExperimentalStatusRulesTest {
    private val components = File("src/main/kotlin/dev/monarchui/components")
    private val inventory = File("../inventory")

    private class Entry(
        val id: String,
        val name: String?,
        val family: String,
        val coverage: String,
        val status: String,
        val layer: String?,
    )

    private fun field(text: String, key: String): String? =
        Regex("""^ {2}"$key": (?:"([^"]*)"|null)""", RegexOption.MULTILINE).find(text)
            ?.groupValues?.get(1)?.ifEmpty { null }

    private fun entries(): List<Entry> =
        inventory.listFiles { file -> file.extension == "json" && file.name != "schema.json" }.orEmpty()
            .sortedBy { it.name }
            .map { file ->
                val text = file.readText()
                Entry(
                    id = file.nameWithoutExtension,
                    name = field(text, "name"),
                    family = field(text, "family").orEmpty(),
                    coverage = field(text, "coverage").orEmpty(),
                    status = field(text, "status").orEmpty(),
                    layer = field(text, "layer"),
                )
            }

    private fun sourceOf(entry: Entry): File? {
        val name = entry.name ?: return null
        val folder = "${entry.family.replace("-", "")}/${name.removePrefix("Monarch").lowercase()}"
        val file = File(components, "$folder/$name.kt")
        return file.takeIf { entry.coverage == "build" && it.isFile }
    }

    /** Every public declaration named [name] in [text], and whether it is marked experimental. */
    private fun declarations(text: String, name: String): List<Boolean> =
        Regex("""((?:^@[^\n]*\n)*)^public $KINDS (?:<[^>]*> )?$name\b""", RegexOption.MULTILINE).findAll(text)
            .map { "@ExperimentalMonarchApi" in it.groupValues[1] }
            .toList()

    @Test
    fun theInventoryIsWhereThisTestExpectsIt() {
        assertTrue(inventory.isDirectory, "Run from the monarch-compose module directory")
        assertTrue(entries().size > 400)
    }

    @Test
    fun theStatusAndTheMarkingAgree() {
        val wrong = entries().mapNotNull { entry ->
            val file = sourceOf(entry) ?: return@mapNotNull null
            val marks = declarations(file.readText(), entry.name!!)
            val experimental = entry.status == "experimental"
            when {
                marks.isEmpty() -> "${entry.id}: no public declaration ${entry.name} in ${file.name}"
                experimental && !marks.all { it } ->
                    "${entry.id}: experimental, but ${entry.name} lacks @ExperimentalMonarchApi"
                !experimental && marks.any { it } ->
                    "${entry.id}: ${entry.status}, but ${entry.name} has @ExperimentalMonarchApi"
                else -> null
            }
        }
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun aMarkedDeclarationBelongsToAnExperimentalEntry() {
        val byFolder = entries().filter { it.coverage == "build" && it.name != null }
            .groupBy { "${it.family.replace("-", "")}/${it.name!!.removePrefix("Monarch").lowercase()}" }
        val marked = Regex("""@ExperimentalMonarchApi\n(?:@[^\n]*\n)*public [a-z ]*?$KINDS (?:<[^>]*> )?(\w+)""")
        val wrong = components.walkTopDown().filter { it.isFile && it.extension == "kt" }.sortedBy { it.path }
            .toList()
            .flatMap { file ->
                val folder = file.parentFile!!.relativeTo(components).invariantSeparatorsPath
                val owners = byFolder[folder].orEmpty()
                marked.findAll(file.readText()).mapNotNull { match ->
                    val stable = owners.filter { it.status != "experimental" }.map { it.id }
                    if (owners.isNotEmpty() && stable.size == owners.size) {
                        "${file.name}: ${match.groupValues[1]} is marked experimental, but $stable is not"
                    } else {
                        null
                    }
                }.toList()
            }
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun everyEntryHasALayerAndAPatternIsExperimental() {
        val wrong = entries().mapNotNull { entry ->
            when {
                entry.layer !in setOf("components", "patterns") -> "${entry.id}: layer ${entry.layer}"
                entry.layer == "patterns" && entry.status != "experimental" ->
                    "${entry.id}: a pattern that is ${entry.status}"
                else -> null
            }
        }
        assertEquals(emptyList(), wrong)
    }

    private companion object {
        const val KINDS = "(?:fun|class|object|interface|data class|sealed interface)"
    }
}
