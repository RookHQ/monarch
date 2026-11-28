package dev.monarchui.components.intelligence.aiconnectorpicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiConnectorPickerPreview() {
    PreviewFrame {
        MonarchAiConnectorPicker(
            connectors = listOf(
                MonarchAiConnector(
                    id = "drive",
                    name = "Drive",
                    status = MonarchAiConnectorStatus.Connected,
                    enabled = true,
                    account = "alex@example.com",
                    tools = listOf(
                        MonarchAiConnectorTool(
                            "search",
                            "Search files",
                            MonarchAiConnectorToolRisk.Read,
                            MonarchAiConnectorToolPermission.Allow,
                        ),
                    ),
                ),
                MonarchAiConnector(
                    id = "mail",
                    name = "Mail",
                    status = MonarchAiConnectorStatus.NeedsSignIn,
                    enabled = false,
                ),
            ),
            onConnectorsChange = {},
            onConnect = {},
            onDone = {},
        )
    }
}
