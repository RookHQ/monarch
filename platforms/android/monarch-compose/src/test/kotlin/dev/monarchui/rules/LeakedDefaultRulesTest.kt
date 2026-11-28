package dev.monarchui.rules

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Leaked platform defaults (CROSS-PLATFORM.md section 6): stock Material 3 visuals that are not Monarch. Checked by
 * reading the library sources and the sample app, so a new component is covered as soon as its file exists.
 *
 * - A `LinearProgressIndicator` draws Material's stop dot and the gap between fill and track unless the call says
 *   `drawStopIndicator = {}` and `gapSize = ...` (the web bar is one continuous fill).
 * - `android.R.drawable` is 2008 clip art in mixed colours; components draw `MonarchGlyphs`, examples `SampleIcons`.
 * - `MaterialTheme.colorScheme`, `.typography` and `.shapes` in a component read Material's values, not Monarch's
 *   (`MonarchTheme`). The mapping itself lives in theme/ and the one platform layer below.
 * - `MonarchTheme` provides Monarch's ripple, so no surface shows Material's default ripple.
 */
class LeakedDefaultRulesTest {
    private val main = File("src/main/kotlin/dev/monarchui")
    private val sample = File("../sample/src/main/kotlin")

    private fun kotlinFiles(root: File): List<File> =
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.sortedBy { it.path }.toList()

    /** [text] with comment lines (KDoc, `//`) blanked, keeping line numbers. */
    private fun code(text: String): String = text.lines().joinToString("\n") { line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/**")) "" else line
    }

    /** Every call of [name] in [text]: its line and its argument list. */
    private fun calls(text: String, name: String): List<Pair<Int, String>> =
        Regex("""(?<![\w.])$name\(""").findAll(text).map { match ->
            var depth = 1
            var i = match.range.last + 1
            while (depth > 0 && i < text.length) {
                when (text[i]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                i++
            }
            text.substring(0, match.range.first).count { it == '\n' } + 1 to text.substring(match.range.first, i)
        }.toList()

    @Test
    fun theSourceTreesAreWhereThisTestExpectsThem() {
        assertTrue(File(main, "components").isDirectory, "Run from the monarch-compose module directory")
        assertTrue(kotlinFiles(sample).size > 100)
    }

    @Test
    fun noLinearProgressIndicatorShowsMaterialsStopDotOrTrackGap() {
        val offenders = (kotlinFiles(main) + kotlinFiles(sample)).flatMap { file ->
            calls(code(file.readText()), "LinearProgressIndicator").mapNotNull { (line, call) ->
                val missing = listOf("drawStopIndicator", "gapSize").filter { it !in call }
                if (missing.isEmpty()) null else "${file.name}:$line needs ${missing.joinToString(" and ")}"
            }
        }
        assertEquals(emptyList(), offenders, "Pass drawStopIndicator = {} and gapSize = 0, or use MonarchProgress")
    }

    @Test
    fun noFrameworkDrawableIsUsed() {
        val offenders = kotlinFiles(main).filter { "android.R.drawable" in code(it.readText()) }.map { it.name }
        assertEquals(emptyList(), offenders, "Draw MonarchGlyphs (internal) instead")
    }

    @Test
    fun componentsReadMonarchsThemeNotMaterials() {
        val material = Regex("""MaterialTheme\.(colorScheme|typography|shapes)\b""")
        val offenders = kotlinFiles(File(main, "components")).flatMap { file ->
            code(file.readText()).lines().mapIndexedNotNull { index, line ->
                if (material.containsMatchIn(line)) "${file.name}:${index + 1}" else null
            }
        }
        assertEquals(emptyList(), offenders, "Use MonarchTheme.colors, .typography and .shapes")
    }

    @Test
    fun theThemeProvidesMonarchsRipple() {
        val theme = File(main, "theme/MonarchTheme.kt").readText()
        assertTrue("LocalRippleConfiguration provides" in theme, "MonarchTheme must provide a RippleConfiguration")
    }
}
