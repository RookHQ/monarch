package com.rookhq.monarch.testing

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The icon rules of the visual spec (icons.md, PREMIUM.md rule 9) for the code people copy: the inventory
 * examples, the sample app and the docs showcases. They draw outlined icons from `SampleIcons` only, so no
 * page mixes filled and outlined glyphs and no example shows Android 1.x clip art. Nothing is listed here:
 * the test walks the folders, in the style of `SourceRulesTest`.
 */
class IconSourceRulesTest {
    private val android = File("..")
    private val inventory = File(android, "inventory")
    private val sample = File(android, "sample/src")
    private val showcases = File("src/test/kotlin/com/rookhq/monarch/screenshots/showcase")
    private val previews = File("src/debug/kotlin")

    private fun files(root: File, extension: String): List<File> =
        root.walkTopDown().filter { it.isFile && it.extension == extension }.sortedBy { it.path }.toList()

    /** Every file in scope, with the inventory files read as the code of their examples. */
    private fun sources(): List<File> = files(inventory, "json") + files(sample, "kt") + files(showcases, "kt") +
        files(previews, "kt")

    private fun violations(pattern: Regex): List<String> =
        sources().flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (pattern.containsMatchIn(line)) "${file.relativeTo(android).path}:${index + 1}" else null
            }
        }

    @Test
    fun theFoldersAreWhereThisTestExpectsThem() {
        assertTrue(files(inventory, "json").size > 100, "Run from the monarch-compose module directory")
        assertTrue(files(sample, "kt").size > 100)
        assertTrue(files(showcases, "kt").isNotEmpty())
        assertTrue(files(previews, "kt").size > 100)
    }

    @Test
    fun noFrameworkDrawableIsUsedAsAnIcon() {
        assertEquals(emptyList(), violations(Regex("""android\.R\.drawable""")), "Use SampleIcons (docs/PREVIEWS.md)")
    }

    @Test
    fun noFilledIconIsUsed() {
        val filled = Regex("""Icons\.(AutoMirrored\.)?(Default|Filled|Rounded|Sharp|TwoTone)\b""")
        val filledImport = Regex("""material\.icons\.(automirrored\.)?(filled|rounded|sharp|twotone)\.""")
        assertEquals(emptyList(), violations(filled) + violations(filledImport), "Use the outlined SampleIcons")
    }

    /** One place to swap the icon source: the Material icons are reached through `SampleIcons` only. */
    @Test
    fun materialIconsAreReachedThroughSampleIcons() {
        val direct = Regex("""\bIcons\.(Outlined|AutoMirrored)\b|import androidx\.compose\.material\.icons\.""")
        assertEquals(emptyList(), violations(direct), "Use SampleIcons.<Name>; add a name there if one is missing")
    }

    @Test
    fun theOldHandDrawnCatalogueIconsAreGone() {
        assertEquals(emptyList(), violations(Regex("""\bCatalogueIcons\b""")), "Use SampleIcons")
    }
}
