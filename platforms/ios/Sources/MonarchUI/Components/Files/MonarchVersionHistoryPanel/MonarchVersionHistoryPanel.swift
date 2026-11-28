import SwiftUI

/// The version history of a document or record: the saved versions, what
/// changed between any version and now, its full text, and restore behind
/// a confirmation.
///
/// ```swift
/// .sheet(isPresented: $showsHistory) {
///     MonarchVersionHistoryPanel(
///         versions: document.versions,
///         selection: $versionID,
///         currentLines: document.paragraphs,
///         selectedLines: loadedParagraphs,
///         onRestore: { id in try await document.restore(id) },
///         onClose: { showsHistory = false }
///     )
/// }
/// .task(id: versionID) { loadedParagraphs = await versionID.map(document.paragraphs(of:)) }
/// ```
///
/// The panel is a navigation stack: a list of versions, newest first, and a
/// screen per version pushed with the system back button and gesture. Show
/// it in a `.sheet`, or in an `.inspector` beside the document on iPad. The
/// app loads data: when `selection` changes, load that version's lines and
/// pass them as `selectedLines` (the screen shows a placeholder until then).
/// The version's screen has a segmented control for Changes (a
/// ``MonarchDiffViewer`` of the version against now) and Full text. Restore
/// asks for confirmation; when `onRestore` throws, the screen stays with an
/// error. With `onSaveVersion`, the list starts with a name field and Save
/// version. Times are relative ("2 hours ago") and stay current.
///
/// Accessibility: each version is one navigation link read as when, who
/// and its name, with "Current" as the value of the current version.
/// Avatars are decorative. The author line and the date on the version's
/// screen are read together. Saving, restoring and their errors are
/// announced. Restore is behind the system confirmation dialog.
public struct MonarchVersionHistoryPanel: View {
    private let title: String?
    private let versions: [MonarchVersionHistoryPanelVersion]
    @Binding private var selection: String?
    private let currentLines: [String]
    private let selectedLines: [String]?
    private let currentID: String?
    private let isLoading: Bool
    private let labels: MonarchVersionHistoryPanelLabels
    private let onSaveVersion: (@MainActor (String) async throws -> Void)?
    private let onRestore: (@MainActor (String) async throws -> Void)?
    private let onClose: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var note = ""
    @State private var isSaving = false
    @State private var saveError: String?

    /// A version history panel.
    ///
    /// - Parameters:
    ///   - title: The title of the list. "History" by default.
    ///   - versions: The saved versions, newest first.
    ///   - selection: The id of the version on screen, or `nil` for the list.
    ///   - currentLines: The content now, one entry per block or paragraph.
    ///   - selectedLines: The selected version's content, or `nil` while it loads.
    ///   - currentID: The version that matches the content now. The newest by default.
    ///   - isLoading: Shows placeholder rows while the list loads.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSaveVersion: Shows a name field and Save version. Receives the trimmed name. Throw to show an error.
    ///   - onRestore: Shows Restore behind a confirmation on other versions. Throw to keep the screen with an error.
    ///   - onClose: Shows a Close button in the navigation bar.
    public init(
        _ title: String? = nil,
        versions: [MonarchVersionHistoryPanelVersion],
        selection: Binding<String?>,
        currentLines: [String],
        selectedLines: [String]? = nil,
        currentID: String? = nil,
        isLoading: Bool = false,
        labels: MonarchVersionHistoryPanelLabels = MonarchVersionHistoryPanelLabels(),
        onSaveVersion: (@MainActor (String) async throws -> Void)? = nil,
        onRestore: (@MainActor (String) async throws -> Void)? = nil,
        onClose: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.versions = versions
        _selection = selection
        self.currentLines = currentLines
        self.selectedLines = selectedLines
        self.currentID = currentID
        self.isLoading = isLoading
        self.labels = labels
        self.onSaveVersion = onSaveVersion
        self.onRestore = onRestore
        self.onClose = onClose
    }

    private var current: String? { MonarchVersionHistoryPanelTokens.currentID(currentID, versions: versions) }

    /// The path of the navigation stack: the selected version, if any.
    private var path: Binding<[String]> {
        Binding(
            get: { selection.map { [$0] } ?? [] },
            set: { selection = $0.last }
        )
    }

    /// The content of the view.
    public var body: some View {
        NavigationStack(path: path) {
            list
                .navigationTitle(title ?? MonarchVersionHistoryPanelStrings.title)
                .navigationDestination(for: String.self) { id in
                    if let version = versions.first(where: { $0.id == id }) {
                        MonarchVersionHistoryPanelDetail(
                            version: version,
                            currentLines: currentLines,
                            selectedLines: selectedLines,
                            canRestore: MonarchVersionHistoryPanelTokens.canRestore(
                                versionID: id,
                                currentID: current,
                                hasHandler: onRestore != nil
                            ),
                            labels: labels,
                            onRestore: onRestore,
                            onRestored: { selection = nil }
                        )
                    }
                }
                .toolbar {
                    if let onClose {
                        ToolbarItem(placement: .cancellationAction) {
                            Button(labels.close ?? MonarchVersionHistoryPanelStrings.close, action: onClose)
                        }
                    }
                }
        }
    }

