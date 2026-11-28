package dev.monarchui.rules

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The source rules of docs/ADDING-COMPONENTS.md that can be checked by reading
 * the source. Nothing is listed here: the test walks the folders, so a new
 * component is covered as soon as its files exist.
 */
class SourceRulesTest {
    private val main = File("src/main/kotlin/dev/monarchui")
    private val debug = File("src/debug/kotlin/dev/monarchui")
    private val components = File(main, "components")

    private fun kotlinFiles(root: File): List<File> =
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.sortedBy { it.path }.toList()

    /** `components/<family>/<name>` folders, as paths from `components`. */
    private fun componentFolders(): List<String> =
        components.listFiles { file -> file.isDirectory }.orEmpty().flatMap { family ->
            family.listFiles { file -> file.isDirectory }.orEmpty().map { "${family.name}/${it.name}" }
        }.sorted()

    private fun violations(files: List<File>, pattern: Regex): List<String> =
        files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val code = line.substringBefore("//")
                val isDoc = line.trimStart().startsWith("*") || line.trimStart().startsWith("/**")
                if (!isDoc && pattern.containsMatchIn(code)) "${file.name}:${index + 1}: ${line.trim()}" else null
            }
        }

    @Test
    fun theSourceTreeIsWhereThisTestExpectsIt() {
        assertTrue(components.isDirectory, "Run from the monarch-compose module directory")
        assertTrue(componentFolders().isNotEmpty())
    }

    @Test
    fun componentsLiveInAFamilyFolder() {
        val families = components.listFiles { file -> file.isDirectory }.orEmpty()
        val strays = components.listFiles { file -> file.isFile }.orEmpty().map { it.name } +
            families.flatMap { family ->
                family.listFiles { file -> file.isFile }.orEmpty().map { "${family.name}/${it.name}" }
            }
        assertEquals(emptyList(), strays, "Files must be in components/<family>/<name>/")
        val unknown = components.listFiles { file -> file.isDirectory }.orEmpty().map { it.name } - FAMILIES
        assertEquals(emptyList(), unknown, "Not a family folder")
    }

    @Test
    fun everyFileIsInThePackageOfItsFolder() {
        val wrong = (kotlinFiles(main) + kotlinFiles(debug)).mapNotNull { file ->
            val root = if (file.path.startsWith(debug.path)) debug else main
            val folder = checkNotNull(file.parentFile).relativeTo(root).invariantSeparatorsPath
            val segments = listOf("dev.monarchui", folder.replace('/', '.')).filter { it.isNotEmpty() }
            val expected = segments.joinToString(".")
            val declared = file.readLines().firstOrNull { it.startsWith("package ") }?.removePrefix("package ")
            if (declared == expected) null else "${file.name}: package $declared, expected $expected"
        }
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun componentsUseNoLiteralColours() {
        val literal = Regex("""Color\(\s*0x|Color\(\s*\d|Color\.($NAMED_COLOURS)""")
        assertEquals(emptyList(), violations(kotlinFiles(components), literal))
    }

    @Test
    fun foundationsThemeAccessibilityAndInternalsUseNoLiteralColours() {
        val literal = Regex("""Color\(\s*0x|Color\.($NAMED_COLOURS)""")
        val files = listOf("theme", "foundation", "accessibility", "internal").flatMap { kotlinFiles(File(main, it)) }
        assertEquals(emptyList(), violations(files, literal))
    }

    @Test
    fun componentsReadColoursFromTheThemeNotFromTokenPrimitives() {
        val rawColour = Regex("""MonarchTokens\.($COLOUR_TOKEN_GROUPS)\b""")
        assertEquals(emptyList(), violations(kotlinFiles(components), rawColour))
    }

    @Test
    fun componentsUseNoLiteralDpOrSpSizes() {
        val literalSize = Regex("""\b\d+(\.\d+)?f?\.(dp|sp)\b""")
        assertEquals(emptyList(), violations(kotlinFiles(components), literalSize))
    }

    @Test
    fun componentsUseNoAlphaToDimColours() {
        val dimmed = Regex("""\.copy\(\s*alpha\s*=|\.alpha\(\s*0?\.\d""")
        assertEquals(emptyList(), violations(kotlinFiles(components), dimmed))
    }

    /** Previews are in src/debug, so they are not published; every component folder has one there. */
    @Test
    fun everyComponentFolderHasAPreviewFile() {
        val missing = componentFolders().filter { folder ->
            File(debug, "components/$folder").listFiles().orEmpty().none { it.name.endsWith("Previews.kt") }
        }
        assertEquals(emptyList(), missing, "Add src/debug/…/components/<family>/<name>/<Name>Previews.kt")
    }

    @Test
    fun noPreviewIsInThePublishedSourceSet() {
        val inMain = kotlinFiles(main).filter { "@Preview" in it.readText() || "@MonarchPreviews" in it.readText() }
        assertEquals(emptyList(), inMain.map { it.name })
    }

    /** One public composable name per file; overloads of that name are allowed. */
    @Test
    fun everyFileHasAtMostOnePublicComponent() {
        val publicComposable = Regex("""^public fun (Monarch\w+)\(""")
        val offenders = kotlinFiles(components).mapNotNull { file ->
            val names = file.readLines().mapNotNull { publicComposable.find(it)?.groupValues?.get(1) }.toSet()
            when {
                names.size > 1 -> "${file.name}: $names"
                names.size == 1 && names.single() != file.nameWithoutExtension -> "${file.name}: $names"
                else -> null
            }
        }
        assertEquals(emptyList(), offenders)
    }

    /** A component folder is named after its component: `MonarchSwitchRow` lives in `switchrow`. */
    @Test
    fun everyComponentFolderHoldsTheComponentItIsNamedAfter() {
        val wrong = componentFolders().filter { folder ->
            val name = folder.substringAfter('/')
            File(components, folder).listFiles().orEmpty()
                .none { it.nameWithoutExtension.removePrefix("Monarch").lowercase() == name }
        }
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun builtInStringsAreOneFilePerComponent() {
        val values = File("src/main/res/values")
        val perComponent = Regex("""strings_[a-z0-9_]+\.xml""")
        val wrong = values.listFiles().orEmpty().map { it.name }.filterNot { perComponent.matches(it) }
        assertEquals(emptyList(), wrong, "Use res/values/strings_<component>.xml")
    }

    private companion object {
        const val NAMED_COLOURS = "Black|White|Red|Green|Blue|Gray|Yellow|Cyan|Magenta"
        const val COLOUR_TOKEN_GROUPS = "light|dark|hcLight|hcDark|Neutral|Disabled|Brand|Status|Spectrum|Accents"
        val FAMILIES = setOf(
            "actions", "billing", "charts", "collaboration", "datadisplay", "datagrid", "feedback", "files", "forms",
            "geo", "identity", "intelligence", "interaction", "layout", "navigation", "overlays", "scheduling",
            "search", "technical", "typography", "workflow",
        )
    }
}
