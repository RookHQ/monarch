package com.rookhq.monarch.components.scheduling.teamcalendar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.overlays.tooltip.MonarchTooltip
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/** The built-in strings of a team calendar, resolved. */
internal class TeamCalendarStrings(
    val person: String,
    val grid: String,
    val today: String,
    val holiday: (String) -> String,
    val range: (String, String) -> String,
    val firstHalf: String,
    val secondHalf: String,
    val pending: String,
    val legend: String?,
)

/** Everything the grid of a team calendar shows. */
@Suppress("LongParameterList")
internal class TeamCalendarModel(
    val members: List<MonarchTeamCalendarMember>,
    val days: List<Long>,
    val cells: Map<String, List<TeamCalendarCell?>>,
    val shaded: List<Boolean>,
    val holidays: Map<Long, String>,
    val today: Long,
    val formats: TeamCalendarFormats,
    val strings: TeamCalendarStrings,
    val onEntryClick: ((MonarchTeamCalendarEntry, MonarchTeamCalendarMember) -> Unit)?,
    val onCellClick: ((MonarchTeamCalendarMember, Long) -> Unit)?,
)

/** People down the start, days across in one shared horizontal scroll, the day header pinned at the top. */
@Composable
internal fun TeamCalendarGrid(model: TeamCalendarModel, scroll: ScrollState, modifier: Modifier) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    LazyColumn(
        modifier = modifier
            .clip(shape)
            .border(MonarchShapeDefaults.BorderWidth, colors.borderDefault, shape)
            .background(colors.bgSurface),
    ) {
        stickyHeader(key = "header") {
            Column(modifier = Modifier.background(colors.bgSurface)) {
                TeamCalendarRow(
                    scroll = scroll,
                    person = { TeamCalendarCorner(model.strings) },
                    days = { TeamCalendarHeaderDays(model) },
                )
                MonarchDivider()
            }
        }
        items(model.members, key = { it.id }) { member ->
            Column {
                TeamCalendarRow(
                    scroll = scroll,
                    person = { TeamCalendarPerson(member) },
                    days = { TeamCalendarMemberDays(model, member) },
                )
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
            }
        }
        model.strings.legend?.let { legend ->
            item(key = "legend") {
                Text(
                    text = legend,
                    modifier = Modifier.padding(MonarchTeamCalendarTokens.PersonHorizontalPadding),
                    style = MonarchTheme.typography.caption,
                    color = colors.textMuted,
                )
            }
        }
    }
}

/** One row: the person column, then the days in the shared scroll. Both are as tall as the taller one. */
@Composable
private fun TeamCalendarRow(scroll: ScrollState, person: @Composable () -> Unit, days: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(MonarchTeamCalendarTokens.PersonWidth)
                .fillMaxHeight()
                .heightIn(min = MonarchTeamCalendarTokens.CellSize),
            contentAlignment = Alignment.CenterStart,
        ) { person() }
        Box(modifier = Modifier.weight(1f).fillMaxHeight().horizontalScroll(scroll)) { days() }
    }
}

@Composable
private fun TeamCalendarCorner(strings: TeamCalendarStrings) {
    Text(
        text = strings.person,
        modifier = Modifier
            .padding(horizontal = MonarchTeamCalendarTokens.PersonHorizontalPadding)
            // The corner names the whole grid for TalkBack, as the caption of the web table does.
            .clearAndSetSemantics {
                heading()
                contentDescription = strings.grid
            },
        style = MonarchTheme.typography.caption,
        fontWeight = FontWeight(MonarchTeamCalendarTokens.HEADER_WEIGHT),
        color = MonarchTheme.colors.textMuted,
    )
}

