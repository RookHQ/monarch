import AVFoundation
import Foundation
import MonarchTokens
import SwiftUI
import UniformTypeIdentifiers

/// Component tokens and pure logic of ``MonarchMediaUpload``.
enum MonarchMediaUploadTokens {
    /// The gap between the parts of the field.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the label and the help text.
    static let labelSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The width divided by the height of the image preview.
    static let previewAspectRatio: CGFloat = 16.0 / 9.0
    /// The SF Symbol of the empty field.
    static let symbol = "arrow.up.to.line"
    /// The SF Symbol of the placeholder of an image that is loading.
    static let placeholderSymbol = "photo"
    /// The SF Symbol of the remove button.
    static let removeSymbol = "trash"
    /// The fill behind a preview while it loads.
    static var previewBackground: MonarchColorRole { \.bgMuted }
    /// The border of the preview.
    static var previewBorder: MonarchColorRole { \.borderSubtle }
    /// The colour of the placeholder symbol and the progress text.
    static var secondary: MonarchColorRole { \.textMuted }

    /// Video file extensions recognised in an address, as on the web.
    static let videoExtensions: Set<String> = ["mp4", "m4v", "mov", "webm", "ogv", "avi"]

    /// Whether a file type is a video.
    static func kind(of type: UTType?) -> MonarchMediaUploadKind? {
        guard let type else { return nil }
        if type.conforms(to: .movie) || type.conforms(to: .video) { return .video }
        if type.conforms(to: .image) { return .image }
        return nil
    }

    /// Guesses the kind of an address from its extension: video for the
    /// usual video extensions, image otherwise.
    static func kind(of url: URL) -> MonarchMediaUploadKind {
        videoExtensions.contains(url.pathExtension.lowercased()) ? .video : .image
    }

    /// The kind of a picked file: by its type, then its extension.
    static func kind(of file: MonarchFileUploadFile) -> MonarchMediaUploadKind {
        kind(of: file.contentType) ?? kind(of: UTType(filenameExtension: file.url.pathExtension))
            ?? kind(of: file.url)
    }

    /// The types the pickers offer. Videos are offered when they are taken,
    /// and also when they are refused with a reason, so the refusal can say why.
    static func allowedContentTypes(kinds: Set<MonarchMediaUploadKind>, explainsVideo: Bool) -> [UTType] {
        var types: [UTType] = []
        if kinds.contains(.image) { types.append(.image) }
        if kinds.contains(.video) || explainsVideo { types.append(.movie) }
        return types
    }

    /// A size in bytes, written for people: "10 MB".
    static func size(_ bytes: Int64, locale: Locale) -> String {
        bytes.formatted(.byteCount(style: .file).locale(locale))
    }

    /// A length, written for people: "1 min, 30 sec".
    static func length(_ seconds: TimeInterval, locale: Locale) -> String {
        Duration.seconds(seconds.rounded())
            .formatted(.units(allowed: [.hours, .minutes, .seconds], width: .abbreviated).locale(locale))
    }

    /// The reason a picked file is refused before upload, or `nil` to accept it.
    static func refusal(
        _ file: MonarchFileUploadFile,
        kinds: Set<MonarchMediaUploadKind>,
        maxImageSize: Int64?,
        maxVideoSize: Int64?,
        labels: MonarchMediaUploadLabels,
        locale: Locale
    ) -> String? {
        let kind = kind(of: file)
        if kind == .video && !kinds.contains(.video) {
            return labels.videoNotAllowed?(file.name) ?? MonarchMediaUploadStrings.videoNotAllowed(file.name)
        }
        let limit = kind == .video ? maxVideoSize : maxImageSize
        if let limit, let size = file.size, size > limit {
            let written = Self.size(limit, locale: locale)
            return labels.tooLarge?(file.name, written) ?? MonarchMediaUploadStrings.tooLarge(file.name, written)
        }
        return nil
    }

    /// The reason a video is refused for its length, or `nil` when it fits.
    static func lengthRefusal(
        name: String,
        seconds: TimeInterval,
        maxVideoDuration: TimeInterval?,
        labels: MonarchMediaUploadLabels,
        locale: Locale
    ) -> String? {
        guard let maxVideoDuration, seconds > maxVideoDuration else { return nil }
        let written = length(maxVideoDuration, locale: locale)
        return labels.tooLong?(name, written) ?? MonarchMediaUploadStrings.tooLong(name, written)
    }

