import SwiftUI

/// The release stage of something: planned, preview, stable or deprecated,
/// or a stage of your own.
///
/// ```swift
/// MonarchLifecycleBadge(.preview)
/// MonarchLifecycleBadge(stage: "early_access", tone: .info)
/// ```
///
/// A quiet capsule with a coloured dot and always a text label. The built-in
/// stages have a label and a tone: planned is neutral, preview is info,
/// stable is success and deprecated is warning.
///
/// Accessibility: the badge is read as its label. The dot is decorative, so
/// the label carries the meaning without the colour. The text scales with
/// Dynamic Type.
public struct MonarchLifecycleBadge: View {
    private let key: String
    private let label: String?
    private let tone: MonarchTone?
    private let showsDot: Bool

    @ScaledMetric(relativeTo: .caption) private var dotSize = MonarchLifecycleBadgeTokens.dotSize
    @Environment(\.monarchTheme) private var theme

    /// A badge for a built-in stage.
    ///
    /// - Parameters:
    ///   - lifecycle: The stage.
    ///   - label: Replaces the built-in label of the stage. Already localised.
    ///   - tone: Replaces the tone of the stage.
    ///   - showsDot: Whether the dot before the label is drawn.
    public init(
        _ lifecycle: MonarchLifecycle,
        label: String? = nil,
        tone: MonarchTone? = nil,
        showsDot: Bool = true
    ) {
        self.key = lifecycle.rawValue
        self.label = label
        self.tone = tone
        self.showsDot = showsDot
    }

    /// A badge for a stage named by a key, such as a value from a server.
    ///
    /// A key that is one of `planned`, `preview`, `stable` and `deprecated`
    /// is drawn as that built-in stage. Any other key is drawn with `label`
    /// and `tone`; without a label it is shown as the key with underscores
    /// and hyphens turned into spaces and a capital first letter, so a stage
    /// that the app does not know still shows something.
    ///
    /// - Parameters:
    ///   - stage: The key of the stage.
    ///   - label: The visible text, already localised.
    ///   - tone: The tone. Without one, a built-in stage keeps its tone and
    ///     any other stage is neutral.
    ///   - showsDot: Whether the dot before the label is drawn.
    public init(
        stage: String,
        label: String? = nil,
        tone: MonarchTone? = nil,
        showsDot: Bool = true
    ) {
        self.key = stage
        self.label = label
        self.tone = tone
        self.showsDot = showsDot
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchLifecycleBadgeTokens.appearance(
            tone: tone ?? MonarchLifecycleBadgeTokens.tone(forKey: key)
        )
        HStack(spacing: MonarchLifecycleBadgeTokens.dotSpacing) {
            if showsDot {
                Circle()
                    .fill(theme.colors[role: appearance.dot])
                    .frame(width: dotSize, height: dotSize)
                    .accessibilityHidden(true)
            }
            Text(label ?? MonarchLifecycleBadgeTokens.label(forKey: key))
                .monarchTextStyle(
                    MonarchLifecycleBadgeTokens.textStyle,
                    weight: MonarchLifecycleBadgeTokens.fontWeight
                )
        }
        .padding(.horizontal, MonarchLifecycleBadgeTokens.horizontalPadding)
        .padding(.vertical, MonarchLifecycleBadgeTokens.verticalPadding)
        .monarchSurface(appearance.surface, in: Capsule())
        .accessibilityElement(children: .combine)
    }
}

#if DEBUG
    #Preview("MonarchLifecycleBadge") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchLifecycle.allCases, id: \.self) { lifecycle in
                    MonarchLifecycleBadge(lifecycle)
                }
                MonarchLifecycleBadge(stage: "early_access", tone: .info)
                MonarchLifecycleBadge(.stable, label: "Generally available", showsDot: false)
            }
        }
    }
#endif
