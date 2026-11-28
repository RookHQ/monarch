import Foundation
import MonarchTokens
import SwiftUI
import UniformTypeIdentifiers

/// Component tokens and file checks for ``MonarchFileUpload``.
enum MonarchFileUploadTokens {
    /// Padding inside the zone.
    static let zonePadding: CGFloat = MonarchTokens.Space.space6
    /// Padding inside the compact row.
    static let compactPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the hint.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the area and the messages under it.
    static let messageSpacing: CGFloat = MonarchTokens.Space.space2
    /// The length of the dashes and gaps of the zone's border.
    static let dash: CGFloat = MonarchTokens.Space.space1
    /// The width of the zone's border.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The SF Symbol shown by default.
    static let symbol = "square.and.arrow.up"
    /// The diameter of the round surface behind the symbol.
    static let iconCircle: CGFloat = MonarchTokens.Space.space10
    /// The separator between the parts of the generated hint.
    static let hintSeparator = " · "

    /// The colour roles of the area.
    struct Appearance: Equatable {
        let fill: MonarchColorRole
        let border: MonarchColorRole
        let icon: MonarchColorRole
        let title: MonarchColorRole
    }

    /// The roles for a state: a drag over the area takes the accent, an
    /// invalid area the danger border, a blocked one the disabled roles.
    static func appearance(isTargeted: Bool, isInvalid: Bool, isEnabled: Bool) -> Appearance {
        if !isEnabled {
            return Appearance(fill: \.bgMuted, border: \.borderSubtle, icon: \.textDisabled, title: \.textDisabled)
        }
        if isTargeted {
            return Appearance(fill: \.accentSubtle, border: \.accentBorder, icon: \.accentStrong, title: \.textDefault)
        }
        if isInvalid {
            return Appearance(fill: \.bgSubtle, border: \.dangerStrong, icon: \.textMuted, title: \.textDefault)
        }
        return Appearance(fill: \.bgSubtle, border: \.borderStrong, icon: \.textMuted, title: \.textDefault)
    }

