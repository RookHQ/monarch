package com.rookhq.monarch.rules

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The stock-widget gate (visual-spec/plan.md, "Recipes" item 2): a component draws the Monarch primitive, never
 * the Material 3 widget it replaces. A call of one of [STOCK_WIDGETS] is allowed only in a file of [ALLOWED]:
 * the primitives that wrap the platform widget (the dialog, sheet, menu, tooltip, date picker and slider layers),
 * and callers not migrated yet, each with its reason. The list only shrinks: an entry whose file no longer calls a
 * stock widget fails too, so it is removed when its file is migrated.
 *
 * A call counts when the file imports the widget from `androidx.compose.material3` (by name or with `*`) or calls
 * it by its full name. Comments and string literals are skipped, so KDoc may name the widget a primitive replaces.
 */
class StockWidgetRulesTest {
    private val main = File("src/main/kotlin/com/rookhq/monarch")

    private fun stockCalls(file: File): List<String> {
        val text = file.readText()
        val imported = IMPORT.findAll(text).map { it.groupValues[1] }.toSet()
        val star = "import androidx.compose.material3.*" in text
        val code = codeOnly(text.lines().filterNot { it.startsWith("import ") }.joinToString("\n"))
        return STOCK_WIDGETS.filter { name ->
            val short = (star || name in imported) && Regex("""(?<![\w.])$name\s*[({]""").containsMatchIn(code)
            short || Regex("""androidx\.compose\.material3\.$name\s*[({]""").containsMatchIn(code)
        }
    }

