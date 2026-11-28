import MonarchTokens
import SwiftUI

/// The colour roles of a lifecycle badge: the capsule and the dot.
struct MonarchLifecycleBadgeAppearance: Equatable {
    /// The fill, text and border of the capsule.
    let surface: MonarchSurfaceAppearance
    /// The colour of the dot.
    let dot: MonarchColorRole
}

/// Component tokens and pure logic for ``MonarchLifecycleBadge``.
enum MonarchLifecycleBadgeTokens {
    /// Padding before and after the content.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding above and below the content.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the dot and the label.
    static let dotSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The diameter of the dot before Dynamic Type.
    static let dotSize: CGFloat = MonarchTokens.Space.space1_5
    /// The type role of the label.
    static let textStyle: MonarchTextStyle = .caption
    /// The weight of the label.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)

    /// The tone of a built-in stage.
    static func tone(for lifecycle: MonarchLifecycle) -> MonarchTone {
        switch lifecycle {
        case .planned: .neutral
        case .preview: .info
        case .stable: .success
        case .deprecated: .warning
        }
    }

    /// The tone of a stage key: the built-in stage's tone, or neutral.
    static func tone(forKey key: String) -> MonarchTone {
        MonarchLifecycle(rawValue: key).map(tone(for:)) ?? .neutral
    }

    /// The label of a stage key: the built-in label of a built-in stage, or
    /// the key made readable.
    static func label(forKey key: String) -> String {
        if let lifecycle = MonarchLifecycle(rawValue: key) {
            return MonarchLifecycleBadgeStrings.label(lifecycle)
        }
        return readable(key)
    }

    /// A key with runs of underscores and hyphens turned into one space,
    /// trimmed, and with a capital first letter: `early_access` becomes
    /// "Early access".
    static func readable(_ key: String) -> String {
        let words = key.split(whereSeparator: { $0 == "_" || $0 == "-" || $0.isWhitespace })
        let text = words.joined(separator: " ")
        return text.prefix(1).uppercased() + text.dropFirst()
    }

    /// The colour roles for a tone: a surface-coloured capsule with default
    /// text, the tone's border, and a dot in the tone's strong colour. The
    /// neutral dot is the subtle text colour.
    static func appearance(tone: MonarchTone) -> MonarchLifecycleBadgeAppearance {
        let palette = MonarchTonePalette.palette(for: tone)
        return MonarchLifecycleBadgeAppearance(
            surface: MonarchSurfaceAppearance(
                background: \.bgSurface,
                foreground: \.textDefault,
                border: palette.border
            ),
            dot: tone == .neutral ? \.textSubtle : palette.strong
        )
    }
}
