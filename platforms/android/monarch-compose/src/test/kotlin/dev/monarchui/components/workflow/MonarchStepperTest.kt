package dev.monarchui.components.workflow

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.workflow.stepper.MonarchStepper
import dev.monarchui.components.workflow.stepper.MonarchStepperCompactMode
import dev.monarchui.components.workflow.stepper.MonarchStepperLabels
import dev.monarchui.components.workflow.stepper.MonarchStepperOrientation
import dev.monarchui.components.workflow.stepper.MonarchStepperStatus
import dev.monarchui.components.workflow.stepper.MonarchStepperStep
import dev.monarchui.components.workflow.stepper.MonarchStepperTokens
import dev.monarchui.components.workflow.stepper.monarchStepperIsRow
import dev.monarchui.testing.AllThemes
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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

private val Wizard = listOf(
    MonarchStepperStep(id = "details", label = "Details"),
    MonarchStepperStep(id = "address", label = "Address"),
    MonarchStepperStep(id = "review", label = "Review"),
)

private val Setup = listOf(
    MonarchStepperStep(id = "plan", label = "Plan"),
    MonarchStepperStep(id = "team", label = "Team", description = "Invite people", optional = true),
    MonarchStepperStep(id = "billing", label = "Billing"),
    MonarchStepperStep(id = "review", label = "Review"),
)

private val Never = MonarchStepperCompactMode.Never
private val Vertical = MonarchStepperOrientation.Vertical

internal object MonarchStepperFixtures : ComponentFixtures {
    override val id = "stepper"
    override val fixtures = listOf(
        fixture("horizontal, automatic") { MonarchStepper(steps = Wizard, current = 1) },
        fixture("horizontal, never compact") { MonarchStepper(steps = Wizard, current = 1, compact = Never) },
        fixture("horizontal, selectable") {
            MonarchStepper(steps = Wizard, current = 1, furthest = 2, compact = Never, onSelect = { _, _ -> })
        },
        fixture("vertical, description and optional") {
            MonarchStepper(steps = Setup.take(3), current = 1, orientation = Vertical)
        },
        fixture("vertical, selectable with a disabled step") {
            MonarchStepper(
                steps = listOf(
                    MonarchStepperStep(id = "a", label = "Account"),
                    MonarchStepperStep(id = "b", label = "Billing", enabled = false),
                    MonarchStepperStep(id = "c", label = "Review"),
                ),
                current = 2,
                orientation = Vertical,
                onSelect = { _, _ -> },
            )
        },
        fixture("error and complete flags") {
            MonarchStepper(
                steps = listOf(
                    MonarchStepperStep(id = "a", label = "Account", error = true),
                    MonarchStepperStep(id = "b", label = "Billing"),
                    MonarchStepperStep(id = "c", label = "Extras", complete = true),
                ),
                current = 1,
                orientation = Vertical,
            )
        },
        fixture("always compact") {
            MonarchStepper(steps = Setup, currentId = "billing", compact = MonarchStepperCompactMode.Always)
        },
        fixture("long name, compact") {
            MonarchStepper(
                steps = listOf(
                    MonarchStepperStep(id = "a", label = "Connect your company's identity provider"),
                    MonarchStepperStep(id = "b", label = "Review"),
                ),
                current = 0,
                compact = MonarchStepperCompactMode.Always,
            )
        },
        fixture("one step") { MonarchStepper(steps = Wizard.take(1), current = 0, compact = Never) },
        fixture("finished") { MonarchStepper(steps = Wizard, current = 2, compact = Never, finished = true) },
        fixture("busy") { MonarchStepper(steps = Wizard, current = 1, orientation = Vertical, busy = true) },
        fixture("finished, compact") {
            MonarchStepper(steps = Wizard, current = 2, compact = MonarchStepperCompactMode.Always, finished = true)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchStepperTest {
    @get:Rule
    val rule = createComposeRule()

    private val hasNoClick = SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick)

    @Test
    fun eachStepIsOneElementWithItsPositionNameAndState() {
        rule.setMonarchContent { MonarchStepper(steps = Wizard, current = 1, compact = Never) }
        assertEquals("completed", rule.onNodeWithContentDescription("Step 1 of 3, Details").stateDescription())
        assertEquals("current step", rule.onNodeWithContentDescription("Step 2 of 3, Address").stateDescription())
        assertNull(rule.onNodeWithContentDescription("Step 3 of 3, Review").stateDescription())
        rule.onNodeWithContentDescription("Step 2 of 3, Address").assert(hasNoClick)
        rule.onNodeWithContentDescription("Progress")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo))
    }

