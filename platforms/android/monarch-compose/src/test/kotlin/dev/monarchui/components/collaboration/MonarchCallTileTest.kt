package dev.monarchui.components.collaboration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.collaboration.calltile.MonarchCallQuality
import dev.monarchui.components.collaboration.calltile.MonarchCallReaction
import dev.monarchui.components.collaboration.calltile.MonarchCallTile
import dev.monarchui.components.collaboration.calltile.MonarchCallTileIcons
import dev.monarchui.components.collaboration.calltile.MonarchCallTileLabels
import dev.monarchui.components.collaboration.calltile.MonarchCallTileLogic
import dev.monarchui.components.collaboration.calltile.MonarchCallTileSize
import dev.monarchui.components.collaboration.calltile.MonarchCallTileTokens
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Now = 1_700_000_000_000L

@Composable
private fun TestVideo() {
    Box(modifier = Modifier.fillMaxSize().testTag("video").background(MonarchTheme.colors.bgMuted))
}

@Composable
private fun TestMark() {
    Icon(imageVector = TestIcon, contentDescription = null)
}

// The test window is 320 by 470dp: one tile per fixture.
internal object MonarchCallTileFixtures : ComponentFixtures {
    override val id = "call-tile"
    override val fixtures = listOf(
        fixture("avatar, speaking, buttons") {
            MonarchCallTile(name = "Ana", speaking = true, onPinnedChange = {}, onFullScreen = {})
        },
        fixture("video, you, muted, poor connection") {
            MonarchCallTile(
                name = "Ben",
                self = true,
                micOff = true,
                quality = MonarchCallQuality.Poor,
                status = "Reconnecting…",
                statusTone = MonarchTone.Warning,
            ) { TestVideo() }
        },
        fixture("hand raised, pinned, reaction, large") {
            MonarchCallTile(
                name = "Chloé",
                size = MonarchCallTileSize.Lg,
                handRaised = true,
                pinned = true,
                onPinnedChange = {},
                reaction = MonarchCallReaction("👏", Now - 500L),
                now = Now,
            )
        },
        fixture("filmstrip, sharing screen, fair") {
            MonarchCallTile(
                name = "Dev",
                size = MonarchCallTileSize.Sm,
                screen = true,
                quality = MonarchCallQuality.Fair,
                onFullScreen = {},
                modifier = Modifier.width(160.dp),
            )
        },
        fixture("icons, good connection, every tone") {
            MonarchCallTile(
                name = "Eun-ji",
                micOff = true,
                handRaised = true,
                quality = MonarchCallQuality.Good,
                status = "Presenting",
                statusTone = MonarchTone.Success,
                onPinnedChange = {},
                onFullScreen = {},
                icons = MonarchCallTileIcons(
                    micOff = { TestMark() },
                    handRaised = { TestMark() },
                    pin = { TestMark() },
                    fullScreen = { TestMark() },
                ),
            )
        },
        fixture("long name and status") {
            MonarchCallTile(
                name = "Maria Fernanda de los Santos",
                status = "Reconnecting to the call after a network change",
                statusTone = MonarchTone.Danger,
                showVideo = false,
            ) { TestVideo() }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCallTileTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theNameBarIsReadFirstWithWhatIsTrueNow() {
        rule.setMonarchContent {
            MonarchCallTile(
                name = "Ana",
                self = true,
                speaking = true,
                micOff = true,
                handRaised = true,
                screen = true,
                status = "Presenting",
                quality = MonarchCallQuality.Poor,
            )
        }
        val bar = rule.onNodeWithContentDescription("Ana (you)")
        assertEquals(
            "Speaking, Muted, Hand raised, Sharing screen, Presenting, Poor connection",
            bar.stateDescription(),
        )
        assertEquals(-1f, bar.fetchSemanticsNode().config[SemanticsProperties.TraversalIndex])
    }

    @Test
    fun aQuietTileHasNoState() {
        rule.setMonarchContent { MonarchCallTile(name = "Ana") }
        val node = rule.onNodeWithContentDescription("Ana").fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsProperties.StateDescription))
    }

