package dev.monarchui.components.intelligence.aimodelpicker

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewModels = listOf(
    MonarchAiModelOption(
        id = "large",
        provider = "Rook",
        name = "Rook Large",
        description = "Best for long documents",
        capabilities = listOf(MonarchAiModelCapability.Vision, MonarchAiModelCapability.Tools),
        contextTokens = 200_000,
        speed = MonarchAiModelSpeed.Balanced,
        costTier = 3,
    ),
    MonarchAiModelOption(id = "hosted", provider = "Workspace", name = "Hosted model", requiresKey = true),
    MonarchAiModelOption(id = "old", provider = "Workspace", name = "Legacy", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchAiModelPickerPreview() {
    PreviewFrame {
        MonarchAiModelPicker(selection = "large", models = PreviewModels, onSelectionChange = {})
        MonarchAiModelPicker(
            selection = null,
            models = PreviewModels,
            onSelectionChange = {},
            size = MonarchButtonSize.Sm,
        )
        MonarchAiModelPicker(selection = null, models = PreviewModels, onSelectionChange = {}, enabled = false)
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiModelPickerListPreview() {
    PreviewFrame {
        MonarchAiModelPickerList(
            selection = "large",
            models = PreviewModels,
            canConfigure = true,
            labels = MonarchAiModelPickerLabels(),
            onChoose = { _, _ -> },
        )
    }
}