    @Test
    fun anErrorWinsAndTheOptionalNoteAndDescriptionAreRead() {
        rule.setMonarchContent {
            MonarchStepper(
                steps = Setup.take(2) + MonarchStepperStep(id = "x", label = "Billing", error = true),
                current = 2,
                orientation = Vertical,
            )
        }
        assertEquals(
            "completed, Optional",
            rule.onNodeWithContentDescription("Step 2 of 3, Team, Invite people").stateDescription(),
        )
        assertEquals("needs attention", rule.onNodeWithContentDescription("Step 3 of 3, Billing").stateDescription())
        rule.onNodeWithText("Optional", useUnmergedTree = true).assertExists()
    }

    @Test
    fun onlyReachedStepsOtherThanTheCurrentOneAreButtons() {
        var selected: Pair<String, Int>? = null
        rule.setMonarchContent {
            MonarchStepper(
                steps = Setup,
                current = 1,
                furthest = 2,
                orientation = Vertical,
                onSelect = { step, index -> selected = step.id to index },
            )
        }
        rule.onNodeWithContentDescription("Step 2 of 4, Team, Invite people").assert(hasNoClick)
        rule.onNodeWithContentDescription("Step 4 of 4, Review").assert(hasNoClick)
        rule.onNodeWithContentDescription("Step 3 of 4, Billing").assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("billing" to 2, selected)
        rule.onNodeWithContentDescription("Step 1 of 4, Plan").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("plan" to 0, selected)
    }

    @Test
    fun aDisabledStepIsNotAButtonAndIsMarkedDisabled() {
        rule.setMonarchContent {
            MonarchStepper(
                steps = listOf(
                    MonarchStepperStep(id = "a", label = "Account", enabled = false),
                    MonarchStepperStep(id = "b", label = "Billing"),
                ),
                current = 1,
                orientation = Vertical,
                onSelect = { _, _ -> },
            )
        }
        rule.onNodeWithContentDescription("Step 1 of 2, Account").assertHasNoClickAction().assertIsNotEnabled()
    }

    @Test
    fun withoutOnSelectNoStepIsAButton() {
        rule.setMonarchContent { MonarchStepper(steps = Wizard, current = 2, orientation = Vertical) }
        rule.onNodeWithContentDescription("Step 1 of 3, Details").assert(hasNoClick)
        rule.onNodeWithContentDescription("Step 2 of 3, Address").assert(hasNoClick)
    }

    @Test
    fun aFinishedStepperMarksEveryStepDoneAndHasNoCurrentStep() {
        rule.setMonarchContent { MonarchStepper(steps = Wizard, current = 1, compact = Never, finished = true) }
        listOf("Step 1 of 3, Details", "Step 2 of 3, Address", "Step 3 of 3, Review").forEach {
            assertEquals("completed", rule.onNodeWithContentDescription(it).stateDescription(), it)
        }
    }

    @Test
    fun aFinishedCompactStepperReadsTheLastStepAsCompleted() {
        rule.setMonarchContent {
            MonarchStepper(steps = Wizard, current = 0, compact = MonarchStepperCompactMode.Always, finished = true)
        }
        assertEquals(
            "Step 3 of 3, Review, completed",
            rule.onNodeWithContentDescription("Progress").stateDescription(),
        )
    }

