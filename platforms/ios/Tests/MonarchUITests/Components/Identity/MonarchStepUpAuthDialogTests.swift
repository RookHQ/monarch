import AuthenticationServices
import Foundation
import LocalAuthentication
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// A failure with its own description.
private struct WrongPassword: LocalizedError {
    var errorDescription: String? { "That password is not right." }
}

/// A failure without a description.
private struct BareError: Error {}

enum MonarchStepUpAuthDialogFixtures: ComponentFixtures {
    static let id = "step-up-auth-dialog"
    static let fixtures = [
        Fixture("password with forgot") {
            MonarchStepUpAuthDialog(
                description: "Deleting a workspace needs your password.",
                onForgot: {},
                onVerify: { _ in }
            )
        },
        Fixture("every method") {
            MonarchStepUpAuthDialog(methods: MonarchStepUpMethod.allCases) { _ in }
        },
        Fixture("code") {
            MonarchStepUpAuthDialog(methods: [.code, .password]) { _ in }
        },
        Fixture("passkey") {
            MonarchStepUpAuthDialog(methods: [.passkey]) { _ in }
        },
        Fixture("device owner") {
            MonarchStepUpAuthDialog(methods: [.deviceOwner, .password]) { _ in }
        },
        Fixture("custom title and labels") {
            MonarchStepUpAuthDialog(
                "Confirm to continue",
                description: "Revealing the API key needs you to sign in again.",
                labels: MonarchStepUpAuthDialogLabels(submit: "Continue", cancel: "Not now")
            ) { _ in }
        },
    ]
}

@Suite("MonarchStepUpAuthDialog")
struct MonarchStepUpAuthDialogTests {
    private let labels = MonarchStepUpAuthDialogLabels()

    @Test("Methods are unique, in order, and default to the password")
    func methods() {
        #expect(MonarchStepUpAuthDialogTokens.methods([]) == [.password])
        #expect(MonarchStepUpAuthDialogTokens.methods([.code, .password, .code]) == [.code, .password])
    }

    @Test("Each method has a name in the picker")
    func names() {
        #expect(MonarchStepUpAuthDialogTokens.methodName(.password, labels: labels) == "Password")
        #expect(MonarchStepUpAuthDialogTokens.methodName(.code, labels: labels) == "Authenticator app")
        #expect(MonarchStepUpAuthDialogTokens.methodName(.passkey, labels: labels) == "Passkey")
        #expect(MonarchStepUpAuthDialogTokens.methodName(.deviceOwner, labels: labels) == "This device")
        let custom = MonarchStepUpAuthDialogLabels(methodName: { $0 == .code ? "Code" : nil })
        #expect(MonarchStepUpAuthDialogTokens.methodName(.code, labels: custom) == "Code")
        #expect(MonarchStepUpAuthDialogTokens.methodName(.password, labels: custom) == "Password")
    }

