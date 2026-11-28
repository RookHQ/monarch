import MonarchTokens
import MonarchUI
import SwiftUI

/// A sample of the semantic colour roles.
struct ColourPage: View {
    @Environment(\.monarchTheme) private var theme

    private var roles: [(name: String, role: MonarchColorRole)] {
        [
            ("bgCanvas", \.bgCanvas), ("bgSurface", \.bgSurface), ("bgMuted", \.bgMuted),
            ("textDefault", \.textDefault), ("textMuted", \.textMuted), ("borderDefault", \.borderDefault),
            ("accentSolid", \.accentSolid), ("accentSubtle", \.accentSubtle), ("accentStrong", \.accentStrong),
            ("infoSolid", \.infoSolid), ("successSolid", \.successSolid), ("warningSolid", \.warningSolid),
            ("dangerSolid", \.dangerSolid), ("focusRing", \.focusRing),
        ]
    }

    private static let aiRoles: [(name: String, role: MonarchColorRole)] = [
        ("aiSolid", \.aiSolid), ("aiStrong", \.aiStrong), ("aiSubtle", \.aiSubtle),
    ]

    /// A custom AI colour, derived with the same contrast guarantees as the accent.
    private static let customAI = MonarchBrandTheme.derive(brand: "#0a7cff", ai: "#7c3aed").aiAccent

    var body: some View {
        ForEach(roles, id: \.name) { item in
            Swatch(name: item.name, role: item.role)
        }
        Text("Custom AI colour (withAI)").monarchTextStyle(.label)
        if let ai = Self.customAI {
            ForEach(Self.aiRoles, id: \.name) { item in
                Swatch(name: item.name, role: item.role)
            }
            .environment(\.monarchTheme, MonarchTheme(tokens: theme.tokens.withAI(ai), typography: theme.typography))
        }
    }
}

private struct Swatch: View {
    @Environment(\.monarchTheme) private var theme
    let name: String
    let role: MonarchColorRole

    var body: some View {
        HStack(spacing: theme.spacing.space3) {
            RoundedRectangle(cornerRadius: theme.radius.sm)
                .fill(theme.colors[role: role])
                .overlay(
                    RoundedRectangle(cornerRadius: theme.radius.sm)
                        .strokeBorder(theme.colors.borderDefault, lineWidth: theme.spacing.px)
                )
                .frame(width: theme.spacing.space8, height: theme.spacing.space8)
                .accessibilityHidden(true)
            Text(name).monarchTextStyle(.label)
        }
    }
}
