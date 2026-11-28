package com.rookhq.monarch.components.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeTokens
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.textLayout
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalLayoutApi::class)
internal object MonarchBadgeFixtures : ComponentFixtures {
    override val id = "badge"
    override val fixtures = MonarchBadgeVariant.entries.map { variant ->
        fixture("every tone, ${variant.name}") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchTone.entries.forEach { tone -> MonarchBadge(text = tone.name, tone = tone, variant = variant) }
            }
        }
    } + listOf(
        fixture("with an icon") {
            MonarchBadge(
                text = "Verified",
                tone = MonarchTone.Success,
                leadingIcon = {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
                },
            )
        },
        fixture("long text") { MonarchBadge(text = "Waiting for a second approval from finance") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchBadgeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aBadgeIsReadAsTextAndIsNotActionable() {
        rule.setMonarchContent { MonarchBadge(text = "Draft", tone = MonarchTone.Warning) }
        rule.onNodeWithText("Draft").assertHasNoClickAction()
    }

    @Test
    fun aBadgeNeverBreaksItsTextOntoSeveralLines() {
        rule.setMonarchContent {
            Row(Modifier.width(24.dp)) { MonarchBadge(text = "12") }
        }
        assertEquals(1, rule.onNodeWithText("12", useUnmergedTree = true).textLayout().lineCount)
    }

    @Test
    fun aBadgeGrowsWithTheFontSizeSettingWithoutCuttingItsText() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchBadge(text = "Needs review", modifier = Modifier.testTag("badge"))
        }
        rule.onNodeWithText("Needs review").assertTextFullyVisible()
        rule.onNodeWithTag("badge").assertHeightIsAtLeast(MonarchBadgeTokens.MinHeight)
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val subtle = MonarchBadgeTokens.colors(MonarchTone.Success, MonarchBadgeVariant.Subtle, colors)
            assertEquals(colors.successSubtle, subtle.container, theme.id)
            assertEquals(colors.successStrong, subtle.content, theme.id)
            assertNull(subtle.border, theme.id)
            val solid = MonarchBadgeTokens.colors(MonarchTone.Danger, MonarchBadgeVariant.Solid, colors)
            assertEquals(colors.dangerSolid, solid.container, theme.id)
            assertEquals(colors.dangerOnSolid, solid.content, theme.id)
            val outline = MonarchBadgeTokens.colors(MonarchTone.Info, MonarchBadgeVariant.Outline, colors)
            assertEquals(Color.Transparent, outline.container, theme.id)
            assertEquals(colors.infoStrong, outline.content, theme.id)
            assertEquals(colors.infoBorder, outline.border, theme.id)
            val neutral = MonarchBadgeTokens.colors(MonarchTone.Neutral, MonarchBadgeVariant.Solid, colors)
            assertEquals(colors.bgInverse, neutral.container, theme.id)
            assertEquals(colors.textInverse, neutral.content, theme.id)
        }
    }
}
