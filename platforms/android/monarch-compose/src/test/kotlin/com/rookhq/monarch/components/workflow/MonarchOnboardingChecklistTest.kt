package com.rookhq.monarch.components.workflow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.workflow.onboardingchecklist.MonarchOnboardingChecklist
import com.rookhq.monarch.components.workflow.onboardingchecklist.MonarchOnboardingChecklistLabels
import com.rookhq.monarch.components.workflow.onboardingchecklist.MonarchOnboardingChecklistTokens
import com.rookhq.monarch.components.workflow.onboardingchecklist.MonarchOnboardingStep
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
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
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val twoSteps = listOf(
    MonarchOnboardingStep(id = "profile", title = "Complete your profile", done = true),
    MonarchOnboardingStep(id = "invite", title = "Invite your team", description = "Work together."),
)

internal object MonarchOnboardingChecklistFixtures : ComponentFixtures {
    override val id = "onboarding-checklist"
    override val fixtures = listOf(
        fixture("in progress with an action") {
            MonarchOnboardingChecklist(title = "Get started", steps = twoSteps) {
                MonarchButton(text = "Invite", onClick = {}, size = MonarchButtonSize.Sm)
            }
        },
        fixture("collapsed with a description and dismiss") {
            MonarchOnboardingChecklist(
                title = "Get started",
                steps = twoSteps,
                description = "A few steps to set up your workspace.",
                expanded = false,
                onDismiss = {},
            )
        },
        fixture("complete, not collapsible, with icons") {
            MonarchOnboardingChecklist(
                title = "Set up billing",
                steps = listOf(MonarchOnboardingStep(id = "card", title = "Add a card", done = true)),
                collapsible = false,
                onDismiss = {},
                dismissIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                doneIcon = {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
                },
            )
        },
        fixture("empty") {
            MonarchOnboardingChecklist(title = "Get started", steps = emptyList())
        },
        fixture("long text") {
            MonarchOnboardingChecklist(
                title = "Finish setting up the shared workspace for your whole organization",
                steps = listOf(
                    MonarchOnboardingStep(id = "a", title = "Connect the calendar your team uses for every meeting"),
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchOnboardingChecklistTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun theTitleIsAHeadingAndTheProgressIsSpoken() {
        rule.setMonarchContent { MonarchOnboardingChecklist(title = "Get started", steps = twoSteps) }
        rule.onNodeWithText("Get started").assert(isHeading)
        val bar = rule.onNodeWithContentDescription("Setup progress")
        assertEquals("1 of 2 done", bar.stateDescription())
        bar.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        assertEquals(0.5f, bar.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current)
    }

    @Test
    fun theProgressRowShowsAndHidesTheSteps() {
        rule.setMonarchContent { MonarchOnboardingChecklist(title = "Get started", steps = twoSteps) }
        val row = rule.onNodeWithText("1 of 2 done")
        row.assert(isButton)
        assertEquals("Expanded", row.stateDescription())
        rule.onNodeWithText("Invite your team").assertExists()
        row.performClick()
        rule.waitForIdle()
        assertEquals("Collapsed", row.stateDescription())
        rule.onNodeWithText("Invite your team").assertDoesNotExist()
    }

    @Test
    fun aHeldExpandedStateOnlyChangesThroughTheCallback() {
        val changes = mutableListOf<Boolean>()
        var held by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchOnboardingChecklist(
                title = "Get started",
                steps = twoSteps,
                expanded = held,
                onExpandedChange = { changes += it },
            )
        }
        rule.onNodeWithText("Invite your team").assertDoesNotExist()
        rule.onNodeWithText("1 of 2 done").performClick()
        assertEquals(listOf(true), changes)
        rule.onNodeWithText("Invite your team").assertDoesNotExist()
        held = true
        rule.waitForIdle()
        rule.onNodeWithText("Invite your team").assertExists()
    }

    @Test
    fun eachStepIsOneElementWithItsState() {
        val steps = twoSteps + MonarchOnboardingStep(id = "calendar", title = "Connect a calendar")
        rule.setMonarchContent { MonarchOnboardingChecklist(title = "Get started", steps = steps) }
        assertEquals("Done", rule.onNodeWithText("Complete your profile").stateDescription())
        val current = rule.onNodeWithText("Invite your team")
        assertEquals("Not done, Current step", current.stateDescription())
        // The description is read with the step: one element.
        current.assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(
            androidx.compose.ui.text.AnnotatedString("Invite your team"),
            androidx.compose.ui.text.AnnotatedString("Work together."),
        )))
        assertEquals("Not done", rule.onNodeWithText("Connect a calendar").stateDescription())
        // The step numbers are not read.
        rule.onNodeWithText("2").assertDoesNotExist()
    }

    @Test
    fun actionsShowOnlyForOpenStepsAndAreSeparateElements() {
        val clicked = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchOnboardingChecklist(title = "Get started", steps = twoSteps) { step ->
                MonarchButton(text = "Do ${step.id}", onClick = { clicked += step.id }, size = MonarchButtonSize.Sm)
            }
        }
        rule.onNodeWithText("Do profile").assertDoesNotExist()
        rule.onNodeWithText("Do invite").assert(isButton).performClick()
        assertEquals(listOf("invite"), clicked)
    }