    /// The colour role of the hint.
    static func hintColor(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// Whether the picker lets people choose several files: as asked, or
    /// unless the limit is one file.
    static func allowsMultiple(_ requested: Bool?, maxFiles: Int?) -> Bool {
        requested ?? (maxFiles != 1)
    }

    /// Whether the count limit is reached.
    static func isFull(maxFiles: Int?, currentCount: Int) -> Bool {
        guard let maxFiles else { return false }
        return currentCount >= maxFiles
    }

    /// How many more files fit, or `nil` without a limit.
    static func room(maxFiles: Int?, currentCount: Int) -> Int? {
        maxFiles.map { max(0, $0 - currentCount) }
    }

    /// The type of a file: its own, or the one its extension names.
    static func contentType(of file: MonarchFileUploadFile) -> UTType? {
        if let type = file.contentType { return type }
        let ext = (file.name as NSString).pathExtension
        return ext.isEmpty ? nil : UTType(filenameExtension: ext)
    }

    /// Whether a file's type conforms to one of the allowed types. A file
    /// of unknown type matches only when any data is allowed.
    static func matches(_ file: MonarchFileUploadFile, allowedContentTypes: [UTType]) -> Bool {
        guard !allowedContentTypes.isEmpty else { return true }
        let type = contentType(of: file) ?? .data
        return allowedContentTypes.contains { type.conforms(to: $0) }
    }

    /// A size in the locale's file units, such as "10 MB".
    static func formattedSize(_ bytes: Int64, locale: Locale) -> String {
        bytes.formatted(ByteCountFormatStyle(style: .file, locale: locale))
    }

    /// The hint: the app's, the "limit reached" text when full, or one made
    /// from the limits, such as "Up to 10 MB each · 5 files max".
    static func hint(
        _ hint: String?,
        maxSize: Int64?,
        maxFiles: Int?,
        currentCount: Int,
        labels: MonarchFileUploadLabels,
        locale: Locale
    ) -> String? {
        if let maxFiles, isFull(maxFiles: maxFiles, currentCount: currentCount) {
            return labels.limitReached?(maxFiles) ?? MonarchFileUploadStrings.limitReached(maxFiles)
        }
        if let hint, !hint.isEmpty { return hint }
        let size = maxSize.map { formattedSize($0, locale: locale) }
        if size == nil && maxFiles == nil { return nil }
        if let limits = labels.limits { return limits(size, maxFiles) }
        let parts = [size.map(MonarchFileUploadStrings.limitSize), maxFiles.map(MonarchFileUploadStrings.limitCount)]
        return parts.compactMap { $0 }.joined(separator: hintSeparator)
    }

    /// Splits files into accepted and refused. Type, size and the app's
    /// check come first; files over the count limit are refused in order,
    /// so the first valid files are kept.
    static func validate(
        _ files: [MonarchFileUploadFile],
        allowedContentTypes: [UTType],
        maxSize: Int64?,
        maxFiles: Int?,
        currentCount: Int,
        labels: MonarchFileUploadLabels,
        locale: Locale,
        validate: (@Sendable (MonarchFileUploadFile) -> String?)?
    ) -> (accepted: [MonarchFileUploadFile], rejected: [MonarchFileUploadRejection]) {
        var accepted: [MonarchFileUploadFile] = []
        var rejected: [MonarchFileUploadRejection] = []
        let room = room(maxFiles: maxFiles, currentCount: currentCount)
        for file in files {
            if !matches(file, allowedContentTypes: allowedContentTypes) {
                let message = labels.invalidType?(file.name) ?? MonarchFileUploadStrings.invalidType(file.name)
                rejected.append(MonarchFileUploadRejection(file: file, reason: .invalidType, message: message))
            } else if let maxSize, let size = file.size, size > maxSize {
                let limit = formattedSize(maxSize, locale: locale)
                let message = labels.tooLarge?(file.name, limit) ?? MonarchFileUploadStrings.tooLarge(file.name, limit)
                rejected.append(MonarchFileUploadRejection(file: file, reason: .tooLarge, message: message))
            } else if let custom = validate?(file), !custom.isEmpty {
                rejected.append(MonarchFileUploadRejection(file: file, reason: .custom, message: custom))
            } else if let room, accepted.count >= room {
                rejected.append(tooMany(file, limit: maxFiles ?? 0, labels: labels))
            } else {
                accepted.append(file)
            }
        }
        return (accepted, rejected)
    }

    /// The refusal of a file over the count limit.
    static func tooMany(
        _ file: MonarchFileUploadFile,
        limit: Int,
        labels: MonarchFileUploadLabels
    ) -> MonarchFileUploadRejection {
        let message = labels.tooMany?(file.name, limit) ?? MonarchFileUploadStrings.tooMany(file.name, limit)
        return MonarchFileUploadRejection(file: file, reason: .tooMany, message: message)
    }

    /// What is announced after a pick: the number added, then each refusal.
    static func announcement(
        accepted: Int,
        rejected: [MonarchFileUploadRejection],
        labels: MonarchFileUploadLabels
    ) -> String? {
        var parts: [String] = []
        if accepted > 0 { parts.append(labels.added?(accepted) ?? MonarchFileUploadStrings.added(accepted)) }
        parts.append(contentsOf: rejected.map(\.message))
        return parts.isEmpty ? nil : parts.joined(separator: " ")
    }

    /// Which media the photo picker offers: images and videos the allowed types accept.
    static func photoKinds(allowedContentTypes: [UTType]) -> (images: Bool, videos: Bool) {
        let accepts = { (media: UTType) in
            allowedContentTypes.isEmpty
                || allowedContentTypes.contains { media.conforms(to: $0) || $0.conforms(to: media) }
        }
        return (accepts(.image), accepts(.movie))
    }

    /// A copy of a received file in a new folder of the temporary directory,
    /// because the system removes the received file when the import ends.
    static func temporaryCopy(of url: URL) throws -> URL {
        let folder = FileManager.default.temporaryDirectory
            .appendingPathComponent("MonarchFileUpload", isDirectory: true)
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        let copy = folder.appendingPathComponent(url.lastPathComponent)
        try FileManager.default.copyItem(at: url, to: copy)
        return copy
    }
}
