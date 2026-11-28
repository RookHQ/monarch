import MonarchTokens
import SwiftUI
import Testing
import UniformTypeIdentifiers

@testable import MonarchUI

enum MonarchFileTypeIconFixtures: ComponentFixtures {
    static let id = "file-type-icon"
    static let fixtures = [
        Fixture("every family, small") { row(.sm) },
        Fixture("every family, medium") { row(.md) },
        Fixture("every family, large with tile") { row(.lg) },
        Fixture("from a file name beside its name") {
            HStack {
                MonarchFileTypeIcon(fileName: "Quarterly report with a very long name 2026.pdf", isDecorative: true)
                Text("Quarterly report with a very long name 2026.pdf").monarchTextStyle(.body)
            }
        },
        Fixture("custom label") { MonarchFileTypeIcon(.other, size: .lg, label: "Sketch file") },
    ]

    @MainActor private static func row(_ size: MonarchFileTypeIconSize) -> some View {
        VStack(alignment: .leading) {
            HStack {
                ForEach(MonarchFileTypeFamily.allCases.prefix(6), id: \.self) { MonarchFileTypeIcon($0, size: size) }
            }
            HStack {
                ForEach(MonarchFileTypeFamily.allCases.dropFirst(6), id: \.self) { MonarchFileTypeIcon($0, size: size) }
            }
        }
    }
}

@Suite("MonarchFileTypeIcon")
struct MonarchFileTypeIconTests {
    @Test("The extension decides the family")
    func familyFromExtension() {
        #expect(MonarchFileTypeFamily(fileName: "photo.HEIC") == .image)
        #expect(MonarchFileTypeFamily(fileName: "clip.mov") == .video)
        #expect(MonarchFileTypeFamily(fileName: "song.flac") == .audio)
        #expect(MonarchFileTypeFamily(fileName: "report.pdf") == .pdf)
        #expect(MonarchFileTypeFamily(fileName: "letter.docx") == .document)
        #expect(MonarchFileTypeFamily(fileName: "budget.csv") == .spreadsheet)
        #expect(MonarchFileTypeFamily(fileName: "deck.key") == .presentation)
        #expect(MonarchFileTypeFamily(fileName: "backup.tar.gz") == .archive)
        #expect(MonarchFileTypeFamily(fileName: "App.swift") == .code)
        #expect(MonarchFileTypeFamily(fileName: "notes.md") == .text)
        #expect(MonarchFileTypeFamily(fileName: "data.unknownext") == .other)
    }

    @Test("Every listed extension maps to its own family")
    func extensionTable() {
        for (family, list) in MonarchFileTypeRules.extensions {
            for item in list {
                #expect(MonarchFileTypeFamily(fileName: "file.\(item)") == family, "\(item)")
            }
        }
    }

    @Test("Names without a usable extension are other")
    func noExtension() {
        #expect(MonarchFileTypeFamily(fileName: nil) == .other)
        #expect(MonarchFileTypeFamily(fileName: "") == .other)
        #expect(MonarchFileTypeFamily(fileName: "README") == .other)
        #expect(MonarchFileTypeFamily(fileName: ".env") == .other)
        #expect(MonarchFileTypeRules.fileExtension("/tmp/archive.v2/photo.final.PNG") == "png")
        #expect(MonarchFileTypeRules.fileExtension("C:\\docs\\a.TXT") == "txt")
        #expect(MonarchFileTypeRules.fileExtension("folder.d/file").isEmpty)
    }

    @Test("A specific MIME type wins over the extension")
    func familyFromMIMEType() {
        #expect(MonarchFileTypeFamily(fileName: "download.bin", contentType: "image/png") == .image)
        #expect(MonarchFileTypeFamily(fileName: "a.txt", contentType: "application/pdf") == .pdf)
        #expect(MonarchFileTypeFamily(fileName: nil, contentType: "Text/CSV; charset=utf-8") == .spreadsheet)
        #expect(
            MonarchFileTypeFamily(
                fileName: nil,
                contentType: "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ) == .document
        )
        #expect(
            MonarchFileTypeFamily(
                fileName: nil,
                contentType: "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            ) == .presentation
        )
        #expect(MonarchFileTypeFamily(fileName: nil, contentType: "application/zip") == .archive)
        #expect(MonarchFileTypeFamily(fileName: nil, contentType: "application/json") == .code)
        #expect(MonarchFileTypeFamily(fileName: nil, contentType: "text/plain") == .text)
    }

