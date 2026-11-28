package dev.monarchui.components.datadisplay.provenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchSurface
import dev.monarchui.internal.rememberMonarchRelativeClock
import dev.monarchui.theme.MonarchTheme

/**
 * Where a value came from and how fresh it is: "Registry · 2 hours ago".
 *
 * Built by Monarch from a `Row` of texts: Material 3 has no equivalent. Put it
 * next to any value the product did not get from the user. TalkBack reads it
 * as one element, "Source: Registry, checked 2 hours ago"; the icon and the
 * dot are hidden. The relative time is redrawn every minute. With [onClick]
 * the note is one button, at least 48dp tall, that opens the source record.
 * Nothing is drawn when [source] is empty.
 *
 * @param source where the value came from, already localised: "Registry", "Imported file".
 * @param modifier the modifier of the note.
 * @param variant a small bordered chip, or plain muted text.
 * @param checkedAt when the value was fetched, checked or computed, in epoch milliseconds (UTC). Shown as
 * relative time; `null` leaves the time out.
 * @param icon a decorative icon before the text, for example a `MonarchIcon` of size `Sm` with a `null`
 * content description. Monarch bundles no icons.
 * @param onClick opens the source record. Monarch does not navigate: the app opens the screen or the link.
 * @param labels replacements for the spoken "Source: …" and "checked …".
 */
@Composable
public fun MonarchProvenance(
    source: String,
    modifier: Modifier = Modifier,
    variant: MonarchProvenanceVariant = MonarchProvenanceVariant.Chip,
    checkedAt: Long? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    labels: MonarchProvenanceLabels = MonarchProvenanceLabels(),
) {
    if (source.isEmpty()) return
    val themeColors = MonarchTheme.colors
    val colors = MonarchProvenanceTokens.colors(variant, themeColors)
    val now = minuteClock(running = checkedAt != null, times = listOf(checkedAt))
    val time = checkedAt?.let { MonarchProvenanceTokens.relativeTime(it, now) }
    val spokenSource = labels.source?.invoke(source) ?: stringResource(R.string.monarch_provenance_source, source)
    val spoken = if (time == null) {
        spokenSource
    } else {
        val checked = labels.checked?.invoke(time) ?: stringResource(R.string.monarch_provenance_checked, time)
        stringResource(R.string.monarch_provenance_spoken, spokenSource, checked)
    }
    val text = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = MonarchProvenanceTokens.sourceColor(onClick != null, variant, themeColors),
                textDecoration = if (onClick != null) TextDecoration.Underline else null,
            ),
        ) { append(source) }
        if (time != null) append(MonarchProvenanceTokens.SEPARATOR + time)
    }
    val interaction = if (onClick != null) {
        Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button, interactionSource = null, indication = ripple(), onClick = onClick)
    } else {
        Modifier
    }
    Box(modifier = modifier.then(interaction).clearAndSetSemantics { contentDescription = spoken }) {
        Row(
            modifier = Modifier
                .monarchSurface(colors, MonarchShapeDefaults.Row)
                .padding(MonarchProvenanceTokens.padding(variant)),
            horizontalArrangement = Arrangement.spacedBy(MonarchProvenanceTokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) CompositionLocalProvider(LocalContentColor provides colors.content) { icon() }
            Text(text = text, style = MonarchTheme.typography.caption, color = colors.content)
        }
    }
}

/** The current time, redrawn as often as the freshest of [times] needs while [running], so relative times stay true. */
@Composable
private fun minuteClock(running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, running = running)
