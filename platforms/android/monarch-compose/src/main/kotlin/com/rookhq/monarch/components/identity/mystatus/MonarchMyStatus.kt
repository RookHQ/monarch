package com.rookhq.monarch.components.identity.mystatus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.internal.monarchPresenceName
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/**
 * The signed-in person's avatar, presence and custom status, as one row that
 * opens their status settings.
 *
 * Built by Monarch from a clickable `Row` with `MonarchAvatar` (and its
 * presence dot) and two lines of text. Under the name it shows the emoji and
 * message of [customStatus], else a presence other than online and offline,
 * else "Set a status". A status whose until time has passed is hidden; without
 * [now] the row reads the clock and hides it at that moment by itself.
 *
 * The row is one TalkBack button named by [name], whose state is the presence
 * and the line ("Online, On holiday"), so the presence never depends on the
 * dot's colour. It is at least 48dp high and its text wraps.
 *
 * @param name the person's display name, already localised. Also the avatar's initials.
 * @param presence drawn as the avatar's presence dot and read in words.
 * @param onClick opens the status settings, usually a sheet the app presents.
 * @param modifier the modifier of the row.
 * @param image the avatar picture, loaded by the app; `null` shows the initials.
 * @param customStatus the person's own message and emoji, with an optional until time.
 * @param now the time in epoch milliseconds that until is compared with. `null` reads the clock and redraws
 * when the status ends; pass it for a fixed rendering.
 * @param enabled whether the row can be pressed.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the interaction source of the row.
 */
@Composable
public fun MonarchMyStatus(
    name: String,
    presence: MonarchPresenceStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: Painter? = null,
    customStatus: MonarchMyStatusCustomStatus? = null,
    now: Long? = null,
    enabled: Boolean = true,
    labels: MonarchMyStatusLabels = MonarchMyStatusLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    MonarchMyStatusRow(
        name = name,
        presence = presence,
        onClick = onClick,
        modifier = modifier,
        image = image,
        customStatus = customStatus,
        now = now,
        enabled = enabled,
        labels = labels,
        interactionSource = interactionSource,
        clock = System::currentTimeMillis,
    )
}

/** The row, with the clock injected so tests can move time. */
@Composable
internal fun MonarchMyStatusRow(
    name: String,
    presence: MonarchPresenceStatus,
    onClick: () -> Unit,
    modifier: Modifier,
    image: Painter?,
    customStatus: MonarchMyStatusCustomStatus?,
    now: Long?,
    enabled: Boolean,
    labels: MonarchMyStatusLabels,
    interactionSource: MutableInteractionSource?,
    clock: () -> Long,
) {
    val tokens = MonarchMyStatusTokens
    val currentClock by rememberUpdatedState(clock)
    var tick by remember { mutableIntStateOf(0) }
    val time = now ?: remember(tick, customStatus) { currentClock() }
    if (now == null) {
        LaunchedEffect(customStatus, tick) {
            val wait = tokens.waitMillis(customStatus, currentClock()) ?: return@LaunchedEffect
            delay(wait)
            tick++
        }
    }
    val presenceText = monarchPresenceName(presence, labels.presence?.invoke(presence))
    val setStatus = monarchString(labels.setStatus, R.string.monarch_mystatus_set_status)
    val hint = monarchString(labels.hint, R.string.monarch_mystatus_hint)
    val line = tokens.line(tokens.activeStatus(customStatus, time), presence, presenceText, setStatus)
    val spoken = tokens.spokenState(line, presenceText)
    Row(
        modifier = modifier
            .heightIn(min = MonarchTouchTarget.Minimum)
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClickLabel = hint,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = name
                stateDescription = spoken
            }
            .padding(tokens.Padding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchAvatar(
            image = image,
            name = name,
            size = MonarchAvatarSize.Sm,
            status = presence,
            decorative = true,
            statusLabel = presenceText,
        )
        // The row is named and described above; the visible lines are not read a second time.
        Column {
            Text(
                text = name,
                modifier = Modifier.semantics { hideFromAccessibility() },
                style = MonarchTheme.typography.bodyStrong, color = MonarchTheme.colors.textDefault,
            )
            Text(
                text = line.text,
                modifier = Modifier.semantics { hideFromAccessibility() },
                style = MonarchTheme.typography.caption,
                color = tokens.lineColor(line.kind, MonarchTheme.colors),
            )
        }
    }
}
