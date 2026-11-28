import MonarchTokens
import SwiftUI

/// One event in a calendar view: a colour dot or bar, the title, the time
/// and the status.
///
/// ```swift
/// MonarchEventChip("Stand-up", time: "09:30")
/// MonarchEventChip("Design review", time: "14:00 – 15:00", detail: "Room 4", variant: .bar) {
///     open(event)
/// }
/// ```
///
/// With an action the chip is a button that opens the event; without one it
/// is a label. The event colour is only ever the dot, the bar and a light
/// tint behind the text. Text keeps the theme's text colours, so it stays
/// readable in every theme. A tentative event is hollow or dashed, and a
/// cancelled or declined one is muted with the title struck through.
///
/// A `block` chip fills the frame it is given, so give it the height of the
/// event in the time grid.
///
/// Accessibility: the chip is read as "title, time, status, detail" in one
/// element. The status is left out when confirmed, and the detail in a dot
/// chip. With an action it has the button trait and a touch target of at
/// least 44 points in height. The text scales with Dynamic Type.
public struct MonarchEventChip: View {
    private let title: String
    private let time: String?
    private let timeLabel: String?
    private let detail: String?
    private let color: Color?
    private let status: MonarchEventChipStatus
    private let variant: MonarchEventChipVariant
    private let isPast: Bool
    private let statusLabel: String?
    private let isCompact: Bool
    private let action: (@MainActor () -> Void)?

    /// An event chip.
    ///
    /// - Parameters:
    ///   - title: The event title, already localised.
    ///   - time: The visible time, already formatted: "09:30" in a month
    ///     cell, "09:30 – 10:15" in an agenda. Leave it out for an all-day
    ///     event in an all-day row.
    ///   - timeLabel: The time as VoiceOver reads it when `time` is
    ///     abbreviated, such as "9:30 to 10:15" or "All day". It replaces
    ///     `time` in the spoken name.
    ///   - detail: A second, muted line in bar and block chips: a place, a
    ///     service, an organiser.
    ///   - color: The event's colour, for the dot, the bar and the tint. The
    ///     default is a neutral grey.
    ///   - status: Confirmed, tentative, cancelled or declined.
    ///   - variant: Dot, bar or block.
    ///   - isPast: Whether the event has ended. It is drawn faded.
    ///   - statusLabel: Replaces the built-in name of the status.
    ///   - isCompact: Whether a block chip is one line, time then title, for
    ///     a short event. It hides the status word and the detail.
    ///   - action: Opens the event. Without it the chip is not a button.
    public init(
        _ title: String,
        time: String? = nil,
        timeLabel: String? = nil,
        detail: String? = nil,
        color: Color? = nil,
        status: MonarchEventChipStatus = .confirmed,
        variant: MonarchEventChipVariant = .dot,
        isPast: Bool = false,
        statusLabel: String? = nil,
        isCompact: Bool = false,
        action: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.time = time
        self.timeLabel = timeLabel
        self.detail = detail
        self.color = color
        self.status = status
        self.variant = variant
        self.isPast = isPast
        self.statusLabel = statusLabel
        self.isCompact = isCompact
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        let content = MonarchEventChipContent(
            title: title,
            time: time,
            detail: detail,
            color: color,
            status: status,
            variant: variant,
            isPast: isPast,
            isCompact: isCompact,
            statusLabel: resolvedStatusLabel
        )
        let label = MonarchEventChipTokens.accessibilityLabel(
            title: title,
            time: time,
            timeLabel: timeLabel,
            status: status,
            statusLabel: resolvedStatusLabel,
            detail: detail,
            variant: variant
        )
        if let action {
            Button(action: action) { content }
                .buttonStyle(MonarchEventChipButtonStyle(content: content))
                .accessibilityLabel(label)
        } else {
            content
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        }
    }

    private var resolvedStatusLabel: String {
        statusLabel ?? MonarchEventChipStrings.status(status)
    }
}

/// Redraws the chip for the pressed state and gives it the touch target.
private struct MonarchEventChipButtonStyle: ButtonStyle {
    let content: MonarchEventChipContent

    func makeBody(configuration: Configuration) -> some View {
        var pressed = content
        pressed.isPressed = configuration.isPressed
        return pressed.monarchMinimumHitTarget()
    }
}

#if DEBUG
    #Preview("MonarchEventChip") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchEventChipStatus.allCases, id: \.self) { status in
                    MonarchEventChip("Stand-up", time: "09:30", status: status)
                    MonarchEventChip(
                        "Design review",
                        time: "14:00 – 15:00",
                        detail: "Room 4",
                        status: status,
                        variant: .bar
                    )
                    MonarchEventChip("Workshop", time: "10:00 – 12:00", detail: "Lab", status: status, variant: .block)
                        .frame(height: MonarchTokens.Space.space24)
                }
                MonarchEventChip("Past event", time: "08:00", variant: .bar, isPast: true)
                MonarchEventChip("Short call", time: "16:00", variant: .block, isCompact: true) {}
                    .frame(height: MonarchAccessibility.minimumHitTarget)
            }
        }
    }
#endif
