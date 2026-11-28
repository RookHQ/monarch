package dev.monarchui.components.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeFamily
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIcon
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIconSize
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIconTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalLayoutApi::class)
internal object MonarchFileTypeIconFixtures : ComponentFixtures {
    override val id = "file-type-icon"
    override val fixtures = MonarchFileTypeIconSize.entries.map { size ->
        fixture("every family, ${size.name}") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchFileTypeFamily.entries.forEach { MonarchFileTypeIcon(family = it, size = size) }
            }
        }
    } + listOf(
        fixture("from a file name, beside its name") {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchFileTypeIcon(fileName = "Quarterly report.pdf", decorative = true)
                Text("Quarterly report.pdf")
            }
        },
        fixture("unknown type") { MonarchFileTypeIcon(fileName = "notes", contentType = "application/octet-stream") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFileTypeIconTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theFamilyComesFromASpecificMimeTypeBeforeTheExtension() {
        assertEquals(MonarchFileTypeFamily.Pdf, MonarchFileTypeFamily.of("scan.png", "application/pdf"))
        assertEquals(MonarchFileTypeFamily.Spreadsheet, MonarchFileTypeFamily.of(null, "text/csv"))
        assertEquals(
            MonarchFileTypeFamily.Document,
            MonarchFileTypeFamily.of(null, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        )
        assertEquals(MonarchFileTypeFamily.Code, MonarchFileTypeFamily.of(null, "application/json; charset=utf-8"))
        assertEquals(MonarchFileTypeFamily.Text, MonarchFileTypeFamily.of(null, "text/plain"))
        assertEquals(MonarchFileTypeFamily.Image, MonarchFileTypeFamily.of(null, "IMAGE/HEIC"))
    }

    @Test
    fun aGenericOrMissingMimeTypeFallsBackToTheExtension() {
        assertEquals(
            MonarchFileTypeFamily.Archive,
            MonarchFileTypeFamily.of("backup.tar.gz", "application/octet-stream"),
        )
        assertEquals(MonarchFileTypeFamily.Presentation, MonarchFileTypeFamily.of("/sdcard/Deck.KEY"))
        assertEquals(MonarchFileTypeFamily.Code, MonarchFileTypeFamily.of("C:\\src\\Main.kt"))
        assertEquals(MonarchFileTypeFamily.Audio, MonarchFileTypeFamily.of("song.flac", ""))
        assertEquals(MonarchFileTypeFamily.Other, MonarchFileTypeFamily.of(".gitignore"))
        assertEquals(MonarchFileTypeFamily.Other, MonarchFileTypeFamily.of("README"))
        assertEquals(MonarchFileTypeFamily.Other, MonarchFileTypeFamily.of(null))
        assertEquals(MonarchFileTypeFamily.Other, MonarchFileTypeFamily.of("data.unknownext"))
    }

    @Test
    fun theIconIsOneImageNamedByItsFamily() {
        rule.setMonarchContent { MonarchFileTypeIcon(fileName = "invoice.pdf") }
        rule.onNodeWithContentDescription("PDF document")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
            .assertHasNoClickAction()
    }

    @Test
    fun theLabelReplacesTheBuiltInName() {
        rule.setMonarchContent { MonarchFileTypeIcon(family = MonarchFileTypeFamily.Other, label = "Fichier") }
        rule.onNodeWithContentDescription("Fichier").assertExists()
        rule.onNodeWithContentDescription("File").assertDoesNotExist()
    }

    @Test
    fun aDecorativeIconIsHiddenFromTalkBack() {
        rule.setMonarchContent {
            MonarchFileTypeIcon(fileName = "a.zip", modifier = Modifier.testTag("icon"), decorative = true)
        }
        rule.onNodeWithContentDescription("Archive").assertDoesNotExist()
        val config = rule.onNodeWithTag("icon").fetchSemanticsNode().config
        assertFalse(config.contains(SemanticsProperties.ContentDescription))
    }

    @Test
    fun eachSizeHasItsSideAndTheLargeOneATile() {
        rule.setMonarchContent {
            Row {
                MonarchFileTypeIconSize.entries.forEach { size ->
                    MonarchFileTypeIcon(
                        family = MonarchFileTypeFamily.Image,
                        modifier = Modifier.testTag(size.name),
                        size = size,
                    )
                }
            }
        }
        rule.onNodeWithTag("Sm").assertWidthIsEqualTo(MonarchFileTypeIconTokens.glyphSize(MonarchFileTypeIconSize.Sm))
        rule.onNodeWithTag("Md").assertHeightIsEqualTo(MonarchFileTypeIconTokens.glyphSize(MonarchFileTypeIconSize.Md))
        rule.onNodeWithTag("Lg").assertWidthIsEqualTo(MonarchFileTypeIconTokens.TileSize)
    }

    @Test
    fun everyFamilyHasADistinctMarkAndAName() {
        val marks = MonarchFileTypeFamily.entries.map { MonarchFileTypeIconTokens.mark(it) }
        assertEquals(marks.size, marks.toSet().size)
        val names = MonarchFileTypeFamily.entries.map { MonarchFileTypeIconTokens.label(it) }
        assertEquals(MonarchFileTypeFamily.entries.size, names.toSet().size)
    }

    @Test
    fun coloursComeFromTheDataRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.data1, MonarchFileTypeIconTokens.color(MonarchFileTypeFamily.Video, colors), theme.id)
            assertEquals(colors.data2, MonarchFileTypeIconTokens.color(MonarchFileTypeFamily.Text, colors), theme.id)
            assertEquals(colors.data3, MonarchFileTypeIconTokens.color(MonarchFileTypeFamily.Code, colors), theme.id)
            assertEquals(colors.data4, MonarchFileTypeIconTokens.color(MonarchFileTypeFamily.Audio, colors), theme.id)
            assertEquals(colors.data5, MonarchFileTypeIconTokens.color(MonarchFileTypeFamily.Pdf, colors), theme.id)
            val other = MonarchFileTypeIconTokens.color(MonarchFileTypeFamily.Other, colors)
            assertEquals(colors.textMuted, other, theme.id)
            assertEquals(colors.bgMuted, MonarchFileTypeIconTokens.tileColor(colors), theme.id)
            assertTrue(colors.data5 != colors.bgMuted, theme.id)
        }
    }
}
