package com.rookhq.monarch.components.identity.securityalertcard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSecurityAlertCardPreview() {
    val now = System.currentTimeMillis()
    PreviewFrame {
        MonarchSecurityAlertCard(
            alerts = listOf(
                MonarchSecurityAlert(
                    id = "1",
                    title = "New sign-in on a Pixel 9",
                    description = "Lisbon, Portugal",
                    severity = MonarchSecurityAlertSeverity.High,
                    dateMillis = now,
                    actionLabel = "Review sessions",
                ),
                MonarchSecurityAlert(id = "2", title = "Password changed", read = true),
            ),
            now = now,
            onAction = {},
            onMarkRead = {},
            onDismiss = {},
        )
        MonarchSecurityAlertCard(alerts = emptyList())
    }
}
