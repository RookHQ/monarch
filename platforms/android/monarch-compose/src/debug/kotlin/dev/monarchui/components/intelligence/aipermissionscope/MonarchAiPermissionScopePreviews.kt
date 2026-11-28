package dev.monarchui.components.intelligence.aipermissionscope

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiPermissionScopePreview() {
    PreviewFrame {
        MonarchAiPermissionScope(
            scopes = listOf(
                MonarchAiPermissionScopeItem("Calendar", MonarchAiAccessLevel.Read),
                MonarchAiPermissionScopeItem("Drive", MonarchAiAccessLevel.Write, resources = "Shared projects only"),
                MonarchAiPermissionScopeItem("Mail", MonarchAiAccessLevel.None),
            ),
            onManage = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAiPermissionScopeEditablePreview() {
    PreviewFrame {
        MonarchAiPermissionScope(
            scopes = listOf(MonarchAiPermissionScopeItem("Calendar", MonarchAiAccessLevel.Read)),
            onScopesChange = {},
        )
    }
}
