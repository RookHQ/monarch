import Foundation
import MonarchTokens
import SwiftUI
import Testing
import UniformTypeIdentifiers

@testable import MonarchUI

enum MonarchFileUploadFixtures: ComponentFixtures {
    static let id = "file-upload"

    static let fixtures = [
        Fixture("zone with limits and photos") {
            MonarchFileUpload(maxSize: 10_000_000, maxFiles: 5, allowsPhotos: true) { _, _ in }
        },
        Fixture("zone, custom hint and icon") {
            MonarchFileUpload(
                allowedContentTypes: [.pdf],
                hint: "PDF only, signed copies please.",
                systemImage: "doc.badge.plus"
            ) { _, _ in }
        },
        Fixture("compact") {
            MonarchFileUpload(allowedContentTypes: [.image], maxSize: 2_000_000, variant: .compact, allowsPhotos: true)
            {
                _,
                _ in
            }
        },
        Fixture("limit reached") {
            MonarchFileUpload(maxFiles: 3, currentCount: 3) { _, _ in }
        },
        Fixture("invalid") {
            MonarchFileUpload(isInvalid: true) { _, _ in }
        },
        Fixture("disabled") {
            MonarchFileUpload(maxFiles: 1) { _, _ in }.disabled(true)
        },
        Fixture("custom labels") {
            MonarchFileUpload(
                maxSize: 1_000,
                maxFiles: 2,
                labels: MonarchFileUploadLabels(
                    title: "Add attachments",
                    browse: "Pick",
                    limits: { size, count in "\(size ?? "") / \(count ?? 0)" }
                )
            ) { _, _ in }
        },
    ]
}

@Suite("MonarchFileUpload")
struct MonarchFileUploadTests {
    private typealias Tokens = MonarchFileUploadTokens
    private let english = Locale(identifier: "en_US")

    private func file(_ name: String, size: Int64? = nil, type: UTType? = nil) -> MonarchFileUploadFile {
        MonarchFileUploadFile(url: URL(filePath: "/tmp/\(name)"), name: name, size: size, contentType: type)
    }

    @Test("Types match by conformance, with the extension as a fallback")
    func matching() {
        #expect(Tokens.matches(file("a.png", type: .png), allowedContentTypes: [.image]))
        #expect(Tokens.matches(file("a.pdf"), allowedContentTypes: [.pdf]))
        #expect(!Tokens.matches(file("a.pdf"), allowedContentTypes: [.image]))
        #expect(Tokens.matches(file("README"), allowedContentTypes: [.item]))
        #expect(!Tokens.matches(file("README"), allowedContentTypes: [.image]))
        #expect(Tokens.matches(file("a.bin"), allowedContentTypes: []))
    }

    @Test("Files are checked for type, size, the app's rule and count, in that order")
    func validation() {
        let files = [
            file("a.pdf", size: 100),
            file("b.exe", size: 100),
            file("c.pdf", size: 5_000_000),
            file("d.pdf", size: 100),
            file("e.pdf", size: 100),
            file("f.pdf", size: 100),
        ]
        let result = Tokens.validate(
            files,
            allowedContentTypes: [.pdf],
            maxSize: 1_000_000,
            maxFiles: 3,
            currentCount: 1,
            labels: MonarchFileUploadLabels(),
            locale: english,
            validate: { $0.name == "d.pdf" ? "d.pdf is already attached." : nil }
        )
        #expect(result.accepted.map(\.name) == ["a.pdf", "e.pdf"])
        #expect(result.rejected.map(\.file.name) == ["b.exe", "c.pdf", "d.pdf", "f.pdf"])
        #expect(result.rejected.map(\.reason) == [.invalidType, .tooLarge, .custom, .tooMany])
        #expect(result.rejected[0].message == "b.exe is not an allowed file type.")
        #expect(result.rejected[1].message == "c.pdf is larger than 1 MB.")
        #expect(result.rejected[2].message == "d.pdf is already attached.")
        #expect(result.rejected[3].message == "f.pdf was not added: up to 3 files.")
    }

    @Test("The public validate gives the same answer")
    func publicValidate() {
        let result = MonarchFileUpload.validate([file("a.png", size: 10, type: .png)], allowedContentTypes: [.pdf])
        #expect(result.accepted.isEmpty)
        #expect(result.rejected.first?.reason == .invalidType)
    }

    @Test("A file of unknown size passes the size check")
    func unknownSize() {
        let result = Tokens.validate(
            [file("a.pdf")],
            allowedContentTypes: [.pdf],
            maxSize: 1,
            maxFiles: nil,
            currentCount: 0,
            labels: MonarchFileUploadLabels(),
            locale: english,
            validate: nil
        )
        #expect(result.accepted.count == 1)
    }

