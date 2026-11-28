import SwiftUI

/// Downloads a report: one button for a single format, or a menu of formats
/// (CSV, Excel, PDF). It shows progress while the file is prepared, says
/// when it is saved or queued, and offers Try again when it fails.
///
/// ```swift
/// MonarchDownloadReportButton(
///     formats: [
///         MonarchDownloadReportFormat("CSV", id: "csv"),
///         MonarchDownloadReportFormat("PDF", id: "pdf"),
///     ],
///     onDownload: { format in
///         .file(try await reports.export(format.id))
///     }
/// )
/// ```
///
/// `onDownload` prepares the file. Return ``MonarchDownloadReportResult/file(_:)``
/// with a local URL to let people save it with the system's save dialog,
/// ``MonarchDownloadReportResult/downloaded`` when the app saved or shared it
/// itself, or ``MonarchDownloadReportResult/queued(message:)`` when it is
/// prepared in the background. Throw to show the failure with Try again.
///
/// Accessibility: while the file is prepared the button reads "Preparing
/// CSV…" as its value and does not start a second download. Preparing,
/// saved, queued and failed are announced. The progress bar has a spoken
/// name. The icons are decorative.
public struct MonarchDownloadReportButton: View {
    private let formats: [MonarchDownloadReportFormat]
    private let progress: Double?
    private let variant: MonarchButtonVariant
    private let size: MonarchButtonSize
    private let labels: MonarchDownloadReportButtonLabels
    private let errorMessage: (@MainActor (any Error, MonarchDownloadReportFormat) -> String)?
    private let onDownload: @MainActor (MonarchDownloadReportFormat) async throws -> MonarchDownloadReportResult

    @State private var phase = MonarchDownloadReportButtonTokens.Phase.idle
    @State private var exportFormat: MonarchDownloadReportFormat?

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchDownloadReportButtonTokens

    /// A download button.
    ///
    /// - Parameters:
    ///   - formats: The formats offered. One shows a plain button; several
    ///     open a menu. Default: CSV.
    ///   - progress: How far the download is, 0 to 1, while it runs.
    ///     Without it the button only shows a spinner.
    ///   - variant: The button's visual weight.
    ///   - size: The button's height and padding.
    ///   - labels: Replacements for the built-in strings.
    ///   - errorMessage: Turns a thrown error into the text shown. Default:
    ///     "The CSV download failed."
    ///   - onDownload: Prepares the file in the chosen format and says where
    ///     it is. Throw when it fails.
    public init(
        formats: [MonarchDownloadReportFormat] = [.csv],
        progress: Double? = nil,
        variant: MonarchButtonVariant = .secondary,
        size: MonarchButtonSize = .sm,
        labels: MonarchDownloadReportButtonLabels = MonarchDownloadReportButtonLabels(),
        errorMessage: (@MainActor (any Error, MonarchDownloadReportFormat) -> String)? = nil,
        onDownload: @escaping @MainActor (MonarchDownloadReportFormat) async throws -> MonarchDownloadReportResult
    ) {
        self.formats = formats
        self.progress = progress
        self.variant = variant
        self.size = size
        self.labels = labels
        self.errorMessage = errorMessage
        self.onDownload = onDownload
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            trigger
                .accessibilityValue(Tokens.value(for: phase, labels: labels))
            if let value = Tokens.progress(progress, phase: phase) {
                ProgressView(value: value)
                    .progressViewStyle(.monarchLinear)
                    .accessibilityLabel(labels.progress ?? MonarchDownloadReportButtonStrings.progress)
            }
            switch phase {
            case .queued(_, let message):
                Text(message)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: Tokens.noteColor])
            case .failed(let format, let message):
                MonarchInlineErrorRetry(message) {
                    run(format)
                }
            default:
                EmptyView()
            }
        }
        .fileExporter(
            isPresented: isExporting,
            document: exportedFile,
            contentType: exportedFile?.contentType ?? .data,
            defaultFilename: exportedFile.map { Tokens.filename($0.url) }
        ) { result in
            if let format = exportFormat {
                phase = Tokens.phase(afterExport: result, format: format, labels: labels)
            }
        }
        .onChange(of: phase) { _, phase in
            if let message = Tokens.announcement(for: phase, labels: labels) {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    @ViewBuilder private var trigger: some View {
        let title = Tokens.title(formats: formats, labels: labels)
        if formats.count == 1, let format = formats.first {
            MonarchButton(title, systemImage: Tokens.symbol, variant: variant, size: size, isLoading: phase.isBusy) {
                run(format)
            }
        } else {
            Menu {
                ForEach(formats) { format in
                    Button {
                        run(format)
                    } label: {
                        Text(format.label)
                        if let description = format.description {
                            Text(description)
                        }
                    }
                }
            } label: {
                HStack(spacing: MonarchButtonTokens.iconSpacing) {
                    if phase.isBusy {
                        ProgressView().controlSize(.small)
                    } else {
                        MonarchIcon(systemName: Tokens.symbol, size: .sm)
                    }
                    Text(title)
                    MonarchIcon(systemName: Tokens.menuSymbol, size: .sm)
                }
            }
            .buttonStyle(.monarch(variant, size: size))
            .disabled(phase.isBusy)
        }
    }

    private var exportedFile: MonarchDownloadReportFile? {
        if case .exporting(_, let url) = phase { return MonarchDownloadReportFile(url: url) }
        return nil
    }

    /// Presented while a file waits to be saved. Closing the dialog returns
    /// to idle; the completion handler, which may run after that, reads the
    /// format from `exportFormat` and records the outcome.
    private var isExporting: Binding<Bool> {
        Binding(
            get: { exportedFile != nil },
            set: { isPresented in
                if !isPresented, exportedFile != nil { phase = .idle }
            }
        )
    }

    private func run(_ format: MonarchDownloadReportFormat) {
        guard !phase.isBusy else { return }
        phase = .pending(format)
        Task { @MainActor in
            do {
                let result = try await onDownload(format)
                exportFormat = format
                phase = Tokens.phase(after: result, format: format, labels: labels)
            } catch {
                phase = Tokens.phase(
                    afterError: error,
                    format: format,
                    message: errorMessage?(error, format),
                    labels: labels
                )
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchDownloadReportButton") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchDownloadReportButton { _ in .downloaded }
                MonarchDownloadReportButton(
                    formats: [
                        MonarchDownloadReportFormat("CSV", id: "csv", description: "Opens in any spreadsheet"),
                        MonarchDownloadReportFormat("PDF", id: "pdf"),
                    ],
                    variant: .primary,
                    size: .md
                ) { _ in .queued(message: nil) }
            }
        }
    }
#endif
