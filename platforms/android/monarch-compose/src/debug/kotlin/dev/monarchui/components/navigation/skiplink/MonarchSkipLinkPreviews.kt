package dev.monarchui.components.navigation.skiplink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSkipLinkPreview() {
    val main = remember { FocusRequester() }
    PreviewFrame {
        MonarchSkipLink(target = main)
        MonarchText(text = "Main content starts here.", modifier = Modifier.monarchSkipLinkTarget(main))
    }
}
