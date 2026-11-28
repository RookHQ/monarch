package com.rookhq.monarch.components.identity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.identity.rolepicker.MonarchRoleOption
import com.rookhq.monarch.components.identity.rolepicker.MonarchRolePicker
import com.rookhq.monarch.components.identity.rolepicker.MonarchRolePickerLayout
import com.rookhq.monarch.components.identity.rolepicker.MonarchRolePickerTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import kotlin.test.assertNull

private val Roles = listOf(
    MonarchRoleOption("viewer", title = "Viewer", description = "Can see everything, change nothing."),
    MonarchRoleOption(
        "editor",
        title = "Editor",
        description = "Can edit content.",
        capabilities = listOf("Invite guests"),
    ),
    MonarchRoleOption("owner", title = "Owner", enabled = false, disabledReason = "Only owners can add owners."),
)

internal object MonarchRolePickerFixtures : ComponentFixtures {
    override val id = "role-picker"
    override val fixtures = listOf(
        fixture("cards, selected") { MonarchRolePicker(roles = Roles.take(2), selected = "viewer", onSelect = {}) },
        fixture("cards, disabled role with helper") {
            MonarchRolePicker(roles = Roles.drop(2), selected = null, onSelect = {}, helperText = "Owners can delete.")
        },
        fixture("cards, all disabled") {
            MonarchRolePicker(roles = Roles.take(1), selected = "viewer", onSelect = {}, enabled = false)
        },
        fixture("compact, empty with error") {
            MonarchRolePicker(
                roles = Roles,
                selected = null,
                onSelect = {},
                layout = MonarchRolePickerLayout.Compact,
                errorText = "Choose a role.",
            )
        },
        fixture("compact, selected") {
            MonarchRolePicker(
                roles = Roles,
                selected = "editor",
                onSelect = {},
                layout = MonarchRolePickerLayout.Compact,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchRolePickerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachCardIsARadioButtonWithItsDetails() {
        rule.setMonarchContent {
            var role by remember { mutableStateOf<String?>("viewer") }
            MonarchRolePicker(roles = Roles, selected = role, onSelect = { role = it })
        }
        rule.onNodeWithText("Viewer").assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        rule.onNodeWithText("Editor").assertTouchTargetAtLeast48dp(rule.density).performClick().assertIsSelected()
        rule.onNodeWithText("Can edit content.\n• Invite guests", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Role", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aDisabledRoleStaysVisibleWithItsReasonAndCannotBeChosen() {
        var chosen: String? = null
        rule.setMonarchContent { MonarchRolePicker(roles = Roles, selected = null, onSelect = { chosen = it }) }
        rule.onNodeWithText("Owner").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Only owners can add owners.", useUnmergedTree = true).assertExists()
        assertNull(chosen)
    }

    @Test
    fun theCompactFieldOpensAMenuAndShowsTheChosenRole() {
        rule.setMonarchContent {
            var role by remember { mutableStateOf<String?>(null) }
            MonarchRolePicker(
                roles = Roles,
                selected = role,
                onSelect = { role = it },
                layout = MonarchRolePickerLayout.Compact,
            )
        }
        rule.onNodeWithText("Choose a role").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Editor").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Editor").assertExists()
    }

    @Test
    fun anErrorReplacesTheHelperAndIsAnErrorOnTheCompactField() {
        rule.setMonarchContent {
            MonarchRolePicker(
                roles = Roles,
                selected = null,
                onSelect = {},
                layout = MonarchRolePickerLayout.Compact,
                helperText = "Help",
                errorText = "Choose a role.",
            )
        }
        rule.onNodeWithText("Help").assertDoesNotExist()
        rule.onNodeWithText("Choose a role.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Choose a role", substring = false)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Choose a role."))
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchRolePicker(
                roles = Roles,
                selected = null,
                onSelect = {},
                layout = MonarchRolePickerLayout.Compact,
                label = "Rol",
                placeholder = "Elige un rol",
            )
        }
        rule.onNodeWithText("Rol", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Elige un rol").assertExists()
    }

    @Test
    fun cardsWorkRightToLeftAndAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchRolePicker(roles = Roles.take(1), selected = "viewer", onSelect = {})
        }
        rule.onNodeWithText("Can see everything, change nothing.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theDetailsJoinDescriptionCapabilitiesAndReason() {
        assertEquals("Can see everything, change nothing.", MonarchRolePickerTokens.details(Roles[0]))
        assertEquals("Can edit content.\n• Invite guests", MonarchRolePickerTokens.details(Roles[1]))
        assertEquals("Only owners can add owners.", MonarchRolePickerTokens.details(Roles[2]))
        assertNull(MonarchRolePickerTokens.details(MonarchRoleOption("x", title = "X")))
        assertNull(MonarchRolePickerTokens.chosen(Roles, "missing"))
        assertEquals("Editor", MonarchRolePickerTokens.chosen(Roles, "editor")?.title)
    }

    @Test
    fun theSupportColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.dangerStrong, MonarchRolePickerTokens.supportColor(error = true, c), theme.id)
            assertEquals(c.textMuted, MonarchRolePickerTokens.supportColor(error = false, c), theme.id)
        }
    }
}
