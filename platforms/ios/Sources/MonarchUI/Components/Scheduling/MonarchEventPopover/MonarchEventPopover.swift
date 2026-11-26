import MonarchTokens
import SwiftUI

/// A quick view of a calendar event: its colour, title, status, date and
/// time in a stated zone, labelled details, a note and actions.
///
/// ```swift
/// MonarchEventChip("Design review", time: "14:00", variant: .bar) { isShown = true }
///     .popover(isPresented: $isShown) {
///         MonarchEventPopover("Design review", start: start, end: end, status: .tentative) {
///             MonarchButton("Open", variant: .primary, size: .sm) { open() }
///         }
///         .presentationCompactAdaptation(.popover)
///     }
/// ```
///
/// This is the popover's content. Present it with `.popover` from the
/// event's chip, which is a popover with an arrow on iPad and, with
/// `.presentationCompactAdaptation(.popover)`, on iPhone too; or with
/// `.sheet` and a medium detent where a sheet suits the screen better. The
/// system then handles dismissal and returns VoiceOver focus to the chip.
///
/// Times are formatted with the environment's locale, so they follow the
/// person's 12- or 24-hour setting, in `timeZone` or the environment's zone.
///
/// Accessibility: the title has the header trait and the colour swatch is
/// decorative. The status is a badge with its name in words. The time row
/// is read as "Time", then the date and time; each detail as its label and
/// value, including rows that show an icon instead of the label. Cancelled
/// and declined titles are struck through and the status is also named.
/// Actions are separate buttons. Text wraps and scales with Dynamic Type.
public struct MonarchEventPopover<Actions: View>: View {
    private let title: String
    private let subtitle: String?
    private let start: Date?
    private let end: Date?
    private let isAllDay: Bool
    private let color: Color?
    private let status: MonarchEventChipStatus?
    private let details: [MonarchEventPopoverDetail]
    private let note: String?
    private let timeZone: TimeZone?
    private let labels: MonarchEventPopoverLabels
    private let actions: Actions
    private let hasActions: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var environmentTimeZone
    @ScaledMetric(relativeTo: .headline) private var swatchHeight = MonarchTokens.FontSize.lg

