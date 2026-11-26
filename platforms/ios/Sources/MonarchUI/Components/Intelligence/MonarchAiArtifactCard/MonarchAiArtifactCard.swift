import SwiftUI

/// The compact card a reply shows for an artifact it made, such as
/// "Q3 summary · Document · v3". One button that opens the artifact.
///
/// ```swift
/// MonarchAiArtifactCard("Q3 summary", kind: .document, version: 3, isActive: shown == artifact.id) {
///     shown = artifact.id
/// }
/// ```
///
/// Accessibility: one button whose label reads the title, the kind, the
/// version in words ("version 3"), the description and the action. The
/// artifact on show has the selected trait, reads "Showing" instead of
/// "Open" and has a heavier border, so the state does not depend on colour.
/// The icon and the chevron are decorative; the chevron mirrors in
/// right-to-left layouts. The card is at least 44 points high, and at the
/// accessibility text sizes the action moves under the text.
public struct MonarchAiArtifactCard: View {
    private let title: String
    private let kind: MonarchAiArtifactKind
    private let version: Int
    private let description: String?
    private let isActive: Bool
    private let kindLabel: String?
    private let versionLabel: String?
    private let actionLabel: String?
    private let action: @MainActor () -> Void

    /// An artifact card.
    ///
    /// - Parameters:
    ///   - title: The title of the artifact.
    ///   - kind: What the artifact is, which selects the icon and the kind name.
    ///   - version: The version number.
    ///   - description: Secondary text under the title, such as "Updated just now".
    ///   - isActive: Whether the artifact is the one currently on show.
    ///   - kindLabel: Replaces the built-in name of the kind.
    ///   - versionLabel: Replaces the built-in "v3", and "version 3" for VoiceOver.
    ///   - actionLabel: Replaces the built-in "Open" or "Showing".
    ///   - action: Runs when the card is activated. Open the artifact here.
    public init(
        _ title: String,
        kind: MonarchAiArtifactKind,
        version: Int,
        description: String? = nil,
        isActive: Bool = false,
        kindLabel: String? = nil,
        versionLabel: String? = nil,
        actionLabel: String? = nil,
        action: @escaping @MainActor () -> Void
    ) {
        self.title = title
        self.kind = kind
        self.version = version
        self.description = description
        self.isActive = isActive
        self.kindLabel = kindLabel
        self.versionLabel = versionLabel
        self.actionLabel = actionLabel
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        Button(action: action) {
            MonarchAiArtifactCardLabel(
                title: title,
                symbol: MonarchAiArtifactCardTokens.symbol(kind),
                meta: MonarchAiArtifactCardTokens.meta(
                    kind: kind,
                    version: version,
                    kindLabel: kindLabel,
                    versionLabel: versionLabel
                ),
                description: description,
                actionText: MonarchAiArtifactCardTokens.actionText(isActive: isActive, actionLabel: actionLabel)
            )
        }
        .buttonStyle(MonarchAiArtifactCardStyle(isActive: isActive))
        .accessibilityLabel(
            MonarchAiArtifactCardTokens.accessibilityLabel(
                title: title,
                kind: kind,
                version: version,
                description: description,
                isActive: isActive,
                kindLabel: kindLabel,
                versionLabel: versionLabel,
                actionLabel: actionLabel
            )
        )
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }
}

private struct MonarchAiArtifactCardAppearanceKey: EnvironmentKey {
    static var defaultValue: MonarchAiArtifactCardTokens.Appearance {
        MonarchAiArtifactCardTokens.appearance(isActive: false, isPressed: false, isEnabled: true)
    }
}

extension EnvironmentValues {
    /// The colour roles of the card being drawn, from the button style to its label.
    fileprivate var monarchAiArtifactCardAppearance: MonarchAiArtifactCardTokens.Appearance {
        get { self[MonarchAiArtifactCardAppearanceKey.self] }
        set { self[MonarchAiArtifactCardAppearanceKey.self] = newValue }
    }
}

/// The icon tile, the text and the action of the card.
private struct MonarchAiArtifactCardLabel: View {
    let title: String
    let symbol: String
    let meta: String
    let description: String?
    let actionText: String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.monarchAiArtifactCardAppearance) private var appearance
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var tileSize = MonarchAiArtifactCardTokens.iconTileSize

    var body: some View {
        let layout =
            dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: MonarchAiArtifactCardTokens.contentSpacing))
            : AnyLayout(HStackLayout(spacing: MonarchAiArtifactCardTokens.contentSpacing))
        layout {
            HStack(spacing: MonarchAiArtifactCardTokens.contentSpacing) {
                iconTile
                text
            }
            actionView
        }
    }

    private var iconTile: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchAiArtifactCardTokens.iconTileRadius, style: .continuous)
        return MonarchIcon(systemName: symbol, size: .sm)
            .foregroundStyle(theme.colors[role: appearance.accent])
            .frame(width: tileSize, height: tileSize)
            .background(shape.fill(theme.colors[role: appearance.iconTile]))
    }

    private var text: some View {
        VStack(alignment: .leading, spacing: MonarchAiArtifactCardTokens.textSpacing) {
            Text(title)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors[role: appearance.title])
            Text(meta)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: appearance.secondary])
            if let description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: appearance.secondary])
            }
        }
        .multilineTextAlignment(.leading)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var actionView: some View {
        HStack(spacing: MonarchAiArtifactCardTokens.actionSpacing) {
            Text(actionText).monarchTextStyle(.caption)
            MonarchIcon(systemName: MonarchAiArtifactCardTokens.chevronSymbol, size: .sm)
        }
        .foregroundStyle(theme.colors[role: appearance.accent])
        .fixedSize()
    }
}

/// Draws the card surface for the pressed, active and disabled states.
private struct MonarchAiArtifactCardStyle: ButtonStyle {
    let isActive: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchAiArtifactCardSurface(configuration: configuration, isActive: isActive)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchAiArtifactCardSurface: View {
    let configuration: ButtonStyleConfiguration
    let isActive: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchAiArtifactCardTokens.appearance(
            isActive: isActive,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let shape = RoundedRectangle(cornerRadius: MonarchAiArtifactCardTokens.cornerRadius, style: .continuous)
        configuration.label
            .environment(\.monarchAiArtifactCardAppearance, appearance)
            .padding(.horizontal, MonarchAiArtifactCardTokens.horizontalPadding)
            .padding(.vertical, MonarchAiArtifactCardTokens.verticalPadding)
            .frame(
                maxWidth: MonarchAiArtifactCardTokens.maxWidth,
                minHeight: MonarchAccessibility.minimumHitTarget,
                alignment: .leading
            )
            .background(shape.fill(theme.colors[role: appearance.background]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: appearance.border],
                    lineWidth: MonarchAiArtifactCardTokens.borderWidth(isActive: isActive && isEnabled)
                )
            )
            .contentShape(shape)
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

#if DEBUG
    #Preview("MonarchAiArtifactCard") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiArtifactCard("Q3 summary", kind: .document, version: 3) {}
                MonarchAiArtifactCard(
                    "Revenue by region",
                    kind: .chart,
                    version: 1,
                    description: "Updated just now",
                    isActive: true
                ) {}
                MonarchAiArtifactCard("parser.swift", kind: .code, version: 2) {}.disabled(true)
            }
        }
    }
#endif
