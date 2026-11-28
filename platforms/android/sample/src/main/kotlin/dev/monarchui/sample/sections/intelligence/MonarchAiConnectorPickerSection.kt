package dev.monarchui.sample.sections.intelligence

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
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.intelligence.aiconnectorpicker.MonarchAiConnector
import dev.monarchui.components.intelligence.aiconnectorpicker.MonarchAiConnectorPicker
import dev.monarchui.components.intelligence.aiconnectorpicker.MonarchAiConnectorStatus
import dev.monarchui.components.intelligence.aiconnectorpicker.MonarchAiConnectorTool
import dev.monarchui.components.intelligence.aiconnectorpicker.MonarchAiConnectorToolPermission
import dev.monarchui.components.intelligence.aiconnectorpicker.MonarchAiConnectorToolRisk
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

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
