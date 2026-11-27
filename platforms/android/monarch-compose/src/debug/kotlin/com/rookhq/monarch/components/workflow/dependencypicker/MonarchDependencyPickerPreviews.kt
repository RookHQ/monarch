package com.rookhq.monarch.components.workflow.dependencypicker

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusCategory
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusOption
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val PreviewDone = MonarchStatusOption("done", "Done", MonarchStatusCategory.Completed)

@MonarchPreviews
@Composable
internal fun MonarchDependencyPickerPreview() {
    PreviewFrame {
        MonarchDependencyPicker(
            value = listOf(
                MonarchDependencyLink(
                    id = "l1",
                    relation = MonarchDependencyRelation.BlockedBy,
                    item = MonarchDependencyItem("i1", "Fix the login redirect", key = "STR-12", status = PreviewDone),
                ),
            ),
            onAdd = { _, _ -> },
            onRemove = {},
        )
    }
}
