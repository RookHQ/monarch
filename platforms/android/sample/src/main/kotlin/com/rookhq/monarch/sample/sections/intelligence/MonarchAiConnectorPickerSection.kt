package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnector
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorPicker
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorStatus
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorTool
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorToolPermission
import com.rookhq.monarch.components.intelligence.aiconnectorpicker.MonarchAiConnectorToolRisk
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val StartingConnectors = listOf(
    MonarchAiConnector(
        id = "drive",
        name = "Drive",
        status = MonarchAiConnectorStatus.Connected,
        enabled = true,
        description = "Files in your drive and shared drives",
        account = "alex@example.com",
        tools = listOf(
            MonarchAiConnectorTool(
                "search",
                "Search files",
                MonarchAiConnectorToolRisk.Read,
                MonarchAiConnectorToolPermission.Allow,
            ),
            MonarchAiConnectorTool(
                "edit",
                "Edit files",
                MonarchAiConnectorToolRisk.Write,
                MonarchAiConnectorToolPermission.Ask,
            ),
            MonarchAiConnectorTool(
                "delete",
                "Delete files",
                MonarchAiConnectorToolRisk.Destructive,
                MonarchAiConnectorToolPermission.Ask,
            ),
        ),
    ),
    MonarchAiConnector("calendar", "Calendar", MonarchAiConnectorStatus.Connected, enabled = false),
    MonarchAiConnector(id = "mail", name = "Mail", status = MonarchAiConnectorStatus.NeedsSignIn, enabled = false),
    MonarchAiConnector(id = "crm", name = "CRM", status = MonarchAiConnectorStatus.Error, enabled = false),
)

/** The catalogue page of AiConnectorPicker. */
object MonarchAiConnectorPickerSection : CataloguePage {
    override val title = "AiConnectorPicker"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        // The connectors hold painters, which cannot be saved; the page keeps them while it is shown.
        var connectors by remember { mutableStateOf(StartingConnectors) }
        var note by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("In a sheet from the composer")
        MonarchButton(text = "Connectors", onClick = { open = true })
        note?.let { MonarchText(text = it) }
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }) {
                MonarchAiConnectorPicker(
                    connectors = connectors,
                    onConnectorsChange = { connectors = it },
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    onConnect = { id ->
                        connectors = connectors.map {
                            if (it.id == id) it.copy(status = MonarchAiConnectorStatus.Connected) else it
                        }
                    },
                    onManage = { note = "Settings of $it would open here." },
                    onDone = { dismiss() },
                )
            }
        }
    }
}
