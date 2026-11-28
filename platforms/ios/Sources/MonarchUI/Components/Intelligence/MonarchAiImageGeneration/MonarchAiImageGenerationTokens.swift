import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiImageGeneration``.
enum MonarchAiImageGenerationTokens {
    /// The gap between the status, the progress bar and the grid.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between grid cells, both ways.
    static let gridSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between an image, its field and its actions.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The least width of a grid column at the default text size (10rem on
    /// the web); scaled with Dynamic Type, so large text gives one column.
    static let minimumColumnWidth: CGFloat = MonarchTokens.Space.space20 * 2
    /// The blur over a refining preview.
    static let refiningBlur: CGFloat = MonarchTokens.Space.space1
    /// The width of the ring round the chosen image.
    static let selectedRingWidth: CGFloat = MonarchTokens.Space.space0_5
    /// Padding round the "Selected" mark.
    static let markPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The padding of the mark's text inside its capsule.
    static let markInset: CGFloat = MonarchTokens.Space.space1
    /// The fill of an empty slot.
    static var slot: MonarchColorRole { \.bgMuted }
    /// The tint that pulses over a slot while generating.
    static var shimmer: MonarchColorRole { \.aiSubtle }

    /// The address schemes that are loaded.
    static let loadableSchemes: Set<String> = ["http", "https", "file"]

    /// What a slot shows.
    enum Slot: Equatable {
        /// The image, final.
        case image
        /// The image, still refining.
        case refining
        /// A pulsing placeholder.
        case placeholder
        /// "Preview unavailable".
        case unavailable
    }

    /// Whether generation is under way.
    static func isGenerating(_ status: MonarchAiImageGenerationStatus) -> Bool {
        status == .pending || status == .progressive
    }

    /// Progress clamped to 0...1, or `nil` when unknown or not a number.
    static func clamped(_ progress: Double?) -> Double? {
        guard let progress, progress.isFinite else { return nil }
        return min(max(progress, 0), 1)
    }

    /// Whether an address is loaded: `http`, `https` or `file` only.
    static func isLoadable(_ url: URL?) -> Bool {
        guard let scheme = url?.scheme?.lowercased() else { return false }
        return loadableSchemes.contains(scheme)
    }

    /// Whether an image has a source that can be shown.
    static func hasSource(_ image: MonarchAiGeneratedImage) -> Bool {
        image.image != nil || isLoadable(image.url)
    }

    /// What a slot shows for an image in a status.
    static func slot(for image: MonarchAiGeneratedImage, status: MonarchAiImageGenerationStatus) -> Slot {
        let shows = hasSource(image) && status != .pending
        if shows { return status == .progressive ? .refining : .image }
        return isGenerating(status) ? .placeholder : .unavailable
    }

    /// Whether the alt text field shows its error: empty alt text once an
    /// image can be seen (not while pending).
    static func isAltInvalid(_ alt: String, status: MonarchAiImageGenerationStatus) -> Bool {
        status != .pending && alt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// Whether an image's actions show: finished, shown, and at least one action given.
    static func showsActions(
        for image: MonarchAiGeneratedImage,
        status: MonarchAiImageGenerationStatus,
        canSelect: Bool,
        canVary: Bool
    ) -> Bool {
        status == .done && slot(for: image, status: status) == .image && (canSelect || canVary)
    }

    /// The spoken name of an image: its alt text, or "Generated image n".
    static func spokenName(alt: String, position: Int, labels: MonarchAiImageGenerationLabels) -> String {
        let trimmed = alt.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? labels.imageName(position) : trimmed
    }

    /// The status line, or `nil` in the error state.
    static func statusText(
        status: MonarchAiImageGenerationStatus,
        progress: Double?,
        count: Int,
        locale: Locale,
        labels: MonarchAiImageGenerationLabels
    ) -> String? {
        let percent = clamped(progress).map { $0.formatted(.percent.precision(.fractionLength(0)).locale(locale)) }
        return switch status {
        case .pending: labels.pending(percent)
        case .progressive: labels.progressive(percent)
        case .done: labels.done(count)
        case .error: nil
        }
    }

    /// Whether a status change announces completion: only on the way into `done`.
    static func announcesCompletion(from old: MonarchAiImageGenerationStatus, to new: MonarchAiImageGenerationStatus)
        -> Bool
    {
        new == .done && old != .done
    }

}
