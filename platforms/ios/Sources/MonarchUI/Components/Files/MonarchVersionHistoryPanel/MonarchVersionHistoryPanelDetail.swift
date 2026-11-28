import SwiftUI

/// The screen of one version in ``MonarchVersionHistoryPanel``: who saved
/// it, the diff against now or the full text, and Restore behind a
/// confirmation.
struct MonarchVersionHistoryPanelDetail: View {
    let version: MonarchVersionHistoryPanelVersion
    let currentLines: [String]
    let selectedLines: [String]?
    let canRestore: Bool
    let labels: MonarchVersionHistoryPanelLabels
    let onRestore: (@MainActor (String) async throws -> Void)?
    let onRestored: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var tab: MonarchVersionHistoryPanelTab = .changes
    @State private var isConfirming = false
    @State private var isRestoring = false
    @State private var restoreError: String?

    var body: some View {
        TimelineView(MonarchRelativeTimeSchedule(dates: [version.date])) { context in
            let relative = MonarchVersionHistoryPanelTokens.relativeTime(
                version.date,
                now: context.date,
                locale: locale
            )
            ScrollView {
                VStack(alignment: .leading, spacing: MonarchVersionHistoryPanelTokens.detailSpacing) {
                    byline
                    Picker(labels.view ?? MonarchVersionHistoryPanelStrings.view, selection: $tab) {
                        ForEach(MonarchVersionHistoryPanelTab.allCases, id: \.self) { tab in
                            Text(MonarchVersionHistoryPanelTokens.title(tab, labels: labels)).tag(tab)
                        }
                    }
                    .pickerStyle(.segmented)
                    content(relative: relative)
                    if canRestore { restoreArea }
                }
                .padding(MonarchVersionHistoryPanelTokens.detailPadding)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .navigationTitle(relative)
        }
        .confirmationDialog(
            labels.restoreTitle ?? MonarchVersionHistoryPanelStrings.restoreTitle,
            isPresented: $isConfirming,
            titleVisibility: .visible,
            actions: {
                Button(labels.restoreConfirm ?? MonarchVersionHistoryPanelStrings.restoreConfirm, role: .destructive) {
                    restore()
                }
            },
            message: {
                Text(labels.restoreDescription ?? MonarchVersionHistoryPanelStrings.restoreDescription)
            }
        )
    }

    private var byline: some View {
        HStack(spacing: MonarchVersionHistoryPanelTokens.rowSpacing) {
            MonarchVersionHistoryPanelAvatar(version: version)
            VStack(alignment: .leading, spacing: MonarchVersionHistoryPanelTokens.textSpacing) {
                Text(MonarchVersionHistoryPanelTokens.byline(version, labels: labels))
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                Text(MonarchVersionHistoryPanelTokens.absoluteTime(version.date, locale: locale))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchVersionHistoryPanelTokens.secondary])
            }
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private func content(relative: String) -> some View {
        if let selectedLines {
            switch tab {
            case .changes:
                if MonarchVersionHistoryPanelTokens.isIdentical(selectedLines, to: currentLines) {
                    Text(labels.identical ?? MonarchVersionHistoryPanelStrings.identical)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors[role: MonarchVersionHistoryPanelTokens.secondary])
                } else {
                    MonarchDiffViewer(
                        before: selectedLines,
                        after: currentLines,
                        beforeLabel: relative,
                        afterLabel: labels.now ?? MonarchVersionHistoryPanelStrings.now,
                        showsLineNumbers: false,
                        labels: labels.diff
                    )
                }
            case .fullText:
                VStack(alignment: .leading, spacing: MonarchVersionHistoryPanelTokens.paragraphSpacing) {
                    ForEach(Array(selectedLines.enumerated()), id: \.offset) { _, line in
                        Text(line)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textDefault)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .textSelection(.enabled)
            }
        } else {
            MonarchVersionHistoryPanelPlaceholder(labels: labels)
        }
    }

    private var restoreArea: some View {
        VStack(alignment: .leading, spacing: MonarchVersionHistoryPanelTokens.paragraphSpacing) {
            if let restoreError {
                MonarchInlineNotice(restoreError, tone: .danger)
            }
            MonarchButton(
                labels.restore ?? MonarchVersionHistoryPanelStrings.restore,
                systemImage: MonarchVersionHistoryPanelTokens.restoreSymbol,
                variant: .primary,
                isLoading: isRestoring,
                fullWidth: true
            ) {
                isConfirming = true
            }
        }
    }

    private func restore() {
        guard let onRestore else { return }
        isRestoring = true
        restoreError = nil
        Task { @MainActor in
            do {
                try await onRestore(version.id)
                isRestoring = false
                AccessibilityNotification.Announcement(labels.restored ?? MonarchVersionHistoryPanelStrings.restored)
                    .post()
                onRestored()
            } catch {
                isRestoring = false
                let message = labels.restoreFailed ?? MonarchVersionHistoryPanelStrings.restoreFailed
                restoreError = message
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }
}

/// Redacted lines shown while text loads, read as "Loading".
struct MonarchVersionHistoryPanelPlaceholder: View {
    let labels: MonarchVersionHistoryPanelLabels

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchVersionHistoryPanelTokens.paragraphSpacing) {
            ForEach(MonarchVersionHistoryPanelTokens.placeholderLines, id: \.self) { line in
                Text(line).monarchTextStyle(.body)
            }
        }
        .redacted(reason: .placeholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.loading ?? MonarchVersionHistoryPanelStrings.loading)
    }
}

/// The author's avatar, from a picture or an address. Decorative, because
/// the name is written beside it.
struct MonarchVersionHistoryPanelAvatar: View {
    let version: MonarchVersionHistoryPanelVersion

    var body: some View {
        if let url = version.authorURL {
            MonarchAvatar(
                url: url,
                name: version.authorName,
                size: MonarchVersionHistoryPanelTokens.rowAvatarSize,
                isDecorative: true
            )
        } else {
            MonarchAvatar(
                image: version.authorImage,
                name: version.authorName,
                size: MonarchVersionHistoryPanelTokens.rowAvatarSize,
                isDecorative: true
            )
        }
    }
}
