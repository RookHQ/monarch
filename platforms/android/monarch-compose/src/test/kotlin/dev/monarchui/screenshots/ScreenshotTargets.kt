package dev.monarchui.screenshots

import androidx.compose.runtime.Composable
import dev.monarchui.generated.AllComponentFixtures
import dev.monarchui.generated.examples.AllInventoryExamples
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.tokens.MonarchThemeTokens

/** One labelled state of a component in a screenshot: a fixture, or an inventory example. */
internal class ScreenshotCell(val label: String, val tall: Boolean, val content: @Composable () -> Unit)

/** What one baseline image shows: every cell of one component, stacked. */
internal class ScreenshotTarget(val id: String, val cells: List<ScreenshotCell>, val source: Source) {
    /** Where the cells come from. */
    enum class Source { Fixtures, Examples }
}

/** The theme, text scale and layout direction of one baseline image. */
internal class ScreenshotVariant(val theme: MonarchThemeTokens, val fontScale: Float, val rtl: Boolean) {
    /** The part of the file name after the id: `light`, `dark.x2.rtl`. The default is the bare theme id. */
    val suffix: String
        get() = buildString {
            append(theme.id)
            if (fontScale != 1f) append(".x2")
            if (rtl) append(".rtl")
        }

    /** The two images the docs show for every component. */
    val isPreview: Boolean get() = fontScale == 1f && !rtl && (theme.id == "light" || theme.id == "dark")
}

/**
 * The fixture-to-capture adapter. Every component that has fixtures is shown through its fixtures (the same
 * `ComponentFixtures` objects that `ComponentMatrixTest` renders); every other inventory entry that has
 * compiled examples (the native and compose recipes) is shown through its examples. Nothing is listed here:
 * a new fixture object or a new inventory example is picked up by the generated registries.
 */
internal object ScreenshotTargets {
    /**
     * Components that cannot be shown in a repeatable image, with the reason. They are checked by the other tests
     * (`ComponentMatrixTest`) and listed in the report; a new entry here needs a reason a reviewer accepts.
     */
    val excluded: Map<String, String> = mapOf(
        "emoji-picker" to "hosts the system emoji picker View (androidx.emoji2 EmojiPickerView), which loads its " +
            "emoji on a background thread, so the image is empty or full depending on timing, and the test font " +
            "has no colour emoji glyphs to draw. A stub of the picker would show a layout that is not Monarch's. " +
            "The behaviour is tested in MonarchEmojiPickerTest.",
    )

    /**
     * Cells whose image is empty on purpose, as `<id>/<cell label>`, with the reason. Any other empty cell fails
     * the run, because it means a fixture, a window or a capture is broken.
     */
    val intentionallyBlank: Map<String, String> = mapOf(
        "org-chart/empty" to "a chart with no people draws nothing",
        "typing-indicator/nobody, space reserved" to "the indicator reserves its space and draws nothing while " +
            "nobody types",
    )

    /** All targets, sorted by id. */
    val all: List<ScreenshotTarget> by lazy {
        val fromFixtures = AllComponentFixtures.groupBy { it.id }.map { (id, sets) ->
            ScreenshotTarget(
                id,
                sets.flatMap { it.fixtures }.map { ScreenshotCell(it.name, it.tall, it.content) },
                ScreenshotTarget.Source.Fixtures,
            )
        }
        val withFixtures = fromFixtures.map { it.id }.toSet()
        val fromExamples = AllInventoryExamples.filter { it.id !in withFixtures }.groupBy { it.id }.map { (id, list) ->
            ScreenshotTarget(
                id,
                list.map { ScreenshotCell(it.title, tall = false, content = it.content) },
                ScreenshotTarget.Source.Examples,
            )
        }
        (fromFixtures + fromExamples).filter { it.id !in excluded }.sortedBy { it.id }
    }

    /** The default image of every component: light and dark, default text size, left to right. */
    private val basic: List<ScreenshotVariant> =
        AllThemes.filter { it.id == "light" || it.id == "dark" }.map { ScreenshotVariant(it, 1f, rtl = false) }

    /** The core set: four themes, text scale 1.0 and 2.0, left to right and right to left. */
    private val full: List<ScreenshotVariant> = AllThemes.flatMap { theme ->
        listOf(1f, LargestFontScale).flatMap { scale ->
            listOf(false, true).map { ScreenshotVariant(theme, scale, it) }
        }
    }

    /** The variants of [id] per D9. The default light and dark images come first. */
    fun variantsFor(id: String, core: Set<String>): List<ScreenshotVariant> =
        if (id in core) full.sortedByDescending { it.isPreview } else basic
}
