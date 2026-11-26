import SwiftUI

/// The Monarch look for the standard `DisclosureGroup`: one section that
/// shows and hides its content.
///
/// ```swift
/// DisclosureGroup("Advanced settings") {
///     Toggle("Use a proxy", isOn: $usesProxy)
/// }
/// .disclosureGroupStyle(.monarch)
/// ```
///
/// The group stays a standard `DisclosureGroup`, so `isExpanded:` bindings
/// and `disabled(_:)` work as usual. The header is the label followed by a
/// chevron that turns when the content is shown; the content opens below it.
///
/// Accessibility: the header is one button whose label is the label of the
/// group and whose value is "Expanded" or "Collapsed". The chevron is
/// decorative. Hidden content is removed, so VoiceOver does not reach it. The
/// header is at least 44 points high and the whole row responds to touch.
/// The content appears without animation when Reduce Motion is on.
public struct MonarchDisclosureGroupStyle: DisclosureGroupStyle {
    private let expandedLabel: String?
    private let collapsedLabel: String?
    private let pointsRightWhenCollapsed: Bool

    /// A disclosure group style.
    ///
    /// - Parameters:
    ///   - expandedLabel: Replaces the built-in "Expanded" spoken value.
    ///   - collapsedLabel: Replaces the built-in "Collapsed" spoken value.
    ///   - pointsRightWhenCollapsed: Draws the chevron pointing right while collapsed and down while expanded,
    ///     as the filter and tree sections do, instead of down while collapsed and up while expanded.
    public init(
        expandedLabel: String? = nil,
        collapsedLabel: String? = nil,
        pointsRightWhenCollapsed: Bool = false
    ) {
        self.expandedLabel = expandedLabel
        self.collapsedLabel = collapsedLabel
        self.pointsRightWhenCollapsed = pointsRightWhenCollapsed
    }

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        MonarchDisclosureGroupBody(
            configuration: configuration,
            expandedLabel: expandedLabel,
            collapsedLabel: collapsedLabel,
            pointsRightWhenCollapsed: pointsRightWhenCollapsed
        )
    }
}

extension DisclosureGroupStyle where Self == MonarchDisclosureGroupStyle {
    /// The Monarch disclosure group style.
    public static var monarch: MonarchDisclosureGroupStyle { MonarchDisclosureGroupStyle() }

    /// The Monarch disclosure group style with a chevron that points right while collapsed and down while
    /// expanded.
    public static var monarchSection: MonarchDisclosureGroupStyle {
        MonarchDisclosureGroupStyle(pointsRightWhenCollapsed: true)
    }
}

/// Reads the environment, which a `DisclosureGroupStyle` value cannot do itself.
private struct MonarchDisclosureGroupBody: View {
    let configuration: DisclosureGroupStyleConfiguration
    let expandedLabel: String?
    let collapsedLabel: String?
    var pointsRightWhenCollapsed = false

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.monarchDisclosureChevron) private var chevronPlacement

    var body: some View {
        let isExpanded = configuration.isExpanded
        VStack(alignment: .leading, spacing: MonarchDisclosureGroupTokens.contentSpacing) {
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
                    .monarchTextStyle(MonarchDisclosureGroupTokens.contentTextStyle, weight: .regular)
                    .foregroundStyle(theme.colors[role: MonarchDisclosureGroupTokens.contentColor])
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.bottom, MonarchDisclosureGroupTokens.contentBottomPadding)
                    .transition(.opacity)
            }
        }
    }

    private func header(isExpanded: Bool) -> some View {
        HStack(spacing: MonarchDisclosureGroupTokens.chevronSpacing) {
            if let leading = chevronPlacement.leadingRole {
                // A forward chevron before the label that turns down when open, as the web draws the AI
                // disclosures (reasoning, run steps).
                MonarchIcon(systemName: MonarchDisclosureChevron.symbol, size: .sm)
                    .foregroundStyle(theme.colors[role: isEnabled ? leading : \.textDisabled])
                    .rotationEffect(.degrees(isExpanded ? MonarchDisclosureChevron.openRotation : 0))
            }
            configuration.label
                .monarchTextStyle(
                    MonarchDisclosureGroupTokens.textStyle,
                    weight: MonarchDisclosureGroupTokens.labelWeight
                )
                .foregroundStyle(theme.colors[role: MonarchDisclosureGroupTokens.label(isEnabled: isEnabled)])
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
            if chevronPlacement.leadingRole == nil {
                MonarchIcon(
                    systemName: pointsRightWhenCollapsed ? "chevron.right" : MonarchDisclosureGroupTokens.chevronSymbol,
                    size: .sm
                )
                // A small chevron with the same stroke as the web's, not a large thin one.
                .imageScale(.small)
                .fontWeight(.semibold)
                .foregroundStyle(theme.colors[role: MonarchDisclosureGroupTokens.chevron(isEnabled: isEnabled)])
                .rotationEffect(
                    .degrees(
                        pointsRightWhenCollapsed
                            ? (isExpanded ? 90 : 0) : MonarchDisclosureGroupTokens.rotation(isExpanded: isExpanded)
                    )
                )
            }
        }
        .padding(.vertical, MonarchDisclosureGroupTokens.headerVerticalPadding)
        .monarchMinimumHitTarget()
        .contentShape(Rectangle())
    }
}

/// Where a disclosure's chevron sits: at the end, pointing down (the default), or before the label, pointing
/// forward and turning down when open, in a colour role.
struct MonarchDisclosureChevronPlacement: Equatable, @unchecked Sendable {
    var leadingRole: MonarchColorRole?

    static let trailing = MonarchDisclosureChevronPlacement(leadingRole: nil)
    static func leading(_ role: MonarchColorRole = \.textMuted) -> MonarchDisclosureChevronPlacement {
        MonarchDisclosureChevronPlacement(leadingRole: role)
    }
}

private struct MonarchDisclosureChevronKey: EnvironmentKey {
    static let defaultValue = MonarchDisclosureChevronPlacement.trailing
}

extension EnvironmentValues {
    var monarchDisclosureChevron: MonarchDisclosureChevronPlacement {
        get { self[MonarchDisclosureChevronKey.self] }
        set { self[MonarchDisclosureChevronKey.self] = newValue }
    }
}

#if DEBUG
    #Preview("MonarchDisclosureGroupStyle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                DisclosureGroup("Collapsed") { Text("Content") }
                DisclosureGroup("Expanded", isExpanded: .constant(true)) {
                    Text("The content of the section.").monarchTextStyle(.body)
                }
                DisclosureGroup("Disabled") { Text("Content") }.disabled(true)
            }
            .disclosureGroupStyle(.monarch)
        }
    }
#endif
