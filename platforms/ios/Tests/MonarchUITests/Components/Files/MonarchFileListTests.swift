import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchFileListFixtures: ComponentFixtures {
    static let id = "file-list"

    static var files: [MonarchFileListItem] {
        [
            MonarchFileListItem(id: "1", name: "Q3 report.pdf", size: 2_400_000, meta: "Edited yesterday"),
            MonarchFileListItem(id: "2", name: "Budget 2027.xlsx", size: 48_000, badges: ["Shared"]),
            MonarchFileListItem(id: "3", name: "photo.heic", thumbnail: Image(systemName: "photo")),
            MonarchFileListItem(id: "4", name: "clip.mov", thumbnailURL: URL(filePath: "/missing.png")),
            MonarchFileListItem(id: "5", name: "Locked.zip", contentType: "application/zip", isDisabled: true),
        ]
    }

    static let fixtures = [
        Fixture("list, selectable, some selected, bulk actions, menu") {
            MonarchFileList(
                files,
                selection: .constant(["1", "2"]),
                label: "Project files",
                bulkActions: [
                    MonarchBulkAction(id: "download", title: "Download", systemImage: "arrow.down.circle") {},
                    MonarchBulkAction(id: "delete", title: "Delete", systemImage: "trash", isDestructive: true) {},
                ],
                onOpen: { _ in },
                menu: { _ in Button("Rename") {} }
            )
        },
        Fixture("grid, every selectable file selected") {
            MonarchFileList(files, selection: .constant(["1", "2", "3", "4"]), layout: .grid, onOpen: { _ in })
        },
        Fixture("list, read only") {
            MonarchFileList(files)
        },
        Fixture("grid with menu, nothing selected") {
            MonarchFileList(
                files,
                selection: .constant([]),
                layout: .grid,
                menu: { _ in Button("Delete", role: .destructive) {} }
            )
        },
        Fixture("long names") {
            MonarchFileList([
                MonarchFileListItem(
                    id: "long",
                    name: "A very long file name for the quarterly planning review of every team in the company.docx",
                    size: 120_000
                )
            ])
        },
        Fixture("only disabled files") {
            MonarchFileList([files[4]], selection: .constant([]))
        },
        Fixture("empty") {
            MonarchFileList([], selection: .constant([]))
        },
        Fixture("empty, custom labels") {
            MonarchFileList([], labels: MonarchFileListLabels(selectAll: "All", empty: "Nothing uploaded"))
        },
        Fixture("disabled") {
            MonarchFileList(files, selection: .constant(["1"]), onOpen: { _ in }).disabled(true)
        },
    ]
}

@Suite("MonarchFileList")
struct MonarchFileListTests {
    private typealias Tokens = MonarchFileListTokens
    private let files = MonarchFileListFixtures.files

    @Test("Select all acts on files that are not disabled")
    func selectable() {
        #expect(Tokens.selectableIDs(files) == ["1", "2", "3", "4"])
        #expect(Tokens.selectableIDs([files[4]]).isEmpty)
    }

    @Test("Only selected files in the list are counted")
    func count() {
        #expect(Tokens.selectedCount(files, selection: ["1", "5", "gone"]) == 2)
        #expect(Tokens.selectedCount(files, selection: []) == 0)
    }

    @Test("Selecting, clearing one file and clearing all")
    func setting() {
        #expect(Tokens.setting("1", isOn: true, in: []) == ["1"])
        #expect(Tokens.setting("1", isOn: false, in: ["1", "2"]) == ["2"])
        #expect(Tokens.cleared(["1", "2"]).isEmpty)
    }

    @Test("Cards are wide enough for a name and meta line")
    func cardWidth() {
        #expect(Tokens.minimumCardWidth >= MonarchAccessibility.minimumHitTarget * 3)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchFileListStrings.selectAll == "Select all files")
        #expect(MonarchFileListStrings.empty == "No files yet")
    }
}
