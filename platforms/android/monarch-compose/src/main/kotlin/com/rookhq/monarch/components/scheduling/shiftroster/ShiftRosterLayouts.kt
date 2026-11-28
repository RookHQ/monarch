package com.rookhq.monarch.components.scheduling.shiftroster

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.navigation.tabs.MonarchTabs
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One day at a time: `MonarchTabs` of the days, the day's summary and a row per person.
 * Opens on today when it is in the range.
 */
@Composable
internal fun ShiftRosterByDay(model: ShiftRosterModel) {
    val range = model.range
    var selected by rememberSaveable(range.first(), range.size) {
        mutableIntStateOf(MonarchShiftRosterTokens.initialIndex(range, model.today))
    }
    val index = selected.coerceIn(0, range.lastIndex)
    val day = range[index]
    Column(verticalArrangement = Arrangement.spacedBy(MonarchShiftRosterTokens.Gap)) {
        ShiftRosterTabs(
            name = model.text.day,
            selected = index,
            count = range.size,
            onSelect = { selected = it },
            spoken = { model.spokenDay(range[it]) },
            secondary = { model.formats.dayNumber(range[it]) },
            accent = { range[it] == model.today },
        ) { i ->
            val tabDay = range[i]
            if (tabDay == model.today) model.text.today else model.formats.weekday(tabDay)
        }
        ShiftRosterDaySummary(model, day)
        Column {
            model.members.forEachIndexed { i, member ->
                if (i > 0) MonarchDivider()
                ShiftRosterRow(
                    lead = { ShiftRosterPerson(model, member, it) },
                    cell = { ShiftRosterCell(model, member, day, compact = false, modifier = it) },
                )
            }
        }
    }
}

/** One person at a time: tabs of the people, the person with their hours, and a row per day. */
@Composable
internal fun ShiftRosterByPerson(model: ShiftRosterModel) {
    var selected by rememberSaveable(model.members.size) { mutableIntStateOf(0) }
    val index = selected.coerceIn(0, model.members.lastIndex)
    val member = model.members[index]
    Column(verticalArrangement = Arrangement.spacedBy(MonarchShiftRosterTokens.Gap)) {
        ShiftRosterTabs(
            name = model.text.person,
            selected = index,
            count = model.members.size,
            onSelect = { selected = it },
            spoken = { model.members[it].name },
        ) { i -> model.members[i].name }
        ShiftRosterPerson(model, member, Modifier.fillMaxWidth())
        Column {
            model.range.forEachIndexed { i, day ->
                if (i > 0) MonarchDivider()
                ShiftRosterRow(
                    lead = { ShiftRosterDayLabel(model, day, it) },
                    cell = { ShiftRosterCell(model, member, day, compact = false, modifier = it) },
                )
            }
        }
    }
}

/** The day or person tabs: a line of [MonarchTabs], each tab read by the name [spoken] gives it. */
@Composable
private fun ShiftRosterTabs(
    name: String,
    selected: Int,
    count: Int,
    onSelect: (Int) -> Unit,
    spoken: (Int) -> String,
    secondary: (Int) -> String? = { null },
    accent: (Int) -> Boolean = { false },
    label: (Int) -> String,
) {
    MonarchTabs(
        selectedIndex = selected,
        onSelectedIndexChange = onSelect,
        modifier = Modifier.semantics { contentDescription = name },
    ) {
        repeat(count) { i ->
            tab(text = label(i), contentDescription = spoken(i), secondary = secondary(i), accent = accent(i))
        }
    }
}

/**
 * People down the side and days across, with the hours per person and the number working per day. Scrolls
 * sideways when the columns do not fit. TalkBack reads it as a grid of people by days.
 */
@Composable
internal fun ShiftRosterGrid(model: ShiftRosterModel, personWidth: Dp) {
    val tokens = MonarchShiftRosterTokens
    val colors = MonarchTheme.colors
    val range = model.range
    Column(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .semantics {
                contentDescription = model.text.roster
                collectionInfo = CollectionInfo(rowCount = model.members.size, columnCount = range.size)
            },
    ) {
        Row(modifier = Modifier.background(tokens.headerContainer(colors)), verticalAlignment = Alignment.Bottom) {
            ShiftRosterHeaderText(model.text.person, personWidth, TextAlign.Start)
            range.forEach { day ->
                val today = day == model.today
                Column(
                    modifier = Modifier
                        .width(tokens.DayColumnWidth)
                        .padding(tokens.HeaderPadding)
                        .semantics(mergeDescendants = true) {
                            heading()
                            contentDescription = model.spokenDay(day)
                        },
                ) {
                    val color = tokens.dayColor(colors)
                    if (today) Text(model.text.today, style = MonarchTheme.typography.caption, color = color)
                    Text(model.formats.weekday(day), style = MonarchTheme.typography.label, color = color)
                    Text(model.formats.date(day), style = MonarchTheme.typography.caption, color = color)
                }
            }
            if (model.showTotals) ShiftRosterHeaderText(model.text.hours, tokens.HoursColumnWidth, TextAlign.End)
        }
        val width = personWidth + tokens.fixedColumnsWidth(range.size, model.showTotals)
        MonarchDivider(modifier = Modifier.width(width))
        model.members.forEachIndexed { row, member ->
            if (row > 0) MonarchDivider(modifier = Modifier.width(width))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShiftRosterPerson(model, member, Modifier.width(personWidth).padding(tokens.HeaderPadding))
                range.forEachIndexed { column, day ->
                    ShiftRosterCell(
                        model = model,
                        member = member,
                        day = day,
                        compact = true,
                        modifier = Modifier
                            .width(tokens.DayColumnWidth)
                            .padding(tokens.CellPadding)
                            .semantics { collectionItemInfo = CollectionItemInfo(row, 1, column, 1) },
                    )
                }
                if (model.showTotals) {
                    Text(
                        text = model.hours(member),
                        modifier = Modifier
                            .width(tokens.HoursColumnWidth)
                            .padding(tokens.HeaderPadding)
                            .semantics { contentDescription = "${model.text.hours}: ${model.hours(member)}" },
                        style = MonarchTheme.typography.caption,
                        color = tokens.muted(colors),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
        if (model.showTotals) ShiftRosterGridFooter(model, personWidth, width)
    }
}

@Composable
private fun ShiftRosterGridFooter(model: ShiftRosterModel, personWidth: Dp, width: Dp) {
    val tokens = MonarchShiftRosterTokens
    val colors = MonarchTheme.colors
    MonarchDivider(modifier = Modifier.width(width))
    Row(modifier = Modifier.background(tokens.headerContainer(colors))) {
        ShiftRosterHeaderText(model.text.working, personWidth, TextAlign.Start)
        model.range.forEach { day ->
            val count = model.working(day)
            Box(modifier = Modifier.width(tokens.DayColumnWidth).padding(tokens.HeaderPadding)) {
                Text(
                    text = model.formats.hours(count.toDouble()),
                    modifier = Modifier.semantics {
                        contentDescription = "${model.formats.fullDay(day)}: ${model.text.workingValue(count)}"
                    },
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(colors),
                )
            }
        }
    }
}

@Composable
private fun ShiftRosterHeaderText(text: String, width: Dp, align: TextAlign) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(MonarchShiftRosterTokens.HeaderPadding)
            .semantics { heading() },
        style = MonarchTheme.typography.label,
        color = MonarchShiftRosterTokens.muted(MonarchTheme.colors),
        textAlign = align,
    )
}
