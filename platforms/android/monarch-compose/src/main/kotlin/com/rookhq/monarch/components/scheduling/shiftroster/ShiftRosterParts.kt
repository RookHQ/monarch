package com.rookhq.monarch.components.scheduling.shiftroster

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A person: a decorative `MonarchAvatar`, the name and a muted line with the description and, with totals, the
 * hours. Read as one element, "Alex Kim, Support, Hours: 40 h"; a button when the roster has `onMemberClick`.
 */
@Composable
internal fun ShiftRosterPerson(
    model: ShiftRosterModel,
    member: MonarchShiftRosterMember,
    modifier: Modifier = Modifier,
) {
    val tokens = MonarchShiftRosterTokens
    val hours = if (model.showTotals) model.hours(member) else null
    val name = listOfNotNull(member.name, member.description, hours?.let { "${model.text.hours}: $it" })
        .joinToString(", ")
    val click = model.onMemberClick
    val interaction = if (click != null) {
        Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button) { click(member) }
            .semantics { contentDescription = name }
    } else {
        Modifier.semantics(mergeDescendants = true) { contentDescription = name }
    }
    Row(
        modifier = modifier.then(interaction),
        horizontalArrangement = Arrangement.spacedBy(tokens.PersonGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchAvatar(image = member.image, name = member.name, size = MonarchAvatarSize.Sm, decorative = true)
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = member.name,
                style = MonarchTheme.typography.bodyStrong,
                color = MonarchTheme.colors.textDefault,
            )
            val supporting = listOfNotNull(member.description, hours).joinToString(" · ")
            if (supporting.isNotEmpty()) {
                Text(
                    text = supporting,
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(MonarchTheme.colors),
                )
            }
        }
    }
}

/** A day as the start of a row in the one-person layout: the date and a Today badge. Read as one element. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShiftRosterDayLabel(model: ShiftRosterModel, day: Long, modifier: Modifier = Modifier) {
    val today = day == model.today
    FlowRow(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = model.spokenDay(day) },
        horizontalArrangement = Arrangement.spacedBy(MonarchShiftRosterTokens.SummaryGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = model.formats.fullDay(day),
            style = MonarchTheme.typography.bodyStrong,
            color = MonarchShiftRosterTokens.dayColor(MonarchTheme.colors),
        )
        if (today) MonarchBadge(text = model.text.today, tone = MonarchTone.Info)
    }
}

/**
 * The line under the day tabs: the date as a heading, a Today badge and, with totals, how many people work. The
 * count is a polite live region, so TalkBack says the new count after an assignment.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShiftRosterDaySummary(model: ShiftRosterModel, day: Long, modifier: Modifier = Modifier) {
    val tokens = MonarchShiftRosterTokens
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.SummaryGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = model.formats.fullDay(day),
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.subheading,
            color = tokens.dayColor(MonarchTheme.colors),
        )
        if (day == model.today) MonarchBadge(text = model.text.today, tone = MonarchTone.Info)
        if (model.showTotals) {
            Text(
                text = model.text.workingValue(model.working(day)),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.caption,
                color = tokens.muted(MonarchTheme.colors),
            )
        }
    }
}

/**
 * A row of the one-day and one-person layouts: [lead] (the person or the day) and the [cell] side by side, or the
 * cell under the lead from font scale 1.5, so neither is squeezed.
 */
@Composable
internal fun ShiftRosterRow(
    lead: @Composable (Modifier) -> Unit,
    cell: @Composable (Modifier) -> Unit,
) {
    val tokens = MonarchShiftRosterTokens
    val padding = Modifier.fillMaxWidth().padding(vertical = tokens.RowPadding)
    if (LocalDensity.current.fontScale >= tokens.STACK_FONT_SCALE) {
        Column(modifier = padding, verticalArrangement = Arrangement.spacedBy(tokens.RowPadding)) {
            lead(Modifier.fillMaxWidth())
            cell(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = padding,
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            lead(Modifier.weight(1f))
            cell(Modifier.weight(1f))
        }
    }
}
