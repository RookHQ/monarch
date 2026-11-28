import Foundation
import MonarchTokens
import SwiftUI
import Testing
import UniformTypeIdentifiers

@testable import MonarchUI

enum MonarchMediaUploadFixtures: ComponentFixtures {
    static let id = "media-upload"

    static let imageURL = URL(fileURLWithPath: "/nonexistent/cover.png")
    static let videoURL = URL(fileURLWithPath: "/nonexistent/tour.mov")

    static func upload(_ file: MonarchFileUploadFile, _ context: MonarchMediaUploadContext) async throws
        -> MonarchMediaUploadResult
    {
        MonarchMediaUploadResult(url: file.url)
    }

    static let fixtures = [
        Fixture("empty, images only, with help and a reason for no video") {
            MonarchMediaUpload(
                "Cover image",
                url: .constant(nil),
                helper: "Shown at the top of the page.",
                maxImageSize: 10_000_000,
                videoUnavailable: "Videos can’t be added to this item.",
                onUpload: upload
            )
        },
        Fixture("empty, images and videos with limits") {
            MonarchMediaUpload(
                "Tile media",
                url: .constant(nil),
                kinds: [.image, .video],
                maxImageSize: 10_000_000,
                maxVideoSize: 20_000_000,
                maxVideoDuration: 90,
                onUpload: upload
            )
        },
        Fixture("filled with an image") {
            MonarchMediaUpload("Cover image", url: .constant(imageURL), onUpload: upload)
        },
        Fixture("filled with a video") {
            MonarchMediaUpload("Tile media", url: .constant(videoURL), kinds: [.image, .video], onUpload: upload)
        },
        Fixture("long label, disabled") {
            MonarchMediaUpload(
                "A banner image for the profile page that is shown on every device",
                url: .constant(imageURL),
                kind: .image,
                previewLabel: "The team at the summit",
                onUpload: upload
            )
            .disabled(true)
        },
        Fixture("preview: image") {
            MonarchMediaUploadPreview(
                url: imageURL,
                kind: .image,
                label: "Uploaded image",
                labels: MonarchMediaUploadLabels()
            )
        },
    ]
}

private struct MonarchMediaUploadTestError: LocalizedError {
    var errorDescription: String? { "Storage is full. Remove an old file and try again." }
}

private struct MonarchMediaUploadPlainError: Error {}

@Suite("MonarchMediaUpload")
@MainActor
struct MonarchMediaUploadTests {
    private typealias Tokens = MonarchMediaUploadTokens
    private let english = Locale(identifier: "en_US")
    private let labels = MonarchMediaUploadLabels()

    private func file(_ name: String, size: Int64? = nil, type: UTType? = nil) -> MonarchFileUploadFile {
        MonarchFileUploadFile(url: URL(fileURLWithPath: "/tmp/\(name)"), name: name, size: size, contentType: type)
    }

    @Test("Kinds come from the type, then the extension")
    func kinds() {
        #expect(Tokens.kind(of: file("a.mov", type: .quickTimeMovie)) == .video)
        #expect(Tokens.kind(of: file("a.mp4", type: .mpeg4Movie)) == .video)
        #expect(Tokens.kind(of: file("a.heic", type: .heic)) == .image)
        #expect(Tokens.kind(of: file("clip.mov")) == .video)
        #expect(Tokens.kind(of: file("photo.jpg")) == .image)
        #expect(Tokens.kind(of: URL(string: "https://cdn.example.com/a/clip.webm?x=1")!) == .video)
        #expect(Tokens.kind(of: URL(string: "https://cdn.example.com/a/cover")!) == .image)
    }

    @Test("Videos are offered when taken or when their refusal is explained")
    func allowedTypes() {
        #expect(Tokens.allowedContentTypes(kinds: [.image], explainsVideo: false) == [.image])
        #expect(Tokens.allowedContentTypes(kinds: [.image], explainsVideo: true) == [.image, .movie])
        #expect(Tokens.allowedContentTypes(kinds: [.image, .video], explainsVideo: false) == [.image, .movie])
    }