    /** [source] without its comments and the text of its string literals (templates are dropped too). */
    private fun codeOnly(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            val skipped = skipLiteralOrComment(source, i)
            if (skipped == null) {
                out.append(source[i++])
            } else {
                out.append(skipped.second)
                i = skipped.first
            }
        }
        return out.toString()
    }

    /** At [i], a comment or a literal: the index after it and what stands in for it; `null` for code. */
    private fun skipLiteralOrComment(source: String, i: Int): Pair<Int, String>? = when {
        source.startsWith("//", i) -> (endOf(source, "\n", i) - 1) to ""
        source.startsWith("/*", i) -> endOf(source, "*/", i + 2) to ""
        source.startsWith("\"\"\"", i) -> endOf(source, "\"\"\"", i + 3) to "\"\""
        source[i] == '"' -> endOfString(source, i + 1) to "\"\""
        source[i] == '\'' && i + 2 < source.length -> {
            val end = source.indexOf('\'', i + if (source[i + 1] == '\\') 3 else 2)
            (if (end < 0) source.length else end + 1) to "' '"
        }
        else -> null
    }

    /** The index after the closing quote of a one-line string that starts at [from]. */
    private fun endOfString(source: String, from: Int): Int {
        var i = from
        while (i < source.length && source[i] != '"' && source[i] != '\n') i += if (source[i] == '\\') 2 else 1
        return i + 1
    }

    /** The index after the first [token] at or after [from], or the end of [source]. */
    private fun endOf(source: String, token: String, from: Int): Int =
        source.indexOf(token, from).let { if (it < 0) source.length else it + token.length }

    private fun callers(): Map<String, List<String>> =
        main.walkTopDown().filter { it.isFile && it.extension == "kt" }.sortedBy { it.path }
            .associate { it.relativeTo(main).invariantSeparatorsPath to stockCalls(it) }
            .filterValues { it.isNotEmpty() }

    @Test
    fun theSourceTreeIsWhereThisTestExpectsIt() {
        assertTrue(File(main, "components").isDirectory, "Run from the monarch-compose module directory")
    }

    @Test
    fun componentsDrawMonarchPrimitivesNotStockMaterialWidgets() {
        val offenders = callers().filterKeys { it !in ALLOWED }.map { (path, names) -> "$path: $names" }
        assertEquals(
            emptyList(),
            offenders,
            "Use the Monarch primitive (docs/PRIMITIVES.md) in place of these Material 3 widgets",
        )
    }

    @Test
    fun theAllowListOnlyShrinks() {
        val calling = callers().keys
        val stale = ALLOWED.keys.filterNot { it in calling }
        assertEquals(emptyList(), stale, "These files no longer call a stock widget: remove them from ALLOWED")
    }

    @Test
    fun everyAllowedFileHasAReason() {
        assertEquals(emptyList(), ALLOWED.filterValues { it.isBlank() }.keys.toList())
    }

    private companion object {
        val IMPORT = Regex("""^import androidx\.compose\.material3\.(\w+)\s*$""", RegexOption.MULTILINE)

        /** The Material 3 widgets that have a Monarch primitive (docs/PRIMITIVES.md). */
        val STOCK_WIDGETS = listOf(
            "OutlinedTextField", "TextField", "AlertDialog", "BasicAlertDialog", "ModalBottomSheet",
            "DropdownMenu", "DropdownMenuItem", "ExposedDropdownMenuBox", "ListItem",
            "FilterChip", "ElevatedFilterChip", "AssistChip", "InputChip", "SuggestionChip",
            "Checkbox", "TriStateCheckbox", "RadioButton", "Switch",
            "SegmentedButton", "SingleChoiceSegmentedButtonRow", "MultiChoiceSegmentedButtonRow",
            "Tab", "LeadingIconTab", "TabRow", "PrimaryTabRow", "SecondaryTabRow", "ScrollableTabRow",
            "PrimaryScrollableTabRow", "SecondaryScrollableTabRow",
            "IconButton", "FilledIconButton", "FilledTonalIconButton", "OutlinedIconButton",
            "IconToggleButton", "FilledIconToggleButton", "OutlinedIconToggleButton",
            "HorizontalDivider", "VerticalDivider", "Divider", "TooltipBox",
            "DatePickerDialog", "DatePicker", "DateRangePicker", "TimePickerDialog", "TimePicker", "TimeInput",
            "Slider", "RangeSlider",
            "TextButton", "OutlinedButton", "Button", "ElevatedButton", "FilledTonalButton",
            "Card", "OutlinedCard", "ElevatedCard",
        )

        const val PLATFORM_LAYER = "Monarch primitive that wraps the platform widget (decision D10.1)"
        const val GHOST_TEXT = "MonarchAiGhostText is the one caller left on a stock field (OutlinedTextField on a " +
            "TextFieldState). MonarchTextField has no TextFieldState overload yet, and the field needs three things " +
            "from it: an OutputTransformation that draws the suggestion after the caret, the TextLayoutResult of " +
            "the text, and the text's own coordinates inside the box (the label sits above it) to turn a tap into " +
            "a position in the text. The move needs a device check of the tap on the suggestion, so it stays until " +
            "that overload is added"

        /** Paths from `src/main/kotlin/com/rookhq/monarch`. */
        val ALLOWED: Map<String, String> = mapOf(
            "components/overlays/dialog/MonarchDialog.kt" to PLATFORM_LAYER,
            "components/overlays/bottomsheet/MonarchBottomSheet.kt" to PLATFORM_LAYER,
            "components/overlays/menu/MonarchMenu.kt" to PLATFORM_LAYER,
            "components/overlays/tooltip/MonarchTooltip.kt" to PLATFORM_LAYER,
            "internal/MonarchDatePickerDialog.kt" to PLATFORM_LAYER,
            "internal/MonarchPickerDialog.kt" to PLATFORM_LAYER,
            "components/layout/divider/MonarchDivider.kt" to PLATFORM_LAYER,
            "components/forms/slider/MonarchSlider.kt" to PLATFORM_LAYER,
            "components/forms/rangeslider/MonarchRangeSlider.kt" to PLATFORM_LAYER,
            "components/forms/select/MonarchSelect.kt" to PLATFORM_LAYER,
            "components/forms/select/MonarchMenuRow.kt" to PLATFORM_LAYER,
            "components/forms/combobox/ComboboxFields.kt" to PLATFORM_LAYER,
            "components/intelligence/aighosttext/MonarchAiGhostText.kt" to GHOST_TEXT,
        )
    }
}
