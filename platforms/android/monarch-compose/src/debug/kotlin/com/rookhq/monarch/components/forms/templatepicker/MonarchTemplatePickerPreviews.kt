package com.rookhq.monarch.components.forms.templatepicker

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

/** Lazy lists need a bounded height inside the scrolling preview column. */
private const val PreviewHeightSteps = 4

@MonarchPreviews
@Composable
internal fun MonarchTemplatePickerPreview() {
    PreviewFrame {
        MonarchTemplatePicker(
            label = "Templates",
            templates = listOf(
                MonarchTemplatePickerItem("standup", "Daily stand-up", "Yesterday, today, blockers", "meetings"),
                MonarchTemplatePickerItem("retro", "Retrospective", category = "meetings", badge = "New"),
                MonarchTemplatePickerItem("roadmap", "Roadmap", "Quarters and themes", "planning", enabled = false),
            ),
            selected = "retro",
            onSelect = {},
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            categories = listOf(
                MonarchTemplatePickerCategory("meetings", "Meetings"),
                MonarchTemplatePickerCategory("planning", "Planning"),
            ),
            blank = MonarchTemplatePickerBlank(description = "Start with an empty page"),
        )
    }
}
