import SwiftUI

private typealias Tokens = MonarchWeeklyHoursEditorTokens

/// One day of a ``MonarchWeeklyHoursEditor``: the day's switch (or its name and hours as text
/// when read-only), its intervals, its problem and its add and copy buttons, read by VoiceOver
/// as one container named by the weekday.
struct MonarchWeeklyHoursEditorDay: View {
    let name: String
    let ranges: [MonarchWeeklyHoursInterval]
    let error: MonarchWeeklyHoursError?
    let step: Int
    let maxIntervals: Int
    let isReadOnly: Bool
    let formats: MonarchWeeklyHoursEditorFormats
    let text: MonarchWeeklyHoursEditorText
    let onToggle: @MainActor (Bool) -> Void
    let onRangesChange: @MainActor ([MonarchWeeklyHoursInterval]) -> Void
    let onAdd: @MainActor () -> Void
    let onCopy: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    private var isOn: Bool { !ranges.isEmpty }

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.controlGap) {
            if isReadOnly {
                readOnly
            } else {
                daySwitch
                ForEach(Array(ranges.enumerated()), id: \.offset) { index, range in
                    intervalRow(range, index: index)
                }
            }
            if let error, isOn {
                MonarchInlineNotice(text.error(error), tone: .danger, variant: .plain)
            }
            if !isReadOnly, isOn {
                actions
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(name)
    }

    private var readOnly: some View {
        VStack(alignment: .leading, spacing: Tokens.textGap) {
            Text(name)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            muted(text.summary(ranges, formats: formats), style: .body)
        }
        .accessibilityElement(children: .combine)
    }

    private var daySwitch: some View {
        HStack(spacing: Tokens.controlGap) {
            Toggle(name, isOn: Binding(get: { isOn }, set: { onToggle($0) }))
                .labelsHidden()
                .fixedSize()
            Text(name)
                .monarchTextStyle(.body, weight: .medium)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .frame(minWidth: Tokens.nameWidth, alignment: .leading)
            if !isOn {
                muted(text.unavailable, style: .body)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
        }
        .monarchMinimumHitTarget()
        .accessibilityElement(children: .contain)
    }

    private func intervalRow(_ range: MonarchWeeklyHoursInterval, index: Int) -> some View {
        let start = picker(
            text.start,
            options: Tokens.startOptions(step: step, current: range.start),
            selection: Binding(
                get: { range.start },
                set: { onChange(index, MonarchWeeklyHoursInterval(day: range.day, start: $0, end: range.end)) }
            )
        )
        let end = picker(
            text.end,
            options: Tokens.endOptions(step: step, current: range.end),
            selection: Binding(
                get: { range.end },
                set: { onChange(index, MonarchWeeklyHoursInterval(day: range.day, start: range.start, end: $0)) }
            )
        )
        let dash = Text(verbatim: Tokens.separator)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors[role: Tokens.muted])
            .accessibilityHidden(true)
        let remove = Button(text.remove, systemImage: Tokens.removeSymbol, role: .destructive) {
            onRangesChange(ranges.enumerated().filter { $0.offset != index }.map(\.element))
        }
        .labelStyle(.iconOnly)
        .buttonStyle(.monarch(.ghost, size: .sm))
        .accessibilityLabel(text.removeItem(name, index + 1))
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: Tokens.controlGap) {
                start
                dash
                end
                Spacer(minLength: 0)
                remove
            }
            VStack(alignment: .leading, spacing: Tokens.controlGap) {
                start
                end
                remove
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(text.interval(name, index + 1))
    }

    private func picker(_ title: String, options: [Int], selection: Binding<Int>) -> some View {
        MonarchSelectField(
            title,
            selection: selection,
            valueText: formats.time(selection.wrappedValue),
            style: .compact
        ) {
            ForEach(options, id: \.self) { minutes in
                Text(formats.time(minutes)).tag(minutes)
            }
        }
    }

    private var actions: some View {
        let add = Button(text.add, systemImage: Tokens.addSymbol) { onAdd() }
            .labelStyle(.iconOnly)
            .buttonStyle(.monarch(.ghost, size: .sm))
            .accessibilityLabel(text.addItem(name))
        let copy = Button(text.copyToAll, systemImage: Tokens.copySymbol) { onCopy() }
            .buttonStyle(.monarch(.ghost, size: .sm))
            .accessibilityLabel(text.copyToAllItem(name))
        let canAdd = Tokens.canAdd(count: ranges.count, maxIntervals: maxIntervals)
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: Tokens.controlGap) {
                if canAdd { add }
                copy
            }
            VStack(alignment: .leading, spacing: Tokens.controlGap) {
                if canAdd { add }
                copy
            }
        }
    }

    private func onChange(_ index: Int, _ interval: MonarchWeeklyHoursInterval) {
        var next = ranges
        guard next.indices.contains(index) else { return }
        next[index] = interval
        onRangesChange(next)
    }

    private func muted(_ string: String, style: MonarchTextStyle) -> some View {
        Text(string)
            .monarchTextStyle(style)
            .foregroundStyle(isEnabled ? theme.colors[role: Tokens.muted] : theme.colors.textDisabled)
    }
}
