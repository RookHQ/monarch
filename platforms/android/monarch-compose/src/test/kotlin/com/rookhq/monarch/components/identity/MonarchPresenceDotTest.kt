package com.rookhq.monarch.components.identity

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceDot
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceDotForm
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceDotSize
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceDotTokens
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal object MonarchPresenceDotFixtures : ComponentFixtures {
    override val id = "presence-dot"
    override val fixtures = MonarchPresenceDotSize.entries.map { size ->
        fixture("every status, ${size.name}") {
            Row { MonarchPresenceStatus.entries.forEach { MonarchPresenceDot(status = it, size = size) } }
        }
    } + listOf(
        fixture("overlay") {
            Row {
                MonarchPresenceStatus.entries.forEach {
                    MonarchPresenceDot(status = it, size = MonarchPresenceDotSize.Lg, overlay = true)
                }
            }
        },
        fixture("custom status") {
            MonarchPresenceDot(status = MonarchPresenceStatus.Away, customStatus = "On holiday until the 3rd")
        },
        fixture("decorative") { MonarchPresenceDot(status = MonarchPresenceStatus.Online, decorative = true) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPresenceDotTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theDotIsReadAsThePresenceAndIsNotActionable() {
        rule.setMonarchContent { MonarchPresenceDot(status = MonarchPresenceStatus.DoNotDisturb) }
        rule.onNodeWithContentDescription("Do not disturb").assertHasNoClickAction()
    }

    @Test
    fun theCustomStatusIsReadAfterThePresence() {
        rule.setMonarchContent {
            MonarchPresenceDot(status = MonarchPresenceStatus.Away, customStatus = "  In a meeting ")
        }
        rule.onNodeWithContentDescription("Away · In a meeting").assertExists()
    }

    @Test
    fun aBlankCustomStatusIsIgnored() {
        rule.setMonarchContent { MonarchPresenceDot(status = MonarchPresenceStatus.Online, customStatus = "   ") }
        rule.onNodeWithContentDescription("Online").assertExists()
        assertNull(MonarchPresenceDotTokens.customText("  "))
        assertEquals("Lunch", MonarchPresenceDotTokens.customText(" Lunch "))
    }

    @Test
    fun theStatusLabelReplacesTheBuiltInName() {
        rule.setMonarchContent { MonarchPresenceDot(status = MonarchPresenceStatus.Busy, statusLabel = "Occupé") }
        rule.onNodeWithContentDescription("Occupé").assertExists()
    }

    @Test
    fun aDecorativeDotIsHiddenFromTalkBack() {
        rule.setMonarchContent {
            MonarchPresenceDot(
                status = MonarchPresenceStatus.Online,
                modifier = Modifier.testTag("dot"),
                decorative = true,
            )
        }
        rule.onNodeWithTag("dot").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun theDotGrowsWithTheFontSizeSettingAndTheOverlayRing() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Row {
                MonarchPresenceDot(MonarchPresenceStatus.Online, Modifier.testTag("plain"), MonarchPresenceDotSize.Sm)
                MonarchPresenceDot(
                    MonarchPresenceStatus.Online,
                    Modifier.testTag("overlay"),
                    MonarchPresenceDotSize.Sm,
                    overlay = true,
                )
            }
        }
        val diameter = MonarchPresenceDotTokens.diameter(MonarchPresenceDotSize.Sm) * LargestFontScale
        rule.onNodeWithTag("plain").assertWidthIsEqualTo(diameter)
        rule.onNodeWithTag("overlay").assertWidthIsEqualTo(diameter + MonarchPresenceDotTokens.StrokeWidth * 2)
    }

    @Test
    fun everyStatusHasItsOwnFormInTheHighContrastThemes() {
        val forms = MonarchPresenceStatus.entries.map { MonarchPresenceDotTokens.form(it, highContrast = true) }
        assertEquals(MonarchPresenceStatus.entries.size, forms.toSet().size)
        fun standard(status: MonarchPresenceStatus) = MonarchPresenceDotTokens.form(status, highContrast = false)
        assertEquals(MonarchPresenceDotForm.Barred, standard(MonarchPresenceStatus.DoNotDisturb))
        assertEquals(MonarchPresenceDotForm.Ring, standard(MonarchPresenceStatus.Offline))
        assertEquals(MonarchPresenceDotForm.Filled, standard(MonarchPresenceStatus.Away))
    }

    @Test
    fun theColoursComeFromTheStatusRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            fun color(status: MonarchPresenceStatus) = MonarchPresenceDotTokens.color(status, colors)
            assertEquals(colors.successSolid, color(MonarchPresenceStatus.Online), theme.id)
            assertEquals(colors.warningSolid, color(MonarchPresenceStatus.Away), theme.id)
            assertEquals(colors.dangerSolid, color(MonarchPresenceStatus.Busy), theme.id)
            assertEquals(colors.dangerSolid, color(MonarchPresenceStatus.DoNotDisturb), theme.id)
            assertEquals(colors.textSubtle, color(MonarchPresenceStatus.Offline), theme.id)
            assertEquals(colors.dangerOnSolid, MonarchPresenceDotTokens.barColor(colors), theme.id)
            assertEquals(colors.bgSurface, MonarchPresenceDotTokens.surfaceColor(colors), theme.id)
        }
    }
}
