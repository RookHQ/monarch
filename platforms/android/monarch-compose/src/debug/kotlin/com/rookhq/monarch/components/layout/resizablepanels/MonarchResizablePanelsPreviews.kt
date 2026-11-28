package com.rookhq.monarch.components.layout.resizablepanels

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchResizablePanelsPreview() {
    PreviewFrame {
        var fraction by remember { mutableFloatStateOf(0.4f) }
        MonarchResizablePanels(
            fraction = fraction,
            onFractionChange = { fraction = it },
            modifier = Modifier.height(120.dp),
            showGrip = true,
            stacksWhenCompact = false,
            first = { MonarchText(text = "Editor") },
            second = { MonarchText(text = "Preview") },
        )
        MonarchResizablePanels(
            fraction = 0.5f,
            onFractionChange = {},
            modifier = Modifier.height(160.dp),
            axis = MonarchResizablePanelsAxis.Vertical,
            first = { MonarchText(text = "Query") },
            second = { MonarchText(text = "Results") },
        )
    }
}