    private var list: some View {
        TimelineView(MonarchRelativeTimeSchedule(dates: versions.map(\.date))) { context in
            ScrollView {
                VStack(alignment: .leading, spacing: MonarchVersionHistoryPanelTokens.sectionSpacing) {
                    header
                    if onSaveVersion != nil { saveRow }
                    if isLoading {
                        ForEach(0..<MonarchVersionHistoryPanelTokens.placeholderRows, id: \.self) { _ in
                            MonarchVersionHistoryPanelPlaceholder(labels: labels)
                        }
                    } else if versions.isEmpty {
                        MonarchEmptyState(
                            labels.empty ?? MonarchVersionHistoryPanelStrings.empty,
                            systemImage: MonarchVersionHistoryPanelTokens.emptySymbol,
                            size: .sm
                        )
                        .frame(maxWidth: .infinity)
                    } else {
                        VStack(alignment: .leading, spacing: 0) {
                            ForEach(versions) { version in
                                Button {
                                    selection = version.id
                                } label: {
                                    row(version, now: context.date)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(MonarchVersionHistoryPanelTokens.panelPadding)
            }
            .monarchCanvas()
            #if os(iOS)
                .toolbar(.hidden, for: .navigationBar)
            #endif
        }
    }

    /// The title with the close button at the trailing edge, as the web panel's header.
    private var header: some View {
        HStack {
            Text(title ?? MonarchVersionHistoryPanelStrings.title)
                .monarchTextStyle(.subheading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 0)
            if let onClose {
                MonarchButton(
                    labels.close ?? MonarchVersionHistoryPanelStrings.close,
                    systemImage: "xmark",
                    variant: .ghost,
                    size: .sm,
                    isIconOnly: true,
                    action: onClose
                )
            }
        }
    }

    /// The version name field with the save button beside it, and the error under both.
    private var saveRow: some View {
        VStack(alignment: .leading, spacing: MonarchVersionHistoryPanelTokens.textSpacing) {
            HStack(spacing: MonarchVersionHistoryPanelTokens.rowSpacing) {
                MonarchTextField(
                    labels.noteLabel ?? MonarchVersionHistoryPanelStrings.noteLabel,
                    text: $note,
                    prompt: labels.notePlaceholder ?? MonarchVersionHistoryPanelStrings.notePlaceholder,
                    labelVisibility: .hidden
                )
                .disabled(isSaving)
                .onSubmit(save)
                MonarchButton(
                    labels.save ?? MonarchVersionHistoryPanelStrings.save,
                    variant: .secondary,
                    size: .sm,
                    isLoading: isSaving,
                    action: save
                )
                .fixedSize()
            }
            if let saveError {
                MonarchInlineNotice(saveError, tone: .danger, variant: .plain)
            }
        }
    }

    private func row(_ version: MonarchVersionHistoryPanelVersion, now: Date) -> some View {
        let relative = MonarchVersionHistoryPanelTokens.relativeTime(version.date, now: now, locale: locale)
        let isCurrent = version.id == current
        return HStack(spacing: MonarchVersionHistoryPanelTokens.rowSpacing) {
            MonarchVersionHistoryPanelAvatar(version: version)
            VStack(alignment: .leading, spacing: MonarchVersionHistoryPanelTokens.textSpacing) {
                HStack(spacing: MonarchVersionHistoryPanelTokens.rowSpacing) {
                    Text(relative)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                    if isCurrent {
                        MonarchBadge(labels.current ?? MonarchVersionHistoryPanelStrings.current, tone: .accent)
                    }
                }
                Text(MonarchVersionHistoryPanelTokens.byline(version, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchVersionHistoryPanelTokens.secondary])
            }
        }
        .frame(minHeight: MonarchComboboxTokens.minHeight)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            MonarchVersionHistoryPanelTokens.accessibilityLabel(version, relativeTime: relative, labels: labels)
        )
        .accessibilityValue(MonarchVersionHistoryPanelTokens.accessibilityValue(isCurrent: isCurrent, labels: labels))
    }

    private func save() {
        guard let onSaveVersion, !isSaving else { return }
        isSaving = true
        saveError = nil
        let name = MonarchVersionHistoryPanelTokens.note(note)
        Task { @MainActor in
            do {
                try await onSaveVersion(name)
                note = ""
                isSaving = false
                AccessibilityNotification.Announcement(labels.saved ?? MonarchVersionHistoryPanelStrings.saved).post()
            } catch {
                isSaving = false
                let message = labels.saveFailed ?? MonarchVersionHistoryPanelStrings.saveFailed
                saveError = message
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchVersionHistoryPanel") {
        let now = Date()
        MonarchPreviewMatrix {
            MonarchVersionHistoryPanel(
                versions: [
                    MonarchVersionHistoryPanelVersion(id: "3", date: now, authorName: "Ana Silva", note: "Launch copy"),
                    MonarchVersionHistoryPanelVersion(
                        id: "2",
                        date: now.addingTimeInterval(-7200),
                        authorName: "Sam Lee",
                        isAutomatic: true
                    ),
                ],
                selection: .constant(nil),
                currentLines: ["Ship on Monday."],
                onSaveVersion: { _ in },
                onRestore: { _ in }
            )
        }
    }
#endif
