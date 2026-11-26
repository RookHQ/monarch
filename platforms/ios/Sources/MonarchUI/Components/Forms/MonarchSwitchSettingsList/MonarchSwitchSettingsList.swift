import SwiftUI

/// A titled group of on/off settings that each save on their own, such as
/// notification preferences.
///
/// ```swift
/// MonarchSwitchSettingsList("Email notifications", description: "Choose what we email you about.") {
///     MonarchSwitchSettingsRow("Weekly summary", isOn: $weekly) { try await api.save("weekly", $0) }
///     MonarchSwitchSettingsRow("Product news", isOn: $news) { try await api.save("news", $0) }
/// }
/// ```
///
/// The list draws the heading and a bordered group with a line between rows.
/// Its `labels` and `savedDuration` apply to every row; a row's own labels
/// win. For switches that are submitted together with a Save button, use a
/// `Form` with plain `Toggle`s instead.
///
/// Accessibility: the title is a header. The list is a container, so
/// VoiceOver moves through the rows in order; each row describes its own
/// switch and status.
public struct MonarchSwitchSettingsList<Content: View>: View {
    private let title: String?
    private let description: String?
    private let labels: MonarchSwitchSettingsListLabels
    private let savedDuration: Duration
    private let content: Content

    @Environment(\.monarchTheme) private var theme

    /// A switch settings list.
    ///
    /// - Parameters:
    ///   - title: The heading of the group, already localised.
    ///   - description: A sentence under the heading.
    ///   - labels: Replacements for the built-in strings of every row.
    ///   - savedDuration: How long "Saved" stays after a save.
    ///   - content: ``MonarchSwitchSettingsRow`` views.
    public init(
        _ title: String? = nil,
        description: String? = nil,
        labels: MonarchSwitchSettingsListLabels = MonarchSwitchSettingsListLabels(),
        savedDuration: Duration = .seconds(2),
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.description = description
        self.labels = labels
        self.savedDuration = savedDuration
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchSwitchSettingsListTokens.sectionSpacing) {
            if title != nil || description != nil {
                VStack(alignment: .leading, spacing: MonarchSwitchSettingsListTokens.headerSpacing) {
                    if let title {
                        Text(title)
                            .monarchTextStyle(.bodySmall, weight: .semibold)
                            .foregroundStyle(theme.colors.textDefault)
                            .accessibilityAddTraits(.isHeader)
                    }
                    if let description {
                        Text(description)
                            .monarchTextStyle(.bodySmall)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
            }
            VStack(spacing: 0) {
                content
            }
            .environment(
                \.monarchSwitchSettingsContext,
                MonarchSwitchSettingsContext(labels: labels, savedDuration: savedDuration, isInList: true)
            )
            .background(MonarchShapes.container.fill(theme.colors.bgSurface))
            .clipShape(MonarchShapes.container)
            .overlay(
                MonarchShapes.container.strokeBorder(
                    theme.colors.borderDefault,
                    lineWidth: MonarchSwitchSettingsListTokens.borderWidth
                )
            )
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }
}

#if DEBUG
    #Preview("MonarchSwitchSettingsList") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSwitchSettingsList("Email notifications", description: "Choose what we email you about.") {
                    MonarchSwitchSettingsRow("Weekly summary", isOn: .constant(true), description: "Sent on Mondays.") {
                        _ in
                    }
                    MonarchSwitchSettingsRow("Product news", isOn: .constant(false)) { _ in }
                    MonarchSwitchSettingsRow("Security alerts", isOn: .constant(true)) { _ in }
                        .disabled(true)
                }
                MonarchSwitchSettingsList {
                    MonarchSwitchSettingsRow("Untitled group", isOn: .constant(false)) { _ in }
                }
            }
        }
    }
#endif
