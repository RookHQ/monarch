package dev.monarchui.components.typography.prosestyle

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchProseStylePreview() {
    PreviewFrame {
        MonarchProse {
            heading("Release notes", level = 1)
            paragraph("Invoices now show the customer's tax number, and exports keep your column order.")
            heading("Fixed", level = 3)
            listItem("Search finds archived projects")
            listItem("Exports keep the time zone", depth = 1)
            listItem("Install the update", marker = "1.")
            quote(AnnotatedString("Faster than I expected."))
            code("./gradlew assembleRelease")
            rule()
            caption("Version 4.2, 12 January")
        }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchProseStyleSmallPreview() {
    PreviewFrame {
        MonarchProse(style = MonarchProseStyle(MonarchProseSize.Sm, limitLineLength = false)) {
            heading("Comment", level = 4)
            paragraph("Looks good to me. Ship it after the review.")
        }
    }
}
