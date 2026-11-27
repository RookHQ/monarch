package com.rookhq.monarch.components.identity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.identity.devicelist.MonarchDevice
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceKind
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceList
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceListLabels
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceListState
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceListTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

private const val Hour = 3_600_000L
private const val Now = 1_800_000_000_000L

private val Phone = MonarchDevice(
    id = "pixel",
    name = "Ana's Pixel",
    kind = MonarchDeviceKind.Mobile,
    os = "Android 16",
    browser = "Rook app",
    location = "Lisbon",
    lastUsedMillis = Now,
    current = true,
)
private val Laptop = MonarchDevice(
    id = "mac",
    name = "MacBook",
    os = "macOS",
    lastUsedMillis = Now - 5 * Hour,
    firstSeenMillis = Now - 50 * Hour,
    blocked = true,
)
private val Unnamed = MonarchDevice(id = "x", name = " ")

internal object MonarchDeviceListFixtures : ComponentFixtures {
    override val id = "device-list"
    override val fixtures = listOf(
        fixture("blocked with an icon and Remove") {
            MonarchDeviceList(
                devices = listOf(Laptop),
                title = "Devices",
                description = "",
                now = Now,
                onRemove = {},
                deviceIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("current device") { MonarchDeviceList(devices = listOf(Phone), now = Now, onRemove = {}) },
        fixture("unnamed, no actions") { MonarchDeviceList(devices = listOf(Unnamed), now = Now) },
        fixture("loading") { MonarchDeviceList(devices = emptyList(), state = MonarchDeviceListState.Loading) },
        fixture("failed") {
            MonarchDeviceList(devices = emptyList(), state = MonarchDeviceListState.Failed, onRetry = {})
        },
        fixture("empty") { MonarchDeviceList(devices = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDeviceListTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchDeviceListTokens

    @Test
    fun eachDeviceIsOneElementWithItsDetailsInWords() {
        rule.setMonarchContent { MonarchDeviceList(devices = listOf(Phone, Laptop, Unnamed), now = Now) }
        rule.onNodeWithText("Devices", useUnmergedTree = true).assertExists()
        assertEquals(
            "This device, Android 16 · Rook app · Lisbon, Last used ${tokens.relativeTime(Now, Now)}",
            rule.onNodeWithContentDescription("Ana's Pixel").stateDescription(),
        )
        val times = "Last used ${tokens.relativeTime(Now - 5 * Hour, Now)} · " +
            "First seen ${tokens.relativeTime(Now - 50 * Hour, Now)}"
        assertEquals("Blocked, macOS, $times", rule.onNodeWithContentDescription("MacBook").stateDescription())
        rule.onNodeWithContentDescription("Unknown device").assertExists()
    }

    @Test
    fun theCurrentDeviceHasNoRemoveButton() {
        rule.setMonarchContent { MonarchDeviceList(devices = listOf(Phone, Laptop), now = Now, onRemove = {}) }
        rule.onNodeWithContentDescription("Remove Ana's Pixel").assertDoesNotExist()
        rule.onNodeWithContentDescription("Remove MacBook").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun removingAsksFirstAndOnlyOneRunsAtATime() {
        val gate = CompletableDeferred<Unit>()
        val removed = mutableListOf<String>()
        val second = MonarchDevice(id = "tab", name = "Tablet", kind = MonarchDeviceKind.Tablet)
        rule.setMonarchContent {
            MonarchDeviceList(
                devices = listOf(Laptop, second),
                now = Now,
                onRemove = {
                    gate.await()
                    removed += it
                },
            )
        }
        rule.onNodeWithContentDescription("Remove MacBook").performClick()
        rule.onNode(hasText("Remove MacBook?") and hasAnyAncestor(isDialog())).assertExists()
        rule.onNode(hasText("Remove") and hasAnyAncestor(isDialog())).performClick()
        rule.onNode(isDialog()).assertDoesNotExist()
        assertEquals("Loading", rule.onNodeWithContentDescription("Remove MacBook").stateDescription())
        rule.onNodeWithContentDescription("Remove Tablet").assertIsNotEnabled()
        gate.complete(Unit)
        rule.waitForIdle()
        assertEquals(listOf("mac"), removed)
        rule.onNodeWithContentDescription("Remove Tablet").assertIsEnabled()
        rule.onNodeWithContentDescription("Removed MacBook")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun aFailedRemovalShowsANoticeAndIsAnnounced() {
        rule.setMonarchContent {
            MonarchDeviceList(
                devices = listOf(Laptop),
                now = Now,
                confirmRemoval = false,
                onRemove = { throw IOException() },
            )
        }
        rule.onNodeWithContentDescription("Remove MacBook").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Couldn’t remove MacBook. Try again.", substring = true).assertExists()
        rule.onNodeWithContentDescription("Couldn’t remove MacBook. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun rowActionsAreAddedToEachRow() {
        var blocked by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            MonarchDeviceList(
                devices = listOf(Laptop),
                now = Now,
                rowActions = { device ->
                    MonarchButton(
                        text = "Block",
                        onClick = { blocked = device.id },
                        modifier = Modifier.testTag("block"),
                        size = MonarchButtonSize.Sm,
                    )
                },
            )
        }
        rule.onNodeWithTag("block").performClick()
        assertEquals("mac", blocked)
    }

    @Test
    fun loadingFailedAndEmptyStates() {
        var retries = 0
        rule.setMonarchContent {
            MonarchDeviceList(devices = emptyList(), state = MonarchDeviceListState.Failed, onRetry = { retries++ })
        }
        rule.onNodeWithText("Couldn’t load your devices", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun theLoadingAndEmptyStatesSaySo() {
        rule.setMonarchContent {
            MonarchDeviceList(devices = emptyList(), state = MonarchDeviceListState.Loading)
            MonarchDeviceList(devices = emptyList())
        }
        rule.onNodeWithContentDescription("Loading devices", substring = true).assertExists()
        rule.onNodeWithText("No devices yet", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchDeviceList(
                devices = listOf(Phone, Unnamed),
                title = "Dispositivos",
                now = Now,
                labels = MonarchDeviceListLabels(current = "Este dispositivo", unnamed = "Desconocido"),
            )
        }
        rule.onNodeWithText("Dispositivos", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Desconocido").assertExists()
        assertEquals(
            true,
            rule.onNodeWithContentDescription("Ana's Pixel").stateDescription()?.startsWith("Este dispositivo"),
        )
    }

    @Test
    fun theRemoveButtonFollowsTheTextRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchDeviceList(
                devices = listOf(Laptop),
                now = Now,
                onRemove = {},
                rowActions = { MonarchButton(text = "Block", onClick = {}, size = MonarchButtonSize.Sm) },
            )
        }
        rule.onNodeWithContentDescription("Remove MacBook")
            .assertIsBefore(rule.onNodeWithText("Block"), LayoutDirection.Rtl)
    }

    @Test
    fun detailsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDeviceList(devices = listOf(Phone), now = Now, onRemove = {})
        }
        rule.onNodeWithText("Devices that have accessed your account.", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun linesAndNamesAreBuiltFromThePartsThatExist() {
        assertEquals(null, tokens.line(null, " ", null))
        assertEquals("A · C", tokens.line("A", null, "C"))
        assertEquals("Unknown", tokens.displayName("  ", "Unknown"))
        assertEquals("Pixel", tokens.displayName("Pixel", "Unknown"))
        assertEquals("Blocked, A", tokens.spokenDetails(listOf("Blocked"), "A", null))
    }

    @Test
    fun theIconColourComesFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.dangerStrong, tokens.iconColor(blocked = true, c), theme.id)
            assertEquals(c.textMuted, tokens.iconColor(blocked = false, c), theme.id)
        }
    }
}
