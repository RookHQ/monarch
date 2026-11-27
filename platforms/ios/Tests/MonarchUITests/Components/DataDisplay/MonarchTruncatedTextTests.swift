import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTruncatedTextFixtures: ComponentFixtures {
    static let id = "truncated-text"
    private static let long =
        "The supplier confirmed the delivery for Thursday, but the warehouse is closed that day, "
        + "so the driver needs the side entrance code and a contact number. Call the site manager "
        + "before noon to arrange access, and update the order once the pallets are signed for."
    static let fixtures = [
        Fixture("long text, cut") {
            MonarchTruncatedText(long)
        },
        Fixture("one line, not expandable") {
            MonarchTruncatedText(long, lineLimit: 1, isExpandable: false)
        },
        Fixture("expanded by a binding, custom titles") {
            MonarchTruncatedText(
                long,
                lineLimit: 2,
                isExpanded: .constant(true),
                showMoreLabel: "Read the note",
                showLessLabel: "Hide the note"
            )
        },
        Fixture("short text and empty text") {
            VStack {
                MonarchTruncatedText("Short.")
                MonarchTruncatedText("", lineLimit: 0)
                Text("After")
            }
        },
    ]
}

@Suite("MonarchTruncatedText")
struct MonarchTruncatedTextTests {
    @Test("The line limit is at least one line")
    func lineLimit() {
        #expect(MonarchTruncatedTextTokens.lineLimit(3) == 3)
        #expect(MonarchTruncatedTextTokens.lineLimit(0) == 1)
        #expect(MonarchTruncatedTextTokens.lineLimit(-4) == 1)
    }

    @Test("Text is cut only when the whole text is higher than the limited lines")
    func isTruncated() {
        #expect(MonarchTruncatedTextTokens.isTruncated(fullHeight: 120, limitedHeight: 60))
        #expect(!MonarchTruncatedTextTokens.isTruncated(fullHeight: 60, limitedHeight: 60))
        #expect(!MonarchTruncatedTextTokens.isTruncated(fullHeight: 60.5, limitedHeight: 60))
        #expect(!MonarchTruncatedTextTokens.isTruncated(fullHeight: 0, limitedHeight: 0))
    }

    @Test("The button shows for cut text and stays while expanded")
    func showsButton() {
        #expect(MonarchTruncatedTextTokens.showsButton(isExpandable: true, isTruncated: true, isExpanded: false))
        #expect(MonarchTruncatedTextTokens.showsButton(isExpandable: true, isTruncated: false, isExpanded: true))
        #expect(!MonarchTruncatedTextTokens.showsButton(isExpandable: true, isTruncated: false, isExpanded: false))
        #expect(!MonarchTruncatedTextTokens.showsButton(isExpandable: false, isTruncated: true, isExpanded: false))
        #expect(!MonarchTruncatedTextTokens.showsButton(isExpandable: false, isTruncated: true, isExpanded: true))
    }

    @Test("The button title says what the button does and can be replaced")
    func buttonTitle() {
        #expect(
            MonarchTruncatedTextTokens.buttonTitle(isExpanded: false, showMoreLabel: nil, showLessLabel: nil)
                == "Show more"
        )
        #expect(
            MonarchTruncatedTextTokens.buttonTitle(isExpanded: true, showMoreLabel: nil, showLessLabel: nil)
                == "Show less"
        )
        #expect(
            MonarchTruncatedTextTokens.buttonTitle(isExpanded: false, showMoreLabel: "Read on", showLessLabel: "Hide")
                == "Read on"
        )
        #expect(
            MonarchTruncatedTextTokens.buttonTitle(isExpanded: true, showMoreLabel: "Read on", showLessLabel: "Hide")
                == "Hide"
        )
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchTruncatedTextStrings.showMore != "monarch.truncatedText.showMore")
        #expect(MonarchTruncatedTextStrings.showLess != "monarch.truncatedText.showLess")
    }

    @Test("Colours come from the text and link roles")
    func colours() {
        #expect(MonarchTruncatedTextTokens.text == \MonarchColorTokens.textDefault)
        #expect(MonarchTruncatedTextTokens.button == \MonarchColorTokens.textLink)
    }

    @Test("An expanded text is higher than the same text cut to two lines")
    @MainActor
    func expandedIsHigher() {
        let long = String(repeating: "Delivery notes for the warehouse team. ", count: 12)
        let cut = Host.layout(MonarchTruncatedText(long, lineLimit: 2, isExpanded: .constant(false)))
        let whole = Host.layout(MonarchTruncatedText(long, lineLimit: 2, isExpanded: .constant(true)))
        #expect(whole.height > cut.height)
    }
}
