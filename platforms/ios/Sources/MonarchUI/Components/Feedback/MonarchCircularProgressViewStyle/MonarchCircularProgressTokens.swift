import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchCircularProgressViewStyle``.
enum MonarchCircularProgressTokens {
    /// The diameter of the ring before Dynamic Type is applied.
    static func diameter(_ size: MonarchCircularProgressSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space5
        case .md: MonarchTokens.Space.space8
        case .lg: MonarchTokens.Space.space12
        case .xl: MonarchTokens.Space.space16 + MonarchTokens.Space.space2
        }
    }

    /// The width of the ring before Dynamic Type is applied.
    static func lineWidth(_ size: MonarchCircularProgressSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space0_5
        case .md: MonarchTokens.Space.space1
        case .lg: MonarchTokens.Space.space1_5
        case .xl: MonarchTokens.Space.space1_5
        }
    }

    /// The gap between the ring and the label.
    static let labelSpacing: CGFloat = MonarchTokens.Space.space2
    /// The track role.
    static var track: MonarchColorRole { \.bgMuted }
    /// The type role of the value in the middle of the ring.
    static let valueStyle: MonarchTextStyle = .caption
    /// How far the value text may shrink to stay inside the ring.
    static let valueMinimumScale: CGFloat = 0.5
    /// Where the arc starts: the top of the ring, a quarter turn back from
    /// the angle SwiftUI starts at.
    static let startAngle = Angle.degrees(-90)
    /// The part of the ring the indeterminate arc covers.
    static let indeterminateArc: CGFloat = 0.25
    /// A whole turn of the indeterminate arc.
    static let fullTurn = Angle.degrees(360)
    /// How long the indeterminate arc takes to go round, in seconds.
    static let indeterminateDuration: TimeInterval = TimeInterval(MonarchTokens.Duration.shimmer) / 1000

    /// The colour role of the arc: the solid colour of the tone. No tone is
    /// the accent tone, so `nil` and `.accent` draw the same arc.
    static func fill(tone: MonarchTone?) -> MonarchColorRole {
        MonarchTonePalette.palette(for: tone ?? .accent).solid
    }

    /// A fraction limited to 0 through 1. Not-a-number counts as 0.
    static func clamped(_ fraction: Double) -> Double {
        guard !fraction.isNaN else { return 0 }
        return min(max(fraction, 0), 1)
    }

    /// Whether the value is drawn in the middle of the ring: only when
    /// asked for, on the lg and xl rings, with a known fraction.
    static func showsValue(_ showsValue: Bool, size: MonarchCircularProgressSize, fraction: Double?) -> Bool {
        showsValue && (size == .lg || size == .xl) && fraction != nil
    }

    /// The fraction as a whole percentage in a locale, or an empty string
    /// for indeterminate progress.
    static func valueText(_ fraction: Double?, locale: Locale = .autoupdatingCurrent) -> String {
        guard let fraction else { return "" }
        return clamped(fraction).formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }
}
