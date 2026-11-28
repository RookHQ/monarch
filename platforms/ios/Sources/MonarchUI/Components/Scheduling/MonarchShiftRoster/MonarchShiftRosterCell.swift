import SwiftUI

/// One person on one day: the shift as a ``MonarchEventChip`` bar, "Off" or a
/// dashed "Not assigned" slot. Editable, it is a system `Menu` named
/// "<person>, <day>: <assignment>" whose inline `Picker` lists the shifts and
/// Off with a checkmark on the current one, and Clear when the day is
/// assigned. Read only, it is one element with the same name.
struct MonarchShiftRosterCell: View {
    let model: MonarchShiftRosterModel
    let member: MonarchShiftRosterMember
    let day: Date
    /// The grid's narrow cell: the short name, and a dash for a day with no assignment.
    let isCompact: Bool

    var body: some View {
        let assignment = model.assignment(member, on: day)
        let label = model.cellLabel(member, on: day, isCompact: isCompact)
        if model.isReadOnly {
            MonarchShiftRosterCellFace(model: model, assignment: assignment, isCompact: isCompact)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        } else {
            Menu {
                Section(model.menuTitle(member, on: day)) {
                    Picker(selection: selection) {
                        ForEach(model.shifts) { shift in
                            Text(model.menuItem(shift))
                                .accessibilityLabel(model.spokenShift(shift))
                                .tag(Optional(MonarchShiftRosterAssignment.shift(shift.id)))
                        }
                        Text(model.text.off)
                            .tag(Optional(MonarchShiftRosterAssignment.off))
                    } label: {
                        Text(model.menuTitle(member, on: day))
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }
                if assignment != nil {
                    Button(model.text.clear) {
                        model.assign(nil, to: member, on: day)
                    }
                }
            } label: {
                MonarchShiftRosterCellFace(model: model, assignment: assignment, isCompact: isCompact)
                    .monarchMinimumHitTarget()
                    .contentShape(Rectangle())
            }
            .menuStyle(.button)
            .buttonStyle(.plain)
            .accessibilityLabel(label)
        }
    }

    private var selection: Binding<MonarchShiftRosterAssignment?> {
        Binding(
            get: { model.assignment(member, on: day) },
            set: { model.assign($0, to: member, on: day) }
        )
    }
}

/// What a cell draws.
struct MonarchShiftRosterCellFace: View {
    let model: MonarchShiftRosterModel
    let assignment: MonarchShiftRosterAssignment?
    let isCompact: Bool

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        if let shift = model.shift(of: assignment) {
            MonarchEventChip(
                isCompact ? (shift.shortName ?? shift.name) : shift.name,
                time: model.visibleRange(shift),
                color: shift.color,
                variant: .bar
            )
            .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            let isOff = assignment == .off
            let appearance = MonarchShiftRosterTokens.slotAppearance(isOff: isOff)
            let shape = MonarchShiftRosterTokens.slotShape
            Text(isOff ? model.text.off : (isCompact ? MonarchShiftRosterTokens.unassignedMark : model.text.unassigned))
                .monarchTextStyle(MonarchShiftRosterTokens.captionStyle)
                .foregroundStyle(theme.colors[role: appearance.content])
                .padding(.horizontal, MonarchShiftRosterTokens.slotHorizontalPadding)
                .padding(.vertical, MonarchShiftRosterTokens.slotVerticalPadding)
                .frame(maxWidth: .infinity, minHeight: MonarchShiftRosterTokens.slotMinHeight, alignment: .leading)
                .background {
                    if let fill = appearance.fill {
                        shape.fill(theme.colors[role: fill])
                    }
                }
                .overlay {
                    if let border = appearance.border {
                        shape.strokeBorder(
                            theme.colors[role: border],
                            style: StrokeStyle(
                                lineWidth: MonarchShapes.borderWidth,
                                dash: [MonarchShiftRosterTokens.dashLength, MonarchShiftRosterTokens.dashLength]
                            )
                        )
                    }
                }
        }
    }
}