    @Test
    fun everyStepDoneShowsTheCompleteMessage() {
        val steps = listOf(
            MonarchOnboardingStep(id = "a", title = "Verify your email", done = true),
            MonarchOnboardingStep(
                id = "b",
                title = "Add a payment method",
                description = "Hidden once done",
                done = true,
            ),
        )
        rule.setMonarchContent { MonarchOnboardingChecklist(title = "Get started", steps = steps) }
        rule.onNodeWithText("All done. You are set up.").assertExists()
        rule.onNodeWithText("2 of 2 done").assertExists()
        rule.onNodeWithText("Hidden once done", substring = true).assertDoesNotExist()
    }

    @Test
    fun anEmptyListIsNeverComplete() {
        rule.setMonarchContent { MonarchOnboardingChecklist(title = "Get started", steps = emptyList()) }
        rule.onNodeWithText("0 of 0 done").assertExists()
        rule.onNodeWithText("All done. You are set up.").assertDoesNotExist()
        assertTrue(!MonarchOnboardingChecklistTokens.complete(emptyList()))
        assertEquals(0f, MonarchOnboardingChecklistTokens.fraction(0, 0))
        assertEquals(-1, MonarchOnboardingChecklistTokens.current(emptyList()))
        assertEquals(1, MonarchOnboardingChecklistTokens.current(twoSteps))
    }

    @Test
    fun theDismissButtonCallsBackWithOrWithoutAnIcon() {
        var dismissed = 0
        rule.setMonarchContent {
            androidx.compose.foundation.layout.Column {
                MonarchOnboardingChecklist(
                    title = "A",
                    steps = emptyList(),
                    collapsible = false,
                    onDismiss = { dismissed++ },
                    dismissIcon = null,
                )
                MonarchOnboardingChecklist(
                    title = "B",
                    steps = emptyList(),
                    collapsible = false,
                    onDismiss = { dismissed += 10 },
                    dismissIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                    labels = MonarchOnboardingChecklistLabels(dismiss = "Close setup"),
                )
            }
        }
        rule.onNodeWithText("Dismiss checklist").assert(isButton)
            .assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Close setup").assert(isButton)
            .assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(11, dismissed)
    }

    @Test
    fun withoutCollapsingTheStepsAlwaysShowAndTheProgressIsReadOnce() {
        rule.setMonarchContent {
            MonarchOnboardingChecklist(title = "Get started", steps = twoSteps, collapsible = false)
        }
        rule.onNodeWithText("Invite your team").assertExists()
        rule.onNodeWithText("1 of 2 done").assertDoesNotExist()
        assertEquals("1 of 2 done", rule.onNodeWithContentDescription("Setup progress").stateDescription())
    }