    @Test("Messages can be replaced")
    func customMessages() {
        let labels = MonarchFileUploadLabels(
            invalidType: { "Wrong type: \($0)" },
            tooLarge: { "\($0) > \($1)" },
            tooMany: { "\($0) over \($1)" }
        )
        let result = Tokens.validate(
            [file("a.txt"), file("b.pdf", size: 2_000), file("c.pdf"), file("d.pdf")],
            allowedContentTypes: [.pdf],
            maxSize: 1_000,
            maxFiles: 1,
            currentCount: 0,
            labels: labels,
            locale: english,
            validate: nil
        )
        #expect(
            result.rejected.map(\.message) == [
                "Wrong type: a.txt", "b.pdf > \(Tokens.formattedSize(1_000, locale: english))", "d.pdf over 1",
            ]
        )
    }

    @Test("The hint is made from the limits, and says when the limit is reached")
    func hint() {
        let labels = MonarchFileUploadLabels()
        #expect(
            Tokens.hint(nil, maxSize: 10_000_000, maxFiles: 5, currentCount: 0, labels: labels, locale: english)
                == "Up to 10 MB each · 5 files max"
        )
        #expect(
            Tokens.hint(nil, maxSize: nil, maxFiles: 1, currentCount: 0, labels: labels, locale: english)
                == "1 file max"
        )
        #expect(Tokens.hint(nil, maxSize: nil, maxFiles: nil, currentCount: 0, labels: labels, locale: english) == nil)
        #expect(
            Tokens.hint("PDF only", maxSize: 1, maxFiles: nil, currentCount: 0, labels: labels, locale: english)
                == "PDF only"
        )
        #expect(
            Tokens.hint("PDF only", maxSize: nil, maxFiles: 2, currentCount: 2, labels: labels, locale: english)
                == "Limit reached: 2 files attached."
        )
        #expect(
            Tokens.hint(nil, maxSize: nil, maxFiles: 1, currentCount: 1, labels: labels, locale: english)
                == "Limit reached: 1 file attached."
        )
    }

    @Test("Multiple selection follows the limit unless asked, and the room left")
    func counts() {
        #expect(Tokens.allowsMultiple(nil, maxFiles: nil))
        #expect(!Tokens.allowsMultiple(nil, maxFiles: 1))
        #expect(Tokens.allowsMultiple(true, maxFiles: 1))
        #expect(!Tokens.allowsMultiple(false, maxFiles: 5))
        #expect(Tokens.isFull(maxFiles: 2, currentCount: 2))
        #expect(!Tokens.isFull(maxFiles: nil, currentCount: 99))
        #expect(Tokens.room(maxFiles: 5, currentCount: 2) == 3)
        #expect(Tokens.room(maxFiles: 1, currentCount: 4) == 0)
        #expect(Tokens.room(maxFiles: nil, currentCount: 4) == nil)
    }

    @Test("The photo picker offers what the types allow")
    func photoKinds() {
        #expect(Tokens.photoKinds(allowedContentTypes: [.item]).images == true)
        #expect(Tokens.photoKinds(allowedContentTypes: [.item]).videos == true)
        #expect(Tokens.photoKinds(allowedContentTypes: [.image]).images == true)
        #expect(Tokens.photoKinds(allowedContentTypes: [.image]).videos == false)
        #expect(Tokens.photoKinds(allowedContentTypes: [.png]).images == true)
        #expect(Tokens.photoKinds(allowedContentTypes: [.png]).videos == false)
        #expect(Tokens.photoKinds(allowedContentTypes: [.pdf]).images == false)
        #expect(Tokens.photoKinds(allowedContentTypes: [.pdf]).videos == false)
        #expect(Tokens.photoKinds(allowedContentTypes: [.movie]).images == false)
        #expect(Tokens.photoKinds(allowedContentTypes: [.movie]).videos == true)
    }

    @Test("The announcement says how many were added, then each refusal")
    func announcement() {
        let refusal = MonarchFileUploadRejection(file: file("a"), reason: .tooMany, message: "a was not added.")
        let labels = MonarchFileUploadLabels()
        #expect(
            Tokens.announcement(accepted: 2, rejected: [refusal], labels: labels) == "2 files added. a was not added."
        )
        #expect(Tokens.announcement(accepted: 1, rejected: [], labels: labels) == "1 file added.")
        #expect(Tokens.announcement(accepted: 0, rejected: [], labels: labels) == nil)
    }

    @Test("The area's colours for each state")
    func appearance() {
        let rest = Tokens.appearance(isTargeted: false, isInvalid: false, isEnabled: true)
        #expect(rest.border == \MonarchColorTokens.borderStrong)
        #expect(rest.fill == \MonarchColorTokens.bgSubtle)
        let targeted = Tokens.appearance(isTargeted: true, isInvalid: true, isEnabled: true)
        #expect(targeted.fill == \MonarchColorTokens.accentSubtle)
        #expect(targeted.border == \MonarchColorTokens.accentBorder)
        let invalid = Tokens.appearance(isTargeted: false, isInvalid: true, isEnabled: true)
        #expect(invalid.border == \MonarchColorTokens.dangerStrong)
        let blocked = Tokens.appearance(isTargeted: true, isInvalid: true, isEnabled: false)
        #expect(blocked.title == \MonarchColorTokens.textDisabled)
        #expect(blocked.fill == \MonarchColorTokens.bgMuted)
        #expect(Tokens.hintColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("A file read from disk takes its name, size and type")
    func describesFile() throws {
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: folder) }
        let url = folder.appendingPathComponent("notes.txt")
        try Data("hello".utf8).write(to: url)
        let described = MonarchFileUploadFile(url: url)
        #expect(described.name == "notes.txt")
        #expect(described.size == 5)
        #expect(described.contentType?.conforms(to: .plainText) == true)
        let copy = try Tokens.temporaryCopy(of: url)
        defer { try? FileManager.default.removeItem(at: copy.deletingLastPathComponent()) }
        #expect(copy != url)
        #expect(copy.lastPathComponent == "notes.txt")
        #expect(try Data(contentsOf: copy) == Data("hello".utf8))
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchFileUploadStrings.title == "Choose files to upload")
        #expect(MonarchFileUploadStrings.browse == "Browse files")
        #expect(MonarchFileUploadStrings.photos == "Choose photos")
        #expect(MonarchFileUploadStrings.dropHere == "Drop to upload")
        #expect(MonarchFileUploadStrings.tooMany("a", 1) == "a was not added: up to 1 file.")
        #expect(MonarchFileUploadStrings.limitSize("1 MB") == "Up to 1 MB each")
        #expect(MonarchFileUploadStrings.added(3) == "3 files added.")
    }
}
