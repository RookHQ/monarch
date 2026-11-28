import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchToast`` and its host.
enum MonarchToastTokens {
    /// Padding before and after the content of a toast.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below the content of a toast.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon, the text and the buttons.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the message.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between toasts.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The margin between the toasts and the edges of the screen.
    static let screenMargin: CGFloat = MonarchTokens.Space.space4
    /// The widest a toast gets, on a wide screen.
    static let maxWidth: CGFloat = MonarchTokens.Breakpoint.sm
    /// How far a toast must be dragged towards its edge to close.
    static let swipeDistance: CGFloat = MonarchTokens.Space.space6
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The fill role of a toast.
    static var background: MonarchColorRole { \.bgRaised }
    /// The border role of a toast.
    static var border: MonarchColorRole { \.borderDefault }
    /// The SF Symbol of the dismiss button.
    static let dismissSymbol = "xmark"
    /// Seconds a toast stays when it has no action and is not an error.
    static let shortDuration: TimeInterval = 5
    /// Seconds a danger toast or a toast with an action stays.
    static let longDuration: TimeInterval = 10

    /// The colour role of the icon of a tone.
    static func iconRole(tone: MonarchTone) -> MonarchColorRole {
        MonarchTonePalette.palette(for: tone).strong
    }

    /// The width of the stripe on the leading edge.
    static let accentWidth: CGFloat = MonarchTokens.Space.space1

    /// The colour role of the leading stripe: the tone's strong colour, the strong border for neutral.
    static func accentRole(tone: MonarchTone) -> MonarchColorRole {
        tone == .neutral ? \.borderStrong : MonarchTonePalette.palette(for: tone).strong
    }

    /// The SF Symbol of a tone.
    static func symbol(tone: MonarchTone) -> String {
        MonarchTonePalette.palette(for: tone).symbol
    }

    /// Seconds before a toast closes itself, or `nil` when it stays until
    /// dismissed. The scale multiplies every duration; an infinite or
    /// not-a-number result means the toast stays.
    static func lifetime(of toast: MonarchToast, scale: Double) -> TimeInterval? {
        let base = toast.duration ?? (toast.tone == .danger || toast.hasAction ? longDuration : shortDuration)
        let scaled = base * scale
        guard scaled.isFinite else { return nil }
        return max(0, scaled)
    }

    /// The most toasts shown at once: at least one.
    static func limit(_ requested: Int) -> Int {
        max(1, requested)
    }

    /// The toasts that remain after one is added: the oldest close first
    /// when there are more than the limit.
    static func adding(_ toast: MonarchToast, to toasts: [MonarchToast], limit: Int) -> [MonarchToast] {
        Array((toasts + [toast]).suffix(self.limit(limit)))
    }

    /// What is announced when a toast appears: the tone name, the title,
    /// then the message.
    static func announcement(for toast: MonarchToast) -> String {
        [MonarchToastStrings.toneName(toast.tone), toast.title, toast.message]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }

    /// Whether an announcement interrupts speech: only for the danger tone.
    static func isUrgent(tone: MonarchTone) -> Bool {
        tone == .danger
    }

    /// Whether a drag closes a toast: far enough towards the edge the
    /// toasts sit on.
    static func dragDismisses(translation: CGFloat, position: MonarchToastPosition) -> Bool {
        switch position {
        case .bottom: translation >= swipeDistance
        case .top: translation <= -swipeDistance
        }
    }
}

extension MonarchToastStrings {
    /// The spoken name of a tone, from the shared tone strings.
    static func toneName(_ tone: MonarchTone) -> String? {
        MonarchToneStrings.name(tone)
    }
}
