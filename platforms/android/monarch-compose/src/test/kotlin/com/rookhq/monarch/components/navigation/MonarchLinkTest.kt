package com.rookhq.monarch.components.navigation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.navigation.link.MonarchLink
import com.rookhq.monarch.components.navigation.link.MonarchLinkDefaults
import com.rookhq.monarch.components.navigation.link.MonarchLinkTokens
import com.rookhq.monarch.components.navigation.link.MonarchLinkVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.MonarchControlState
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchLinkFixtures : ComponentFixtures {
    override val id = "link"
    override val fixtures = MonarchLinkVariant.entries.map { variant ->
        fixture(variant.name) { MonarchLink(text = "View all invoices", onClick = {}, variant = variant) }
    } + listOf(
        fixture("disabled") { MonarchLink(text = "Resend the code", onClick = {}, enabled = false) },
        fixture("external with an icon") {
            MonarchLink(
                text = "Terms of service",
                onClick = {},
                variant = MonarchLinkVariant.Standalone,
                external = true,
                trailingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("long text in a narrow width") {
            MonarchLink(
                text = "Read how we process the personal data of your customers",
                onClick = {},
                modifier = Modifier.width(160.dp),
            )
        },
        fixture("caption size") {
            ProvideTextStyle(MonarchTheme.typography.caption) {
                MonarchLink(text = "Forgot password?", onClick = {}, variant = MonarchLinkVariant.Subtle)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLinkTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aTapCallsOnClickAndTheActionIsNamed() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchLink(text = "View all invoices", onClick = { clicks++ }, modifier = Modifier.testTag("link"))
        }
        val link = rule.onNodeWithTag("link").assertHasClickAction().assertIsEnabled()
        assertEquals("open link", link.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        link.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun aDisabledLinkDoesNotCallOnClick() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchLink(text = "Resend", onClick = { clicks++ }, modifier = Modifier.testTag("link"), enabled = false)
        }
        rule.onNodeWithTag("link").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun anExternalLinkSaysThatItOpensTheBrowser() {
        rule.setMonarchContent {
            MonarchLink(text = "Terms", onClick = {}, modifier = Modifier.testTag("link"), external = true)
        }
        rule.onNodeWithTag("link").assertContentDescriptionEquals("Terms, opens in the browser")
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchLink(
                text = "Conditions",
                onClick = {},
                modifier = Modifier.testTag("link"),
                external = true,
                externalLabel = "s’ouvre dans le navigateur",
                actionLabel = "ouvrir le lien",
            )
        }
        val link = rule.onNodeWithTag("link")
        link.assertContentDescriptionEquals("Conditions, s’ouvre dans le navigateur")
        assertEquals("ouvrir le lien", link.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
    }

    @Test
    fun anInternalLinkHasNoExtraDescription() {
        rule.setMonarchContent { MonarchLink(text = "Settings", onClick = {}, modifier = Modifier.testTag("link")) }
        val config = rule.onNodeWithTag("link").fetchSemanticsNode().config
        assertNull(config.getOrNull(SemanticsProperties.ContentDescription))
    }

    @Test
    fun theTouchTargetIsReservedAtEveryVariant() {
        rule.setMonarchContent {
            Row {
                MonarchLinkVariant.entries.forEach { variant ->
                    MonarchLink(text = "Go", onClick = {}, modifier = Modifier.testTag(variant.name), variant = variant)
                }
            }
        }
        MonarchLinkVariant.entries.forEach { rule.onNodeWithTag(it.name).assertTouchTargetAtLeast48dp(rule.density) }
    }

    @Test
    fun theIconFollowsTheTextInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchLink(
                text = "Terms",
                onClick = {},
                external = true,
                trailingIcon = {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null, modifier = Modifier.testTag("icon"))
                },
            )
        }
        for (direction in LayoutDirection.entries) {
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Terms", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithTag("icon", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun longTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLink(
                text = "Read how we process the personal data of your customers",
                onClick = {},
                modifier = Modifier.width(160.dp),
            )
        }
        rule.onNodeWithText("Read how we process the personal data of your customers", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theLookFollowsTheVariantAndTheStateInEveryTheme() {
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            val rest = MonarchControlState()
            val pressed = MonarchControlState(pressed = true)
            val disabled = MonarchControlState(enabled = false)
            val hc = theme.isHighContrast
            val inline = MonarchLinkTokens.appearance(MonarchLinkVariant.Inline, rest, hc, colors)
            assertEquals(colors.textLink, inline.color)
            assertTrue(inline.underlined)
            assertNull(inline.fontWeight)

            val standalone = MonarchLinkTokens.appearance(MonarchLinkVariant.Standalone, rest, hc, colors)
            assertEquals(colors.textLink, standalone.color)
            assertEquals(hc, standalone.underlined, "${theme.id}: underlined at rest only in high contrast")
            assertEquals(FontWeight.Medium, standalone.fontWeight)
            assertTrue(MonarchLinkTokens.appearance(MonarchLinkVariant.Standalone, pressed, hc, colors).underlined)

            val subtle = MonarchLinkTokens.appearance(MonarchLinkVariant.Subtle, rest, hc, colors)
            assertEquals(colors.textMuted, subtle.color)
            assertEquals(hc, subtle.underlined)
            val subtlePressed = MonarchLinkTokens.appearance(MonarchLinkVariant.Subtle, pressed, hc, colors)
            assertEquals(colors.textDefault, subtlePressed.color)
            assertTrue(subtlePressed.underlined)

            MonarchLinkVariant.entries.forEach { variant ->
                val off = MonarchLinkTokens.appearance(variant, disabled.copy(pressed = true), hc, colors)
                assertEquals(colors.textDisabled, off.color, "${theme.id}, $variant disabled")
            }
        }
    }

    @Test
    fun pressingUnderlinesAStandaloneLink() {
        val source = MutableInteractionSource()
        var decoration: TextDecoration? = null
        rule.setMonarchContent {
            MonarchLink(
                text = "Invoices",
                onClick = {},
                variant = MonarchLinkVariant.Standalone,
                interactionSource = source,
            )
        }
        fun underline() = rule.onNodeWithText("Invoices", useUnmergedTree = true)
            .fetchSemanticsNode().let { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                results.first().layoutInput.style.textDecoration
            }
        decoration = underline()
        assertFalse(decoration == TextDecoration.Underline)
        rule.runOnIdle { source.tryEmit(PressInteraction.Press(Offset.Zero)) }
        rule.waitForIdle()
        assertEquals(TextDecoration.Underline, underline())
    }

    @Test
    fun theInlineStylesUseTheSameRules() {
        var styles: TextLinkStyles? = null
        var subtle: TextLinkStyles? = null
        var colors: MonarchColors? = null
        rule.setMonarchContent {
            styles = MonarchLinkDefaults.styles()
            subtle = MonarchLinkDefaults.styles(MonarchLinkVariant.Subtle)
            colors = MonarchTheme.colors
        }
        rule.waitForIdle()
        val theme = assertNotNull(colors)
        assertEquals(theme.textLink, styles?.style?.color)
        assertEquals(TextDecoration.Underline, styles?.style?.textDecoration)
        assertEquals(theme.textMuted, subtle?.style?.color)
        assertEquals(theme.textDefault, subtle?.pressedStyle?.color)
        assertEquals(TextDecoration.Underline, subtle?.focusedStyle?.textDecoration)
    }

    @Test
    fun aLinkInsideASentenceOpensItsUrl() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        rule.setMonarchContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                MonarchText(
                    text = buildAnnotatedString {
                        append("Read the ")
                        withLink(LinkAnnotation.Url("https://example.com/terms", MonarchLinkDefaults.styles())) {
                            append("terms")
                        }
                        append(".")
                    },
                )
            }
        }
        rule.onNodeWithText("terms", substring = true, useUnmergedTree = true).assertExists()
        rule.onNode(hasClickAction() and hasAnyAncestor(hasText("Read the terms.")), useUnmergedTree = true)
            .performClick()
        assertEquals(listOf("https://example.com/terms"), opened)
    }
}
