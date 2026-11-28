package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aionboardingtour.MonarchAiOnboardingTour
import com.rookhq.monarch.components.intelligence.aionboardingtour.MonarchAiOnboardingTourStep
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val TourSteps = listOf(
    MonarchAiOnboardingTourStep(
        "What the assistant can see",
        "The page you are on and the files you attach. Nothing else.",
    ),
    MonarchAiOnboardingTourStep(
        "What it can do",
        "Draft replies and summarise threads. You review before anything is sent.",
    ),
    MonarchAiOnboardingTourStep("How to turn it off", "Turn off AI here, or later in Settings, then AI."),
)

/** The catalogue page of AiOnboardingTour. */
object MonarchAiOnboardingTourSection : CataloguePage {
    override val title = "AiOnboardingTour"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var step by rememberSaveable { mutableIntStateOf(0) }
        var open by rememberSaveable { mutableStateOf(false) }
        var outcome by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Inline, with the step kept by the page")
        MonarchAiOnboardingTour(
            steps = TourSteps,
            step = step,
            onStepChange = { step = it },
            onDisable = { outcome = "AI turned off" },
            onFinish = { outcome = "Tour finished" },
            onSkip = { outcome = "Tour skipped" },
        )
        if (outcome.isNotEmpty()) MonarchText(text = outcome)
        CatalogueCaption("In a bottom sheet")
        MonarchButton(text = "Show the tour", onClick = { open = true })
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }) {
                MonarchAiOnboardingTour(
                    steps = TourSteps,
                    onDisable = { dismiss() },
                    onFinish = { dismiss() },
                    onSkip = { dismiss() },
                )
            }
        }
    }
}
