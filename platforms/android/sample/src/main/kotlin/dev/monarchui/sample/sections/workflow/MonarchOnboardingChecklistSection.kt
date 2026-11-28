package dev.monarchui.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.workflow.onboardingchecklist.MonarchOnboardingChecklist
import dev.monarchui.components.workflow.onboardingchecklist.MonarchOnboardingStep
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of OnboardingChecklist. */
object MonarchOnboardingChecklistSection : CataloguePage {
    override val title = "OnboardingChecklist"

    @Composable
    override fun Content() {
        var invited by rememberSaveable { mutableStateOf(false) }
        var connected by rememberSaveable { mutableStateOf(false) }
        var dismissed by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Steps done by their actions")
        if (dismissed) {
            MonarchButton(text = "Show the checklist again", onClick = { dismissed = false })
        } else {
            MonarchOnboardingChecklist(
                title = "Get started",
                steps = listOf(
                    MonarchOnboardingStep(id = "profile", title = "Complete your profile", done = true),
                    MonarchOnboardingStep(
                        id = "invite",
                        title = "Invite your team",
                        description = "Work together on shared projects.",
                        done = invited,
                    ),
                    MonarchOnboardingStep(id = "calendar", title = "Connect a calendar", done = connected),
                ),
                description = "A few steps to set up your workspace.",
                onDismiss = { dismissed = true },
                expandIcon = { MonarchIcon(imageVector = SampleIcons.ChevronRight, contentDescription = null) },
                doneIcon = {
                    MonarchIcon(imageVector = SampleIcons.Check, contentDescription = null, size = MonarchIconSize.Sm)
                },
            ) { step ->
                MonarchButton(
                    text = if (step.id == "invite") "Invite" else "Connect",
                    onClick = { if (step.id == "invite") invited = true else connected = true },
                    size = MonarchButtonSize.Sm,
                )
            }
        }
        CatalogueCaption("Complete, not collapsible")
        MonarchOnboardingChecklist(
            title = "Set up billing",
            steps = listOf(
                MonarchOnboardingStep(id = "card", title = "Add a payment method", done = true),
                MonarchOnboardingStep(id = "address", title = "Add a billing address", done = true),
            ),
            collapsible = false,
        )
    }
}
