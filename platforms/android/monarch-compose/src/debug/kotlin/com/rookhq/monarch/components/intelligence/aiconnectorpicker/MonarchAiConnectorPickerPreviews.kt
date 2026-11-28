package com.rookhq.monarch.components.intelligence.aiconnectorpicker

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
