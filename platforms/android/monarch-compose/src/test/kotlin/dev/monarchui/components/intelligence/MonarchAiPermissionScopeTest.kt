package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aipermissionscope.MonarchAiAccessLevel
import dev.monarchui.components.intelligence.aipermissionscope.MonarchAiPermissionScope
import dev.monarchui.components.intelligence.aipermissionscope.MonarchAiPermissionScopeItem
import dev.monarchui.components.intelligence.aipermissionscope.MonarchAiPermissionScopeLabels
import dev.monarchui.components.intelligence.aipermissionscope.MonarchAiPermissionScopeTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchAiPermissionScopeFixtures : ComponentFixtures {
    override val id = "ai-permission-scope"
    override val fixtures = listOf(
        fixture("read only, every level") {
            MonarchAiPermissionScope(
                scopes = listOf(
                    MonarchAiPermissionScopeItem("Calendar", MonarchAiAccessLevel.Read),
                    MonarchAiPermissionScopeItem(
                        "Drive",
                        MonarchAiAccessLevel.Write,
                        resources = "Shared projects only",
                    ),
                    MonarchAiPermissionScopeItem("Mail", MonarchAiAccessLevel.None),
                ),
                accessIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("editable, with manage") {
            MonarchAiPermissionScope(
                scopes = listOf(MonarchAiPermissionScopeItem("Calendar", MonarchAiAccessLevel.Read)),
                onScopesChange = {},
                onManage = {},
            )
        },
        fixture("editable, disabled") {
            MonarchAiPermissionScope(
                scopes = listOf(MonarchAiPermissionScopeItem("Drive", MonarchAiAccessLevel.Write)),
                onScopesChange = {},
                enabled = false,
            )
        },
        fixture("long names") {
            MonarchAiPermissionScope(
                scopes = listOf(
                    MonarchAiPermissionScopeItem(
                        "Customer relationship management",
                        MonarchAiAccessLevel.Read,
                        resources = "Accounts and opportunities owned by your team",
                    ),
                ),
            )
        },
        fixture("empty") { MonarchAiPermissionScope(scopes = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiPermissionScopeTest {
    @get:Rule
    val rule = createComposeRule()

    private val scopes = listOf(
        MonarchAiPermissionScopeItem("Calendar", MonarchAiAccessLevel.Read),
        MonarchAiPermissionScopeItem("Drive", MonarchAiAccessLevel.Write, resources = "Shared projects only"),
        MonarchAiPermissionScopeItem("Mail", MonarchAiAccessLevel.None, resources = ""),
    )

    @Test
    fun theListIsAGroupNamedAfterWhatTheAiCanAccess() {
        rule.setMonarchContent { MonarchAiPermissionScope(scopes = scopes) }
        rule.onNodeWithContentDescription("What the AI can access")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
    }

    @Test
    fun aReadOnlyRowIsOneElementWithTheAccessAsItsState() {
        rule.setMonarchContent { MonarchAiPermissionScope(scopes = scopes) }
        val drive = rule.onNodeWithContentDescription("Drive, Shared projects only")
        assertEquals("Can read and edit", drive.stateDescription())
        assertEquals("Can read", rule.onNodeWithContentDescription("Calendar").stateDescription())
        // An empty resources string is not read.
        assertEquals("No access", rule.onNodeWithContentDescription("Mail").stateDescription())
    }

    @Test
    fun theAccessIsWrittenSoItDoesNotDependOnColour() {
        rule.setMonarchContent { MonarchAiPermissionScope(scopes = scopes) }
        rule.onNodeWithText("No access", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Can read and edit", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theAccessIconReceivesTheLevelOfItsRow() {
        val seen = mutableListOf<MonarchAiAccessLevel>()
        rule.setMonarchContent {
            MonarchAiPermissionScope(scopes = scopes, accessIcon = { level -> seen += level })
        }
        rule.waitForIdle()
        assertEquals(
            setOf(MonarchAiAccessLevel.Read, MonarchAiAccessLevel.Write, MonarchAiAccessLevel.None),
            seen.toSet(),
        )
    }

    @Test
    fun anEditableRowChangesOneRowAndHandsBackTheWholeList() {
        var changed: List<MonarchAiPermissionScopeItem>? = null
        rule.setMonarchContent {
            var current by remember { mutableStateOf(scopes.take(2)) }
            MonarchAiPermissionScope(
                scopes = current,
                onScopesChange = {
                    changed = it
                    current = it
                },
            )
        }
        rule.onNodeWithContentDescription("Access to Calendar").assertExists()
        val calendarWrite = rule.onAllNodesWithText("Write")[0]
        calendarWrite.assertIsNotSelected().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(
            listOf(MonarchAiAccessLevel.Write, MonarchAiAccessLevel.Write),
            changed?.map { it.access },
        )
        rule.onAllNodesWithText("Write")[0].assertIsSelected()
    }

    @Test
    fun pickingTheCurrentLevelDoesNotCallBack() {
        var calls = 0
        rule.setMonarchContent {
            MonarchAiPermissionScope(scopes = scopes.take(1), onScopesChange = { calls++ })
        }
        rule.onNodeWithText("Read").assertIsSelected().performClick()
        assertEquals(0, calls)
    }

    @Test
    fun aDisabledControlDoesNotCallBack() {
        var calls = 0
        rule.setMonarchContent {
            MonarchAiPermissionScope(scopes = scopes.take(1), onScopesChange = { calls++ }, enabled = false)
        }
        rule.onNodeWithText("None").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test
    fun theManageLinkCallsBack() {
        var opened = 0
        rule.setMonarchContent { MonarchAiPermissionScope(scopes = scopes, onManage = { opened++ }) }
        rule.onNodeWithText("Manage permissions").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun theEmptyListSaysSo() {
        rule.setMonarchContent { MonarchAiPermissionScope(scopes = emptyList()) }
        rule.onNodeWithText("The AI has no access to any app.").assertExists()
        rule.onNode(hasContentDescription("What the AI can access")).assertDoesNotExist()
    }

    @Test
    fun everyStringCanBeReplaced() {
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchAiPermissionScope(
                    scopes = scopes.take(1),
                    modifier = Modifier.testTag("read"),
                    labels = MonarchAiPermissionScopeLabels(
                        label = "Assistant access",
                        access = { "Level ${it.name}" },
                    ),
                )
                MonarchAiPermissionScope(
                    scopes = listOf(MonarchAiPermissionScopeItem("Docs", MonarchAiAccessLevel.None)),
                    onScopesChange = {},
                    labels = MonarchAiPermissionScopeLabels(
                        option = { "Opt ${it.name}" },
                        control = { "Rights in $it" },
                        manage = "Settings",
                    ),
                    onManage = {},
                )
                MonarchAiPermissionScope(
                    scopes = emptyList(),
                    labels = MonarchAiPermissionScopeLabels(empty = "Nothing shared"),
                )
            }
        }
        rule.onNodeWithContentDescription("Assistant access").assertExists()
        assertEquals("Level Read", rule.onNodeWithContentDescription("Calendar").stateDescription())
        rule.onNodeWithContentDescription("Rights in Docs").assertExists()
        rule.onNode(hasText("Opt Write")).assertExists()
        rule.onNodeWithText("Settings").assertExists()
        rule.onNodeWithText("Nothing shared").assertExists()
    }

    @Test
    fun theAccessFollowsTheAppInTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiPermissionScope(scopes = scopes.take(1))
        }
        rule.onNodeWithText("Calendar", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Can read", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun longNamesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiPermissionScope(
                scopes = listOf(
                    MonarchAiPermissionScopeItem(
                        "Customer relationship management",
                        MonarchAiAccessLevel.Read,
                        resources = "Accounts and opportunities owned by your team",
                    ),
                ),
                onScopesChange = {},
            )
        }
        rule.onNodeWithText("Customer relationship management", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Accounts and opportunities owned by your team", useUnmergedTree = true)
            .assertTextFullyVisible()
        rule.onNodeWithText("Write", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun levelsAreOrderedAndChangesAreOneRow() {
        assertTrue(MonarchAiAccessLevel.Write.includes(MonarchAiAccessLevel.Read))
        assertTrue(MonarchAiAccessLevel.Read.includes(MonarchAiAccessLevel.None))
        assertFalse(MonarchAiAccessLevel.Read.includes(MonarchAiAccessLevel.Write))
        val tokens = MonarchAiPermissionScopeTokens
        assertNull(tokens.changed(scopes, 0, MonarchAiAccessLevel.Read))
        assertNull(tokens.changed(scopes, 9, MonarchAiAccessLevel.Read))
        val changed = tokens.changed(scopes, 2, MonarchAiAccessLevel.Read)
        assertEquals(
            listOf(MonarchAiAccessLevel.Read, MonarchAiAccessLevel.Write, MonarchAiAccessLevel.Read),
            changed?.map { it.access },
        )
        assertEquals("Drive, Shared projects only", tokens.appDescription(scopes[1]))
        assertEquals("Mail", tokens.appDescription(scopes[2]))
    }

    @Test
    fun noAccessIsMutedInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchAiPermissionScopeTokens
            assertEquals(colors.textMuted, tokens.accessColor(MonarchAiAccessLevel.None, colors), theme.id)
            assertEquals(colors.textDefault, tokens.accessColor(MonarchAiAccessLevel.Write, colors), theme.id)
        }
    }
}
