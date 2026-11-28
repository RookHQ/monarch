import MonarchTokens
import SwiftUI

/// The fill of an event chip: a theme colour, the event colour mixed over
/// it, and an outline in the event colour.
struct MonarchEventChipSurface: Equatable {
    /// The theme colour under the tint, or `nil` for no fill.
    let base: MonarchColorRole?
    /// How much of the event colour is laid over the base, from 0 to 1.
    let tint: Double
    /// Whether the chip has a border in the event colour.
    let isOutlined: Bool
}

/// How the dot or the bar of an event chip is drawn.
enum MonarchEventChipMarker: Equatable {
    /// Filled with the event colour.
    case filled
    /// A ring in the event colour (the dot of a tentative event).
    case hollow
    /// A dashed line in the event colour (the bar of a tentative event).
    case dashed
    /// The strong border colour instead of the event colour: a ring for a
    /// dot, a solid bar otherwise (cancelled and declined events).
    case muted
}

/// Component tokens and pure logic for ``MonarchEventChip``.
enum MonarchEventChipTokens {
    /// The gap between the dot, the time and the title of a dot chip.
    static let dotSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between the parts on one line of a bar chip.
    static let barSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the time and the title of a compact block chip.
    static let compactSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below a dot chip.
    static let dotVerticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// Padding before and after a dot chip.
    static let dotHorizontalPadding: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below a bar or block chip.
    static let tintedVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// Padding after the content of a bar or block chip.
    static let tintedTrailingPadding: CGFloat = MonarchTokens.Space.space2
    /// The width of the colour bar.
    static let barWidth: CGFloat = MonarchTokens.Space.space1 - MonarchTokens.Space.px
    /// Padding before the content of a bar or block chip: past the bar.
    static let tintedLeadingPadding: CGFloat = MonarchTokens.Space.space2 + barWidth
    /// The diameter of the dot before Dynamic Type.
    static let dotSize: CGFloat = MonarchTokens.Space.space2
    /// The width of the ring of a hollow dot.
    static let dotStroke: CGFloat = MonarchTokens.Space.space0_5
    /// The dash and the gap of a tentative bar.
    static let barDash: [CGFloat] = [MonarchTokens.Space.space1, barWidth]
    /// The share of the event colour laid over the surface.
    static let tintRatio: Double = 0.14
    /// The share of the event colour while the chip is pressed.
    static let pressedTintRatio: Double = 0.22
    /// The share of the event colour for a tentative event.
    static let tentativeTintRatio: Double = 0.06
    /// The opacity of the marker of an event that has ended.
    static let pastMarkerRatio: Double = 0.55
    /// The type role of every line.
    static let textStyle: MonarchTextStyle = .caption
    /// The weight of the title, and of the status word.
    static let titleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// The weight of the title of a block chip.
    static let blockTitleWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The colour role of the time, the status word and the detail.
    static var secondaryText: MonarchColorRole { \.textMuted }
    /// The colour role of the marker when it does not use the event colour.
    static var mutedMarker: MonarchColorRole { \.borderStrong }
    /// The colour role of the marker when the app gives no event colour.
    static var defaultColor: MonarchColorRole { \.textMuted }

    /// The shape of a chip: a 4-point corner for a dot chip, 6 points for
    /// the tinted ones.
    static func shape(_ variant: MonarchEventChipVariant) -> RoundedRectangle {
        let radius = variant == .dot ? MonarchTokens.Radius.xs : MonarchTokens.Radius.sm
        return RoundedRectangle(cornerRadius: radius, style: .continuous)
    }

    /// Whether the event did not or will not take place for this person.
    static func isInactive(_ status: MonarchEventChipStatus) -> Bool {
        status == .cancelled || status == .declined
    }

    /// The colour role of the title: muted for cancelled, declined and past
    /// events.
    static func titleRole(status: MonarchEventChipStatus, isPast: Bool) -> MonarchColorRole {
        isInactive(status) || isPast ? \.textMuted : \.textDefault
    }

    /// Whether the title is struck through.
    static func isStruckThrough(_ status: MonarchEventChipStatus) -> Bool {
        isInactive(status)
    }

    /// How the dot or bar is drawn for a status.
    static func marker(status: MonarchEventChipStatus, variant: MonarchEventChipVariant) -> MonarchEventChipMarker {
        switch status {
        case .confirmed: .filled
        case .tentative: variant == .dot ? .hollow : .dashed
        case .cancelled, .declined: .muted
        }
    }

    /// The fill of the chip for a variant, a status and the pressed state.
    static func surface(
        variant: MonarchEventChipVariant,
        status: MonarchEventChipStatus,
        isPressed: Bool
    ) -> MonarchEventChipSurface {
        if variant == .dot {
            return MonarchEventChipSurface(base: isPressed ? \.bgMuted : nil, tint: 0, isOutlined: false)
        }
        switch status {
        case .confirmed:
            return MonarchEventChipSurface(
                base: \.bgSurface,
                tint: isPressed ? pressedTintRatio : tintRatio,
                isOutlined: false
            )
        case .tentative:
            return MonarchEventChipSurface(
                base: \.bgSurface,
                tint: isPressed ? tintRatio : tentativeTintRatio,
                isOutlined: true
            )
        case .cancelled, .declined:
            return MonarchEventChipSurface(base: isPressed ? \.bgEmphasis : \.bgMuted, tint: 0, isOutlined: false)
        }
    }

    /// Whether the status word is drawn: in bar and regular block chips, for
    /// every status but confirmed. A dot chip only speaks it.
    static func showsStatus(status: MonarchEventChipStatus, variant: MonarchEventChipVariant, isCompact: Bool) -> Bool {
        guard status != .confirmed else { return false }
        switch variant {
        case .dot: return false
        case .bar: return true
        case .block: return !isCompact
        }
    }

    /// Whether the detail line is drawn: in bar and regular block chips.
    static func showsDetail(_ detail: String?, variant: MonarchEventChipVariant, isCompact: Bool) -> Bool {
        guard let detail, !detail.isEmpty else { return false }
        switch variant {
        case .dot: return false
        case .bar: return true
        case .block: return !isCompact
        }
    }

    /// The spoken name: the title, the time (the long form when there is
    /// one), the status unless confirmed, and the detail unless it is a dot
    /// chip, separated by commas.
    static func accessibilityLabel(
        title: String,
        time: String?,
        timeLabel: String?,
        status: MonarchEventChipStatus,
        statusLabel: String,
        detail: String?,
        variant: MonarchEventChipVariant
    ) -> String {
        let parts: [String?] = [
            title,
            timeLabel ?? time,
            status == .confirmed ? nil : statusLabel,
            variant == .dot ? nil : detail,
        ]
        return parts.compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
