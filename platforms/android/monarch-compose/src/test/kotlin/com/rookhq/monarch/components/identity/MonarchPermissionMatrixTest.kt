package com.rookhq.monarch.components.identity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.identity.permissionmatrix.MonarchPermissionMatrix
import com.rookhq.monarch.components.identity.permissionmatrix.MonarchPermissionMatrixLabels
import com.rookhq.monarch.components.identity.permissionmatrix.MonarchPermissionMatrixLayout
import com.rookhq.monarch.components.identity.permissionmatrix.MonarchPermissionMatrixPermission
import com.rookhq.monarch.components.identity.permissionmatrix.MonarchPermissionMatrixRole
import com.rookhq.monarch.components.identity.permissionmatrix.MonarchPermissionMatrixTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val TwoRoles = listOf(
    MonarchPermissionMatrixRole("owner", "Owner", "Everything, including billing."),
    MonarchPermissionMatrixRole("member", "Member"),
)
private val ThreeRoles = TwoRoles + MonarchPermissionMatrixRole("guest", "Guest", "Sees shared projects.")
private val Permissions = listOf(
    MonarchPermissionMatrixPermission("invite", "Invite members", group = "People"),
    MonarchPermissionMatrixPermission("billing", "Manage billing", "Invoices and plans", group = "Billing"),
)
private val Grants = mapOf("owner" to setOf("invite", "billing"), "member" to setOf("invite"))
private val OwnerLocked: (String, String) -> String? = { role, _ -> if (role == "owner") "" else null }

internal object MonarchPermissionMatrixFixtures : ComponentFixtures {
    override val id = "permission-matrix"
    override val fixtures = listOf(
        fixture("grid, two roles, locked owner") {
            MonarchPermissionMatrix(TwoRoles, Permissions, Grants, {}, lockReason = OwnerLocked)
        },
        fixture("by role, titled") {
            MonarchPermissionMatrix(ThreeRoles, Permissions, Grants, {}, title = "Workspace roles")
        },
        fixture("by role, locked with a reason") {
            MonarchPermissionMatrix(
                TwoRoles,
                Permissions,
                Grants,
                {},
                layout = MonarchPermissionMatrixLayout.ByRole,
                lockReason = { _, permission -> if (permission == "billing") "Set by your plan." else null },
            )
        },
        fixture("grid, read only") {
            MonarchPermissionMatrix(
                TwoRoles,
                Permissions,
                Grants,
                {},
                layout = MonarchPermissionMatrixLayout.Grid,
                readOnly = true,
            )
        },
        fixture("by role, read only") {
            MonarchPermissionMatrix(
                TwoRoles,
                Permissions,
                Grants,
                {},
                layout = MonarchPermissionMatrixLayout.ByRole,
                readOnly = true,
            )
        },
        fixture("grid forced, scrolling, disabled") {
            MonarchPermissionMatrix(
                ThreeRoles,
                Permissions.take(1),
                Grants,
                {},
                layout = MonarchPermissionMatrixLayout.Grid,
                enabled = false,
            )
        },
        fixture("no roles") { MonarchPermissionMatrix(emptyList(), Permissions, emptyMap(), {}, title = "Roles") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPermissionMatrixTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aGridCellIsACheckboxNamedPermissionForRoleThatTogglesTheValue() {
        var value by mutableStateOf(Grants)
        rule.setMonarchContent {
            MonarchPermissionMatrix(TwoRoles, Permissions, value, { value = it })
        }
        val cell = rule.onNodeWithContentDescription("Manage billing for Member")
        cell.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(setOf("invite", "billing"), value["member"])
        assertEquals(Grants["owner"], value["owner"])
        rule.onNodeWithContentDescription("Manage billing for Member").assertIsOn()
    }

    @Test
    fun aLockedCellIsDisabledAndSaysWhy() {
        var changes = 0
        rule.setMonarchContent {
            MonarchPermissionMatrix(TwoRoles, Permissions, Grants, { changes++ }, lockReason = OwnerLocked)
        }
        rule.onNodeWithContentDescription("Invite members for Owner, This can’t be changed")
            .assertIsNotEnabled()
            .performClick()
        assertEquals(0, changes)
        // The grid shows a lock mark, not the reason in words: the reason is in the cell's spoken name.
        rule.onAllNodesWithText("Owner: This can’t be changed").assertCountEquals(0)
    }

    @Test
    fun theOneRoleLayoutPicksARoleWithTabs() {
        var value by mutableStateOf(Grants)
        rule.setMonarchContent {
            MonarchPermissionMatrix(ThreeRoles, Permissions, value, { value = it })
        }
        rule.onNodeWithText("Owner").assertIsSelected()
        rule.onNodeWithText("Everything, including billing.").assertExists()
        rule.onNodeWithText("Guest").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Guest").assertIsSelected()
        rule.onNodeWithText("Sees shared projects.").assertExists()
        rule.onNodeWithText("Invite members").assertIsOff().performClick()
        assertEquals(setOf("invite"), value["guest"])
    }

    @Test
    fun aLockedRowWritesItsReasonUnderTheRow() {
        rule.setMonarchContent {
            MonarchPermissionMatrix(
                TwoRoles,
                Permissions,
                Grants,
                {},
                layout = MonarchPermissionMatrixLayout.ByRole,
                lockReason = { _, permission -> if (permission == "billing") "Set by your plan." else null },
            )
        }
        rule.onNodeWithText("Manage billing").assertIsNotEnabled()
        rule.onNodeWithText("Set by your plan.", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun readOnlyCellsReadAllowedOrNotAllowed() {
        rule.setMonarchContent {
            MonarchPermissionMatrix(
                TwoRoles,
                Permissions,
                Grants,
                {},
                layout = MonarchPermissionMatrixLayout.Grid,
                readOnly = true,
            )
        }
        assertEquals("Allowed", rule.onNodeWithContentDescription("Manage billing for Owner").stateDescription())
        assertEquals("Not allowed", rule.onNodeWithContentDescription("Manage billing for Member").stateDescription())
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).assertDoesNotExist()
    }

    @Test
    fun disabledMakesEveryCellUnchangeable() {
        var changes = 0
        rule.setMonarchContent {
            MonarchPermissionMatrix(TwoRoles, Permissions, Grants, { changes++ }, enabled = false)
        }
        rule.onNodeWithContentDescription("Invite members for Member").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theTitleAndHeadingsAreHeadingsAndStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPermissionMatrix(
                TwoRoles,
                Permissions,
                Grants,
                {},
                title = "Workspace roles",
                labels = MonarchPermissionMatrixLabels(
                    permissionHeader = "Recht",
                    cellLabel = { permission, role -> "$role: $permission" },
                ),
            )
        }
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        rule.onNodeWithText("Workspace roles").assert(heading)
        rule.onNodeWithText("Recht").assert(heading)
        rule.onNodeWithText("People").assert(heading)
        rule.onNodeWithContentDescription("Member: Invite members").assertIsOn()
    }

    @Test
    fun withNoRolesOnlyTheTitleIsShown() {
        rule.setMonarchContent { MonarchPermissionMatrix(emptyList(), Permissions, emptyMap(), {}, title = "Roles") }
        rule.onNodeWithText("Roles").assertExists()
        rule.onNodeWithText("Invite members").assertDoesNotExist()
    }

    @Test
    fun thePermissionColumnIsAtTheStartInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchPermissionMatrix(TwoRoles, Permissions, Grants, {})
        }
        rule.onNodeWithText("Invite members")
            .assertIsBefore(rule.onNodeWithContentDescription("Invite members for Owner"), LayoutDirection.Rtl)
    }

