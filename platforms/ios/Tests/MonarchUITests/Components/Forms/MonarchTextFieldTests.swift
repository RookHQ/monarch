import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTextFieldFixtures: ComponentFixtures {
    static let id = "text-field"
    static let fixtures = [
        Fixture("empty with prompt and helper") {
            MonarchTextField("Email", text: .constant(""), prompt: "name@example.com", helper: "Work address")
        },
        Fixture("secure with error") {
            MonarchTextField("Password", text: .constant("secret"), error: "Too short", isSecure: true)
        },
        Fixture("disabled") {
            MonarchTextField("Disabled", text: .constant("Read only")).disabled(true)
        },
        Fixture("required, multiline, read-only") {
            VStack {
                MonarchTextField("Name", text: .constant("Ana Silva"), isRequired: true)
                MonarchTextField(
                    "Notes",
                    text: .constant("A first line of notes.\nA second line, long enough to wrap at large sizes."),
                    axis: .vertical,
                    lineLimit: 2...6
                )
                MonarchTextField("Account ID", text: .constant("acct_1042"), helper: "Copy it.", isReadOnly: true)
            }
        },
        Fixture("accessories and a hidden label") {
            VStack {
                MonarchTextField("Amount", text: .constant("19.99")) {
                    Text(verbatim: "$").accessibilityHidden(true)
                } trailing: {
                    MonarchButton("Clear", systemImage: "xmark.circle.fill", variant: .ghost, isIconOnly: true) {}
                }
                MonarchTextField("Search", text: .constant(""), prompt: "Search", labelVisibility: .hidden)
            }
        },
    ]
}

@Suite("MonarchTextField")
struct MonarchTextFieldTests {
    @Test("VoiceOver reads the error before the helper text")
    func accessibilityHint() {
        #expect(MonarchTextFieldTokens.accessibilityHint(helper: nil, error: nil).isEmpty)
        #expect(MonarchTextFieldTokens.accessibilityHint(helper: "Work address", error: nil) == "Work address")
        #expect(MonarchTextFieldTokens.accessibilityHint(helper: "Work address", error: "") == "Work address")
        #expect(
            MonarchTextFieldTokens.accessibilityHint(helper: "Work address", error: "Too short") == "Error: Too short"
        )
    }

    @Test("The border shows the error unless the field is disabled")
    func border() {
        #expect(MonarchTextFieldTokens.border(hasError: false, isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(MonarchTextFieldTokens.border(hasError: true, isEnabled: true) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchTextFieldTokens.border(hasError: true, isEnabled: false) == \MonarchColorTokens.borderSubtle)
        #expect(MonarchTextFieldTokens.background(isEnabled: true) == \MonarchColorTokens.bgSurface)
        #expect(MonarchTextFieldTokens.background(isEnabled: false) == \MonarchColorTokens.bgSubtle)
    }

    @Test("The field is at least as tall as the touch target")
    func hitTarget() {
        #expect(MonarchTextFieldTokens.minHeight >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The error prefix comes from the component's strings table")
    func strings() {
        #expect(MonarchTextFieldStrings.error("Too short") == "Error: Too short")
    }
}

@Suite("MonarchTextField options")
@MainActor
struct MonarchTextFieldOptionTests {
    private typealias Tokens = MonarchTextFieldTokens

    @Test("The hint reads the error, the help, read only and required, in that order")
    func hint() {
        let hint = MonarchFieldChrome.accessibilityHint(
            helper: "Work address",
            error: "Too short",
            isRequired: true,
            isReadOnly: true
        )
        #expect(hint == "Error: Too short, Work address, Read only, Required")
        #expect(MonarchFieldChrome.accessibilityHint(helper: nil, error: nil, isRequired: true) == "Required")
        let custom = MonarchFormFieldLabels(required: "Obligatoire", readOnly: "Lecture seule")
        #expect(
            MonarchFieldChrome.accessibilityHint(
                helper: nil,
                error: nil,
                isRequired: true,
                isReadOnly: true,
                labels: custom
            ) == "Lecture seule, Obligatoire"
        )
    }

    @Test("A read-only field sits on the subtle fill with a subtle border, unless it has an error")
    func readOnlyChrome() {
        #expect(MonarchFieldChrome.background(isEnabled: true, isReadOnly: true) == \MonarchColorTokens.bgSubtle)
        #expect(MonarchFieldChrome.background(isEnabled: true, isReadOnly: false) == \MonarchColorTokens.bgSurface)
        #expect(
            MonarchFieldChrome.border(hasError: false, isEnabled: true, isReadOnly: true)
                == \MonarchColorTokens.borderSubtle
        )
        #expect(
            MonarchFieldChrome.border(hasError: true, isEnabled: true, isReadOnly: true)
                == \MonarchColorTokens.dangerStrong
        )
        #expect(Tokens.border(hasError: false, isEnabled: true) == \MonarchColorTokens.borderInput)
    }

    @Test("Line limits: one line across, the range or no limit when multiline")
    func lineLimits() {
        #expect(Tokens.lineLimit(axis: .horizontal, range: 2...5) == 1...1)
        #expect(Tokens.lineLimit(axis: .vertical, range: 2...5) == 2...5)
        #expect(Tokens.lineLimit(axis: .vertical, range: 0...0) == 1...1)
        #expect(Tokens.lineLimit(axis: .vertical, range: nil) == nil)
    }

    @Test("A hidden label is not drawn, so the field is shorter")
    func hiddenLabel() {
        #expect(!Tokens.showsLabel(.hidden))
        #expect(Tokens.showsLabel(.visible))
        #expect(Tokens.showsLabel(.automatic))
        let shown = Host.layout(MonarchTextField("Email", text: .constant("")))
        let hidden = Host.layout(MonarchTextField("Email", text: .constant(""), labelVisibility: .hidden))
        #expect(hidden.height < shown.height)
    }

    @Test("A multiline field with several lines is taller than one line")
    func multilineGrows() {
        let text = "One\nTwo\nThree\nFour"
        let single = Host.layout(MonarchTextField("Notes", text: .constant(text)))
        let multi = Host.layout(MonarchTextField("Notes", text: .constant(text), axis: .vertical, lineLimit: 4...4))
        #expect(multi.height > single.height)
    }

    @Test("Accessories lay out inside the field box, at least 44 points high")
    func accessories() {
        let field = MonarchTextField("Amount", text: .constant("1")) {
            Text(verbatim: "$")
        } trailing: {
            Text(verbatim: "USD")
        }
        #expect(Host.layout(field).height >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("A focus binding from the app is accepted")
    func focusBinding() {
        struct Probe: View {
            @FocusState var isFocused: Bool
            var body: some View { MonarchTextField("Email", text: .constant(""), focus: $isFocused) }
        }
        #expect(Host.layout(Probe()).height > 0)
    }
}