    @Test
    fun theVideoIsShownOnlyWhenAskedAndIsHidden() {
        var show by mutableStateOf(true)
        rule.setMonarchContent { MonarchCallTile(name = "Ana", showVideo = show) { TestVideo() } }
        rule.onNodeWithTag("video", useUnmergedTree = true).assertExists()
        show = false
        rule.waitForIdle()
        rule.onAllNodesWithTag("video", useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithText("A", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theWordsReplaceMissingIcons() {
        rule.setMonarchContent { MonarchCallTile(name = "Ana", micOff = true, screen = true, handRaised = true) }
        rule.onNodeWithText("Muted", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Sharing screen", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Hand raised", useUnmergedTree = true).assertExists()
    }

    @Test
    fun iconsReplaceTheWords() {
        rule.setMonarchContent {
            MonarchCallTile(
                name = "Ana",
                micOff = true,
                icons = MonarchCallTileIcons(micOff = { Box(Modifier.size(1.dp).testTag("mic")) }),
            )
        }
        rule.onNodeWithTag("mic", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Muted", useUnmergedTree = true).assertCountEquals(0)
        assertEquals("Muted", rule.onNodeWithContentDescription("Ana").stateDescription())
    }

    @Test
    fun pinIsSelectedWhenPinnedAndCallsWithTheNextState() {
        var pinned by mutableStateOf(false)
        rule.setMonarchContent { MonarchCallTile(name = "Ana", pinned = pinned, onPinnedChange = { pinned = it }) }
        rule.onNodeWithContentDescription("Pin Ana")
            .assertIsNotSelected()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertTrue(pinned)
        rule.onNodeWithContentDescription("Unpin Ana").assertIsSelected().performClick()
        assertFalse(pinned)
    }

    @Test
    fun fullScreenIsAButtonNamedAfterTheParticipant() {
        var calls = 0
        rule.setMonarchContent {
            MonarchCallTile(
                name = "Ana",
                onFullScreen = { calls++ },
                icons = MonarchCallTileIcons(fullScreen = { TestMark() }),
            )
        }
        rule.onNodeWithContentDescription("Show Ana full screen")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, calls)
    }

    @Test
    fun theFilmstripSizeHasNoButtons() {
        rule.setMonarchContent {
            MonarchCallTile(name = "Ana", size = MonarchCallTileSize.Sm, onPinnedChange = {}, onFullScreen = {})
        }
        rule.onAllNodesWithContentDescription("Pin Ana").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Show Ana full screen").assertCountEquals(0)
    }

    @Test
    fun aReactionShowsForThreeSecondsAndIsAnnounced() {
        var now by mutableLongStateOf(Now)
        val reaction = MonarchCallReaction("👏", Now)
        rule.setMonarchContent { MonarchCallTile(name = "Ana", reaction = reaction, now = now) }
        rule.onNodeWithContentDescription("Ana reacted 👏")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("👏", useUnmergedTree = true).assertExists()
        now = Now + 2_999L
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Ana reacted 👏").assertExists()
        now = Now + 3_000L
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("Ana reacted 👏").assertCountEquals(0)
    }

    @Test
    fun theTileIsAtLeastSixteenByNineAndGrowsWithItsText() {
        rule.setMonarchContent {
            Box(Modifier.width(320.dp)) {
                MonarchCallTile(name = "Ana", modifier = Modifier.fillMaxWidth().testTag("tile"))
            }
        }
        rule.onNodeWithTag("tile").assertHeightIsEqualTo(180.dp)
    }

    @Test
    fun aLongStatusWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Box(Modifier.width(320.dp)) {
                MonarchCallTile(
                    name = "Maria Fernanda de los Santos",
                    status = "Reconnecting to the call after a network change",
                    modifier = Modifier.fillMaxWidth().testTag("tile"),
                )
            }
        }
        rule.onNodeWithText("Reconnecting to the call after a network change", useUnmergedTree = true)
            .assertTextFullyVisible()
        rule.onNodeWithTag("tile").assertHeightIsAtLeast(180.dp)
    }

    @Test
    fun theHandBadgeIsAtTheStartAndTheButtonsAtTheEnd() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchCallTile(name = "Ana", handRaised = true, onFullScreen = {})
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Hand raised", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithContentDescription("Show Ana full screen"), direction)
        }
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCallTile(
                name = "Ana",
                self = true,
                micOff = true,
                quality = MonarchCallQuality.Good,
                onPinnedChange = {},
                reaction = MonarchCallReaction("🎉", Now),
                now = Now,
                labels = MonarchCallTileLabels(
                    you = { "$it (tú)" },
                    muted = "Silenciado",
                    pin = { "Fijar a $it" },
                    pinButton = "Fijar",
                    reacted = { name, emoji -> "$name: $emoji" },
                    quality = { "Calidad ${it.name}" },
                ),
            )
        }
        assertEquals("Silenciado, Calidad Good", rule.onNodeWithContentDescription("Ana (tú)").stateDescription())
        rule.onNodeWithContentDescription("Fijar a Ana").assertExists()
        rule.onNodeWithText("Fijar", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Ana: 🎉").assertExists()
    }

    @Test
    fun theRulesMatchTheWebAndIos() {
        val t = MonarchCallTileLogic
        assertEquals(3, t.litBars(MonarchCallQuality.Good))
        assertEquals(2, t.litBars(MonarchCallQuality.Fair))
        assertEquals(1, t.litBars(MonarchCallQuality.Poor))
        val reaction = MonarchCallReaction("👏", Now)
        assertTrue(t.reactionVisible(reaction, Now))
        assertTrue(t.reactionVisible(reaction, Now + 2_999L))
        assertFalse(t.reactionVisible(reaction, Now + 3_000L))
        assertFalse(t.reactionVisible(reaction, Now - 1L))
        assertFalse(t.reactionVisible(null, Now))
        assertEquals(1_000L, t.reactionRemaining(reaction, Now + 2_000L))
        assertEquals(0L, t.reactionRemaining(reaction, Now + 5_000L))
        assertEquals(MonarchAvatarSize.Md, t.avatarSize(MonarchCallTileSize.Sm))
        assertEquals(MonarchAvatarSize.Lg, t.avatarSize(MonarchCallTileSize.Lg))
        assertFalse(t.showsActions(MonarchCallTileSize.Md, pinnable = false, fullScreen = false))
        assertEquals("Muted, Presenting", t.spokenState(listOf(null, "Muted", " ", "Presenting")))
        assertEquals(MonarchCallReaction("👏", Now), reaction)
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchCallTileTokens
            assertEquals(c.bgEmphasis, t.tileColors(c).container, theme.id)
            assertEquals(c.bgInverse, t.barColors(c).container, theme.id)
            assertEquals(c.textInverse, t.barColors(c).content, theme.id)
            assertEquals(c.warningSolid, t.handColors(c).container, theme.id)
            assertEquals(c.warningOnSolid, t.handColors(c).content, theme.id)
            assertEquals(c.successSolid, t.speakingColor(c), theme.id)
            assertEquals(c.dangerStrongInverse, t.mutedColor(c), theme.id)
            assertEquals(c.textInverseMuted, t.statusColor(MonarchTone.Neutral, c), theme.id)
            assertEquals(c.infoStrongInverse, t.statusColor(MonarchTone.Info, c), theme.id)
            assertEquals(c.successStrongInverse, t.statusColor(MonarchTone.Success, c), theme.id)
            assertEquals(c.warningStrongInverse, t.statusColor(MonarchTone.Warning, c), theme.id)
            assertEquals(c.dangerStrongInverse, t.statusColor(MonarchTone.Danger, c), theme.id)
            assertEquals(c.successSolid, t.qualityColor(MonarchCallQuality.Good, c), theme.id)
            assertEquals(c.warningSolid, t.qualityColor(MonarchCallQuality.Fair, c), theme.id)
            assertEquals(c.dangerSolid, t.qualityColor(MonarchCallQuality.Poor, c), theme.id)
            assertEquals(c.textInverseMuted, t.unlitColor(c), theme.id)
        }
    }
}