    @Test
    fun theProgressRowHasA48dpTarget() {
        rule.setMonarchContent { MonarchOnboardingChecklist(title = "Get started", steps = twoSteps) }
        rule.onNodeWithText("1 of 2 done").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        val steps = twoSteps + MonarchOnboardingStep(id = "c", title = "Third")
        rule.setMonarchContent {
            MonarchOnboardingChecklist(
                title = "Premiers pas",
                steps = steps,
                labels = MonarchOnboardingChecklistLabels(
                    progress = { done, total -> "$done sur $total" },
                    progressLabel = "Progression",
                    done = "Fait",
                    notDone = "À faire",
                    current = "Étape en cours",
                    expanded = "Ouvert",
                ),
            )
        }
        assertEquals("1 sur 3", rule.onNodeWithContentDescription("Progression").stateDescription())
        assertEquals("Ouvert", rule.onNodeWithText("1 sur 3").stateDescription())
        assertEquals("Fait", rule.onNodeWithText("Complete your profile").stateDescription())
        assertEquals("À faire, Étape en cours", rule.onNodeWithText("Invite your team").stateDescription())
        assertEquals("À faire", rule.onNodeWithText("Third").stateDescription())
    }

    @Test
    fun theCompleteMessageCanBeReplaced() {
        rule.setMonarchContent {
            MonarchOnboardingChecklist(
                title = "Get started",
                steps = listOf(MonarchOnboardingStep(id = "a", title = "A", done = true)),
                completeMessage = "Ready to go",
            )
        }
        rule.onNodeWithText("Ready to go").assertExists()
    }

    @Test
    fun theLayoutMirrorsInRightToLeft() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchOnboardingChecklist(title = "Get started", steps = twoSteps, onDismiss = {}, dismissIcon = null) {
                MonarchButton(text = "Invite", onClick = {}, size = MonarchButtonSize.Sm)
            }
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Get started").assertIsBefore(rule.onNodeWithText("Dismiss checklist"), direction)
            // The action sits at the end of the step row, after the text, as on the web.
            val title = rule.onNodeWithText("Invite your team", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val action = rule.onNodeWithText("Invite").getUnclippedBoundsInRoot()
            val atTheEnd = when (direction) {
                LayoutDirection.Ltr -> action.left > title.left
                LayoutDirection.Rtl -> action.right < title.right
            }
            assertTrue(atTheEnd, "The action comes after the step text in $direction")
        }
    }

    @Test
    fun longTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchOnboardingChecklist(
                title = "Finish setting up the shared workspace",
                steps = listOf(MonarchOnboardingStep(id = "a", title = "Connect the calendar your team uses")),
            )
        }
        rule.onNodeWithText("Finish setting up the shared workspace").assertTextFullyVisible()
        rule.onNodeWithText("Connect the calendar your team uses", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("0 of 1 done", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchOnboardingChecklistTokens
            val done = t.marker(done = true, current = false, colors)
            assertEquals(colors.successSolid, done.container, theme.id)
            assertEquals(colors.successOnSolid, done.content, theme.id)
            val current = t.marker(done = false, current = true, colors)
            assertEquals(colors.accentStrong, current.border, theme.id)
            assertEquals(colors.accentStrong, current.content, theme.id)
            val open = t.marker(done = false, current = false, colors)
            assertEquals(colors.borderStrong, open.border, theme.id)
            assertEquals(colors.textMuted, open.content, theme.id)
            assertEquals(Color.Transparent, open.container, theme.id)
            assertEquals(colors.bgSubtle, t.stepBackground(true, colors), theme.id)
            assertEquals(Color.Transparent, t.stepBackground(false, colors), theme.id)
            assertEquals(colors.textMuted, t.stepTitle(true, colors), theme.id)
            assertEquals(colors.textDefault, t.stepTitle(false, colors), theme.id)
            assertEquals(colors.successSubtle, t.completeColors(colors).container, theme.id)
            assertEquals(colors.successStrong, t.completeColors(colors).content, theme.id)
        }
        assertEquals(0.5f, MonarchOnboardingChecklistTokens.fraction(1, 2))
    }
}
