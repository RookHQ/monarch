import SwiftUI

/// A collapsible section of a document: a summary line with a chevron
/// before it that shows or hides the content under it.
///
/// ```swift
/// MonarchToggleBlock("Implementation notes") {
///     Text("The importer runs nightly.")
/// }
/// ```
///
/// It is a standard `DisclosureGroup` drawn as a document block: the
/// chevron leads, the summary is strong body text and the content is
/// indented under the summary. For a settings section with the chevron at
/// the end, use `DisclosureGroup` with the ``MonarchDisclosureGroupStyle``.
///
/// Accessibility: the summary line is one button whose label is the summary
/// and whose value is "Expanded" or "Collapsed". The chevron is decorative
/// and points the right way in right-to-left layouts. Hidden content is
/// removed, so VoiceOver does not reach it. The line is at least 44 points
/// high and the content appears without animation when Reduce Motion is on.
public struct MonarchToggleBlock<Content: View>: View {
    private let summary: String
    private let isExpanded: Binding<Bool>?
    private let expandedLabel: String?
    private let collapsedLabel: String?
    private let content: Content

    @State private var isExpandedInternally: Bool

    /// A toggle block that keeps its own state.
    ///
    /// - Parameters:
    ///   - summary: The line that stays visible, already localised.
    ///   - initiallyExpanded: Whether the content shows at first.
    ///   - expandedLabel: Replaces the built-in "Expanded" spoken value.
    ///   - collapsedLabel: Replaces the built-in "Collapsed" spoken value.
    ///   - content: The content that shows and hides.
    public init(
        _ summary: String,
        initiallyExpanded: Bool = false,
        expandedLabel: String? = nil,
        collapsedLabel: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.summary = summary
        self.isExpanded = nil
        self.expandedLabel = expandedLabel
        self.collapsedLabel = collapsedLabel
        self.content = content()
        _isExpandedInternally = State(initialValue: initiallyExpanded)
    }

    /// A toggle block whose state the app keeps, for example to save it
    /// with the document.
    ///
    /// - Parameters:
    ///   - summary: The line that stays visible, already localised.
    ///   - isExpanded: Whether the content shows.
    ///   - expandedLabel: Replaces the built-in "Expanded" spoken value.
    ///   - collapsedLabel: Replaces the built-in "Collapsed" spoken value.
    ///   - content: The content that shows and hides.
    public init(
        _ summary: String,
        isExpanded: Binding<Bool>,
        expandedLabel: String? = nil,
        collapsedLabel: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.summary = summary
        self.isExpanded = isExpanded
        self.expandedLabel = expandedLabel
        self.collapsedLabel = collapsedLabel
        self.content = content()
        _isExpandedInternally = State(initialValue: isExpanded.wrappedValue)
    }

    /// The content of the view.
    public var body: some View {
        DisclosureGroup(isExpanded: isExpanded ?? $isExpandedInternally) {
            content
        } label: {
            Text(summary)
        }
        .disclosureGroupStyle(
            MonarchToggleBlockStyle(expandedLabel: expandedLabel, collapsedLabel: collapsedLabel)
        )
    }
}

/// The block look for `DisclosureGroup`: a leading chevron and indented content.
private struct MonarchToggleBlockStyle: DisclosureGroupStyle {
    let expandedLabel: String?
    let collapsedLabel: String?

    func makeBody(configuration: Configuration) -> some View {
        MonarchToggleBlockBody(
            configuration: configuration,
            expandedLabel: expandedLabel,
            collapsedLabel: collapsedLabel
        )
    }
}

/// Reads the environment, which a `DisclosureGroupStyle` value cannot do itself.
private struct MonarchToggleBlockBody: View {
    let configuration: DisclosureGroupStyleConfiguration
    let expandedLabel: String?
    let collapsedLabel: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .body) private var chevronWidth = MonarchToggleBlockTokens.chevronWidth

    var body: some View {
        let isExpanded = configuration.isExpanded
        VStack(alignment: .leading, spacing: MonarchToggleBlockTokens.contentSpacing) {
            Button {
                withAnimation(theme.motion.animation(.base, reduceMotion: reduceMotion)) {
                    configuration.isExpanded.toggle()
                }
            } label: {
                header(isExpanded: isExpanded)
            }
            .buttonStyle(.plain)
            .accessibilityValue(
                MonarchDisclosureGroupTokens.accessibilityValue(
                    isExpanded: isExpanded,
                    expandedLabel: expandedLabel,
                    collapsedLabel: collapsedLabel
                )
            )
            if isExpanded {
                configuration.content
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.leading, chevronWidth + MonarchToggleBlockTokens.chevronSpacing)
                    .transition(.opacity)
            }
        }
    }

    private func header(isExpanded: Bool) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchToggleBlockTokens.chevronSpacing) {
            MonarchIcon(systemName: MonarchToggleBlockTokens.chevronSymbol(isExpanded: isExpanded), size: .sm)
                .foregroundStyle(theme.colors[role: MonarchToggleBlockTokens.chevron(isEnabled: isEnabled)])
                .frame(width: chevronWidth)
            configuration.label
                .monarchTextStyle(MonarchToggleBlockTokens.textStyle)
                .foregroundStyle(theme.colors[role: MonarchToggleBlockTokens.summary(isEnabled: isEnabled)])
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .contentShape(Rectangle())
    }
}

#if DEBUG
    #Preview("MonarchToggleBlock") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchToggleBlock("Collapsed section") { Text("Hidden") }
                MonarchToggleBlock("Expanded section", initiallyExpanded: true) {
                    Text("The importer runs nightly and keeps a week of logs.").monarchTextStyle(.body)
                }
                MonarchToggleBlock("Disabled") { Text("Hidden") }.disabled(true)
            }
        }
    }
#endif