    @Test
    fun theLargestFontScaleShowsOneRoleAtATimeWithoutCuttingText() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchPermissionMatrix(TwoRoles, Permissions, Grants, {})
        }
        rule.onNodeWithContentDescription("Invite members for Owner").assertDoesNotExist()
        rule.onNodeWithText("Manage billing", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheLayoutAndTheValue() {
        val tokens = MonarchPermissionMatrixTokens
        assertTrue(tokens.fitsGrid(320.dp, 2, 1f))
        assertFalse(tokens.fitsGrid(320.dp, 3, 1f))
        assertFalse(tokens.fitsGrid(600.dp, 2, 2f))
        assertEquals(240.dp, tokens.permissionColumnWidth(400.dp, 2))
        assertEquals(tokens.PermissionColumnMinWidth, tokens.permissionColumnWidth(200.dp, 3))
        val added = tokens.toggled(mapOf("a" to setOf("x")), "b", "y", granted = true)
        assertEquals(mapOf("a" to setOf("x"), "b" to setOf("y")), added)
        assertEquals(mapOf("a" to emptySet()), tokens.toggled(mapOf("a" to setOf("x")), "a", "x", granted = false))
        assertEquals(listOf("People", "Billing"), tokens.groups(Permissions).map { it.first })
        assertNull(tokens.lockReason(null, "Locked"))
        assertEquals("Locked", tokens.lockReason(" ", "Locked"))
        assertEquals("Why", tokens.lockReason("Why", "Locked"))
    }

    @Test
    fun theColoursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.successStrong, MonarchPermissionMatrixTokens.mark(true, colors))
            assertEquals(colors.textSubtle, MonarchPermissionMatrixTokens.mark(false, colors))
            assertEquals(colors.textMuted, MonarchPermissionMatrixTokens.muted(colors))
        }
    }
}
