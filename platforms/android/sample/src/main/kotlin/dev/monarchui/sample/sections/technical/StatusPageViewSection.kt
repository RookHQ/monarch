package dev.monarchui.sample.sections.technical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBar
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarSegment
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarSize
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.feedback.calloutbanner.MonarchCalloutBanner
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.technical.incidenttimeline.MonarchIncidentStage
import dev.monarchui.components.technical.incidenttimeline.MonarchIncidentTimeline
import dev.monarchui.components.technical.incidenttimeline.MonarchIncidentUpdate
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val IncidentStart = 1_794_650_400_000L
private const val QuarterHour = 900_000L
private const val Days = 30
private const val SlowDay = 12
private const val SlowUptime = 99.2
private const val FullUptime = 100.0

/** The catalogue page of StatusPageView: overall state, services with history, an incident and subscribe. */
object StatusPageViewSection : CataloguePage {
    override val title = "StatusPageView"

    private class Service(val name: String, val state: MonarchStatusDefinition, val slow: Boolean)

    private val operational = MonarchStatusDefinition(label = "Operational", tone = MonarchTone.Success)
    private val degraded = MonarchStatusDefinition(label = "Degraded", tone = MonarchTone.Warning)

    private val services = listOf(
        Service("Website", operational, slow = false),
        Service("API", degraded, slow = true),
    )

    private fun history(slow: Boolean) = (1..Days).map { day ->
        MonarchUptimeBarSegment(
            id = "d$day",
            label = "Day $day",
            uptime = if (slow && day == SlowDay) SlowUptime else FullUptime,
        )
    }

    @Composable
    override fun Content() {
        var email by rememberSaveable { mutableStateOf("") }
        var subscribed by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Status as an in-app screen")
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            MonarchCalloutBanner(
                message = "Some API requests are slower than usual.",
                tone = MonarchTone.Warning,
                title = "Partial degradation",
            )
            MonarchText(text = "Services", style = MonarchTextStyle.Heading)
            services.forEach { service ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonarchText(
                        text = service.name,
                        modifier = Modifier.weight(1f),
                        style = MonarchTextStyle.BodyStrong,
                    )
                    MonarchStatusBadge(definition = service.state)
                }
                MonarchUptimeBar(
                    segments = history(service.slow),
                    startLabel = "30 days ago",
                    endLabel = "Today",
                    size = MonarchUptimeBarSize.Sm,
                )
            }
            MonarchText(text = "Active incident", style = MonarchTextStyle.Heading)
            MonarchIncidentTimeline(
                updates = listOf(
                    MonarchIncidentUpdate(
                        id = "1",
                        stage = MonarchIncidentStage.Investigating,
                        body = "API responses are slow in one region.",
                        time = IncidentStart,
                    ),
                    MonarchIncidentUpdate(
                        id = "2",
                        stage = MonarchIncidentStage.Monitoring,
                        body = "Traffic moved to a healthy region. We are watching it.",
                        time = IncidentStart + QuarterHour,
                    ),
                ),
                live = true,
            )
            MonarchText(text = "Get updates", style = MonarchTextStyle.Heading)
            if (subscribed) {
                MonarchText(text = "Check your inbox to confirm.", color = MonarchTheme.colors.textMuted)
            } else {
                MonarchTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                MonarchButton(
                    text = "Subscribe",
                    onClick = { subscribed = true },
                    variant = MonarchButtonVariant.Primary,
                    enabled = "@" in email,
                )
            }
        }
    }
}
