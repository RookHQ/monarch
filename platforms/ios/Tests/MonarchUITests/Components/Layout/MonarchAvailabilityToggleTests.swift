import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAvailabilityToggleFixtures: ComponentFixtures {
    static let id = "availability-toggle"
    static let fixtures = [
        Fixture("available and away") {
            VStack {
                MonarchAvailabilityToggle(isAvailable: .constant(true))
                MonarchAvailabilityToggle(isAvailable: .constant(false))
            }
        },
        Fixture("with the hint") {
            VStack {
                MonarchAvailabilityToggle(isAvailable: .constant(true), showsHint: true)
                MonarchAvailabilityToggle(isAvailable: .constant(false), showsHint: true)
            }
        },
        Fixture("pending and disabled") {
            VStack {
                MonarchAvailabilityToggle(isAvailable: .constant(true), isPending: true, showsHint: true)
                MonarchAvailabilityToggle(isAvailable: .constant(false), showsHint: true).disabled(true)
            }
        },
        Fixture("overridden strings") {
            MonarchAvailabilityToggle(
                isAvailable: .constant(false),
                isPending: true,
                label: "Take new tickets",
                availableLabel: "Taking tickets",
                awayLabel: "Not taking tickets until you switch this back on",
                availableHint: "Round robin includes you.",
                awayHint: "Round robin skips you until you come back. Tickets you already have stay with you.",
                pendingLabel: "Updating",
                showsHint: true
            )
        },
    ]
}

@Suite("MonarchAvailabilityToggle")
struct MonarchAvailabilityToggleTests {
    typealias Tokens = MonarchAvailabilityToggleTokens

    @Test("The mark is the success role when available and the warning role when away")
    func mark() {
        #expect(Tokens.mark(isAvailable: true, isEnabled: true) == \MonarchColorTokens.successSolid)
        #expect(Tokens.mark(isAvailable: false, isEnabled: true) == \MonarchColorTokens.warningSolid)
    }

    @Test("A disabled mark uses the disabled role", arguments: [true, false])
    func disabledMark(isAvailable: Bool) {
        #expect(Tokens.mark(isAvailable: isAvailable, isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("VoiceOver reads the pending word before the explanation")
    func accessibilityHint() {
        #expect(Tokens.accessibilityHint(hint: nil, pending: nil).isEmpty)
        #expect(Tokens.accessibilityHint(hint: "Skips you.", pending: nil) == "Skips you.")
        #expect(Tokens.accessibilityHint(hint: nil, pending: "Saving") == "Saving")
        #expect(Tokens.accessibilityHint(hint: "Skips you.", pending: "Saving") == "Saving. Skips you.")
        #expect(Tokens.accessibilityHint(hint: "", pending: "Saving") == "Saving")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAvailabilityToggleStrings.label == "Available for new work")
        #expect(MonarchAvailabilityToggleStrings.available == "Available")
        #expect(MonarchAvailabilityToggleStrings.away == "Away")
        #expect(MonarchAvailabilityToggleStrings.availableHint == "Automatic assignment can give you new work.")
        #expect(
            MonarchAvailabilityToggleStrings.awayHint
                == "Automatic assignment skips you. You can still work on what you have."
        )
        #expect(MonarchAvailabilityToggleStrings.pending == "Saving")
    }

    @Test("The row is at least as tall as the touch target, and the hint makes it taller")
    @MainActor
    func height() {
        let plain = Host.layout(MonarchAvailabilityToggle(isAvailable: .constant(true)))
        let hinted = Host.layout(MonarchAvailabilityToggle(isAvailable: .constant(false), showsHint: true))
        #expect(plain.height >= MonarchAccessibility.minimumHitTarget)
        #expect(hinted.height > plain.height)
    }
}