    /// The hint of the empty field: one line per kind taken, then the
    /// reason videos are not taken.
    static func hint(
        kinds: Set<MonarchMediaUploadKind>,
        maxImageSize: Int64?,
        maxVideoSize: Int64?,
        maxVideoDuration: TimeInterval?,
        videoUnavailable: String?,
        labels: MonarchMediaUploadLabels,
        locale: Locale
    ) -> String {
        var lines: [String] = []
        if kinds.contains(.image) {
            let size = maxImageSize.map { Self.size($0, locale: locale) }
            lines.append(
                labels.imageHint?(size) ?? size.map(MonarchMediaUploadStrings.imagesUpTo)
                    ?? MonarchMediaUploadStrings.images
            )
        }
        if kinds.contains(.video) {
            let size = maxVideoSize.map { Self.size($0, locale: locale) }
            let duration = maxVideoDuration.map { length($0, locale: locale) }
            if let custom = labels.videoHint {
                lines.append(custom(size, duration))
            } else {
                switch (size, duration) {
                case (let size?, let duration?):
                    lines.append(MonarchMediaUploadStrings.videosUpToAtMost(size, duration))
                case (let size?, nil): lines.append(MonarchMediaUploadStrings.videosUpTo(size))
                case (nil, let duration?): lines.append(MonarchMediaUploadStrings.videosAtMost(duration))
                case (nil, nil): lines.append(MonarchMediaUploadStrings.videos)
                }
            }
        } else if let videoUnavailable, !videoUnavailable.isEmpty {
            lines.append(videoUnavailable)
        }
        return lines.joined(separator: "\n")
    }

    /// The title of the empty field.
    static func addTitle(kinds: Set<MonarchMediaUploadKind>, labels: MonarchMediaUploadLabels) -> String {
        kinds.contains(.video)
            ? labels.addMedia ?? MonarchMediaUploadStrings.addMedia
            : labels.addImage ?? MonarchMediaUploadStrings.addImage
    }

    /// The text of the progress row: "Checking clip.mov…", or
    /// "Uploading photo.jpg" with the percentage when it is known.
    static func progressText(_ phase: MonarchMediaUploadPhase, labels: MonarchMediaUploadLabels, locale: Locale)
        -> String
    {
        switch phase {
        case .idle:
            return ""
        case .checking(let name):
            return labels.checking?(name) ?? MonarchMediaUploadStrings.checking(name)
        case .uploading(let name, let fraction):
            let text = labels.uploading?(name) ?? MonarchMediaUploadStrings.uploading(name)
            guard let fraction else { return text }
            return "\(text) · \(fraction.formatted(.percent.precision(.fractionLength(0)).locale(locale)))"
        }
    }

    /// The title of the replace area for the kind on show.
    static func replaceTitle(_ kind: MonarchMediaUploadKind, labels: MonarchMediaUploadLabels) -> String {
        switch kind {
        case .image: labels.replaceImage ?? MonarchMediaUploadStrings.replaceImage
        case .video: labels.replaceVideo ?? MonarchMediaUploadStrings.replaceVideo
        }
    }

    /// The remove button for the kind on show.
    static func removeTitle(_ kind: MonarchMediaUploadKind, labels: MonarchMediaUploadLabels) -> String {
        switch kind {
        case .image: labels.removeImage ?? MonarchMediaUploadStrings.removeImage
        case .video: labels.removeVideo ?? MonarchMediaUploadStrings.removeVideo
        }
    }

    /// Announced after an upload.
    static func uploaded(_ kind: MonarchMediaUploadKind, labels: MonarchMediaUploadLabels) -> String {
        switch kind {
        case .image: labels.imageUploaded ?? MonarchMediaUploadStrings.imageUploaded
        case .video: labels.videoUploaded ?? MonarchMediaUploadStrings.videoUploaded
        }
    }

    /// Announced after a removal.
    static func removed(_ kind: MonarchMediaUploadKind, labels: MonarchMediaUploadLabels) -> String {
        switch kind {
        case .image: labels.imageRemoved ?? MonarchMediaUploadStrings.imageRemoved
        case .video: labels.videoRemoved ?? MonarchMediaUploadStrings.videoRemoved
        }
    }

    /// The spoken name of the preview: the app's text, or "Uploaded image".
    static func previewLabel(_ kind: MonarchMediaUploadKind, custom: String?, labels: MonarchMediaUploadLabels)
        -> String
    {
        if let custom, !custom.isEmpty { return custom }
        switch kind {
        case .image: return labels.previewImage ?? MonarchMediaUploadStrings.previewImage
        case .video: return labels.previewVideo ?? MonarchMediaUploadStrings.previewVideo
        }
    }

    /// The message of a failed upload: the error's own description when it
    /// has one, or "The upload failed. Try again."
    static func failureMessage(_ error: any Error, labels: MonarchMediaUploadLabels) -> String {
        MonarchFailureMessage.message(
            for: error,
            override: labels.uploadFailed,
            fallback: MonarchMediaUploadStrings.uploadFailed
        )
    }

    /// A progress report kept between 0 and 1.
    static func clamped(_ fraction: Double) -> Double {
        guard fraction.isFinite else { return 0 }
        return min(1, max(0, fraction))
    }

    /// The length of a video file in seconds, read from its metadata.
    static func videoDuration(of url: URL) async throws -> TimeInterval {
        try await AVURLAsset(url: url).load(.duration).seconds
    }
}
