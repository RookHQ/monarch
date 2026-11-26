package com.rookhq.monarch.components.intelligence.aionboardingtour

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewSteps = listOf(
    MonarchAiOnboardingTourStep("What the assistant can see", "The page you are on and files you attach."),
    MonarchAiOnboardingTourStep("What it can do", "Draft replies and summarise threads. You review first."),
    MonarchAiOnboardingTourStep("How to turn it off", "Settings, then AI. Your data stays yours."),
)

@MonarchPreviews
@Composable
internal fun MonarchAiOnboardingTourPreview() {
    PreviewFrame {
        MonarchAiOnboardingTour(steps = PreviewSteps, onDisable = {}, onFinish = {}, onSkip = {})
        MonarchAiOnboardingTour(steps = PreviewSteps, step = 2, onStepChange = {}, onDisable = {}, onFinish = {})
    }
}
