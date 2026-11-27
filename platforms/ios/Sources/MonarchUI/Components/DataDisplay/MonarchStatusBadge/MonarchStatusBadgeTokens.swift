import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and rules for ``MonarchStatusBadge``.
enum MonarchStatusBadgeTokens {
    /// The gap between the icon and the label.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below the label.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The key of the unknown status.
    static let unknownKey = "unknown"

    /// The built-in keys, their tones and their symbols, in order.
    static let builtIn: [(key: String, tone: MonarchTone, symbol: String)] = [
        ("operational", .success, "checkmark.circle"),
        ("degraded", .warning, "exclamationmark.triangle"),
        ("partial", .warning, "circle.righthalf.filled"),
        ("down", .danger, "xmark.circle"),
        ("maintenance", .info, "wrench"),
        ("unknown", .neutral, "questionmark.circle"),
        ("success", .success, "checkmark.circle"),
        ("warning", .warning, "exclamationmark.triangle"),
        ("danger", .danger, "xmark.circle"),
        ("info", .info, "info.circle"),
        ("neutral", .neutral, "circle.fill"),
    ]

    /// The built-in statuses with their localised labels.
    static var builtInDefinitions: [String: MonarchStatusDefinition] {
        Dictionary(
            uniqueKeysWithValues: builtIn.map { entry in
                (
                    entry.key,
                    MonarchStatusDefinition(
                        MonarchStatusBadgeStrings.label(entry.key),
                        tone: entry.tone,
                        systemImage: entry.symbol
                    )
                )
            }
        )
    }

    /// A readable label from a key that is not in any registry:
    /// "pending_delete" gives "Pending delete".
    static func formatLabel(_ key: String) -> String {
        let words = key.replacingOccurrences(of: "[_-]+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespaces)
            .lowercased()
        guard let first = words.first else { return MonarchStatusBadgeStrings.label(unknownKey) }
        return first.uppercased() + words.dropFirst()
    }

    /// Looks a key up in the registries in order, then in the built-in
    /// statuses. An unknown key gets the unknown look and a label made from it.
    static func resolve(_ key: String?, in registries: [MonarchStatusRegistry]) -> MonarchStatusDefinition {
        let builtIn = builtInDefinitions
        let lookup = key ?? unknownKey
        for registry in registries {
            if let found = registry.definitions[lookup] { return found }
        }
        if let found = builtIn[lookup] { return found }
        let unknown = builtIn[unknownKey] ?? MonarchStatusDefinition(lookup, tone: .neutral)
        return MonarchStatusDefinition(
            key.map(formatLabel) ?? unknown.label,
            tone: unknown.tone,
            systemImage: unknown.systemImage
        )
    }

    /// The surface colours of a tone in a capsule variant. The dot variant
    /// has no surface.
    static func appearance(tone: MonarchTone, variant: MonarchStatusBadgeVariant) -> MonarchSurfaceAppearance? {
        switch variant {
        case .subtle: MonarchTonePalette.surface(tone: tone, emphasis: .subtle)
        case .outline: MonarchTonePalette.surface(tone: tone, emphasis: .outline)
        case .solid: MonarchTonePalette.surface(tone: tone, emphasis: .solid)
        case .dot: nil
        }
    }

    /// The SF Symbol drawn before the label, or `nil`. The dot variant shows
    /// its dot instead of the status's symbol, unless the app passes one.
    static func symbol(
        definition: MonarchStatusDefinition,
        override: String?,
        showsIcon: Bool,
        variant: MonarchStatusBadgeVariant
    ) -> String? {
        guard showsIcon else { return nil }
        if let override, !override.isEmpty { return override }
        return variant == .dot ? nil : definition.systemImage
    }

    /// The type role of a size.
    static func textStyle(_ size: MonarchStatusBadgeSize) -> MonarchTextStyle {
        switch size {
        case .sm: .caption
        case .md: .label
        }
    }

    /// Padding before and after the label.
    static func horizontalPadding(_ size: MonarchStatusBadgeSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1_5
        case .md: MonarchTokens.Space.space2
        }
    }

    /// The weight of the label.
    static let fontWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
}
