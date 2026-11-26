@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.technical

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.technical.installverify.MonarchInstallSnippet
import com.rookhq.monarch.components.technical.installverify.MonarchInstallTarget
import com.rookhq.monarch.components.technical.installverify.MonarchInstallVerify
import com.rookhq.monarch.components.technical.installverify.MonarchInstallVerifyLabels
import com.rookhq.monarch.components.technical.installverify.MonarchInstallVerifyPart
import com.rookhq.monarch.components.technical.installverify.MonarchInstallVerifyStatus
import com.rookhq.monarch.components.technical.installverify.MonarchInstallVerifyTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private const val Now = 1_772_636_400_000L
private val Snippets = listOf(
    MonarchInstallSnippet("html", "HTML", "<script src=\"w.js\"></script>", "html", "Paste it before </head>."),
    MonarchInstallSnippet("npm", "npm", "npm install widget", "bash", "Run it in your project."),
)
private val Targets = listOf(
    MonarchInstallTarget("www.example.com", Now - 5 * 60_000L),
    MonarchInstallTarget("shop.example.com"),
)

internal object MonarchInstallVerifyFixtures : ComponentFixtures {
    override val id = "install-verify"
    override val fixtures = listOf(
        fixture("install half with a handoff") {
            MonarchInstallVerify(
                snippets = Snippets,
                status = MonarchInstallVerifyStatus.Waiting,
                part = MonarchInstallVerifyPart.Install,
                developerInstructions = "Add the snippet.",
                onShareInstructions = {},
            )
        },
        fixture("waiting") {
            MonarchInstallVerify(
                snippets = Snippets,
                status = MonarchInstallVerifyStatus.Waiting,
                part = MonarchInstallVerifyPart.Verify,
            )
        },
        fixture("connected with sites") {
            MonarchInstallVerify(
                snippets = Snippets,
                status = MonarchInstallVerifyStatus.Connected,
                part = MonarchInstallVerifyPart.Verify,
                targets = Targets,
                now = Now,
            )
        },
        fixture("unavailable with a disabled test page") {
            MonarchInstallVerify(
                snippets = Snippets,
                status = MonarchInstallVerifyStatus.Unavailable,
                part = MonarchInstallVerifyPart.Verify,
                testDisabled = true,
                testHint = "Publish the site first.",
                onOpenTest = {},
            )
        },
        fixture("one snippet") {
            MonarchInstallVerify(
                snippets = Snippets.take(1),
                status = MonarchInstallVerifyStatus.Waiting,
                part = MonarchInstallVerifyPart.Install,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchInstallVerifyTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun bothHalvesHaveHeadingsAndTheSnippetDescriptionFollowsTheTab() {
        val picked = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchInstallVerify(
                snippets = Snippets,
                status = MonarchInstallVerifyStatus.Waiting,
                onSelectionChange = { picked += it },
            )
        }
        rule.onNodeWithText("Install").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Verify").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Paste it before </head>.").assertExists()
        rule.onNodeWithText("npm").performClick()
        assertEquals(listOf("npm"), picked)
        rule.onNodeWithText("Run it in your project.").assertExists()
        rule.onNodeWithText("Paste it before </head>.").assertDoesNotExist()
    }

    @Test
    fun theStatusIsOneLiveElementAndConnectedFiresOnce() {
        var status by mutableStateOf(MonarchInstallVerifyStatus.Waiting)
        var connected = 0
        rule.setMonarchContent {
            MonarchInstallVerify(
                snippets = Snippets,
                status = status,
                part = MonarchInstallVerifyPart.Verify,
                targets = Targets,
                now = Now,
                onConnected = { connected++ },
            )
        }
        rule.onNodeWithText("Waiting for your site…")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        rule.onNodeWithText(
            "Add the snippet and open any page of www.example.com, shop.example.com. This updates by itself.",
        ).assertExists()
        status = MonarchInstallVerifyStatus.Connected
        rule.waitForIdle()
        rule.onNodeWithText("Connected on www.example.com").assertExists()
        status = MonarchInstallVerifyStatus.Unavailable
        rule.waitForIdle()
        rule.onNodeWithText("Live verification isn’t available").assertExists()
        status = MonarchInstallVerifyStatus.Connected
        rule.waitForIdle()
        assertEquals(1, connected)
    }

    @Test
    fun sitesAreReadWithWhenTheyWereSeen() {
        rule.setMonarchContent {
            MonarchInstallVerify(
                snippets = Snippets,
                status = MonarchInstallVerifyStatus.Connected,
                part = MonarchInstallVerifyPart.Verify,
                targets = Targets,
                now = Now,
            )
        }
        rule.onNodeWithContentDescription("Websites").assertExists()
        rule.onNodeWithText("seen 5 minutes ago").assertExists()
        rule.onNodeWithText("not seen yet").assertExists()
    }

    @Test
    fun handoffSharesAndTheTestPageButtonFollowsItsState() {
        val shared = mutableListOf<String>()
        var opened = 0
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchInstallVerify(
                    snippets = Snippets.take(1),
                    status = MonarchInstallVerifyStatus.Waiting,
                    part = MonarchInstallVerifyPart.Install,
                    developerInstructions = "Add the snippet.",
                    onShareInstructions = { shared += it },
                )
                MonarchInstallVerify(
                    snippets = Snippets,
                    status = MonarchInstallVerifyStatus.Waiting,
                    part = MonarchInstallVerifyPart.Verify,
                    onOpenTest = { opened++ },
                    testDisabled = true,
                )
            }
        }
        rule.onNodeWithContentDescription("Copy instructions for your developer")
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Share with your developer").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("Add the snippet."), shared)
        rule.onNodeWithText("Open a test page").assertIsNotEnabled().performClick()
        assertEquals(0, opened)
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchInstallVerify(
                snippets = Snippets,
                status = MonarchInstallVerifyStatus.Connected,
                part = MonarchInstallVerifyPart.Verify,
                labels = MonarchInstallVerifyLabels(connectedTitle = { "Connecté (${it.size})" }),
                connectedOn = listOf("a.example"),
            )
        }
        rule.onNodeWithText("Connecté (1)").assertExists()
    }

    @Test
    fun siteDotSitsBeforeItsNameRightToLeftAndTextWraps() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl, fontScale = LargestFontScale) {
            MonarchInstallVerify(
                snippets = Snippets,
                status = MonarchInstallVerifyStatus.Waiting,
                part = MonarchInstallVerifyPart.Verify,
                targets = Targets.take(1),
                now = Now,
            )
        }
        rule.onNodeWithText("www.example.com", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("seen 5 minutes ago", useUnmergedTree = true), LayoutDirection.Rtl)
        rule.onNodeWithText("Waiting for your site…", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun logicPicksHostsSnippetsAndColors() {
        val tokens = MonarchInstallVerifyTokens
        assertEquals(listOf("www.example.com"), tokens.hosts(null, Targets))
        assertEquals(listOf("x"), tokens.hosts(listOf("x"), Targets))
        assertEquals("npm", tokens.current("npm", Snippets))
        assertEquals("html", tokens.current("missing", Snippets))
        assertEquals(null, tokens.current(null, emptyList()))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val connected = tokens.statusColors(MonarchInstallVerifyStatus.Connected, colors)
            assertEquals(colors.successSubtle to colors.successBorder, connected)
            assertEquals(colors.infoSolid, tokens.indicatorColor(MonarchInstallVerifyStatus.Unavailable, colors))
            assertEquals(colors.successSolid, tokens.dotColor(true, colors))
            assertEquals(colors.borderStrong, tokens.dotColor(false, colors))
        }
    }
}
