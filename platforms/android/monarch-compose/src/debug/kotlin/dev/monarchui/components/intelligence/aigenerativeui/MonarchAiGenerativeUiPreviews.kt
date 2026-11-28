package dev.monarchui.components.intelligence.aigenerativeui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiUiPart
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiGenerativeUiPreview() {
    val registry = remember {
        MonarchAiUiRegistry().apply {
            register("City", { it["city"]?.stringValue }) { city, _ -> Text(city) }
        }
    }
    PreviewFrame {
        MonarchAiGenerativeUi(
            MonarchAiUiPart("City", MonarchAiJSON.Object(mapOf("city" to MonarchAiJSON.Text("Lisbon")))),
            registry,
        )
        MonarchAiGenerativeUi(MonarchAiUiPart("Script", MonarchAiJSON.Object(emptyMap())), registry)
        MonarchAiGenerativeUi(MonarchAiUiPart("City", MonarchAiJSON.Object(emptyMap())), registry)
    }
}
