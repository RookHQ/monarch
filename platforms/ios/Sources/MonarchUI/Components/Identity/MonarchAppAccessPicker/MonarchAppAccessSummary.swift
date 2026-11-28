import SwiftUI

/// A read-only line of the apps someone can open, for a member list: the
/// first names as tags, then "+N"; "No app access" when there are none.
///
/// ```swift
/// MonarchAppAccessSummary(apps, granted: member.appIDs)
/// ```
///
/// Names are sorted; an id without a matching app shows the id.
///
/// Accessibility: the summary is one element that reads every name, the
/// hidden ones too ("Docs, Mail, and 2 more: Notes, Sheets").
@_spi(Experimental) public struct MonarchAppAccessSummary: View {
    private let apps: [MonarchAppAccessOption]
    private let granted: Set<String>
    private let maxVisible: Int
    private let labels: MonarchAppAccessPickerLabels

    @Environment(\.monarchTheme) private var theme

    /// An app access summary.
    ///
    /// - Parameters:
    ///   - apps: The apps, to look up names.
    ///   - granted: The ids of the apps the person can open.
    ///   - maxVisible: How many names to show before "+N". At least one is shown.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ apps: [MonarchAppAccessOption],
        granted: Set<String>,
        maxVisible: Int = 2,
        labels: MonarchAppAccessPickerLabels = MonarchAppAccessPickerLabels()
    ) {
        self.apps = apps
        self.granted = granted
        self.maxVisible = maxVisible
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        let summary = MonarchAppAccessPickerTokens.summary(apps, granted: granted, maxVisible: maxVisible)
        Group {
            if summary.shown.isEmpty {
                Text(labels.none ?? MonarchAppAccessPickerStrings.none)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            } else {
                MonarchFlowLayout(spacing: MonarchAppAccessPickerTokens.badgeSpacing) {
                    ForEach(summary.shown, id: \.self) { MonarchTag($0, size: .sm) }
                    if !summary.hidden.isEmpty {
                        Text(
                            labels.moreCount?(summary.hidden.count)
                                ?? MonarchAppAccessPickerStrings.moreCount(summary.hidden.count)
                        )
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                    }
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            MonarchAppAccessPickerTokens.summaryLabel(shown: summary.shown, hidden: summary.hidden, labels: labels)
        )
    }
}

#if DEBUG
    #Preview("MonarchAppAccessSummary") {
        let apps = [
            MonarchAppAccessOption(id: "mail", name: "Mail"),
            MonarchAppAccessOption(id: "docs", name: "Docs"),
            MonarchAppAccessOption(id: "notes", name: "Notes"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAppAccessSummary(apps, granted: ["mail", "docs", "notes"])
                MonarchAppAccessSummary(apps, granted: ["mail"])
                MonarchAppAccessSummary(apps, granted: [])
            }
        }
    }
#endif
