package dev.monarchui.components.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.files.tableblock.MonarchTableBlock
import dev.monarchui.components.files.tableblock.MonarchTableBlockCell
import dev.monarchui.components.files.tableblock.MonarchTableBlockData
import dev.monarchui.components.files.tableblock.MonarchTableBlockLabels
import dev.monarchui.components.files.tableblock.MonarchTableBlockTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

private val Plans = MonarchTableBlockData.of(
    listOf(listOf("Plan", "Seats", "Price"), listOf("Team", "10", "12"), listOf("Business", "Unlimited", "")),
)

internal object MonarchTableBlockFixtures : ComponentFixtures {
    override val id = "table-block"
    override val fixtures = listOf(
        fixture("read-only with a caption") { MonarchTableBlock(data = Plans, caption = "Plans") },
        fixture("read-only, no header, long text") {
            MonarchTableBlock(
                data = MonarchTableBlockData.of(
                    listOf(listOf("A cell with enough text to wrap inside its column", "Short")),
                    hasHeaderRow = false,
                ),
            )
        },
        fixture("editable") {
            MonarchTableBlock(data = MonarchTableBlockData.empty(rowCount = 2, columnCount = 2), onDataChange = {})
        },
        fixture("editable, disabled") {
            MonarchTableBlock(
                data = MonarchTableBlockData.of(listOf(listOf("Read only"))),
                onDataChange = {},
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTableBlockTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dataIsNormalisedToTheLimits() {
        val ragged = MonarchTableBlockData.of(listOf(listOf("a"), listOf("b", "c", "d")))
        assertEquals(listOf(listOf("a", "", ""), listOf("b", "c", "d")), ragged.rows)
        assertEquals(listOf(listOf("")), MonarchTableBlockData.of(emptyList()).rows)
        val huge = MonarchTableBlockData.of(List(60) { List(30) { "x" } })
        assertEquals(MonarchTableBlockData.MAXIMUM_ROW_COUNT, huge.rowCount)
        assertEquals(MonarchTableBlockData.MAXIMUM_COLUMN_COUNT, huge.columnCount)
        assertFalse(huge.canAddRow)
        assertSame(huge, huge.addRow(after = 0))
        assertEquals(1, MonarchTableBlockData.empty(rowCount = 0, columnCount = -2).columnCount)
        assertEquals("", ragged[5, 5])
    }

    @Test
    fun editsReturnNewDataAndImpossibleOnesTheSame() {
        val table = MonarchTableBlockData.empty(rowCount = 2, columnCount = 2)
        val added = table.addRow(after = 0).addColumn(after = 1)
        assertEquals(3, added.rowCount)
        assertEquals(3, added.columnCount)
        val edited = added.withCell(1, 2, "x")
        assertEquals("x", edited[1, 2])
        assertEquals("", added[1, 2])
        assertSame(edited, edited.withCell(9, 9, "y"))
        val one = MonarchTableBlockData.empty(rowCount = 1, columnCount = 1)
        assertSame(one, one.deleteRow(at = 0))
        assertSame(one, one.deleteColumn(at = 0))
        assertEquals(1, table.deleteRow(at = 1).rowCount)
        assertEquals(1, table.deleteColumn(at = 0).columnCount)
        assertFalse(table.withHeaderRow(false).isHeader(0))
        assertTrue(table.isHeader(0))
        assertFalse(table.isHeader(1))
        assertEquals(table, MonarchTableBlockData.empty(rowCount = 2, columnCount = 2))
    }

    @Test
    fun aReadOnlyTableIsANamedCollectionWithHeadingsAndEmptyCells() {
        rule.setMonarchContent { MonarchTableBlock(data = Plans, caption = "Plans") }
        rule.onNodeWithText("Plans").assertDoesNotExist()
        rule.onNodeWithContentDescription("Plans").assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.CollectionInfo, CollectionInfo(3, 3)))
            .assertExists()
        rule.onNodeWithText("Seats").assert(isHeading())
        rule.onNodeWithText("Team").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        rule.onNodeWithContentDescription("Empty").assertExists()
    }

    @Test
    fun aLabelNamesTheTableAndTheCaptionIsThenRead() {
        rule.setMonarchContent {
            MonarchTableBlock(data = Plans, caption = "Plans", label = "Prices per seat")
        }
        rule.onNodeWithContentDescription("Prices per seat").assertExists()
        rule.onNodeWithText("Plans").assertExists()
    }

    @Test
    fun withoutACaptionTheTableIsCalledTable() {
        rule.setMonarchContent { MonarchTableBlock(data = Plans) }
        rule.onNodeWithContentDescription("Table").assertExists()
    }

    @Test
    fun anEditableCellIsAFieldNamedByItsPosition() {
        var data by mutableStateOf(MonarchTableBlockData.empty(rowCount = 2, columnCount = 2))
        rule.setMonarchContent { MonarchTableBlock(data = data, onDataChange = { data = it }) }
        val cell = rule.onNodeWithContentDescription("Row 2, column 1")
        cell.assertTouchTargetAtLeast48dp(rule.density)
        cell.performTextReplacement("Team")
        assertEquals("Team", data[1, 0])
        rule.onNodeWithContentDescription("Row 1, column 1").assert(isHeading())
    }

    @Test
    fun theMenuAddsARowBelowTheActiveCellAndFocusesIt() {
        var data by mutableStateOf(MonarchTableBlockData.empty(rowCount = 2, columnCount = 2))
        rule.setMonarchContent { MonarchTableBlock(data = data, onDataChange = { data = it }) }
        rule.onNodeWithContentDescription("Row 1, column 2").performClick()
        rule.onNodeWithText("Table").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Add row below").performClick()
        assertEquals(3, data.rowCount)
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Row 2, column 2").assertIsFocused()
    }

    @Test
    fun theMenuSwitchesTheHeaderRowAndDeletesTheTable() {
        var data by mutableStateOf(MonarchTableBlockData.empty(rowCount = 2, columnCount = 2))
        var deleted = 0
        rule.setMonarchContent {
            MonarchTableBlock(data = data, onDataChange = { data = it }, onDelete = { deleted++ })
        }
        rule.onNodeWithText("Table").performClick()
        rule.onNodeWithText("Header row")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .performClick()
        assertFalse(data.hasHeaderRow)
        rule.onNodeWithText("Table").performClick()
        rule.onNodeWithText("Delete row").performClick()
        assertEquals(1, data.rowCount)
        rule.onNodeWithText("Table").performClick()
        rule.onNodeWithText("Delete row").assertIsNotEnabled()
        rule.onNodeWithText("Delete table").performClick()
        assertEquals(1, deleted)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchTableBlock(
                data = MonarchTableBlockData.empty(rowCount = 1, columnCount = 1),
                onDataChange = {},
                labels = MonarchTableBlockLabels(
                    region = "Tableau",
                    controls = "Options",
                    addRow = "Ajouter une ligne",
                    cellPosition = { row, column -> "Ligne $row, colonne $column" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Tableau").assertExists()
        rule.onNodeWithContentDescription("Ligne 1, colonne 1").assertExists()
        rule.onNodeWithText("Options").performClick()
        rule.onNodeWithText("Ajouter une ligne").assertExists()
    }

    @Test
    fun aDisabledTableDoesNotRespond() {
        rule.setMonarchContent {
            MonarchTableBlock(data = MonarchTableBlockData.empty(1, 1), onDataChange = {}, enabled = false)
        }
        rule.onNodeWithText("Table").assertIsNotEnabled()
        rule.onNode(hasContentDescription("Row 1, column 1")).assertIsNotEnabled()
    }

    @Test
    fun columnsRunFromTheStartAndTextWrapsAtTheLargestFontScale() {
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        rule.setMonarchContent(environment) {
            MonarchTableBlock(data = MonarchTableBlockData.of(listOf(listOf("Plan", "Seats"))))
        }
        rule.onNodeWithText("Plan").assertTextFullyVisible()
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.onNodeWithText("Plan").assertIsBefore(rule.onNodeWithText("Seats"), direction)
        }
    }

    @Test
    fun tokensFollowTheWebAndIos() {
        val tokens = MonarchTableBlockTokens
        assertEquals("Label", tokens.regionLabel("Label", "Caption", "Table"))
        assertEquals("Caption", tokens.regionLabel(null, "Caption", "Table"))
        assertEquals("Table", tokens.regionLabel("", null, "Table"))
        assertTrue(tokens.captionIsSpokenName(null))
        assertFalse(tokens.captionIsSpokenName("Label"))
        assertEquals("Empty", tokens.cellLabel("  ", "Empty"))
        assertEquals(1f, tokens.widthScale(0.85f))
        assertEquals(2f, tokens.widthScale(3f))
        val table = MonarchTableBlockData.empty(rowCount = 2, columnCount = 3)
        assertEquals(MonarchTableBlockCell(1, 2), tokens.target(null, table))
        assertEquals(MonarchTableBlockCell(1, 1), tokens.target(MonarchTableBlockCell(7, 1), table))
        assertEquals(MonarchTableBlockCell(1, 2), tokens.target(MonarchTableBlockCell(-1, 0), table))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.borderDefault, tokens.line(colors), theme.id)
            assertEquals(colors.textMuted, tokens.caption(colors), theme.id)
            assertEquals(colors.bgSubtle, tokens.background(header = true, colors), theme.id)
            assertEquals(colors.bgSurface, tokens.background(header = false, colors), theme.id)
            assertEquals(colors.textDisabled, tokens.foreground(enabled = false, colors), theme.id)
        }
    }
}
