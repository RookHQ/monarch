import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchFileCardFixtures: ComponentFixtures {
    static let id = "file-card"

    static let fixtures = [
        Fixture("card, openable, selected") {
            MonarchFileCard("Q3 report.pdf", isSelected: .constant(true), size: 2_400_000, onOpen: {})
        },
        Fixture("card with menu, badges and meta") {
            MonarchFileCard(
                "Budget 2027.xlsx",
                isSelected: .constant(false),
                size: 48_000,
                meta: "Edited yesterday",
                badges: ["Shared", "Scanning"],
                onOpen: {},
                menu: {
                    Button("Rename") {}
                    Button("Delete", role: .destructive) {}
                }
            )
        },
        Fixture("card, local thumbnail, not openable") {
            MonarchFileCard("photo.heic", thumbnail: Image(systemName: "photo"))
        },
        Fixture("card, remote thumbnail without network") {
            MonarchFileCard("clip.mov", thumbnailURL: URL(filePath: "/missing.png"))
        },
        Fixture("long name") {
            MonarchFileCard(
                "A very long file name for the quarterly planning review of every team in the company.docx",
                size: 120_000,
                onOpen: {}
            )
        },
        Fixture("row with checkbox and menu") {
            MonarchFileCard(
                "notes.txt",
                isSelected: .constant(true),
                size: 900,
                badges: ["Shared"],
                layout: .row,
                onOpen: {},
                menu: { Button("Rename") {} }
            )
        },
        Fixture("row, plain") {
            MonarchFileCard("archive.zip", contentType: "application/zip", layout: .row)
        },
        Fixture("disabled") {
            MonarchFileCard("Q3 report.pdf", isSelected: .constant(true), size: 2_400_000, onOpen: {})
                .disabled(true)
        },
        Fixture("custom labels") {
            MonarchFileCard(
                "data.bin",
                layout: .row,
                labels: MonarchFileCardLabels(
                    select: { "Choose \($0)" },
                    actions: { "More for \($0)" },
                    type: "Binary"
                )
            )
        },
    ]
}

@Suite("MonarchFileCard")
struct MonarchFileCardTests {
    private let english = Locale(identifier: "en_US")

    @Test("The surface for every state")
    func appearance() {
        let rest = MonarchFileCardTokens.appearance(isSelected: false, isPressed: false, isEnabled: true)
        #expect(rest == .init(background: \.bgSurface, border: \.borderSubtle))
        let pressed = MonarchFileCardTokens.appearance(isSelected: false, isPressed: true, isEnabled: true)
        #expect(pressed == .init(background: \.bgSubtle, border: \.borderDefault))
        let selected = MonarchFileCardTokens.appearance(isSelected: true, isPressed: true, isEnabled: true)
        #expect(selected == .init(background: \.accentSubtle, border: \.accentBorder))
        let disabled = MonarchFileCardTokens.appearance(isSelected: true, isPressed: false, isEnabled: false)
        #expect(disabled == .init(background: \.bgSubtle, border: \.borderSubtle))
    }

    @Test("A selected card has a thicker border, not only another colour")
    func border() {
        #expect(
            MonarchFileCardTokens.borderWidth(isSelected: true) > MonarchFileCardTokens.borderWidth(isSelected: false)
        )
    }

    @Test("Text dims when disabled")
    func text() {
        #expect(MonarchFileCardTokens.nameRole(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchFileCardTokens.nameRole(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchFileCardTokens.metaRole(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchFileCardTokens.metaRole(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("The meta line is the type, the size and the app's meta")
    func meta() {
        let parts = MonarchFileCardTokens.metaParts(
            family: .pdf,
            size: 2_400_000,
            meta: "Edited yesterday",
            typeLabel: nil,
            locale: english
        )
        #expect(parts.count == 3)
        #expect(parts[0] == MonarchFileTypeIconStrings.name(.pdf))
        #expect(parts[1].contains("MB"))
        #expect(parts[2] == "Edited yesterday")
        #expect(MonarchFileCardTokens.metaLine(["A", "B"]) == "A · B")
        let bare = MonarchFileCardTokens.metaParts(
            family: .other,
            size: nil,
            meta: "",
            typeLabel: "Binary",
            locale: english
        )
        #expect(bare == ["Binary"])
    }

    @Test("VoiceOver reads the meta parts and the badges")
    func accessibilityValue() {
        #expect(
            MonarchFileCardTokens.accessibilityValue(metaParts: ["PDF document", "2.4 MB"], badges: ["Shared", ""])
                == "PDF document, 2.4 MB, Shared"
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchFileCardStrings.select("a.pdf") == "Select a.pdf")
        #expect(MonarchFileCardStrings.actions("a.pdf") == "Actions for a.pdf")
    }
}
