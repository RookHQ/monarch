package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnector
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorPicker
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorPickerLabels
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorPickerRules
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorPickerTokens
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorStatus
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorTool
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorToolPermission
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorToolRisk
import com.rookhq.monarch.foundation.MonarchTone
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Delete = MonarchAiConnectorTool(
    "delete",
    "Delete files",
    MonarchAiConnectorToolRisk.Destructive,
    MonarchAiConnectorToolPermission.Ask,
)
private val Search = MonarchAiConnectorTool(
    "search",
    "Search files",
    MonarchAiConnectorToolRisk.Read,
    MonarchAiConnectorToolPermission.Allow,
)
private val Drive = MonarchAiConnector(
    id = "drive",
    name = "Drive",
    status = MonarchAiConnectorStatus.Connected,
    enabled = false,
    account = "alex@example.com",
    tools = listOf(Search, Delete),
)
private val Mail = MonarchAiConnector("mail", "Mail", MonarchAiConnectorStatus.NeedsSignIn, enabled = false)
private val Crm = MonarchAiConnector("crm", "Crème CRM", MonarchAiConnectorStatus.Error, enabled = true)
private val Wiki = MonarchAiConnector("wiki", "Wiki", MonarchAiConnectorStatus.Unavailable, enabled = false)

internal object MonarchAiConnectorPickerFixtures : ComponentFixtures {
    override val id = "ai-connector-picker"
    override val fixtures = listOf(
        fixture("connected with tools, searchable") {
            MonarchAiConnectorPicker(
                connectors = listOf(Drive.copy(enabled = true)),
                onConnectorsChange = {},
                onDone = {},
            )
        },
        fixture("sign-in needed and connection problem") {
            MonarchAiConnectorPicker(
                connectors = listOf(Mail, Crm),
                onConnectorsChange = {},
                searchable = false,
                onConnect = {},
            )
        },
        fixture("unavailable with manage") {
            MonarchAiConnectorPicker(
                connectors = listOf(Wiki),
                onConnectorsChange = {},
                searchable = false,
                onManage = {},
            )
        },
        fixture("empty") { MonarchAiConnectorPicker(connectors = emptyList(), onConnectorsChange = {}) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiConnectorPickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
    private val switch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

    @Test
    fun aConnectedConnectorIsASwitchNamedWithItsStatusThatTurnsOn() {
        var connectors by mutableStateOf(listOf(Drive))
        rule.setMonarchContent {
            MonarchAiConnectorPicker(
                connectors = connectors,
                onConnectorsChange = { connectors = it },
                searchable = false,
            )
        }
        rule.onNode(switch)
            .assertContentDescriptionEquals("Use Drive in this conversation, Connected as alex@example.com")
            .assertIsOff()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertTrue(connectors.single().enabled)
        rule.onNode(switch).assertIsOn()
        rule.onNodeWithText("1 of 1 connector on · 1 tool asks first").assertExists()
    }

    @Test
    fun aConnectorThatIsNotConnectedCannotBeTurnedOnAndSaysWhy() {
        var changes = 0
        var connected: String? = null
        rule.setMonarchContent {
            MonarchAiConnectorPicker(
                connectors = listOf(Mail),
                onConnectorsChange = { changes++ },
                searchable = false,
                onConnect = { connected = it },
            )
        }
        rule.onNode(switch).assertIsNotEnabled().performClick()
        assertEquals(0, changes)
        rule.onNodeWithText("Sign-in needed", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Sign in before turning this on.", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Sign in: Mail").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("mail", connected)
    }

    @Test
    fun aConnectionProblemOffersReconnectAndManageIsOfferedOnEveryRow() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiConnectorPicker(
                connectors = listOf(Crm),
                onConnectorsChange = {},
                searchable = false,
                onConnect = { calls += "connect $it" },
                onManage = { calls += "manage $it" },
            )
        }
        rule.onNodeWithText("Connection problem", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Reconnect: Crème CRM").performClick()
        rule.onNodeWithContentDescription("Manage: Crème CRM").performClick()
        assertEquals(listOf("connect crm", "manage crm"), calls)
        // Stored as enabled while not connected: shown on but disabled, and not counted as on.
        rule.onNode(switch).assertIsOn().assertIsNotEnabled()
        rule.onNodeWithText("0 of 1 connector on · 0 tools ask first").assertExists()
    }

    @Test
    fun theToolsOpenWithTheirRiskAndAPermissionChoice() {
        var connectors by mutableStateOf(listOf(Drive.copy(enabled = true, tools = listOf(Delete))))
        rule.setMonarchContent {
            MonarchAiConnectorPicker(
                connectors = connectors,
                onConnectorsChange = { connectors = it },
                searchable = false,
            )
        }
        rule.onNodeWithContentDescription("1 tool: Drive")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
            .performClick()
        rule.onNodeWithText("Can delete data", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Tools that can delete data cannot run without asking.").assertExists()
        rule.onNodeWithContentDescription("Permission for Delete files").assertExists()
        rule.onNodeWithText("Allow").assertIsNotEnabled()
        rule.onNodeWithText("Never").assertIsEnabled().performClick()
        assertEquals(MonarchAiConnectorToolPermission.Never, connectors.single().tools.single().permission)
    }

    @Test
    fun thePermissionChoicesAreDisabledWhileTheConnectorIsOff() {
        rule.setMonarchContent {
            MonarchAiConnectorPicker(connectors = listOf(Drive), onConnectorsChange = {}, searchable = false)
        }
        rule.onNodeWithContentDescription("2 tools: Drive").performClick()
        rule.onAllNodesWithText("Never")[0].assertIsNotEnabled()
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndAnnouncesTheCount() {
        rule.setMonarchContent {
            MonarchAiConnectorPicker(connectors = listOf(Drive, Mail, Crm), onConnectorsChange = {})
        }
        rule.onNode(hasSetTextAction()).performTextInput(" creme ")
        rule.onNodeWithText("Crème CRM", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Drive", useUnmergedTree = true).assertDoesNotExist()
        rule.onNode(liveRegion, useUnmergedTree = true).assertContentDescriptionEquals("1 connector found")
    }

    @Test
    fun aSearchWithNoResultsOffersClearSearch() {
        rule.setMonarchContent {
            MonarchAiConnectorPicker(connectors = listOf(Drive), onConnectorsChange = {})
        }
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        rule.onNodeWithText("No connectors match “zzz”.", useUnmergedTree = true).assertExists()
        rule.onNode(liveRegion, useUnmergedTree = true).assertContentDescriptionEquals("No connectors found")
        rule.onNodeWithText("Clear search").performClick()
        rule.onNodeWithText("Drive", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStringsCanBeReplacedAndDoneCallsBack() {
        var done = 0
        rule.setMonarchContent {
            MonarchAiConnectorPicker(
                connectors = emptyList(),
                onConnectorsChange = {},
                onDone = { done++ },
                labels = MonarchAiConnectorPickerLabels(title = "Verbindungen", done = "Fertig", empty = "Keine."),
            )
        }
        rule.onNodeWithText("Verbindungen").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Keine.").assertExists()
        rule.onNodeWithText("Fertig").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, done)
    }

    @Test
    fun theActionsFlowFromTheStartInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiConnectorPicker(
                connectors = listOf(Mail),
                onConnectorsChange = {},
                searchable = false,
                onConnect = {},
                onManage = {},
            )
        }
        rule.onNodeWithContentDescription("Sign in: Mail")
            .assertIsBefore(rule.onNodeWithContentDescription("Manage: Mail"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongDescriptionWrapsAtTheLargestFontScale() {
        val long = "Files in shared drives and in your own drive"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiConnectorPicker(
                connectors = listOf(Drive.copy(description = long, tools = emptyList())),
                onConnectorsChange = {},
                searchable = false,
            )
        }
        rule.onNodeWithText(long, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesRefuseWhatTheSwitchesAndMenusDoNotOffer() {
        val tokens = MonarchAiConnectorPickerTokens
        val rules = MonarchAiConnectorPickerRules
        val list = listOf(Drive, Mail)
        assertEquals(list, rules.setEnabled(list, "mail", true))
        assertTrue(rules.setEnabled(list, "drive", true).first().enabled)
        val allowed = rules.setPermission(list, "drive", "delete", MonarchAiConnectorToolPermission.Allow, false)
        assertEquals(MonarchAiConnectorToolPermission.Ask, allowed.first().tools[1].permission)
        val forced = rules.setPermission(list, "drive", "delete", MonarchAiConnectorToolPermission.Allow, true)
        assertEquals(MonarchAiConnectorToolPermission.Allow, forced.first().tools[1].permission)
        assertFalse(MonarchAiConnectorToolPermission.Allow in rules.choices(Delete, allowsDestructive = false))
        val stored = Delete.copy(permission = MonarchAiConnectorToolPermission.Allow)
        assertTrue(MonarchAiConnectorToolPermission.Allow in rules.choices(stored, allowsDestructive = false))
        assertEquals(3, rules.choices(Search, allowsDestructive = false).size)
        assertEquals("creme crm", tokens.fold("  Crème CRM "))
        assertEquals(listOf(Drive, Mail), tokens.filter(listOf(Drive, Mail), "   "))
        assertEquals(1, tokens.askingCount(listOf(Drive.copy(enabled = true), Crm)))
        assertEquals(0, tokens.onCount(listOf(Crm)))
        assertEquals(MonarchTone.Danger, tokens.riskTone(MonarchAiConnectorToolRisk.Destructive))
        assertEquals(MonarchTone.Warning, tokens.riskTone(MonarchAiConnectorToolRisk.Write))
    }

    @Test
    fun theColoursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchAiConnectorPickerTokens
            assertEquals(colors.successStrong, tokens.statusColor(MonarchAiConnectorStatus.Connected, colors))
            assertEquals(colors.warningStrong, tokens.statusColor(MonarchAiConnectorStatus.NeedsSignIn, colors))
            assertEquals(colors.dangerStrong, tokens.statusColor(MonarchAiConnectorStatus.Error, colors))
            assertEquals(colors.textMuted, tokens.statusColor(MonarchAiConnectorStatus.Unavailable, colors))
            assertEquals(colors.textMuted, tokens.muted(colors))
        }
    }
}