    @Test("Refusals name the file and the limit")
    func refusals() {
        let video = file("clip.mov", size: 30_000_000, type: .quickTimeMovie)
        #expect(
            Tokens.refusal(
                video,
                kinds: [.image],
                maxImageSize: nil,
                maxVideoSize: nil,
                labels: labels,
                locale: english
            )
                == "clip.mov is a video. Only images can be added here."
        )
        #expect(
            Tokens.refusal(
                video,
                kinds: [.image, .video],
                maxImageSize: nil,
                maxVideoSize: 20_000_000,
                labels: labels,
                locale: english
            ) == "clip.mov is larger than 20 MB."
        )
        let image = file("cover.png", size: 1_000, type: .png)
        #expect(
            Tokens.refusal(
                image,
                kinds: [.image],
                maxImageSize: 10_000,
                maxVideoSize: 1,
                labels: labels,
                locale: english
            )
                == nil
        )
        #expect(
            Tokens.refusal(
                file("raw.png"),
                kinds: [.image],
                maxImageSize: 1,
                maxVideoSize: nil,
                labels: labels,
                locale: english
            )
                == nil
        )
        #expect(
            Tokens.lengthRefusal(name: "clip.mov", seconds: 91, maxVideoDuration: 90, labels: labels, locale: english)
                == "clip.mov is longer than 1 min, 30 sec."
        )
        #expect(
            Tokens.lengthRefusal(name: "clip.mov", seconds: 90, maxVideoDuration: 90, labels: labels, locale: english)
                == nil
        )
        let custom = MonarchMediaUploadLabels(videoNotAllowed: { "No video: \($0)" })
        #expect(
            Tokens.refusal(
                video,
                kinds: [.image],
                maxImageSize: nil,
                maxVideoSize: nil,
                labels: custom,
                locale: english
            )
                == "No video: clip.mov"
        )
    }

    @Test("The hint has a line per kind, with the limits and the reason for no video")
    func hint() {
        #expect(
            Tokens.hint(
                kinds: [.image],
                maxImageSize: 10_000_000,
                maxVideoSize: nil,
                maxVideoDuration: nil,
                videoUnavailable: "Videos need the Pro plan.",
                labels: labels,
                locale: english
            ) == "Images up to 10 MB\nVideos need the Pro plan."
        )
        #expect(
            Tokens.hint(
                kinds: [.image, .video],
                maxImageSize: nil,
                maxVideoSize: 20_000_000,
                maxVideoDuration: 90,
                videoUnavailable: "ignored",
                labels: labels,
                locale: english
            ) == "Images\nVideos up to 20 MB, 1 min, 30 sec at most"
        )
        #expect(
            Tokens.hint(
                kinds: [.video],
                maxImageSize: nil,
                maxVideoSize: nil,
                maxVideoDuration: 45,
                videoUnavailable: nil,
                labels: labels,
                locale: english
            ) == "Videos, 45 sec at most"
        )
    }

    @Test("Titles and announcements follow the kind")
    func titles() {
        #expect(Tokens.addTitle(kinds: [.image], labels: labels) == "Add an image")
        #expect(Tokens.addTitle(kinds: [.image, .video], labels: labels) == "Add an image or video")
        #expect(Tokens.replaceTitle(.video, labels: labels) == "Replace video")
        #expect(Tokens.removeTitle(.image, labels: labels) == "Remove image")
        #expect(Tokens.uploaded(.video, labels: labels) == "Video uploaded")
        #expect(Tokens.removed(.image, labels: labels) == "Image removed")
        #expect(Tokens.previewLabel(.image, custom: nil, labels: labels) == "Uploaded image")
        #expect(Tokens.previewLabel(.image, custom: "The team", labels: labels) == "The team")
    }

    @Test("Progress text, with the percentage once reported")
    func progressText() {
        #expect(Tokens.progressText(.checking("clip.mov"), labels: labels, locale: english) == "Checking clip.mov…")
        #expect(Tokens.progressText(.uploading("a.png", nil), labels: labels, locale: english) == "Uploading a.png")
        #expect(
            Tokens.progressText(.uploading("a.png", 0.4), labels: labels, locale: english) == "Uploading a.png · 40%"
        )
        #expect(Tokens.clamped(1.5) == 1)
        #expect(Tokens.clamped(-1) == 0)
        #expect(Tokens.clamped(.nan) == 0)
    }

    @Test("Failures show the error's description, or the built-in message")
    func failureMessage() {
        #expect(
            Tokens.failureMessage(MonarchMediaUploadTestError(), labels: labels)
                == "Storage is full. Remove an old file and try again."
        )
        #expect(
            Tokens.failureMessage(MonarchMediaUploadPlainError(), labels: labels) == "The upload failed. Try again."
        )
    }

    @Test("An upload reports progress, then returns the result with its warnings")
    func uploadSucceeds() async {
        let model = MonarchMediaUploadModel()
        let result = await model.upload(
            file("cover.png", type: .png),
            kind: .image,
            maxVideoDuration: 90,
            labels: labels,
            locale: english,
            readDuration: { _ in
                Issue.record("An image's length must not be read")
                return 0
            },
            upload: { file, context in
                #expect(context.kind == .image)
                #expect(model.phase == .uploading("cover.png", nil))
                model.report(0.5, generation: model.currentGeneration)
                #expect(model.phase == .uploading("cover.png", 0.5))
                return MonarchMediaUploadResult(url: file.url, warnings: ["Resized to 2048 pixels."])
            }
        )
        #expect(result?.url.lastPathComponent == "cover.png")
        #expect(model.phase == .idle)
        #expect(model.warnings == ["Resized to 2048 pixels."])
        #expect(model.error == nil)
    }

    @Test("A video that is too long or unreadable is refused before upload")
    func videoChecks() async {
        let model = MonarchMediaUploadModel()
        let tooLong = await model.upload(
            file("clip.mov", type: .quickTimeMovie),
            kind: .video,
            maxVideoDuration: 90,
            labels: labels,
            locale: english,
            readDuration: { _ in 120 },
            upload: { _, _ in
                Issue.record("A refused video must not be uploaded")
                return MonarchMediaUploadResult(url: URL(fileURLWithPath: "/x"))
            }
        )
        #expect(tooLong == nil)
        #expect(model.error == "clip.mov is longer than 1 min, 30 sec.")
        let unreadable = await model.upload(
            file("clip.mov", type: .quickTimeMovie),
            kind: .video,
            maxVideoDuration: 90,
            labels: labels,
            locale: english,
            readDuration: { _ in throw MonarchMediaUploadPlainError() },
            upload: { file, _ in MonarchMediaUploadResult(url: file.url) }
        )
        #expect(unreadable == nil)
        #expect(model.error == "The length of clip.mov could not be read.")
        #expect(model.phase == .idle)
    }

    @Test("A failed upload shows its message; a cancelled one changes nothing")
    func failureAndCancel() async {
        let model = MonarchMediaUploadModel()
        let failed = await model.upload(
            file("a.png"),
            kind: .image,
            maxVideoDuration: nil,
            labels: labels,
            locale: english,
            readDuration: { _ in 0 },
            upload: { _, _ in throw MonarchMediaUploadTestError() }
        )
        #expect(failed == nil)
        #expect(model.error == "Storage is full. Remove an old file and try again.")
        let cancelled = await model.upload(
            file("b.png"),
            kind: .image,
            maxVideoDuration: nil,
            labels: labels,
            locale: english,
            readDuration: { _ in 0 },
            upload: { file, _ in
                model.cancel()
                return MonarchMediaUploadResult(url: file.url)
            }
        )
        #expect(cancelled == nil)
        #expect(model.phase == .idle)
        #expect(model.error == nil)
        model.report(0.9, generation: model.currentGeneration - 1)
        #expect(model.phase == .idle)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchMediaUploadStrings.addImage == "Add an image")
        #expect(MonarchMediaUploadStrings.cancel == "Cancel upload")
        #expect(MonarchMediaUploadStrings.cancelled == "Upload cancelled")
        #expect(MonarchMediaUploadStrings.warnings == "Uploaded with warnings.")
        #expect(MonarchMediaUploadStrings.imagesUpTo("5 MB") == "Images up to 5 MB")
        #expect(MonarchMediaUploadStrings.videosUpToAtMost("5 MB", "1 min") == "Videos up to 5 MB, 1 min at most")
        #expect(MonarchMediaUploadStrings.checking("a.mov") == "Checking a.mov…")
        #expect(MonarchMediaUploadStrings.close == "Close")
    }
}
