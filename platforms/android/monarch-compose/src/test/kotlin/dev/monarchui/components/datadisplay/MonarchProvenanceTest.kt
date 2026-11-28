package dev.monarchui.components.datadisplay

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.provenance.MonarchProvenance
import dev.monarchui.components.datadisplay.provenance.MonarchProvenanceLabels
import dev.monarchui.components.datadisplay.provenance.MonarchProvenanceTokens
import dev.monarchui.components.datadisplay.provenance.MonarchProvenanceVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
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

private fun hoursAgo(hours: Int): Long = System.currentTimeMillis() - hours * DateUtils.HOUR_IN_MILLIS

internal object MonarchProvenanceFixtures : ComponentFixtures {
    override val id = "provenance"
    override val fixtures = MonarchProvenanceVariant.entries.map { variant ->
        fixture("${variant.name}, with and without time") {
            Column {
                MonarchProvenance(source = "Registry", variant = variant, checkedAt = hoursAgo(2))
                MonarchProvenance(
                    source = "Imported file",
                    variant = variant,
                    icon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
                )
            }
        }
    } + listOf(
        fixture("opens the source") { MonarchProvenance(source = "Registry", checkedAt = hoursAgo(1), onClick = {}) },
        fixture("long text") {
            MonarchProvenance(
                source = "Companies House register of overseas entities and beneficial owners",
                checkedAt = hoursAgo(26),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchProvenanceTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theNoteIsOneElementReadWithItsSourceAndTime() {
        rule.setMonarchContent {
            MonarchProvenance(source = "Registry", modifier = Modifier.testTag("note"), checkedAt = hoursAgo(2))
        }
        rule.onNodeWithTag("note")
            .assertContentDescriptionEquals("Source: Registry, checked 2 hours ago")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun withoutATimeOnlyTheSourceIsRead() {
        rule.setMonarchContent { MonarchProvenance(source = "Entered by hand", modifier = Modifier.testTag("note")) }
        rule.onNodeWithTag("note").assertContentDescriptionEquals("Source: Entered by hand")
    }

    @Test
    fun anEmptySourceDrawsNothing() {
        rule.setMonarchContent { MonarchProvenance(source = "", modifier = Modifier.testTag("note")) }
        rule.onAllNodesWithTag("note").assertCountEquals(0)
    }

    @Test
    fun theLabelsReplaceTheSpokenWords() {
        rule.setMonarchContent {
            MonarchProvenance(
                source = "Registre",
                modifier = Modifier.testTag("note"),
                checkedAt = hoursAgo(2),
                labels = MonarchProvenanceLabels(source = { "Origine : $it" }, checked = { "vérifié $it" }),
            )
        }
        rule.onNodeWithTag("note").assertContentDescriptionEquals("Origine : Registre, vérifié 2 hours ago")
    }

    @Test
    fun withOnClickTheNoteIsAButtonWithAFullTarget() {
        var opened = 0
        rule.setMonarchContent {
            MonarchProvenance(source = "Registry", modifier = Modifier.testTag("note"), onClick = { opened++ })
        }
        rule.onNodeWithTag("note")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun longTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchProvenance(
                source = "Companies House register of overseas entities",
                modifier = Modifier.width(240.dp),
                checkedAt = hoursAgo(2),
            )
        }
        rule.onNodeWithText("Companies House", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val chip = MonarchProvenanceTokens.colors(MonarchProvenanceVariant.Chip, colors)
            assertEquals(colors.bgSubtle, chip.container, theme.id)
            assertEquals(colors.textMuted, chip.content, theme.id)
            assertEquals(colors.borderSubtle, chip.border, theme.id)
            val inline = MonarchProvenanceTokens.colors(MonarchProvenanceVariant.Inline, colors)
            assertEquals(Color.Transparent, inline.container, theme.id)
            assertEquals(null, inline.border, theme.id)
            assertEquals(
                colors.textLink,
                MonarchProvenanceTokens.sourceColor(clickable = true, MonarchProvenanceVariant.Chip, colors),
                theme.id,
            )
        }
    }
}
