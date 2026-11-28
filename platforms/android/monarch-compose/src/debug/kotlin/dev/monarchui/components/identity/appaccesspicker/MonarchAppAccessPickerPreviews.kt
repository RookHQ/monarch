@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.identity.appaccesspicker

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewApps = listOf(
    MonarchAppAccessOption("crm", "CRM", description = "Contacts, deals and pipelines"),
    MonarchAppAccessOption("books", "Books", description = "Invoices and expenses", badge = "Pro"),
    MonarchAppAccessOption("id", "Rook ID", description = "Included for everyone", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchAppAccessPickerPreview() {
    PreviewFrame {
        MonarchAppAccessPicker(
            apps = PreviewApps,
            value = listOf("crm", "id"),
            onValueChange = {},
            description = "Access changes apply at the next sign-in.",
        )
        MonarchAppAccessSummary(apps = PreviewApps, value = listOf("crm", "books", "id"))
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAppAccessPickerStatesPreview() {
    PreviewFrame {
        MonarchAppAccessPicker(apps = emptyList(), value = emptyList(), onValueChange = {}, loading = true)
        MonarchAppAccessPicker(apps = emptyList(), value = emptyList(), onValueChange = {})
        MonarchAppAccessSummary(apps = PreviewApps, value = emptyList())
    }
}
