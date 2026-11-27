import Foundation
import SwiftUI

/// A "Download your data" card for account or privacy settings: choose
/// what to include and the format, request an export, follow it, and
/// download the file until it expires.
///
/// ```swift
/// MonarchDataExportRequest(
///     status: export.status,
///     requestedAt: export.requestedAt,
///     expiresAt: export.expiresAt,
///     scope: [
///         MonarchCheckboxGroupOption(id: "profile", label: "Profile"),
///         MonarchCheckboxGroupOption(id: "messages", label: "Messages"),
///     ],
///     formats: [MonarchDataExportFormat(id: "zip", label: "ZIP"), MonarchDataExportFormat(id: "json", label: "JSON")],
///     downloadURL: export.url,
///     onRequest: { details in try await api.requestExport(details) }
/// )
/// ```
///
/// The app builds the file and reports each status; the card shows it as a
/// badge with an icon and a sentence with the dates in the environment's
/// locale and time zone, and an indeterminate bar while preparing. The
/// request form (a ``MonarchCheckboxGroup`` with every kind of data checked
/// at first, a `Picker` menu of formats and a primary ``MonarchButton``)
/// shows when `onRequest` is given and the status is idle, expired or
/// failed. The button shows progress while `onRequest` runs; a thrown
/// error is shown in a danger ``MonarchBanner``. When ready, Download opens
/// `downloadURL` (http or https only) with `openURL` and calls `onDownload`.
///
/// Accessibility: the card is a container named by its title, which is a
/// header. The status is a badge with an icon and text, not colour alone,
/// and each new status line is announced. The progress bar is named
/// "Preparing your export". An empty scope is an error on the checklist,
/// announced by it.
@_spi(Experimental) public struct MonarchDataExportRequest: View {
    private let status: MonarchDataExportStatus
    private let requestedAt: Date?
    private let readyAt: Date?
    private let expiresAt: Date?
    private let scope: [MonarchCheckboxGroupOption]
    private let formats: [MonarchDataExportFormat]
    private let preparationNote: String?
    private let failureReason: String?
    private let downloadURL: URL?
    private let isRequesting: Bool
    private let labels: MonarchDataExportRequestLabels
    private let onDownload: (@MainActor () -> Void)?
    private let onRequest: (@MainActor (MonarchDataExportRequestDetails) async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone
    @Environment(\.openURL) private var openURL
    @State private var chosen: Set<String>
    @State private var format: String
    @State private var isRunning = false
    @State private var failure: String?
    @State private var hasScopeError = false

    /// A data export card.
    ///
    /// - Parameters:
    ///   - status: Where the export is, as the app reports it.
    ///   - requestedAt: When the export was requested.
    ///   - readyAt: When the file became ready.
    ///   - expiresAt: When the download stops working.
    ///   - scope: Kinds of data the person can choose to include, all checked at first. No checklist when empty.
    ///   - formats: File formats to choose from. No menu when empty.
    ///   - defaultFormat: The id of the format chosen at first. The first format by default.
    ///   - preparationNote: How long preparing takes and how the person hears about it. Shown before and during preparation.
    ///   - failureReason: Extra detail when the status is failed, such as the reason from the server.
    ///   - downloadURL: The file's link when ready. Only http and https links are opened.
    ///   - isRequesting: Shows the request button as busy, for apps that track the request themselves.
    ///   - labels: Replacements for the built-in strings.
    ///   - onDownload: Runs when Download is tapped, for example to fetch a signed link. Shows Download when ready.
    ///   - onRequest: Requests an export with the chosen scope and format. Shows the request form. A thrown error is shown.
    public init(
        status: MonarchDataExportStatus = .idle,
        requestedAt: Date? = nil,
        readyAt: Date? = nil,
        expiresAt: Date? = nil,
        scope: [MonarchCheckboxGroupOption] = [],
        formats: [MonarchDataExportFormat] = [],
        defaultFormat: String? = nil,
        preparationNote: String? = nil,
        failureReason: String? = nil,
        downloadURL: URL? = nil,
        isRequesting: Bool = false,
        labels: MonarchDataExportRequestLabels = MonarchDataExportRequestLabels(),
        onDownload: (@MainActor () -> Void)? = nil,
        onRequest: (@MainActor (MonarchDataExportRequestDetails) async throws -> Void)? = nil
    ) {
        self.status = status
        self.requestedAt = requestedAt
        self.readyAt = readyAt
        self.expiresAt = expiresAt
        self.scope = scope
        self.formats = formats
        self.preparationNote = preparationNote
        self.failureReason = failureReason
        self.downloadURL = downloadURL
        self.isRequesting = isRequesting
        self.labels = labels
        self.onDownload = onDownload
        self.onRequest = onRequest
        _chosen = State(initialValue: Set(scope.map(\.id)))
        _format = State(initialValue: defaultFormat ?? formats.first?.id ?? "")
    }

    private var title: String { labels.title ?? MonarchDataExportRequestStrings.title }

    private var statusLine: String? {
        MonarchDataExportRequestTokens.statusLine(
            status,
            requestedAt: requestedAt,
            readyAt: readyAt,
            expiresAt: expiresAt,
            locale: locale,
            timeZone: timeZone,
            labels: labels
        )
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            header
            if let statusLine {
                Text(statusLine)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
            }
            if status == .failed, let failureReason, !failureReason.isEmpty {
                Text(failureReason)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if status == .preparing {
                ProgressView()
                    .progressViewStyle(.monarchLinear)
                    .accessibilityLabel(labels.preparingProgress ?? MonarchDataExportRequestStrings.preparingProgress)
            }
            if let preparationNote, !preparationNote.isEmpty, MonarchDataExportRequestTokens.showsNote(status) {
                Text(preparationNote)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if MonarchDataExportRequestTokens.canRequest(status, hasHandler: onRequest != nil) {
                form
            }
            if MonarchDataExportRequestTokens.showsDownload(status, url: downloadURL, hasHandler: onDownload != nil) {
                MonarchButton(
                    labels.download ?? MonarchDataExportRequestStrings.download,
                    systemImage: MonarchDataExportRequestTokens.downloadSymbol,
                    variant: .primary
                ) {
                    if let url = MonarchDataExportRequestTokens.safeDownloadURL(downloadURL) { openURL(url) }
                    onDownload?()
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
        .onChange(of: status) { _, _ in
            if let statusLine { AccessibilityNotification.Announcement(statusLine).post() }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space1) {
            MonarchFlowLayout(spacing: theme.spacing.space2) {
                Text(title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                MonarchBadge(
                    MonarchDataExportRequestTokens.statusName(status, labels: labels),
                    tone: MonarchDataExportRequestTokens.tone(status),
                    systemImage: MonarchDataExportRequestTokens.symbol(status)
                )
            }
            Text(labels.description ?? MonarchDataExportRequestStrings.description)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    @ViewBuilder private var form: some View {
        if !scope.isEmpty {
            MonarchCheckboxGroup(
                labels.scopeLegend ?? MonarchDataExportRequestStrings.scopeLegend,
                selection: $chosen,
                options: scope,
                error: hasScopeError ? labels.scopeRequired ?? MonarchDataExportRequestStrings.scopeRequired : nil
            )
            .onChange(of: chosen) { _, chosen in
                if !chosen.isEmpty { hasScopeError = false }
            }
        }
        if !formats.isEmpty {
            let formatLabel = labels.formatLabel ?? MonarchDataExportRequestStrings.formatLabel
            MonarchSelectField(
                formatLabel,
                selection: $format,
                valueText: formats.first { $0.id == format }?.label
            ) {
                ForEach(formats) { Text($0.label).tag($0.id) }
            }
        }
        if let failure {
            MonarchBanner(message: failure, tone: .danger)
        }
        MonarchButton(
            status == .idle
                ? labels.request ?? MonarchDataExportRequestStrings.request
                : labels.requestAgain ?? MonarchDataExportRequestStrings.requestAgain,
            variant: .primary,
            isLoading: isRequesting || isRunning
        ) {
            submit()
        }
    }

    private func submit() {
        guard let onRequest, !isRequesting, !isRunning else { return }
        guard
            let details = MonarchDataExportRequestTokens.details(
                scope: scope.map(\.id),
                chosen: chosen,
                hasFormats: !formats.isEmpty,
                format: format
            )
        else {
            hasScopeError = true
            return
        }
        hasScopeError = false
        failure = nil
        isRunning = true
        Task {
            do {
                try await onRequest(details)
            } catch {
                failure = MonarchDataExportRequestTokens.failureMessage(error, labels: labels)
                if let failure { AccessibilityNotification.Announcement(failure).post() }
            }
            isRunning = false
        }
    }
}

#if DEBUG
    #Preview("MonarchDataExportRequest") {
        let scope = [
            MonarchCheckboxGroupOption(id: "profile", label: "Profile"),
            MonarchCheckboxGroupOption(id: "messages", label: "Messages", description: "Including attachments."),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchDataExportRequest(
                    scope: scope,
                    formats: [
                        MonarchDataExportFormat(id: "zip", label: "ZIP"),
                        MonarchDataExportFormat(id: "json", label: "JSON"),
                    ],
                    preparationNote: "It can take up to 48 hours.",
                    onRequest: { _ in }
                )
                MonarchDataExportRequest(status: .preparing, requestedAt: Date(timeIntervalSince1970: 1_800_000_000))
                MonarchDataExportRequest(
                    status: .ready,
                    expiresAt: Date(timeIntervalSince1970: 1_800_600_000),
                    downloadURL: URL(string: "https://example.com/export.zip")
                )
            }
        }
    }
#endif
