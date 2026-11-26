import MonarchTokens
import SwiftUI

/// Steps through the versions of an AI artifact: previous and next buttons
/// with the position ("Version 3 of 5"), a menu to jump to any version, and
/// "Compare with previous", which shows a ``MonarchDiffViewer`` of what
/// changed.
///
/// ```swift
/// MonarchAiArtifactVersions(selection: $version, versions: versions)
/// ```
///
/// `selection` is a version number; a number that is not in `versions`
/// shows the newest version. The versions are shown oldest to newest
/// whatever their order. The app shows the selected version's content
/// itself. Compare appears when the version before the selected one exists
/// and both have `content`; while it is on, the diff from the previous
/// version to the selected one is shown under the controls. With no versions
/// nothing is shown.
///
/// Accessibility: the controls are a group named "Versions". Previous and
/// next are ``MonarchAiMessageBranch`` buttons with 44-point touch targets;
/// the position is read as text. The menu is named "Choose a version" and
/// is a system `Picker`, so the chosen version is read as selected. Compare
/// is a toggle button, read as on or off. Every change of version is
/// announced ("Showing version 3 of 5").
@_spi(Experimental) public struct MonarchAiArtifactVersions: View {
    @Binding private var selection: Int
    private let versions: [MonarchAiArtifactVersion]
    private let comparingBinding: Binding<Bool>?
    private let granularity: MonarchDiffViewerGranularity
    private let showsCompare: Bool
    private let labels: MonarchAiArtifactVersionsLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone
    @State private var isComparingState = false

    private typealias Tokens = MonarchAiArtifactVersionsTokens

    /// A version stepper.
    ///
    /// - Parameters:
    ///   - selection: The version number shown.
    ///   - versions: Every version of the artifact, in any order.
    ///   - isComparing: Whether the diff with the previous version shows. Without it the stepper keeps it.
    ///   - granularity: Whether the diff marks changed words or whole lines.
    ///   - showsCompare: Whether "Compare with previous" is offered.
    ///   - labels: Replacements for the built-in strings.
    public init(
        selection: Binding<Int>,
        versions: [MonarchAiArtifactVersion],
        isComparing: Binding<Bool>? = nil,
        granularity: MonarchDiffViewerGranularity = .word,
        showsCompare: Bool = true,
        labels: MonarchAiArtifactVersionsLabels = MonarchAiArtifactVersionsLabels()
    ) {
        _selection = selection
        self.versions = versions
        self.comparingBinding = isComparing
        self.granularity = granularity
        self.showsCompare = showsCompare
        self.labels = labels
    }

    private var isComparing: Binding<Bool> {
        comparingBinding ?? $isComparingState
    }

    /// The content of the view.
    public var body: some View {
        let sorted = Tokens.sorted(versions)
        if let index = Tokens.index(of: selection, in: sorted) {
            let comparison = showsCompare ? Tokens.comparison(at: index, in: sorted) : nil
            VStack(alignment: .leading, spacing: Tokens.spacing) {
                MonarchFlowLayout(spacing: Tokens.spacing) {
                    stepper(sorted, index: index)
                    if comparison != nil {
                        Toggle(isOn: isComparing) {
                            Text(labels.compare ?? MonarchAiArtifactVersionsStrings.compare)
                                .monarchTextStyle(.body)
                        }
                        .toggleStyle(.monarchButton(.ghost, size: .sm, tone: .neutral))
                        .monarchMinimumHitTarget()
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.label ?? MonarchAiArtifactVersionsStrings.label)
                menu(sorted, index: index)
                if let comparison, isComparing.wrappedValue {
                    MonarchDiffViewer(
                        before: comparison.before.content ?? "",
                        after: comparison.after.content ?? "",
                        granularity: granularity,
                        beforeLabel: Tokens.versionName(comparison.before.version, labels: labels),
                        afterLabel: Tokens.versionName(comparison.after.version, labels: labels)
                    )
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func stepper(_ sorted: [MonarchAiArtifactVersion], index: Int) -> some View {
        let total = sorted.count
        let labels = labels
        return MonarchAiMessageBranch(
            selection: Binding(
                get: { index },
                set: { newIndex in
                    guard sorted.indices.contains(newIndex) else { return }
                    selection = sorted[newIndex].version
                }
            ),
            count: total,
            showsSingle: true,
            previousLabel: labels.previous ?? MonarchAiArtifactVersionsStrings.previous,
            nextLabel: labels.next ?? MonarchAiArtifactVersionsStrings.next,
            formatPosition: { position, total in
                Tokens.position(sorted[position - 1].version, total: total, labels: labels)
            },
            formatAnnouncement: { position, total in
                Tokens.announcement(sorted[position - 1].version, total: total, labels: labels)
            }
        )
    }

    private func menu(_ sorted: [MonarchAiArtifactVersion], index: Int) -> some View {
        Menu {
            Picker(
                labels.select ?? MonarchAiArtifactVersionsStrings.select,
                selection: Binding(get: { selection }, set: { choose($0, total: sorted.count) })
            ) {
                ForEach(sorted.reversed()) { version in
                    Text(
                        Tokens.option(
                            version,
                            locale: locale,
                            calendar: calendar,
                            timeZone: timeZone,
                            labels: labels
                        )
                    )
                    .tag(version.version)
                }
            }
            .pickerStyle(.inline)
            .labelsHidden()
        } label: {
            // As the web select: the chosen version written out, with a chevron, in a bordered field.
            HStack(spacing: Tokens.spacing) {
                Text(
                    Tokens.option(
                        sorted[index],
                        locale: locale,
                        calendar: calendar,
                        timeZone: timeZone,
                        labels: labels
                    )
                )
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
                MonarchIcon(systemName: Tokens.menuSymbol, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .padding(.horizontal, MonarchTokens.Space.space3)
            .frame(minHeight: MonarchTokens.Space.space10)
            .overlay(
                MonarchShapes.control.strokeBorder(
                    theme.colors[role: \.borderInput],
                    lineWidth: MonarchShapes.borderWidth
                )
            )
            .contentShape(Rectangle())
        }
        .menuStyle(.button)
        .buttonStyle(.plain)
        .accessibilityLabel(labels.select ?? MonarchAiArtifactVersionsStrings.select)
    }

    private func choose(_ version: Int, total: Int) {
        guard version != selection else { return }
        selection = version
        AccessibilityNotification.Announcement(Tokens.announcement(version, total: total, labels: labels)).post()
    }
}

#if DEBUG
    #Preview("MonarchAiArtifactVersions") {
        let start = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let versions = [
            MonarchAiArtifactVersion(1, createdAt: start, content: "Ship on Friday."),
            MonarchAiArtifactVersion(
                2,
                createdAt: start.addingTimeInterval(600),
                label: "Shorter",
                content: "Ship Monday."
            ),
            MonarchAiArtifactVersion(3, createdAt: start.addingTimeInterval(1_200)),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiArtifactVersions(selection: .constant(2), versions: versions, isComparing: .constant(true))
                MonarchAiArtifactVersions(selection: .constant(3), versions: versions)
            }
        }
    }
#endif
