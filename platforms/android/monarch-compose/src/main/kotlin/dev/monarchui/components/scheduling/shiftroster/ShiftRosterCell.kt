package dev.monarchui.components.scheduling.shiftroster

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuDivider
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.overlays.menu.MonarchMenuLabel
import dev.monarchui.components.scheduling.eventchip.MonarchEventChip
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipVariant
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * One person on one day: the shift as a `MonarchEventChip` bar, "Off" or a dashed "Not assigned" slot. Editable,
 * it is one button named "<person>, <day>: <assignment>" that opens a `MonarchMenu` of the shifts, Off
 * and Clear; read only, it is one text element with the same name.
 *
 * @param compact the grid's narrow cell: the short name and a dash for a day with no assignment.
 */
@Composable
internal fun ShiftRosterCell(
    model: ShiftRosterModel,
    member: MonarchShiftRosterMember,
    day: Long,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val assignment = model.assignment(member, day)
    val name = model.text.cell(member.name, model.spokenDay(day), model.describe(assignment, compact))
    val shape = MonarchShapeDefaults.Row
    var expanded by rememberSaveable(member.id, day) { mutableStateOf(false) }
    val interaction = if (model.readOnly) {
        Modifier.semantics(mergeDescendants = true) { contentDescription = name }
    } else {
        Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(shape)
            .clickable(enabled = model.enabled, role = Role.Button) { expanded = true }
            .semantics { contentDescription = name }
    }
    Box(modifier = modifier.then(interaction), contentAlignment = Alignment.Center) {
        // The face is read once, through the name of the cell.
        Box(modifier = Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            ShiftRosterCellFace(model, assignment, compact)
        }
        if (!model.readOnly) {
            ShiftRosterMenu(model, member, day, assignment, expanded) { expanded = false }
        }
    }
}

@Composable
private fun ShiftRosterCellFace(model: ShiftRosterModel, assignment: MonarchShiftRosterAssignment?, compact: Boolean) {
    val shift = model.shiftOf(assignment)
    if (shift != null) {
        MonarchEventChip(
            title = if (compact) shift.shortName ?: shift.name else shift.name,
            variant = MonarchEventChipVariant.Bar,
            color = shift.color,
            time = model.visibleRange(shift),
        )
        return
    }
    val tokens = MonarchShiftRosterTokens
    val off = assignment == MonarchShiftRosterAssignment.Off
    val colors = tokens.slotColors(MonarchTheme.colors, off)
    val shape = MonarchTheme.shapes.sm
    val border = colors.border
    val dashed = if (border != null) {
        Modifier.drawBehind {
            val stroke = MonarchShapeDefaults.BorderWidth.toPx()
            val dash = tokens.DashLength.toPx()
            val radius = shape.topStart.toPx(size, this)
            drawRoundRect(
                color = border,
                cornerRadius = CornerRadius(radius),
                style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
            )
        }
    } else {
        Modifier
    }
    val text = when {
        off -> model.text.off
        compact -> tokens.UNASSIGNED_MARK
        else -> model.text.unassigned
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.SlotMinHeight)
            .background(colors.container, shape)
            .then(dashed)
            .padding(horizontal = tokens.SlotHorizontalPadding, vertical = tokens.SlotVerticalPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = text, style = MonarchTheme.typography.caption, color = colors.content)
    }
}

@Suppress("LongParameterList")
@Composable
private fun ShiftRosterMenu(
    model: ShiftRosterModel,
    member: MonarchShiftRosterMember,
    day: Long,
    assignment: MonarchShiftRosterAssignment?,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    MonarchMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MonarchMenuLabel(text = model.text.menuTitle(member.name, model.formats.fullDay(day)))
        model.shifts.forEach { shift ->
            val chosen = assignment == MonarchShiftRosterAssignment.Shift(shift.id)
            ShiftRosterMenuItem(
                text = "${shift.name} · ${model.visibleRange(shift)}",
                spoken = model.spokenShift(shift),
                chosen = chosen,
            ) {
                onDismiss()
                if (!chosen) model.assign(member, day, MonarchShiftRosterAssignment.Shift(shift.id))
            }
        }
        val off = assignment == MonarchShiftRosterAssignment.Off
        ShiftRosterMenuItem(text = model.text.off, spoken = model.text.off, chosen = off) {
            onDismiss()
            if (!off) model.assign(member, day, MonarchShiftRosterAssignment.Off)
        }
        if (assignment != null) {
            MonarchMenuDivider()
            MonarchMenuItem(
                text = model.text.clear,
                onClick = {
                    onDismiss()
                    model.assign(member, day, null)
                },
            )
        }
    }
}

@Composable
private fun ShiftRosterMenuItem(text: String, spoken: String, chosen: Boolean, onClick: () -> Unit) {
    MonarchMenuItem(
        text = text,
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = spoken },
        selected = chosen,
    )
}