@Composable
private fun TeamCalendarPerson(member: MonarchTeamCalendarMember) {
    val tokens = MonarchTeamCalendarTokens
    Row(
        modifier = Modifier
            .padding(horizontal = tokens.PersonHorizontalPadding, vertical = tokens.PersonVerticalPadding)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(tokens.PersonGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchAvatar(
            image = member.image,
            name = member.name,
            size = MonarchAvatarSize.Sm,
            seed = member.id,
            decorative = true,
        )
        Column {
            Text(text = member.name, style = MonarchTheme.typography.label, color = MonarchTheme.colors.textDefault)
            member.description?.let {
                Text(text = it, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
            }
        }
    }
}

@Composable
private fun TeamCalendarHeaderDays(model: TeamCalendarModel) {
    val colors = MonarchTheme.colors
    val tokens = MonarchTeamCalendarTokens
    Row(modifier = Modifier.fillMaxHeight()) {
        model.days.forEachIndexed { index, day ->
            val today = day == model.today
            val holiday = model.holidays[day]
            val spoken = tokens.spoken(
                model.strings.today.takeIf { today },
                model.formats.day(day),
                holiday?.let(model.strings.holiday),
            )
            TeamCalendarTooltip(text = holiday?.let(model.strings.holiday)) {
                Column(
                    modifier = Modifier
                        .width(tokens.CellSize)
                        .fillMaxHeight()
                        .background(tokens.columnColor(colors, model.shaded[index]))
                        .padding(vertical = tokens.HeaderVerticalPadding)
                        .clearAndSetSemantics { contentDescription = spoken },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val mark = tokens.headerColors(colors, today)
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = tokens.MarkSize, minHeight = tokens.MarkSize)
                            .monarchSurface(mark, MonarchShapeDefaults.Mark),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = model.formats.number(day),
                            style = MonarchTheme.typography.label,
                            fontWeight = FontWeight(if (today) tokens.TODAY_WEIGHT else tokens.HEADER_WEIGHT),
                            color = mark.content,
                        )
                    }
                    Text(
                        text = model.formats.narrowWeekday(day),
                        style = MonarchTheme.typography.caption,
                        color = colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamCalendarMemberDays(model: TeamCalendarModel, member: MonarchTeamCalendarMember) {
    val tokens = MonarchTeamCalendarTokens
    val cells = model.cells[member.id].orEmpty()
    val segments = remember(cells, model.days) { tokens.segments(cells, model.days) }
    Box(modifier = Modifier.width(tokens.CellSize * model.days.size).fillMaxHeight()) {
        Row(modifier = Modifier.fillMaxHeight()) {
            model.days.forEachIndexed { index, day ->
                TeamCalendarDay(model, member, day, index, cells.getOrNull(index) == null)
            }
        }
        segments.forEach { TeamCalendarBar(model, member, it) }
    }
}

/** The background of one day of a row; an empty day takes a tap when the app adds time away. */
@Composable
private fun TeamCalendarDay(
    model: TeamCalendarModel,
    member: MonarchTeamCalendarMember,
    day: Long,
    index: Int,
    empty: Boolean,
) {
    val tokens = MonarchTeamCalendarTokens
    val onCellClick = model.onCellClick
    val interaction = if (empty && onCellClick != null) {
        val spoken = tokens.spoken(member.name, model.formats.day(day))
        Modifier
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button) { onCellClick(member, day) }
            .semantics { contentDescription = spoken }
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .width(tokens.CellSize)
            .fillMaxHeight()
            .heightIn(min = tokens.CellSize)
            .background(tokens.columnColor(MonarchTheme.colors, model.shaded[index]))
            .then(interaction),
    )
}

/** One bar: a run of days of one entry, one TalkBack element and one target as wide as its days. */
@Composable
private fun TeamCalendarBar(model: TeamCalendarModel, member: MonarchTeamCalendarMember, segment: TeamCalendarSegment) {
    val tokens = MonarchTeamCalendarTokens
    val entry = segment.entry
    val first = model.days[segment.first]
    val last = model.days[segment.first + segment.count - 1]
    val dates = if (segment.count == 1) {
        model.formats.day(first)
    } else {
        model.strings.range(model.formats.day(first), model.formats.day(last))
    }
    val spoken = tokens.spoken(
        entry.label,
        member.name,
        dates,
        when (segment.portion) {
            MonarchTeamCalendarPortion.Full -> null
            MonarchTeamCalendarPortion.FirstHalf -> model.strings.firstHalf
            MonarchTeamCalendarPortion.SecondHalf -> model.strings.secondHalf
        },
        model.strings.pending.takeIf { entry.pending },
    )
    val onEntryClick = model.onEntryClick
    val interaction = if (onEntryClick != null) {
        Modifier
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(role = Role.Button) { onEntryClick(entry, member) }
            .clearAndSetSemantics {
                contentDescription = spoken
            }
    } else {
        Modifier.clearAndSetSemantics { contentDescription = spoken }
    }
    TeamCalendarTooltip(
        text = entry.label,
        modifier = Modifier
            .padding(start = tokens.CellSize * segment.first)
            .width(tokens.CellSize * segment.count)
            .fillMaxHeight(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().then(interaction),
            contentAlignment = when (segment.portion) {
                MonarchTeamCalendarPortion.Full -> Alignment.Center
                MonarchTeamCalendarPortion.FirstHalf -> Alignment.CenterStart
                MonarchTeamCalendarPortion.SecondHalf -> Alignment.CenterEnd
            },
        ) { TeamCalendarBarShape(segment) }
    }
}

@Composable
private fun TeamCalendarBarShape(segment: TeamCalendarSegment) {
    val tokens = MonarchTeamCalendarTokens
    val theme = MonarchTheme.colors
    val base = if (segment.entry.color.isSpecified) segment.entry.color else theme.accentSolid
    // As the web draws it: a solid block per day, faded while the entry waits for approval; no label in the block.
    val fill = if (segment.entry.pending) lerp(theme.bgSurface, base, tokens.PENDING_SHARE) else base
    val shape = RoundedCornerShape(MonarchTokens.Radius.xs.dp)
    val half = segment.portion != MonarchTeamCalendarPortion.Full
    val block = Modifier
        .padding(horizontal = tokens.BarInset)
        .heightIn(min = tokens.BarMinHeight)
        .clip(shape)
        .background(fill)
    if (half) {
        val first = segment.portion == MonarchTeamCalendarPortion.FirstHalf
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (first) Arrangement.Start else Arrangement.End,
        ) {
            Box(modifier = block.width(tokens.CellSize / tokens.HALF_BAR_DIVISOR))
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(segment.count) { Box(modifier = block.weight(1f).height(tokens.BarMinHeight)) }
        }
    }
}

/** A Monarch tooltip on a long press of [content], when there is [text]. */
@Composable
private fun TeamCalendarTooltip(text: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (text == null) {
        Box(modifier = modifier) { content() }
        return
    }
    MonarchTooltip(text = text, modifier = modifier, content = content)
}
