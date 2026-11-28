@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.identity.appaccesspicker.MonarchAppAccessOption
import com.rookhq.monarch.components.identity.appaccesspicker.MonarchAppAccessPicker
import com.rookhq.monarch.components.identity.appaccesspicker.MonarchAppAccessSummary
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

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