    @Test("A generic or empty MIME type falls back to the extension")
    func genericMIMEType() {
        #expect(MonarchFileTypeFamily(fileName: "report.pdf", contentType: "application/octet-stream") == .pdf)
        #expect(MonarchFileTypeFamily(fileName: "report.pdf", contentType: "") == .pdf)
        #expect(MonarchFileTypeFamily(fileName: "report.pdf", contentType: "application/x-unknown-thing") == .pdf)
        #expect(MonarchFileTypeFamily(fileName: nil, contentType: "application/octet-stream") == .other)
    }

    @Test("A uniform type maps to its family, most specific first")
    func familyFromUniformType() {
        #expect(MonarchFileTypeFamily(.png) == .image)
        #expect(MonarchFileTypeFamily(.svg) == .image)
        #expect(MonarchFileTypeFamily(.quickTimeMovie) == .video)
        #expect(MonarchFileTypeFamily(.mp3) == .audio)
        #expect(MonarchFileTypeFamily(.pdf) == .pdf)
        #expect(MonarchFileTypeFamily(.rtf) == .document)
        #expect(MonarchFileTypeFamily(.commaSeparatedText) == .spreadsheet)
        #expect(MonarchFileTypeFamily(.presentation) == .presentation)
        #expect(MonarchFileTypeFamily(.zip) == .archive)
        #expect(MonarchFileTypeFamily(.swiftSource) == .code)
        #expect(MonarchFileTypeFamily(.html) == .code)
        #expect(MonarchFileTypeFamily(.plainText) == .text)
        #expect(MonarchFileTypeFamily(.data) == .other)
        #expect(MonarchFileTypeFamily(.folder) == .other)
    }

    @Test("Every family has its own symbol, so the type does not depend on colour")
    func symbols() {
        let symbols = MonarchFileTypeFamily.allCases.map(MonarchFileTypeIconTokens.symbol)
        #expect(Set(symbols).count == MonarchFileTypeFamily.allCases.count)
        #expect(symbols.allSatisfy { !$0.isEmpty })
    }

    @Test("Families are tinted from the data palette as on the web")
    func colors() {
        #expect(MonarchFileTypeIconTokens.color(.image) == \MonarchColorTokens.data1)
        #expect(MonarchFileTypeIconTokens.color(.video) == \MonarchColorTokens.data1)
        #expect(MonarchFileTypeIconTokens.color(.document) == \MonarchColorTokens.data2)
        #expect(MonarchFileTypeIconTokens.color(.text) == \MonarchColorTokens.data2)
        #expect(MonarchFileTypeIconTokens.color(.spreadsheet) == \MonarchColorTokens.data3)
        #expect(MonarchFileTypeIconTokens.color(.code) == \MonarchColorTokens.data3)
        #expect(MonarchFileTypeIconTokens.color(.audio) == \MonarchColorTokens.data4)
        #expect(MonarchFileTypeIconTokens.color(.presentation) == \MonarchColorTokens.data4)
        #expect(MonarchFileTypeIconTokens.color(.pdf) == \MonarchColorTokens.data5)
        #expect(MonarchFileTypeIconTokens.color(.archive) == \MonarchColorTokens.textMuted)
        #expect(MonarchFileTypeIconTokens.color(.other) == \MonarchColorTokens.textMuted)
    }

    @Test("Only the large size has a tile")
    func sizes() {
        #expect(MonarchFileTypeIconTokens.iconSize(.sm) == .sm)
        #expect(MonarchFileTypeIconTokens.iconSize(.md) == .md)
        #expect(MonarchFileTypeIconTokens.iconSize(.lg) == .lg)
        #expect(!MonarchFileTypeIconTokens.hasTile(.sm))
        #expect(!MonarchFileTypeIconTokens.hasTile(.md))
        #expect(MonarchFileTypeIconTokens.hasTile(.lg))
    }

    @Test("VoiceOver reads the family name unless a label replaces it")
    func accessibilityLabel() {
        #expect(MonarchFileTypeIconTokens.accessibilityLabel(family: .pdf, label: nil) == "PDF document")
        #expect(MonarchFileTypeIconTokens.accessibilityLabel(family: .other, label: "") == "File")
        #expect(MonarchFileTypeIconTokens.accessibilityLabel(family: .other, label: "Sketch file") == "Sketch file")
    }

    @Test("Every family has a built-in name in the strings table")
    func strings() {
        for family in MonarchFileTypeFamily.allCases {
            let name = MonarchFileTypeIconStrings.name(family)
            #expect(!name.isEmpty && !name.hasPrefix("monarch."), "\(family)")
        }
        #expect(MonarchFileTypeIconStrings.name(.text) == "Text file")
    }
}
