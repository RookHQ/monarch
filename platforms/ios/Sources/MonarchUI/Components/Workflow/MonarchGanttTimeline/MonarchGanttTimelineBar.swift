import SwiftUI

/// One item's bar or one-day marker: a touch area at least 44 points wide
/// and as tall as the row, with the visible bar centred in it. It opens the
/// item when tapped (with `onOpen`), and when editable it moves by touch and
/// hold then drag, by the arrow keys and by VoiceOver actions.
struct MonarchGanttTimelineBar: View {
    let model: MonarchGanttTimelineModel
    let item: MonarchGanttTimelineItem
    /// The span as shown, moved while the bar is dragged.
    let span: MonarchGanttTimelineSpan
    let isPoint: Bool
    let box: MonarchGanttTimelineBarBox
    let isDragging: Bool
    let onDrag: @MainActor (MonarchGanttTimelineDrag?) -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var pickup: MonarchGanttTimelineShift?
    @State private var dragDays = 0
    @FocusState private var isFocused: Bool

    var body: some View {
        let isEditable = model.isEditable(item)
        face
            .frame(width: box.nodeWidth)
            .frame(maxHeight: .infinity)
            .contentShape(Rectangle())
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(model.describe(item, span: span))
            .accessibilityActions {
                ForEach(model.actions(item, isPoint: isPoint), id: \.self) { action in
                    Button(action.name) { model.shift(item, action.shift, by: action.days) }
                }
            }
            .modifier(MonarchGanttTimelineOpenModifier(item: item, onOpen: model.onOpen))
            .gesture(drag, including: isEditable ? .all : .subviews)
            .focusable(isEditable)
            .focused($isFocused)
            .monarchFocusRing(isFocused, in: Rectangle())
            .onKeyPress(keys: [.leftArrow, .rightArrow]) { press in
                guard isEditable else { return .ignored }
                model.shift(
                    item,
                    MonarchGanttTimelineBar.keyShift(press.modifiers),
                    by: press.key == .leftArrow ? -1 : 1
                )
                return .handled
            }
            .sensoryFeedback(.selection, trigger: dragDays)
            .sensoryFeedback(trigger: pickup) { old, new in
                old == nil && new != nil ? .impact : nil
            }
    }

    /// Shift changes the end, Option the start, no modifier moves the bar.
    static func keyShift(_ modifiers: EventModifiers) -> MonarchGanttTimelineShift {
        if modifiers.contains(.shift) { return .end }
        if modifiers.contains(.option) { return .start }
        return .move
    }

    private var drag: some Gesture {
        LongPressGesture()
            .sequenced(
                before: DragGesture(
                    minimumDistance: 0,
                    coordinateSpace: .named(MonarchGanttTimelineTokens.gridSpace)
                )
            )
            .onChanged { value in
                guard case .second(true, let drag?) = value else { return }
                let shift =
                    pickup
                    ?? MonarchGanttTimelineRules.dragShift(
                        x: drag.startLocation.x - box.visibleLeft,
                        width: box.visibleWidth,
                        edge: MonarchGanttTimelineTokens.edgeZone,
                        isPoint: isPoint
                    )
                if pickup == nil {
                    pickup = shift
                    onDrag(MonarchGanttTimelineDrag(id: item.id, shift: shift, days: 0))
                }
                let days = MonarchGanttTimelineRules.dragDays(
                    drag.location.x - drag.startLocation.x,
                    dayWidth: model.scale.dayWidth
                )
                if days != dragDays {
                    dragDays = days
                    onDrag(MonarchGanttTimelineDrag(id: item.id, shift: shift, days: days))
                }
            }
            .onEnded { _ in
                if let pickup, dragDays != 0 {
                    model.shift(item, pickup, by: dragDays)
                }
                pickup = nil
                dragDays = 0
                onDrag(nil)
            }
    }

    @ViewBuilder private var face: some View {
        let colors = MonarchGanttTimelineTokens.barColors(tone: item.tone, hasConflict: item.conflict != nil)
        if isPoint {
            Rectangle()
                .fill(theme.colors[role: colors.border])
                .frame(width: MonarchGanttTimelineTokens.pointSize, height: MonarchGanttTimelineTokens.pointSize)
                .rotationEffect(.degrees(MonarchGanttTimelineTokens.diamondDegrees))
        } else {
            let shape = RoundedRectangle(cornerRadius: MonarchGanttTimelineTokens.barRadius)
            ZStack(alignment: .leading) {
                if let progress = item.progress {
                    // The done part is filled, as on the web, not a stripe along the foot.
                    Rectangle()
                        .fill(theme.colors[role: colors.progress].opacity(MonarchGanttTimelineTokens.progressOpacity))
                        .frame(width: box.visibleWidth * min(max(progress, 0), 1))
                        .frame(maxHeight: .infinity)
                }
                if MonarchGanttTimelineTokens.showsBarLabel(width: box.visibleWidth) {
                    HStack(spacing: MonarchGanttTimelineTokens.barGlyphSpacing) {
                        // A scheduling conflict is named by a warning glyph as well as by the dashed outline.
                        if item.conflict != nil {
                            MonarchIcon(systemName: MonarchGanttTimelineTokens.conflictSymbol, size: .sm)
                        }
                        Text(item.label)
                            .monarchTextStyle(MonarchGanttTimelineTokens.captionStyle)
                            .lineLimit(1)
                    }
                    .foregroundStyle(theme.colors[role: colors.content])
                    .padding(.horizontal, MonarchGanttTimelineTokens.barPadding)
                }
            }
            .frame(width: box.visibleWidth, alignment: .leading)
            .frame(maxHeight: .infinity)
            .background(theme.colors[role: colors.container])
            .clipShape(shape)
            .overlay {
                shape.strokeBorder(
                    theme.colors[role: colors.border],
                    style: StrokeStyle(
                        lineWidth: colors.isDashed
                            ? MonarchGanttTimelineTokens.conflictBorderWidth : MonarchGanttTimelineTokens.borderWidth,
                        dash: colors.isDashed
                            ? [MonarchGanttTimelineTokens.dashLength, MonarchGanttTimelineTokens.dashLength] : []
                    )
                )
            }
            .monarchElevation(isDragging ? .lg : .none, fill: colors.container, in: shape)
            .padding(.vertical, MonarchGanttTimelineTokens.barInset)
        }
    }
}

/// Makes a bar or a list row open its item: a tap, the button trait and the
/// VoiceOver default action. Nothing without `onOpen`.
struct MonarchGanttTimelineOpenModifier: ViewModifier {
    let item: MonarchGanttTimelineItem
    let onOpen: (@MainActor (MonarchGanttTimelineItem) -> Void)?

    @ViewBuilder func body(content: Content) -> some View {
        if let onOpen {
            content
                .accessibilityAddTraits(.isButton)
                .accessibilityAction { onOpen(item) }
                .onTapGesture { onOpen(item) }
        } else {
            content
        }
    }
}
