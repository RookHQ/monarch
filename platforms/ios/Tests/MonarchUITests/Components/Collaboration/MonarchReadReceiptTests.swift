import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let readers = ["Ana Silva", "Ben Okafor", "Chen Wei", "Dara Kim", "Eli Roth"].map {
    MonarchReadReceiptReader(name: $0)
}

enum MonarchReadReceiptFixtures: ComponentFixtures {
    static let id = "read-receipt"
    static let fixtures = [
        Fixture("text, one reader") {
            MonarchReadReceipt(Array(readers.prefix(1)))
        },
        Fixture("count, a group") {
            MonarchReadReceipt(readers, display: .count)
        },
        Fixture("avatars, with overflow") {
            MonarchReadReceipt(readers, display: .avatars)
        },
        Fixture("avatars, as a button") {
            MonarchReadReceipt(Array(readers.prefix(2)), display: .avatars, onShowReaders: {})
        },
        Fixture("no readers draws nothing") {
            MonarchReadReceipt([])
        },
        Fixture("custom text, long names") {
            MonarchReadReceipt(
                [MonarchReadReceiptReader(name: "Maximilian Alexander von Hohenberg-Lichtenstein")],
                display: .count,
                labels: MonarchReadReceiptLabels(seenByCount: { "Gelesen von \($0) Personen in dieser Unterhaltung" })
            )
        },
    ]
}

@Suite("MonarchReadReceipt")
@MainActor
struct MonarchReadReceiptTests {
    typealias Tokens = MonarchReadReceiptTokens

    @Test("Each display shows its own text")
    func visibleText() {
        let labels = MonarchReadReceiptLabels()
        #expect(Tokens.visibleText(.text, count: 3, labels: labels) == "Seen")
        #expect(Tokens.visibleText(.avatars, count: 3, labels: labels) == "Seen")
        #expect(Tokens.visibleText(.count, count: 3, labels: labels) == "Seen by 3")
    }

    @Test("The full sentence lists every reader in the locale's list format")
    func sentence() {
        let english = Locale(identifier: "en_GB")
        let labels = MonarchReadReceiptLabels()
        #expect(Tokens.sentence(Array(readers.prefix(1)), labels: labels, locale: english) == "Seen by Ana Silva")
        #expect(
            Tokens.sentence(Array(readers.prefix(2)), labels: labels, locale: english)
                == "Seen by Ana Silva and Ben Okafor"
        )
        #expect(
            Tokens.sentence(Array(readers.prefix(3)), labels: labels, locale: english)
                == "Seen by Ana Silva, Ben Okafor and Chen Wei"
        )
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let labels = MonarchReadReceiptLabels(
            seen: "Gelesen",
            seenByCount: { "Gelesen von \($0)" },
            seenBy: { "Gelesen von \($0)" },
            list: { $0.joined(separator: " + ") }
        )
        #expect(Tokens.visibleText(.text, count: 2, labels: labels) == "Gelesen")
        #expect(Tokens.visibleText(.count, count: 2, labels: labels) == "Gelesen von 2")
        #expect(Tokens.sentence(Array(readers.prefix(2)), labels: labels) == "Gelesen von Ana Silva + Ben Okafor")
    }

    @Test("At least one avatar is shown and a +1 tile never appears")
    func visibleAvatars() {
        #expect(Tokens.visibleAvatars(total: 5, maxAvatars: 3) == 3)
        #expect(Tokens.visibleAvatars(total: 4, maxAvatars: 3) == 4)
        #expect(Tokens.visibleAvatars(total: 2, maxAvatars: 3) == 2)
        #expect(Tokens.visibleAvatars(total: 5, maxAvatars: 0) == 1)
    }

    @Test("A reader's id defaults to the name")
    func readerID() {
        #expect(MonarchReadReceiptReader(name: "Ana").id == "Ana")
        #expect(MonarchReadReceiptReader(id: "u1", name: "Ana").id == "u1")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchReadReceiptStrings.seen == "Seen")
        #expect(MonarchReadReceiptStrings.seenByCount(4) == "Seen by 4")
        #expect(MonarchReadReceiptStrings.seenBy("Ana") == "Seen by Ana")
    }

    @Test("An empty list draws nothing")
    func empty() {
        #expect(Host.layout(MonarchReadReceipt([])).height < 1)
        #expect(Host.layout(MonarchReadReceipt(Array(readers.prefix(1)))).height > 0)
    }

    #if os(iOS)
        @Test("A receipt that is a button is at least 44 points high")
        func touchTarget() {
            let size = Host.layout(MonarchReadReceipt(Array(readers.prefix(1)), onShowReaders: {}))
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
