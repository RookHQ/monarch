package com.rookhq.monarch.components.datagrid.mergedialog

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequence
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchMergeDialogPreview() {
    PreviewFrame {
        MonarchMergeDialog(
            records = listOf(
                MonarchMergeRecord("a", "Acme Ltd", "Created 3 Mar 2025", mapOf("email" to "hello@acme.com")),
                MonarchMergeRecord(
                    "b",
                    "ACME Limited",
                    values = mapOf("email" to "info@acme.com", "phone" to "+44 20 7946 0000", "city" to "London"),
                ),
            ),
            fields = listOf(
                MonarchMergeField("email", "Email"),
                MonarchMergeField("phone", "Phone"),
                MonarchMergeField("city", "City"),
            ),
            onMerge = {},
            consequences = listOf(MonarchConsequence(MonarchConsequenceKind.Changed, "invoices move", count = 12)),
            onCancel = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchMergeDialogBlockedPreview() {
    PreviewFrame {
        MonarchMergeDialog(
            records = listOf(
                MonarchMergeRecord("a", "Ada Lovelace", values = mapOf("email" to "ada@example.com")),
                MonarchMergeRecord("b", "A. Lovelace", values = mapOf("email" to "ada@work.example")),
            ),
            fields = listOf(MonarchMergeField("email", "Email")),
            onMerge = {},
            blockers = listOf("A. Lovelace has an open contract."),
            failed = true,
            onCancel = {},
        )
    }
}
