package com.rookhq.monarch.components.intelligence.aipermissionscope

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
