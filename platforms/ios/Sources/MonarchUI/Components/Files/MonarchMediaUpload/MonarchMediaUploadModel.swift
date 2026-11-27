import Foundation
import Observation

/// Runs the checks and the upload of ``MonarchMediaUpload`` and holds their state.
///
/// Every start and every cancel begins a new generation; a check, a
/// progress report or an answer from an older generation is ignored, so a
/// cancelled or replaced upload never changes what is shown.
@MainActor
@Observable
final class MonarchMediaUploadModel {
    /// A refusal with a message for people.
    private struct Refusal: Error {
        let message: String
    }

    /// Where the upload stands.
    private(set) var phase: MonarchMediaUploadPhase = .idle
    /// The latest error, shown under the field.
    private(set) var error: String?
    /// The warnings of the latest upload.
    private(set) var warnings: [String] = []
    private var generation = 0

    /// Checks a video's length when there is a limit, then uploads the file.
    /// Returns the result, or `nil` when it failed or was cancelled.
    func upload(
        _ file: MonarchFileUploadFile,
        kind: MonarchMediaUploadKind,
        maxVideoDuration: TimeInterval?,
        labels: MonarchMediaUploadLabels,
        locale: Locale,
        readDuration: @Sendable (URL) async throws -> TimeInterval,
        upload: @MainActor (MonarchFileUploadFile, MonarchMediaUploadContext) async throws -> MonarchMediaUploadResult
    ) async -> MonarchMediaUploadResult? {
        generation += 1
        let current = generation
        error = nil
        warnings = []
        do {
            if kind == .video, maxVideoDuration != nil {
                phase = .checking(file.name)
                let seconds: TimeInterval
                do {
                    seconds = try await readDuration(file.url)
                } catch {
                    throw Refusal(
                        message: labels.durationUnreadable?(file.name)
                            ?? MonarchMediaUploadStrings.durationUnreadable(file.name)
                    )
                }
                guard current == generation else { return nil }
                if let message = MonarchMediaUploadTokens.lengthRefusal(
                    name: file.name,
                    seconds: seconds,
                    maxVideoDuration: maxVideoDuration,
                    labels: labels,
                    locale: locale
                ) {
                    throw Refusal(message: message)
                }
            }
            phase = .uploading(file.name, nil)
            let context = MonarchMediaUploadContext(kind: kind) { [weak self] fraction in
                Task { @MainActor in self?.report(fraction, generation: current) }
            }
            let result = try await upload(file, context)
            guard current == generation, !Task.isCancelled else { return nil }
            phase = .idle
            warnings = result.warnings
            return result
        } catch let refusal as Refusal {
            guard current == generation else { return nil }
            phase = .idle
            error = refusal.message
            return nil
        } catch {
            guard current == generation, !Task.isCancelled, !(error is CancellationError) else { return nil }
            phase = .idle
            self.error = MonarchMediaUploadTokens.failureMessage(error, labels: labels)
            return nil
        }
    }

    /// Records a progress report of the given generation.
    func report(_ fraction: Double, generation: Int) {
        guard generation == self.generation, case .uploading(let name, _) = phase else { return }
        phase = .uploading(name, MonarchMediaUploadTokens.clamped(fraction))
    }

    /// The generation of the upload that is running, for progress reports.
    var currentGeneration: Int { generation }

    /// Stops showing the running check or upload; its answer is ignored.
    func cancel() {
        generation += 1
        phase = .idle
    }

    /// Shows a refusal from the pickers, or clears the messages.
    func show(error: String?) {
        self.error = error
        if error != nil { warnings = [] }
    }

    /// Clears the error and the warnings, after a removal.
    func clearMessages() {
        error = nil
        warnings = []
    }
}
