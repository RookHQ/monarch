package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlert
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlertCard
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlertCardState
import com.rookhq.monarch.components.identity.securityalertcard.MonarchSecurityAlertSeverity
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val MinuteMillis = 60_000L
private const val HourMillis = 60 * MinuteMillis

private fun sampleAlerts(now: Long) = listOf(
    MonarchSecurityAlert(
        id = "signin",
        title = "New sign-in on a Pixel 9",
        description = "Lisbon, Portugal",
        severity = MonarchSecurityAlertSeverity.High,
        dateMillis = now - 5 * MinuteMillis,
        actionLabel = "Review sessions",
    ),
    MonarchSecurityAlert(
        id = "codes",
        title = "Recovery code used",
        severity = MonarchSecurityAlertSeverity.Critical,
        dateMillis = now - 2 * HourMillis,
    ),
    MonarchSecurityAlert(
        id = "password",
        title = "Password changed",
        severity = MonarchSecurityAlertSeverity.Info,
        dateMillis = now - 26 * HourMillis,
        read = true,
    ),
)

/** The catalogue page of SecurityAlertCard. */
object MonarchSecurityAlertCardSection : CataloguePage {
    override val title = "SecurityAlertCard"

    @Composable
    override fun Content() {
        val now = System.currentTimeMillis()
        var dismissed by rememberSaveable { mutableStateOf(listOf<String>()) }
        var read by rememberSaveable { mutableStateOf(listOf<String>()) }
        val alerts = sampleAlerts(now)
            .filterNot { it.id in dismissed }
            .map { if (it.id in read) it.copy(read = true) else it }
        CatalogueCaption("Mark as read and dismiss change the list")
        MonarchSecurityAlertCard(
            alerts = alerts,
            onAction = {},
            onMarkRead = { id -> read = read + id },
            onMarkAllRead = { read = alerts.map { it.id } },
            onDismiss = { id -> dismissed = dismissed + id },
        )
        MonarchButton(
            text = "Reset",
            onClick = {
                dismissed = emptyList()
                read = emptyList()
            },
            size = MonarchButtonSize.Sm,
        )
        CatalogueCaption("Loading")
        MonarchSecurityAlertCard(alerts = emptyList(), state = MonarchSecurityAlertCardState.Loading)
        CatalogueCaption("Failed")
        MonarchSecurityAlertCard(alerts = emptyList(), state = MonarchSecurityAlertCardState.Failed, onRetry = {})
    }
}
