@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.identity.appaccesspicker.MonarchAppAccessOption
import dev.monarchui.components.identity.appaccesspicker.MonarchAppAccessPicker
import dev.monarchui.components.identity.appaccesspicker.MonarchAppAccessSummary
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val SampleApps = listOf(
    MonarchAppAccessOption("crm", "CRM", description = "Contacts, deals and pipelines"),
    MonarchAppAccessOption("books", "Books", description = "Invoices and expenses", badge = "Pro"),
    MonarchAppAccessOption("chat", "Chat", description = "Messages and calls"),
    MonarchAppAccessOption("id", "Rook ID", description = "Included for everyone", enabled = false),
)

/** The catalogue page of AppAccessPicker. */
object MonarchAppAccessPickerSection : CataloguePage {
    override val title = "AppAccessPicker"

    @Composable
    override fun Content() {
        var granted by rememberSaveable { mutableStateOf(listOf("crm", "id")) }
        CatalogueCaption("Apps to grant a member")
        MonarchAppAccessPicker(
            apps = SampleApps,
            value = granted,
            onValueChange = { granted = it },
            description = "Access changes apply the next time they sign in.",
        )
        CatalogueCaption("The summary in a member row")
        MonarchAppAccessSummary(apps = SampleApps, value = granted)
        CatalogueCaption("Loading and empty")
        MonarchAppAccessPicker(apps = emptyList(), value = emptyList(), onValueChange = {}, loading = true)
        MonarchAppAccessPicker(apps = emptyList(), value = emptyList(), onValueChange = {}, hideLabel = true)
    }
}