    @Test
    fun aBusyStepperSaysTheCurrentStepIsInProgressAndAFinishedOneIgnoresBusy() {
        rule.setMonarchContent { MonarchStepper(steps = Wizard, current = 1, compact = Never, busy = true) }
        assertEquals(
            "current step, in progress",
            rule.onNodeWithContentDescription("Step 2 of 3, Address").stateDescription(),
        )
        // The ring replaces the number of the current step.
        rule.onNodeWithText("2", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("3", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theBusyStateCanBeRenamed() {
        rule.setMonarchContent {
            MonarchStepper(
                steps = Wizard,
                current = 0,
                compact = Never,
                busy = true,
                labels = MonarchStepperLabels(busy = "saving"),
            )
        }
        val first = rule.onNodeWithContentDescription("Step 1 of 3, Details")
        assertEquals("current step, saving", first.stateDescription())
    }

    @Test
    fun aNarrowHorizontalStepperBecomesOneCompactElement() {
        rule.setMonarchContent {
            MonarchStepper(
                steps = Setup + MonarchStepperStep(id = "done", label = "Done"),
                current = 1,
                modifier = Modifier.width(300.dp),
            )
        }
        val summary = rule.onNodeWithContentDescription("Progress")
        assertEquals("Step 2 of 5, Team", summary.stateDescription())
        rule.onNodeWithContentDescription("Step 1 of 5, Plan").assertDoesNotExist()
        rule.onNodeWithText("Step 2 of 5", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aHorizontalStepperThatFitsShowsEveryStep() {
        rule.setMonarchContent {
            MonarchStepper(steps = Wizard.take(2).map { MonarchStepperStep(it.id, it.label.take(2)) }, current = 0)
        }
        rule.onNodeWithContentDescription("Step 1 of 2, De").assertExists()
        rule.onNodeWithContentDescription("Step 2 of 2, Ad").assertExists()
    }

    @Test
    fun aVerticalStepperIsCompactOnlyWhenAsked() {
        rule.setMonarchContent {
            MonarchStepper(steps = Setup, current = 1, orientation = Vertical, modifier = Modifier.width(120.dp))
        }
        rule.onNodeWithContentDescription("Step 1 of 4, Plan").assertExists()
    }

    @Test
    fun theIdentifierOverloadFindsTheStepsAndFallsBack() {
        rule.setMonarchContent {
            MonarchStepper(
                steps = Wizard,
                currentId = "unknown",
                furthestId = "review",
                compact = Never,
                onSelect = { _, _ -> },
            )
        }
        assertEquals("current step", rule.onNodeWithContentDescription("Step 1 of 3, Details").stateDescription())
        rule.onNodeWithContentDescription("Step 3 of 3, Review").assertHasClickAction()
    }

    @Test
    fun positionsOutsideTheStepsAreMovedInside() {
        rule.setMonarchContent { MonarchStepper(steps = Wizard, current = 10, furthest = -4, orientation = Vertical) }
        assertEquals("current step", rule.onNodeWithContentDescription("Step 3 of 3, Review").stateDescription())
        assertEquals(0, MonarchStepperTokens.clamp(-1, 3))
        assertEquals(0, MonarchStepperTokens.clamp(5, 0))
        assertEquals(2, MonarchStepperTokens.indexOf(Wizard, "review", 0))
        assertEquals(1, MonarchStepperTokens.indexOf(Wizard, "nope", 1))
    }

    @Test
    fun noStepsDrawsNothing() {
        rule.setMonarchContent { MonarchStepper(steps = emptyList(), current = 0) }
        rule.onNodeWithContentDescription("Progress").assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        val labels = MonarchStepperLabels(
            progress = "Fortschritt",
            stepOf = { step, total -> "Schritt $step von $total" },
            complete = "erledigt",
            error = "Fehler",
            current = "aktuell",
            optional = "Freiwillig",
        )
        rule.setMonarchContent {
            MonarchStepper(
                steps = listOf(
                    MonarchStepperStep(id = "a", label = "Konto", error = true),
                    MonarchStepperStep(id = "b", label = "Team", optional = true),
                    MonarchStepperStep(id = "c", label = "Fertig"),
                ),
                current = 2,
                orientation = Vertical,
                labels = labels,
            )
        }
        rule.onNodeWithContentDescription("Fortschritt").assertExists()
        assertEquals("Fehler", rule.onNodeWithContentDescription("Schritt 1 von 3, Konto").stateDescription())
        val team = rule.onNodeWithContentDescription("Schritt 2 von 3, Team")
        assertEquals("erledigt, Freiwillig", team.stateDescription())
        assertEquals("aktuell", rule.onNodeWithContentDescription("Schritt 3 von 3, Fertig").stateDescription())
        rule.onNodeWithText("Freiwillig", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theCompactSummaryUsesTheReplacedStrings() {
        rule.setMonarchContent {
            MonarchStepper(
                steps = Wizard,
                current = 0,
                compact = MonarchStepperCompactMode.Always,
                labels = MonarchStepperLabels(progress = "Setup", stepOf = { step, total -> "$step/$total" }),
            )
        }
        assertEquals("1/3, Details", rule.onNodeWithContentDescription("Setup").stateDescription())
    }

    @Test
    fun stepsRunFromTheStartInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchStepper(steps = Wizard, current = 1, compact = Never, modifier = Modifier.requiredWidth(600.dp))
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Step 1 of 3, Details")
                .assertIsBefore(rule.onNodeWithContentDescription("Step 2 of 3, Address"), direction)
        }
    }

    @Test
    fun namesWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStepper(
                steps = listOf(
                    MonarchStepperStep(id = "a", label = "Connect the identity provider", description = "SAML or OIDC"),
                ),
                current = 0,
                orientation = Vertical,
            )
        }
        rule.onNodeWithText("Connect the identity provider", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("SAML or OIDC", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theCompactSummaryWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStepper(
                steps = listOf(MonarchStepperStep(id = "a", label = "Connect the identity provider")),
                current = 0,
                compact = MonarchStepperCompactMode.Always,
            )
        }
        rule.onNodeWithText("Connect the identity provider", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun aHorizontalStepperThatCannotBeCompactStacksInsteadOfBreakingNames() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStepper(steps = Wizard, current = 1, compact = Never)
        }
        listOf("Details", "Address", "Review").forEach {
            rule.onNodeWithText(it, useUnmergedTree = true).assertTextFullyVisible()
        }
        val first = rule.onNodeWithContentDescription("Step 1 of 3, Details").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithContentDescription("Step 2 of 3, Address").getUnclippedBoundsInRoot()
        assertTrue(second.top >= first.bottom, "the steps are in a column")
        val label = rule.onNodeWithText("Details", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(label.bottom - label.top < 40.dp, "the name is on one line, not one letter per line")
        assertFalse(monarchStepperIsRow(Never, MonarchStepperOrientation.Horizontal, fits = false))
        assertTrue(monarchStepperIsRow(Never, MonarchStepperOrientation.Horizontal, fits = true))
        val automatic = MonarchStepperCompactMode.Automatic
        assertTrue(monarchStepperIsRow(automatic, MonarchStepperOrientation.Horizontal, fits = true))
        assertFalse(monarchStepperIsRow(Never, Vertical, fits = true))
    }

    @Test
    fun statusSelectionAndCompactLogic() {
        val plain = MonarchStepperStep(id = "a", label = "A")
        assertEquals(MonarchStepperStatus.Complete, MonarchStepperTokens.status(0, 1, plain))
        assertEquals(MonarchStepperStatus.Current, MonarchStepperTokens.status(1, 1, plain))
        assertEquals(MonarchStepperStatus.Upcoming, MonarchStepperTokens.status(2, 1, plain))
        val finished = MonarchStepperStep("b", "B", complete = true)
        assertEquals(MonarchStepperStatus.Complete, MonarchStepperTokens.status(3, 1, finished))
        val failed = MonarchStepperStep("c", "C", error = true)
        assertEquals(MonarchStepperStatus.Error, MonarchStepperTokens.status(1, 1, failed))

        assertTrue(MonarchStepperTokens.selectable(0, 1, 1, plain, hasOnSelect = true))
        assertFalse(MonarchStepperTokens.selectable(0, 1, 1, plain, hasOnSelect = false))
        assertFalse(MonarchStepperTokens.selectable(1, 1, 1, plain, hasOnSelect = true))
        assertFalse(MonarchStepperTokens.selectable(2, 1, 1, plain, hasOnSelect = true))
        assertTrue(MonarchStepperTokens.selectable(2, 1, 2, plain, hasOnSelect = true))
        assertTrue(MonarchStepperTokens.selectable(3, 1, 1, MonarchStepperStep("b", "B", complete = true), true))
        assertFalse(MonarchStepperTokens.selectable(0, 1, 1, MonarchStepperStep("d", "D", enabled = false), true))

        val horizontal = MonarchStepperOrientation.Horizontal
        assertTrue(MonarchStepperTokens.showsCompact(MonarchStepperCompactMode.Automatic, horizontal, fits = false))
        assertFalse(MonarchStepperTokens.showsCompact(MonarchStepperCompactMode.Automatic, horizontal, fits = true))
        assertFalse(MonarchStepperTokens.showsCompact(MonarchStepperCompactMode.Automatic, Vertical, fits = false))
        assertTrue(MonarchStepperTokens.showsCompact(MonarchStepperCompactMode.Always, Vertical, fits = true))
        assertFalse(MonarchStepperTokens.showsCompact(Never, horizontal, fits = false))

        assertEquals(MonarchStepperTokens.Marker, MonarchStepperTokens.markerSize(0.85f))
        assertEquals(MonarchStepperTokens.Marker * 2f, MonarchStepperTokens.markerSize(2f))
    }

    @Test
    fun coloursFollowTheStateInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val marker = { status: MonarchStepperStatus -> MonarchStepperTokens.markerColors(status, colors) }
            assertEquals(colors.accentSolid, marker(MonarchStepperStatus.Complete).container, theme.id)
            assertEquals(colors.accentOnSolid, marker(MonarchStepperStatus.Complete).content, theme.id)
            assertEquals(colors.accentSolid, marker(MonarchStepperStatus.Current).border, theme.id)
            assertEquals(colors.accentStrong, marker(MonarchStepperStatus.Current).content, theme.id)
            assertEquals(colors.accentSubtle, marker(MonarchStepperStatus.Current).ring, theme.id)
            assertEquals(colors.dangerSubtle, marker(MonarchStepperStatus.Error).container, theme.id)
            assertEquals(colors.dangerStrong, marker(MonarchStepperStatus.Error).content, theme.id)
            assertEquals(colors.dangerBorder, marker(MonarchStepperStatus.Error).border, theme.id)
            assertEquals(colors.borderStrong, marker(MonarchStepperStatus.Upcoming).border, theme.id)
            assertEquals(colors.textMuted, marker(MonarchStepperStatus.Upcoming).content, theme.id)
            assertEquals(colors.bgSurface, marker(MonarchStepperStatus.Upcoming).container, theme.id)

            val label = { status: MonarchStepperStatus, disabled: Boolean ->
                MonarchStepperTokens.labelColor(status, disabled, colors)
            }
            assertEquals(colors.textDefault, label(MonarchStepperStatus.Current, false))
            assertEquals(colors.textDefault, label(MonarchStepperStatus.Complete, false))
            assertEquals(colors.textMuted, label(MonarchStepperStatus.Upcoming, false))
            assertEquals(colors.dangerStrong, label(MonarchStepperStatus.Error, false))
            assertEquals(colors.textDisabled, label(MonarchStepperStatus.Complete, true))

            assertEquals(colors.accentSolid, MonarchStepperTokens.lineColor(MonarchStepperStatus.Complete, colors))
            assertEquals(colors.borderDefault, MonarchStepperTokens.lineColor(MonarchStepperStatus.Error, colors))
            assertEquals(colors.borderDefault, MonarchStepperTokens.lineColor(MonarchStepperStatus.Current, colors))
            assertEquals(colors.textMuted, MonarchStepperTokens.secondaryColor(colors))
        }
    }
}
