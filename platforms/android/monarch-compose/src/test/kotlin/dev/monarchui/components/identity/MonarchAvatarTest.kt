package dev.monarchui.components.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarGroup
import dev.monarchui.components.identity.avatar.MonarchAvatarLogic
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.avatar.MonarchAvatarTokens
import dev.monarchui.components.identity.presencedot.MonarchPresenceDotSize
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.SamplePictureFill
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.fixture
import dev.monarchui.testing.samplePainter
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val People = listOf("Ana Silva", "Ben Okafor", "Chen Wei", "Dara Kim", "Eli Roth")

internal object MonarchAvatarFixtures : ComponentFixtures {
    override val id = "avatar"
    override val fixtures = MonarchAvatarShape.entries.map { shape ->
        fixture("every size, ${shape.name}") {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchAvatarSize.entries.forEach { size ->
                    MonarchAvatar(image = null, name = "Ana Silva", shape = shape, size = size)
                }
            }
        }
    } + listOf(
        fixture("every presence") {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchPresenceStatus.entries.forEach { status ->
                    MonarchAvatar(image = null, name = "Ben Okafor", status = status)
                }
            }
        },
        fixture("a picture") { MonarchAvatar(image = samplePainter(), name = "Chen Wei", size = MonarchAvatarSize.Lg) },
        fixture("an icon, an emoji name with an icon and an empty name") {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchAvatar(
                    image = null,
                    name = "Rook Labs",
                    shape = MonarchAvatarShape.Square,
                    icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                )
                MonarchAvatar(
                    image = null,
                    name = "\uD83E\uDD8A",
                    icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                )
                MonarchAvatar(image = null, name = "")
            }
        },
        fixture("an image slot") {
            MonarchAvatar(image = { SamplePictureFill() }, name = "Dara Kim", size = MonarchAvatarSize.Lg)
        },
        fixture("a group with a tile") {
            MonarchAvatarGroup(items = People, label = "Assignees", maxVisible = 3, size = MonarchAvatarSize.Sm) {
                MonarchAvatar(image = null, name = it, size = MonarchAvatarSize.Sm)
            }
        },
        fixture("a square group") {
            MonarchAvatarGroup(items = People.take(2), label = "Workspaces", shape = MonarchAvatarShape.Square) {
                MonarchAvatar(image = null, name = it, shape = MonarchAvatarShape.Square)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAvatarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theFiveSizesMatchIos() {
        assertEquals(
            listOf(20.dp, 24.dp, 32.dp, 40.dp, 56.dp),
            MonarchAvatarSize.entries.map { MonarchAvatarTokens.side(it) },
        )
        assertEquals(MonarchTokens.Radius.lg.dp, MonarchAvatarTokens.squareRadius(MonarchAvatarSize.Xl))
        assertEquals(MonarchPresenceDotSize.Sm, MonarchAvatarTokens.dotSize(MonarchAvatarSize.Xs))
        assertEquals(MonarchPresenceDotSize.Lg, MonarchAvatarTokens.dotSize(MonarchAvatarSize.Xl))
    }

    @Test
    fun initialsTakeTheFirstAndLastWordsAndKeepWholeCharacters() {
        assertEquals("AS", MonarchAvatarLogic.initials("ana silva"))
        assertEquals("AS", MonarchAvatarLogic.initials("  Ana  Maria   Silva "))
        assertEquals("C", MonarchAvatarLogic.initials("Chen"))
        assertEquals("", MonarchAvatarLogic.initials("   "))
        assertEquals("🦊", MonarchAvatarLogic.initials("🦊"))
        assertEquals("E\u0301Z", MonarchAvatarLogic.initials("e\u0301lodie zola"))
        assertTrue(MonarchAvatarLogic.hasReadableInitials("AS"))
        assertTrue(MonarchAvatarLogic.hasReadableInitials("7"))
        assertFalse(MonarchAvatarLogic.hasReadableInitials("🦊"))
        assertFalse(MonarchAvatarLogic.hasReadableInitials(""))
    }

    @Test
    fun theSeedPicksTheSamePaletteIndexAsTheWebAndIsStable() {
        // The web hash: h = (h * 31 + code) % 360, then % 5. "A" is 65, so 65 % 5 = 0.
        assertEquals(0, MonarchAvatarLogic.paletteIndex("A"))
        // "AB": (65 * 31 + 66) % 360 = 2081 % 360 = 281; 281 % 5 = 1.
        assertEquals(1, MonarchAvatarLogic.paletteIndex("AB"))
        assertEquals(0, MonarchAvatarLogic.paletteIndex(""))
        assertEquals(MonarchAvatarLogic.paletteIndex("user-42"), MonarchAvatarLogic.paletteIndex("user-42"))
    }

    @Test
    fun fallbackColoursAreSubtleAndStrongPairsOfOneFamilyInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            // The web's soft fallback: a per-name hue, pale in a light theme and deep in a dark one.
            val soft = MonarchAvatarTokens.hueColors(220, theme.isDark)
            val fill = if (theme.isDark) Color.hsl(220f, 0.30f, 0.22f) else Color.hsl(220f, 0.45f, 0.92f)
            assertEquals(fill, soft.container, theme.id)
            assertEquals(MonarchAvatarLogic.hue("A"), MonarchAvatarLogic.hue("A"))
            assertEquals(colors.bgMuted, MonarchAvatarTokens.tileColors(colors).container, theme.id)
            assertEquals(colors.textMuted, MonarchAvatarTokens.tileColors(colors).content, theme.id)
            assertEquals(colors.bgSurface, MonarchAvatarTokens.ringColor(colors), theme.id)
        }
    }

    @Test
    fun anAvatarIsOneImageNamedByTheNameAndThePresence() {
        rule.setMonarchContent {
            Row {
                MonarchAvatar(image = null, name = "Ana Silva", status = MonarchPresenceStatus.Online)
                MonarchAvatar(
                    image = null,
                    name = "Ben Okafor",
                    status = MonarchPresenceStatus.Away,
                    statusLabel = "Absent",
                )
                MonarchAvatar(image = null, name = "Chen Wei")
            }
        }
        rule.onNodeWithContentDescription("Ana Silva, Online")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
            .assertHasNoClickAction()
        rule.onNodeWithContentDescription("Ben Okafor, Absent").assertExists()
        rule.onNodeWithContentDescription("Chen Wei").assertExists()
        // The initials and the dot are not read on their own.
        rule.onNodeWithContentDescription("Online").assertDoesNotExist()
    }

    @Test
    fun aDecorativeAvatarIsHiddenFromTalkBack() {
        rule.setMonarchContent { MonarchAvatar(image = null, name = "Chen Wei", decorative = true) }
        rule.onNodeWithContentDescription("Chen Wei").assertDoesNotExist()
    }

    @Test
    fun theAvatarGrowsWithTheFontSizeSetting() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAvatar(image = null, name = "Ana Silva", modifier = Modifier.testTag("avatar"))
        }
        val side = MonarchAvatarTokens.side(MonarchAvatarSize.Md) * LargestFontScale
        rule.onNodeWithTag("avatar").assertWidthIsAtLeast(side)
    }

    @Test
    fun aGroupShowsATileForTheHiddenAvatarsButNeverForOne() {
        assertEquals(3, MonarchAvatarLogic.visibleCount(5, 3))
        assertEquals(4, MonarchAvatarLogic.visibleCount(4, 3))
        assertEquals(2, MonarchAvatarLogic.visibleCount(2, 3))
        assertEquals(5, MonarchAvatarLogic.visibleCount(5, null))
        assertEquals(0, MonarchAvatarLogic.visibleCount(5, -1))
        rule.setMonarchContent {
            MonarchAvatarGroup(items = People, label = "Assignees", maxVisible = 3) {
                MonarchAvatar(image = null, name = it)
            }
        }
        rule.onNodeWithContentDescription("Assignees").assertExists()
        rule.onNodeWithContentDescription("Chen Wei").assertExists()
        rule.onNodeWithContentDescription("Dara Kim").assertDoesNotExist()
        rule.onNodeWithContentDescription("2 more")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
    }

    @Test
    fun theTileNameCanBeReplacedAndTheGroupStartsAtTheStartEdge() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAvatarGroup(
                items = People,
                label = "Assignees",
                maxVisible = 2,
                overflowLabel = { "$it autres" },
            ) { MonarchAvatar(image = null, name = it) }
        }
        rule.onNodeWithContentDescription("3 autres").assertExists()
        rule.onNodeWithContentDescription("Ana Silva")
            .assertIsBefore(rule.onNodeWithContentDescription("Ben Okafor"), LayoutDirection.Rtl)
    }

    @Test
    fun differentSeedsCanGiveDifferentColours() {
        assertNotEquals(MonarchAvatarLogic.paletteIndex("A"), MonarchAvatarLogic.paletteIndex("AB"))
    }
}
