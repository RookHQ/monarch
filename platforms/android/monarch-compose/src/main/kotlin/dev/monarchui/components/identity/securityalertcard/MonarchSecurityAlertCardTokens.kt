package dev.monarchui.components.identity.securityalertcard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.datadisplay.badge.MonarchBadgeVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure logic of MonarchSecurityAlertCard. */
internal object MonarchSecurityAlertCardTokens {
    /** The gap between the parts of the card. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of an alert. */
    val RowGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the lines and the buttons of an alert. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The side of the tile behind the bell in the header. */
    val BellTile: Dp = MonarchTokens.Space.space8.dp

    /** The size of the unread mark. */
    val UnreadDotSize: Dp = MonarchTokens.Space.space2.dp

    /** The padding above the unread mark, to sit on the first line. */
    val UnreadDotTop: Dp = MonarchTokens.Space.space1_5.dp

    /** The tone of a severity's badge. */
    fun tone(severity: MonarchSecurityAlertSeverity): MonarchTone = when (severity) {
        MonarchSecurityAlertSeverity.Info -> MonarchTone.Info
        MonarchSecurityAlertSeverity.Low -> MonarchTone.Neutral
        MonarchSecurityAlertSeverity.Medium -> MonarchTone.Warning
        MonarchSecurityAlertSeverity.High, MonarchSecurityAlertSeverity.Critical -> MonarchTone.Danger
    }

    /** The badge variant: critical is solid, so it stands out from high. */
    fun badgeVariant(severity: MonarchSecurityAlertSeverity): MonarchBadgeVariant =
        if (severity == MonarchSecurityAlertSeverity.Critical) MonarchBadgeVariant.Solid else MonarchBadgeVariant.Subtle

    /** The colour of a severity's icon. */
    fun iconColor(severity: MonarchSecurityAlertSeverity, colors: MonarchColors): Color = when (severity) {
        MonarchSecurityAlertSeverity.Info -> colors.infoStrong
        MonarchSecurityAlertSeverity.Low -> colors.textMuted
        MonarchSecurityAlertSeverity.Medium -> colors.warningStrong
        MonarchSecurityAlertSeverity.High, MonarchSecurityAlertSeverity.Critical -> colors.dangerStrong
    }

    /** The fill of the round tile behind a severity's icon. */
    fun iconTile(severity: MonarchSecurityAlertSeverity, colors: MonarchColors): Color = when (severity) {
        MonarchSecurityAlertSeverity.Info -> colors.infoSubtle
        MonarchSecurityAlertSeverity.Low -> colors.bgMuted
        MonarchSecurityAlertSeverity.Medium -> colors.warningSubtle
        MonarchSecurityAlertSeverity.High, MonarchSecurityAlertSeverity.Critical -> colors.dangerSubtle
    }

    /** The side of the round tile behind a severity's icon. */
    val IconTile: Dp = MonarchTokens.Space.space8.dp

    /** The colour of the unread mark: the accent, as the web's accent tone; none once read. */
    fun unreadColor(read: Boolean, colors: MonarchColors): Color = if (read) Color.Transparent else colors.accentSolid

    /** The built-in name of a severity. */
    fun severityName(severity: MonarchSecurityAlertSeverity): Int = when (severity) {
        MonarchSecurityAlertSeverity.Info -> R.string.monarch_securityalertcard_severity_info
        MonarchSecurityAlertSeverity.Low -> R.string.monarch_securityalertcard_severity_low
        MonarchSecurityAlertSeverity.Medium -> R.string.monarch_securityalertcard_severity_medium
        MonarchSecurityAlertSeverity.High -> R.string.monarch_securityalertcard_severity_high
        MonarchSecurityAlertSeverity.Critical -> R.string.monarch_securityalertcard_severity_critical
    }

    /** The number of unread alerts. */
    fun unreadCount(alerts: List<MonarchSecurityAlert>): Int = alerts.count { !it.read }

    /** [date] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(date: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(date, now, locale)

    /** What TalkBack reads as the state of an alert: "Unread, High, 5 minutes ago". */
    fun spokenState(unread: String?, severity: String, time: String?): String =
        listOfNotNull(unread, severity, time).joinToString(", ")

    /** What TalkBack reads as the name of an alert: the title, then the description. */
    fun spokenName(title: String, description: String?): String =
        if (description.isNullOrBlank()) title else "$title, $description"
}
