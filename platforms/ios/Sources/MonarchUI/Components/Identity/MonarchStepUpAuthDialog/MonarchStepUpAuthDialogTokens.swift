import AuthenticationServices
import Foundation
import LocalAuthentication
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchStepUpAuthDialog``.
enum MonarchStepUpAuthDialogTokens {
    /// The gap between the parts of the dialog.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the title and the line under it.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding around the content.
    static let padding: CGFloat = MonarchTokens.Space.space6
    /// The length of an authenticator code.
    static let codeLength = 6
    /// The digits per group in the code field.
    static let codeGroupSize = 3
    /// The SF Symbol above the title.
    static let symbol = "lock"
    /// The colour role of the symbol.
    static var symbolRole: MonarchColorRole { \.textDefault }

    /// The methods offered: the given ones without repeats, or the password when none are given.
    static func methods(_ methods: [MonarchStepUpMethod]) -> [MonarchStepUpMethod] {
        var seen = Set<MonarchStepUpMethod>()
        let unique = methods.filter { seen.insert($0).inserted }
        return unique.isEmpty ? [.password] : unique
    }

    /// The name of a method.
    static func methodName(_ method: MonarchStepUpMethod, labels: MonarchStepUpAuthDialogLabels) -> String {
        labels.methodName?(method) ?? MonarchStepUpAuthDialogStrings.methodName(method)
    }

    /// The kind of device check from LocalAuthentication.
    static func biometry(_ type: LABiometryType) -> MonarchStepUpBiometry {
        switch type {
        case .faceID: .faceID
        case .touchID: .touchID
        case .opticID: .opticID
        case .none: .passcode
        @unknown default: .passcode
        }
    }

    /// The title of the primary button for a method.
    static func primaryTitle(
        _ method: MonarchStepUpMethod,
        biometry: MonarchStepUpBiometry,
        labels: MonarchStepUpAuthDialogLabels
    ) -> String {
        switch method {
        case .password, .code:
            labels.submit ?? MonarchStepUpAuthDialogStrings.submit
        case .passkey:
            labels.passkeyButton ?? MonarchStepUpAuthDialogStrings.passkeyButton
        case .deviceOwner:
            labels.deviceOwnerButton?(biometry) ?? MonarchStepUpAuthDialogStrings.deviceOwnerButton(biometry)
        }
    }

    /// The error to show before verifying, or `nil` when the entry is complete.
    static func missingEntry(
        _ method: MonarchStepUpMethod,
        password: String,
        code: String,
        labels: MonarchStepUpAuthDialogLabels
    ) -> String? {
        switch method {
        case .password:
            password.isEmpty ? labels.passwordRequired ?? MonarchStepUpAuthDialogStrings.passwordRequired : nil
        case .code:
            code.count < codeLength ? labels.codeIncomplete ?? MonarchStepUpAuthDialogStrings.codeIncomplete : nil
        case .passkey, .deviceOwner:
            nil
        }
    }

    /// The credential for a method, from what was entered.
    static func credential(_ method: MonarchStepUpMethod, password: String, code: String) -> MonarchStepUpCredential {
        switch method {
        case .password: .password(password)
        case .code: .code(code)
        case .passkey: .passkey
        case .deviceOwner: .deviceOwner
        }
    }

    /// Whether an error means the person cancelled a system sheet, so nothing
    /// is shown: Face ID or the passcode cancelled, the passkey sheet
    /// cancelled, or the task cancelled.
    static func isCancellation(_ error: any Error) -> Bool {
        if error is CancellationError { return true }
        if let error = error as? LAError {
            return [.userCancel, .appCancel, .systemCancel].contains(error.code)
        }
        if let error = error as? ASAuthorizationError { return error.code == .canceled }
        return false
    }

    /// The message of a failed verification: the app's own mapping, else the
    /// error's `LocalizedError` description, else the built-in failure.
    static func failureMessage(
        _ error: any Error,
        custom: String?,
        labels: MonarchStepUpAuthDialogLabels
    ) -> String {
        if let custom, !custom.isEmpty { return custom }
        return MonarchFailureMessage.message(
            for: error,
            fallback: labels.genericError ?? MonarchStepUpAuthDialogStrings.genericError
        )
    }

    /// Whether a failure is shown on the field (password and code) or in a notice.
    static func showsErrorOnField(_ method: MonarchStepUpMethod) -> Bool {
        method == .password || method == .code
    }
}
