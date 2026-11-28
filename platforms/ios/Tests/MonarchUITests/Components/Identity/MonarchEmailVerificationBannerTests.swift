import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct SendFailure: LocalizedError {
    var errorDescription: String? { "Too many requests. Wait a minute." }
}

private func cooldown(_ build: (inout MonarchResendCooldownState) -> Void) -> MonarchResendCooldownState {
    var state = MonarchResendCooldownState()
    build(&state)
    return state
}

enum MonarchEmailVerificationBannerFixtures: ComponentFixtures {
    static let id = "email-verification-banner"
    static let fixtures = [
        Fixture("warning with every button") {
            MonarchEmailVerificationBanner(email: "ana@example.com", onChangeEmail: {}, onDismiss: {}) {}
        },
        Fixture("info, page placement") {
            MonarchEmailVerificationBanner(email: "ana@example.com", tone: .info, placement: .page) {}
        },
        Fixture("sent, cooling down") {
            MonarchEmailVerificationBanner(
                email: "ana@example.com",
                state: cooldown {
                    _ = $0.begin()
                    $0.succeed(cooldown: 42)
                }
            )
        },
        Fixture("sending") {
            MonarchEmailVerificationBanner(email: "ana@example.com", state: cooldown { _ = $0.begin() })
        },
        Fixture("failed, long address") {
            MonarchEmailVerificationBanner(
                email: "a.very.long.address.that.has.to.wrap@subdomain.example-company.com",
                state: cooldown {
                    _ = $0.begin()
                    $0.fail(message: "Couldn’t send the email. Try again.")
                }
            )
        },
    ]
}

@Suite("MonarchEmailVerificationBanner")
struct MonarchEmailVerificationBannerTests {
    private let labels = MonarchEmailVerificationBannerLabels()

    @Test("A send starts the cooldown; sends during it are refused")
    func cooldownFlow() {
        var state = MonarchResendCooldownState()
        #expect(state.canSend)
        let started1 = state.begin()

        #expect(started1)
        #expect(state.isSending)
        let started2 = state.begin()

        #expect(!started2)
        state.succeed(cooldown: 2)
        #expect(state.didSend)
        #expect(state.remaining == 2)
        let started3 = state.begin()

        #expect(!started3)
        state.tick()
        state.tick()
        state.tick()
        #expect(state.remaining == 0)
        let started4 = state.begin()

        #expect(started4)
        #expect(!state.didSend)
    }

    @Test("A failure keeps Resend available and shows its message")
    func failure() {
        var state = MonarchResendCooldownState(remaining: -5)
        #expect(state.remaining == 0)
        _ = state.begin()
        state.fail(message: "Offline")
        #expect(state.errorMessage == "Offline")
        #expect(state.canSend)
        _ = state.begin()
        #expect(state.errorMessage == nil)
    }

    @Test("An initial cooldown blocks the first send")
    func initialCooldown() {
        var state = MonarchResendCooldownState(remaining: 30)
        let started5 = state.begin()

        #expect(!started5)
    }

    @Test("The resend title counts down")
    func resendTitle() {
        #expect(MonarchEmailVerificationBannerTokens.resendTitle(remaining: 0, labels: labels) == "Resend email")
        #expect(MonarchEmailVerificationBannerTokens.resendTitle(remaining: 42, labels: labels) == "Resend in 42s")
        let custom = MonarchEmailVerificationBannerLabels(resendIn: { "Encore \($0) s" })
        #expect(MonarchEmailVerificationBannerTokens.resendTitle(remaining: 3, labels: custom) == "Encore 3 s")
    }

    @Test("The message names the address")
    func message() {
        #expect(
            MonarchEmailVerificationBannerTokens.message(email: "ana@example.com", labels: labels)
                == "We sent a verification link to ana@example.com. Open it to confirm the address belongs to you."
        )
    }

    @Test("A failure reads the error's description, or the fallback")
    func errorMessage() {
        #expect(
            MonarchEmailVerificationBannerTokens.errorMessage(SendFailure(), labels: labels)
                == "Too many requests. Wait a minute."
        )
        #expect(
            MonarchEmailVerificationBannerTokens.errorMessage(CancellationError(), labels: labels)
                == "Couldn’t send the email. Try again."
        )
    }

    @Test("Only info and warning are drawn")
    func tone() {
        #expect(MonarchEmailVerificationBannerTokens.effectiveTone(.info) == .info)
        #expect(MonarchEmailVerificationBannerTokens.effectiveTone(.warning) == .warning)
        #expect(MonarchEmailVerificationBannerTokens.effectiveTone(.danger) == .warning)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchEmailVerificationBannerStrings.title == "Verify your email address")
        #expect(MonarchEmailVerificationBannerStrings.sent == "Verification email sent")
        #expect(MonarchEmailVerificationBannerStrings.changeEmail == "Change email")
        #expect(MonarchEmailVerificationBannerStrings.dismiss == "Dismiss")
    }
}
