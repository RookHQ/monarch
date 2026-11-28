package dev.monarchui.components.workflow.timesheetgrid

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import dev.monarchui.theme.MonarchTheme

/**
 * Rows down the side and days across, with row totals, day totals and summary rows. Scrolls sideways when the
 * columns do not fit. TalkBack reads it as a collection of rows by days named by the caption.
 */
@Composable
internal fun TimesheetGridTable(model: TimesheetGridModel, rowHeaderWidth: Dp) {
    val tokens = MonarchTimesheetGridTokens
    val colors = MonarchTheme.colors
    val width = rowHeaderWidth + TimesheetGridLayoutRules.fixedColumnsWidth(model.days.size)
    Column(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .semantics {
                contentDescription = model.text.caption
                collectionInfo = CollectionInfo(rowCount = model.rows.size, columnCount = model.days.size)
            },
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).background(tokens.headerContainer(colors)),
            verticalAlignment = Alignment.Bottom,
        ) {
            TimesheetGridHeaderText(model.text.rowHeader, rowHeaderWidth, TextAlign.Start)
            model.days.forEach { day ->
                TimesheetGridDayHeading(model, day, Modifier.width(tokens.DayColumnWidth).fillMaxHeight())
            }
            TimesheetGridHeaderText(model.text.total, tokens.TotalColumnWidth, TextAlign.End)
        }
        MonarchDivider(modifier = Modifier.width(width))
        if (model.rows.isEmpty()) {
            Text(
                text = model.emptyMessage,
                modifier = Modifier.width(width).padding(tokens.HeaderPadding),
                style = MonarchTheme.typography.body,
                color = tokens.muted(colors),
            )
        }
        model.rows.forEachIndexed { index, row ->
            if (index > 0) MonarchDivider(modifier = Modifier.width(width))
            TimesheetGridTableRow(model, row, index, rowHeaderWidth)
        }
        MonarchDivider(modifier = Modifier.width(width))
        TimesheetGridTableFooter(model, rowHeaderWidth)
    }
}

@Composable
private fun TimesheetGridTableRow(
    model: TimesheetGridModel,
    row: MonarchTimesheetGridRow,
    index: Int,
    rowHeaderWidth: Dp,
) {
    val tokens = MonarchTimesheetGridTokens
    val colors = MonarchTheme.colors
    Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        TimesheetGridRowHeader(model, row, Modifier.width(rowHeaderWidth).padding(tokens.HeaderPadding))
        model.days.forEachIndexed { column, day ->
            Box(
                modifier = Modifier
                    .width(tokens.DayColumnWidth)
                    .fillMaxHeight()
                    .background(
                        tokens.columnContainer(colors, TimesheetGridRules.locked(day), day.nonWorking),
                    )
                    .padding(tokens.CellPadding)
                    .semantics { collectionItemInfo = CollectionItemInfo(index, 1, column, 1) },
                contentAlignment = Alignment.Center,
            ) {
                TimesheetGridCell(model, row, day)
            }
        }
        val total = TimesheetGridRules.rowTotal(row, model.days)
        TimesheetGridTotalText(
            text = model.shown(total),
            name = model.text.cellLabel(row.label, model.text.total),
            spoken = model.spoken(total),
        )
    }
}

@Composable
private fun TimesheetGridTableFooter(model: TimesheetGridModel, rowHeaderWidth: Dp) {
    val tokens = MonarchTimesheetGridTokens
    val colors = MonarchTheme.colors
    Column(modifier = Modifier.background(tokens.headerContainer(colors))) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            TimesheetGridHeaderText(model.text.dayTotal, rowHeaderWidth, TextAlign.Start)
            model.days.forEach { day ->
                TimesheetGridDayTotal(
                    model = model,
                    day = day,
                    modifier = Modifier.width(tokens.DayColumnWidth).padding(tokens.HeaderPadding),
                )
            }
            val total = TimesheetGridRules.grandTotal(model.rows, model.days)
            TimesheetGridTotalText(
                text = model.shown(total),
                name = model.text.cellLabel(model.text.dayTotal, model.text.total),
                spoken = model.spoken(total),
            )
        }
        model.summaryRows.forEach { summary ->
            Row {
                TimesheetGridHeaderText(summary.label, rowHeaderWidth, TextAlign.Start)
                model.days.forEach { day ->
                    val value = summary.text(day.date)
                    Text(
                        text = value ?: tokens.EMPTY_MARK,
                        modifier = Modifier
                            .width(tokens.DayColumnWidth)
                            .padding(tokens.HeaderPadding)
                            .clearAndSetSemantics {
                                contentDescription = model.text.cellLabel(summary.label, model.text.longDay(day.date))
                                stateDescription = value ?: model.text.noTime
                            },
                        style = MonarchTheme.typography.caption,
                        color = tokens.muted(colors),
                    )
                }
                Text(
                    text = summary.total.orEmpty(),
                    modifier = Modifier
                        .width(tokens.TotalColumnWidth)
                        .padding(tokens.HeaderPadding)
                        .clearAndSetSemantics {
                            contentDescription = model.text.cellLabel(summary.label, model.text.total)
                            summary.total?.let { stateDescription = it }
                        },
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(colors),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun TimesheetGridTotalText(text: String, name: String, spoken: String) {
    val tokens = MonarchTimesheetGridTokens
    Text(
        text = text,
        modifier = Modifier
            .width(tokens.TotalColumnWidth)
            .padding(tokens.HeaderPadding)
            .clearAndSetSemantics {
                contentDescription = name
                stateDescription = spoken
            },
        style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.bodyStrong),
        color = MonarchTheme.colors.textDefault,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun TimesheetGridHeaderText(text: String, width: Dp, align: TextAlign) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(MonarchTimesheetGridTokens.HeaderPadding)
            .semantics { heading() },
        style = MonarchTheme.typography.label,
        color = MonarchTimesheetGridTokens.muted(MonarchTheme.colors),
        textAlign = align,
    )
}