    @Test("The primary button names the action of the method")
    func primaryTitle() {
        #expect(MonarchStepUpAuthDialogTokens.primaryTitle(.password, biometry: .faceID, labels: labels) == "Confirm")
        #expect(MonarchStepUpAuthDialogTokens.primaryTitle(.code, biometry: .faceID, labels: labels) == "Confirm")
        #expect(
            MonarchStepUpAuthDialogTokens.primaryTitle(.passkey, biometry: .faceID, labels: labels) == "Use passkey"
        )
        #expect(
            MonarchStepUpAuthDialogTokens.primaryTitle(.deviceOwner, biometry: .faceID, labels: labels) == "Use Face ID"
        )
        #expect(
            MonarchStepUpAuthDialogTokens.primaryTitle(.deviceOwner, biometry: .touchID, labels: labels)
                == "Use Touch ID"
        )
        #expect(
            MonarchStepUpAuthDialogTokens.primaryTitle(.deviceOwner, biometry: .opticID, labels: labels)
                == "Use Optic ID"
        )
        #expect(
            MonarchStepUpAuthDialogTokens.primaryTitle(.deviceOwner, biometry: .passcode, labels: labels)
                == "Use passcode"
        )
    }

    @Test("LocalAuthentication's biometry maps to the button")
    func biometry() {
        #expect(MonarchStepUpAuthDialogTokens.biometry(.faceID) == .faceID)
        #expect(MonarchStepUpAuthDialogTokens.biometry(.touchID) == .touchID)
        #expect(MonarchStepUpAuthDialogTokens.biometry(.opticID) == .opticID)
        #expect(MonarchStepUpAuthDialogTokens.biometry(.none) == .passcode)
    }

    @Test("An empty password or a short code is caught before verifying")
    func missingEntry() {
        #expect(
            MonarchStepUpAuthDialogTokens.missingEntry(.password, password: "", code: "", labels: labels)
                == "Enter your password."
        )
        #expect(MonarchStepUpAuthDialogTokens.missingEntry(.password, password: "x", code: "", labels: labels) == nil)
        #expect(
            MonarchStepUpAuthDialogTokens.missingEntry(.code, password: "", code: "12345", labels: labels)
                == "Enter all 6 digits."
        )
        #expect(MonarchStepUpAuthDialogTokens.missingEntry(.code, password: "", code: "123456", labels: labels) == nil)
        #expect(MonarchStepUpAuthDialogTokens.missingEntry(.passkey, password: "", code: "", labels: labels) == nil)
        #expect(MonarchStepUpAuthDialogTokens.missingEntry(.deviceOwner, password: "", code: "", labels: labels) == nil)
    }

    @Test("The credential carries what was entered for the method")
    func credential() {
        #expect(MonarchStepUpAuthDialogTokens.credential(.password, password: "pw", code: "1") == .password("pw"))
        #expect(MonarchStepUpAuthDialogTokens.credential(.code, password: "pw", code: "123456") == .code("123456"))
        #expect(MonarchStepUpAuthDialogTokens.credential(.passkey, password: "pw", code: "") == .passkey)
        #expect(MonarchStepUpCredential.code("1").method == .code)
        #expect(MonarchStepUpCredential.deviceOwner.method == .deviceOwner)
    }

    @Test("Cancelling a system sheet shows nothing")
    func cancellation() {
        #expect(MonarchStepUpAuthDialogTokens.isCancellation(CancellationError()))
        #expect(MonarchStepUpAuthDialogTokens.isCancellation(LAError(.userCancel)))
        #expect(MonarchStepUpAuthDialogTokens.isCancellation(LAError(.systemCancel)))
        #expect(MonarchStepUpAuthDialogTokens.isCancellation(ASAuthorizationError(.canceled)))
        #expect(!MonarchStepUpAuthDialogTokens.isCancellation(LAError(.authenticationFailed)))
        #expect(!MonarchStepUpAuthDialogTokens.isCancellation(WrongPassword()))
    }

    @Test("A failure reads the app's mapping, then the error, then the built-in text")
    func failureMessage() {
        #expect(
            MonarchStepUpAuthDialogTokens.failureMessage(WrongPassword(), custom: "Mapped", labels: labels) == "Mapped"
        )
        #expect(
            MonarchStepUpAuthDialogTokens.failureMessage(WrongPassword(), custom: nil, labels: labels)
                == "That password is not right."
        )
        #expect(
            MonarchStepUpAuthDialogTokens.failureMessage(BareError(), custom: nil, labels: labels)
                == "That did not work. Try again."
        )
    }

    @Test("Password and code errors go on the field, others in a notice")
    func errorPlacement() {
        #expect(MonarchStepUpAuthDialogTokens.showsErrorOnField(.password))
        #expect(MonarchStepUpAuthDialogTokens.showsErrorOnField(.code))
        #expect(!MonarchStepUpAuthDialogTokens.showsErrorOnField(.passkey))
        #expect(!MonarchStepUpAuthDialogTokens.showsErrorOnField(.deviceOwner))
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchStepUpAuthDialogStrings.title == "Confirm it’s you")
        #expect(MonarchStepUpAuthDialogStrings.codeHelper == "Enter the 6-digit code from your authenticator app.")
        for value in [
            MonarchStepUpAuthDialogStrings.description, MonarchStepUpAuthDialogStrings.methodPicker,
            MonarchStepUpAuthDialogStrings.passwordLabel, MonarchStepUpAuthDialogStrings.codeLabel,
            MonarchStepUpAuthDialogStrings.passkeyDescription, MonarchStepUpAuthDialogStrings.deviceOwnerDescription,
            MonarchStepUpAuthDialogStrings.deviceOwnerUnavailable, MonarchStepUpAuthDialogStrings.cancel,
            MonarchStepUpAuthDialogStrings.forgot,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