    /// An event popover with actions.
    ///
    /// - Parameters:
    ///   - title: The event title, already localised.
    ///   - subtitle: A second line, such as the calendar or the service.
    ///   - start: When the event starts. Without it there is no time row.
    ///   - end: When the event ends; for an all-day event, its last day.
    ///   - isAllDay: Whether the event lasts whole days.
    ///   - color: The event's colour for the swatch. The default is a neutral grey.
    ///   - status: Confirmed, tentative, cancelled or declined, shown as a badge.
    ///   - details: Labelled rows under the time: hosts, location, reference.
    ///   - note: A sentence under the details, such as a cancellation reason.
    ///   - timeZone: The zone for the times. The default is the environment's.
    ///   - labels: Replacements for the built-in strings.
    ///   - actions: Buttons at the bottom, such as Open, Reschedule or Cancel.
    public init(
        _ title: String,
        subtitle: String? = nil,
        start: Date? = nil,
        end: Date? = nil,
        isAllDay: Bool = false,
        color: Color? = nil,
        status: MonarchEventChipStatus? = nil,
        details: [MonarchEventPopoverDetail] = [],
        note: String? = nil,
        timeZone: TimeZone? = nil,
        labels: MonarchEventPopoverLabels = MonarchEventPopoverLabels(),
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.subtitle = subtitle
        self.start = start
        self.end = end
        self.isAllDay = isAllDay
        self.color = color
        self.status = status
        self.details = details
        self.note = note
        self.timeZone = timeZone
        self.labels = labels
        self.actions = actions()
        self.hasActions = Actions.self != EmptyView.self
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchEventPopoverTokens.sectionSpacing) {
            head
            if start != nil || !details.isEmpty {
                fullWidthDivider
                VStack(alignment: .leading, spacing: MonarchEventPopoverTokens.rowSpacing) {
                    timeRow
                    detailRows
                }
            }
            if let note {
                Text(note)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(MonarchEventPopoverTokens.notePadding)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.colors.bgSubtle, in: MonarchShapes.control)
            }
            if hasActions {
                fullWidthDivider
                MonarchFlowLayout {
                    actions
                }
            }
        }
        .padding(MonarchEventPopoverTokens.padding)
        .frame(idealWidth: MonarchEventPopoverTokens.idealWidth, alignment: .leading)
        .accessibilityElement(children: .contain)
    }

    /// A rule across the whole popover, as the web draws between its head, its facts and its actions.
    private var fullWidthDivider: some View {
        MonarchDivider()
            .padding(.horizontal, -MonarchEventPopoverTokens.padding)
    }

    /// The labelled rows with their values lined up after the longest label, as on the web.
    private var detailRows: some View {
        Grid(
            alignment: .topLeading,
            horizontalSpacing: MonarchEventPopoverTokens.inlineSpacing * 2,
            verticalSpacing: MonarchEventPopoverTokens.rowSpacing
        ) {
            ForEach(Array(details.enumerated()), id: \.offset) { _, detail in
                if let systemImage = detail.systemImage {
                    GridRow {
                        MonarchIcon(systemName: systemImage, size: .sm)
                            .foregroundStyle(theme.colors.textMuted)
                        Text(detail.value)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textDefault)
                            .fixedSize(horizontal: false, vertical: true)
                            .gridColumnAlignment(.leading)
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(detail.label)
                    .accessibilityValue(detail.value)
                } else {
                    GridRow {
                        Text(detail.label)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textMuted)
                        Text(detail.value)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textDefault)
                            .fixedSize(horizontal: false, vertical: true)
                            .gridColumnAlignment(.leading)
                    }
                    .accessibilityElement(children: .combine)
                }
            }
        }
    }

    private var head: some View {
        HStack(alignment: .top, spacing: MonarchEventPopoverTokens.inlineSpacing) {
            RoundedRectangle(cornerRadius: MonarchEventPopoverTokens.swatchRadius, style: .continuous)
                .fill(color ?? theme.colors[role: MonarchEventPopoverTokens.defaultSwatchRole])
                .frame(width: MonarchEventPopoverTokens.swatchWidth, height: swatchHeight)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchEventPopoverTokens.titleSpacing) {
                Text(title)
                    .monarchTextStyle(.subheading)
                    .strikethrough(MonarchEventPopoverTokens.isStruckThrough(status))
                    .foregroundStyle(theme.colors.textDefault)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)
                if let subtitle {
                    Text(subtitle)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let status {
                MonarchBadge(
                    MonarchEventPopoverTokens.statusName(status, labels: labels),
                    tone: MonarchEventPopoverTokens.tone(status)
                )
            }
        }
    }

    @ViewBuilder
    private var timeRow: some View {
        if let start {
            let when = MonarchEventPopoverTokens.when(
                start: start,
                end: end,
                isAllDay: isAllDay,
                timeZone: timeZone ?? environmentTimeZone,
                locale: locale,
                allDayLabel: MonarchEventPopoverTokens.allDayLabel(labels: labels)
            )
            let spoken = MonarchEventPopoverTokens.timeAccessibility(when, labels: labels)
            HStack(alignment: .firstTextBaseline, spacing: MonarchEventPopoverTokens.inlineSpacing) {
                MonarchIcon(systemName: "clock", size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
                (Text(when.date).foregroundStyle(theme.colors.textDefault)
                    + Text(verbatim: " · " + when.time).foregroundStyle(theme.colors.textMuted))
                    .monarchTextStyle(.body)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(spoken.label)
            .accessibilityValue(spoken.value)
        }
    }
}

extension MonarchEventPopover where Actions == EmptyView {
    /// An event popover without actions.
    ///
    /// - Parameters:
    ///   - title: The event title, already localised.
    ///   - subtitle: A second line, such as the calendar or the service.
    ///   - start: When the event starts. Without it there is no time row.
    ///   - end: When the event ends; for an all-day event, its last day.
    ///   - isAllDay: Whether the event lasts whole days.
    ///   - color: The event's colour for the swatch. The default is a neutral grey.
    ///   - status: Confirmed, tentative, cancelled or declined, shown as a badge.
    ///   - details: Labelled rows under the time: hosts, location, reference.
    ///   - note: A sentence under the details, such as a cancellation reason.
    ///   - timeZone: The zone for the times. The default is the environment's.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ title: String,
        subtitle: String? = nil,
        start: Date? = nil,
        end: Date? = nil,
        isAllDay: Bool = false,
        color: Color? = nil,
        status: MonarchEventChipStatus? = nil,
        details: [MonarchEventPopoverDetail] = [],
        note: String? = nil,
        timeZone: TimeZone? = nil,
        labels: MonarchEventPopoverLabels = MonarchEventPopoverLabels()
    ) {
        self.init(
            title,
            subtitle: subtitle,
            start: start,
            end: end,
            isAllDay: isAllDay,
            color: color,
            status: status,
            details: details,
            note: note,
            timeZone: timeZone,
            labels: labels,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchEventPopover") {
        let start = Date(timeIntervalSince1970: 1_794_666_600)
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchEventPopover(
                    "Design review",
                    subtitle: "Product calendar",
                    start: start,
                    end: start.addingTimeInterval(2_700),
                    status: .tentative,
                    details: [
                        MonarchEventPopoverDetail("Location", value: "Room 4", systemImage: "mappin.and.ellipse"),
                        MonarchEventPopoverDetail("Hosts", value: "Ana Silva, Jonas Weber"),
                    ],
                    note: "Bring the Q3 numbers."
                ) {
                    MonarchButton("Open", variant: .primary, size: .sm) {}
                    MonarchButton("Reschedule", size: .sm) {}
                }
                MonarchEventPopover("Offsite", start: start, isAllDay: true, status: .cancelled)
            }
        }
    }
#endif
