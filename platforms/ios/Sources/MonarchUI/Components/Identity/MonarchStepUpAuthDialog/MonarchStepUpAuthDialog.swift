import LocalAuthentication
import MonarchTokens
import SwiftUI

/// Asks a signed-in person to confirm it is still them before a high-risk
/// action: deleting a workspace or account, erasing data, revoking keys,
/// viewing a secret. Present it in a sheet.
///
/// ```swift
/// .sheet(isPresented: $isConfirming) {
///     MonarchStepUpAuthDialog(
///         description: "Deleting a workspace needs your password.",
///         methods: [.password, .code, .deviceOwner],
///         onVerified: { deleteWorkspace() },
///         onVerify: { credential in try await session.reauthenticate(credential) }
///     )
/// }
/// ```
///
/// It offers the account password (the current-password content type, so
/// iOS can fill it), a 6-digit authenticator code that submits itself when
/// complete, a passkey, and the device owner check (Face ID, Touch ID,
/// Optic ID or the passcode through LocalAuthentication). With several
/// methods, a segmented picker switches between them. Verification runs
/// inside the sheet: it cannot be swiped away while checking, a wrong
/// password or code is shown on the field, and the code is cleared so it can
/// be typed again. On success the sheet calls `onVerified` and closes itself.
/// For a passkey, run `ASAuthorizationController` in `onVerify`. The device
/// owner check runs before `onVerify` and needs `NSFaceIDUsageDescription`
/// in the app's Info.plist.
///
/// Accessibility: the title is a header. Errors are shown in words on the
/// field or in a notice and announced. The system Face ID and passkey sheets
/// are the system's. Every button has a 44-point touch target.
public struct MonarchStepUpAuthDialog: View {
    private let title: String?
    private let description: String?
    private let methods: [MonarchStepUpMethod]
    private let labels: MonarchStepUpAuthDialogLabels
    private let errorMessage: (@MainActor (any Error) -> String?)?
    private let onForgot: (@MainActor () -> Void)?
    private let onCancel: (@MainActor () -> Void)?
    private let onVerified: (@MainActor () -> Void)?
    private let onVerify: @MainActor (MonarchStepUpCredential) async throws -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss
    @State private var chosen: MonarchStepUpMethod?
    @State private var password = ""
    @State private var code = ""
    @State private var isVerifying = false
    @State private var isVerified = false
    @State private var missing: String?
    @State private var failure: String?
    @State private var biometry = MonarchStepUpBiometry.passcode

    /// A step-up authentication dialog, the content of a sheet.
    ///
    /// - Parameters:
    ///   - title: Replaces the built-in title "Confirm it’s you".
    ///   - description: Why the person is asked, such as "Deleting a workspace needs your password." Also the
    ///     reason the system shows with Face ID.
    ///   - methods: The methods offered, most preferred first; the first is shown first. The password when empty.
    ///   - labels: Replacements for the built-in strings.
    ///   - errorMessage: Turns a thrown error into a message. Return `nil` to use the error's description or the
    ///     built-in failure.
    ///   - onForgot: Starts password recovery. Shows "Forgot your password?" under the password field.
    ///   - onCancel: Called when the sheet closes without verifying.
    ///   - onVerified: Called after `onVerify` succeeds, just before the sheet closes.
    ///   - onVerify: Checks the credential. Return when it is accepted; throw to show the failure.
    public init(
        _ title: String? = nil,
        description: String? = nil,
        methods: [MonarchStepUpMethod] = [.password],
        labels: MonarchStepUpAuthDialogLabels = MonarchStepUpAuthDialogLabels(),
        errorMessage: (@MainActor (any Error) -> String?)? = nil,
        onForgot: (@MainActor () -> Void)? = nil,
        onCancel: (@MainActor () -> Void)? = nil,
        onVerified: (@MainActor () -> Void)? = nil,
        onVerify: @escaping @MainActor (MonarchStepUpCredential) async throws -> Void
    ) {
        self.title = title
        self.description = description
        self.methods = MonarchStepUpAuthDialogTokens.methods(methods)
        self.labels = labels
        self.errorMessage = errorMessage
        self.onForgot = onForgot
        self.onCancel = onCancel
        self.onVerified = onVerified
        self.onVerify = onVerify
    }

    private var method: MonarchStepUpMethod {
        if let chosen, methods.contains(chosen) { return chosen }
        return methods[0]
    }

    private var reason: String {
        description ?? labels.description ?? MonarchStepUpAuthDialogStrings.description
    }

    /// The content of the view.
    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonarchStepUpAuthDialogTokens.sectionSpacing) {
                header
                if methods.count > 1 {
                    methodPicker
                }
                fields
                if let failure, !MonarchStepUpAuthDialogTokens.showsErrorOnField(method) {
                    MonarchInlineNotice(failure, tone: .danger)
                }
                VStack(spacing: MonarchStepUpAuthDialogTokens.buttonSpacing) {
                    MonarchButton(
                        MonarchStepUpAuthDialogTokens.primaryTitle(method, biometry: biometry, labels: labels),
                        variant: .primary,
                        isLoading: isVerifying,
                        fullWidth: true,
                        action: submit
                    )
                    MonarchButton(
                        labels.cancel ?? MonarchStepUpAuthDialogStrings.cancel,
                        variant: .ghost,
                        fullWidth: true
                    ) {
                        dismiss()
                    }
                    .disabled(isVerifying)
                }
            }
            .padding(MonarchStepUpAuthDialogTokens.padding)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(theme.colors.bgSurface)
        .interactiveDismissDisabled(isVerifying)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .onAppear(perform: readBiometry)
        .onDisappear {
            if !isVerified { onCancel?() }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: MonarchStepUpAuthDialogTokens.textSpacing) {
            MonarchIcon(systemName: MonarchStepUpAuthDialogTokens.symbol, size: .md)
                .foregroundStyle(theme.colors[role: MonarchStepUpAuthDialogTokens.symbolRole])
                .frame(width: MonarchTokens.Space.space10, height: MonarchTokens.Space.space10)
                .background(theme.colors.bgMuted, in: Circle())
                .padding(.bottom, MonarchTokens.Space.space2)
            Text(title ?? labels.title ?? MonarchStepUpAuthDialogStrings.title)
                .monarchTextStyle(.heading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            Text(reason)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var methodPicker: some View {
        let selection = Binding(
            get: { method },
            set: { next in
                chosen = next
                missing = nil
                failure = nil
            }
        )
        let label = labels.methodPicker ?? MonarchStepUpAuthDialogStrings.methodPicker
        return ViewThatFits(in: .horizontal) {
            Picker(label, selection: selection) {
                ForEach(methods) { item in
                    Text(MonarchStepUpAuthDialogTokens.methodName(item, labels: labels)).tag(item)
                }
            }
            .pickerStyle(.segmented)
            .fixedSize()
            MonarchSelectField(
                label,
                selection: selection,
                valueText: MonarchStepUpAuthDialogTokens.methodName(method, labels: labels),
                style: .compact
            ) {
                ForEach(methods) { item in
                    Text(MonarchStepUpAuthDialogTokens.methodName(item, labels: labels)).tag(item)
                }
            }
        }
        .disabled(isVerifying)
    }

    @ViewBuilder private var fields: some View {
        let fieldError = missing ?? (MonarchStepUpAuthDialogTokens.showsErrorOnField(method) ? failure : nil)
        switch method {
        case .password:
            VStack(alignment: .leading, spacing: MonarchStepUpAuthDialogTokens.buttonSpacing) {
                MonarchPasswordInput(
                    labels.passwordLabel ?? MonarchStepUpAuthDialogStrings.passwordLabel,
                    text: $password,
                    error: fieldError,
                    purpose: .current,
                    isRequired: true
                )
                .onSubmit(submit)
                .onChange(of: password) { missing = nil }
                .disabled(isVerifying)
                if let onForgot {
                    Button(labels.forgot ?? MonarchStepUpAuthDialogStrings.forgot, action: onForgot)
                        .buttonStyle(.monarchLink(.standalone))
                        .monarchTextStyle(.caption)
                        .disabled(isVerifying)
                }
            }
        case .code:
            MonarchOtpPinInput(
                labels.codeLabel ?? MonarchStepUpAuthDialogStrings.codeLabel,
                text: $code,
                length: MonarchStepUpAuthDialogTokens.codeLength,
                helper: labels.codeHelper ?? MonarchStepUpAuthDialogStrings.codeHelper,
                error: fieldError,
                groupSize: MonarchStepUpAuthDialogTokens.codeGroupSize
            ) { _ in
                submit()
            }
            .onChange(of: code) { missing = nil }
            .disabled(isVerifying)
        case .passkey:
            Text(labels.passkeyDescription ?? MonarchStepUpAuthDialogStrings.passkeyDescription)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
        case .deviceOwner:
            Text(labels.deviceOwnerDescription ?? MonarchStepUpAuthDialogStrings.deviceOwnerDescription)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
        }
    }

    private func readBiometry() {
        guard methods.contains(.deviceOwner) else { return }
        let context = LAContext()
        _ = context.canEvaluatePolicy(.deviceOwnerAuthentication, error: nil)
        biometry = MonarchStepUpAuthDialogTokens.biometry(context.biometryType)
    }

    private func submit() {
        guard !isVerifying else { return }
        let current = method
        if let missing = MonarchStepUpAuthDialogTokens.missingEntry(
            current,
            password: password,
            code: code,
            labels: labels
        ) {
            self.missing = missing
            AccessibilityNotification.Announcement(missing).post()
            return
        }
        missing = nil
        failure = nil
        isVerifying = true
        let credential = MonarchStepUpAuthDialogTokens.credential(current, password: password, code: code)
        let reason = reason
        Task { @MainActor in
            do {
                if current == .deviceOwner {
                    try await confirmDeviceOwner(reason: reason)
                }
                try await onVerify(credential)
                isVerifying = false
                isVerified = true
                onVerified?()
                dismiss()
            } catch {
                isVerifying = false
                if current == .code { code = "" }
                guard !MonarchStepUpAuthDialogTokens.isCancellation(error) else { return }
                let message = MonarchStepUpAuthDialogTokens.failureMessage(
                    error,
                    custom: errorMessage?(error),
                    labels: labels
                )
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    /// Runs the system's Face ID, Touch ID, Optic ID or passcode check.
    private func confirmDeviceOwner(reason: String) async throws {
        let context = LAContext()
        var problem: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &problem) else {
            throw DeviceOwnerUnavailable(
                message: labels.deviceOwnerUnavailable ?? MonarchStepUpAuthDialogStrings.deviceOwnerUnavailable
            )
        }
        try await context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason)
    }
}

/// The device has no passcode, so the device owner cannot be checked.
private struct DeviceOwnerUnavailable: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

#if DEBUG
    #Preview("MonarchStepUpAuthDialog") {
        MonarchPreviewMatrix {
            VStack {
                MonarchStepUpAuthDialog(
                    description: "Deleting a workspace needs your password.",
                    methods: [.password, .code, .passkey],
                    onForgot: {},
                    onVerify: { _ in }
                )
                MonarchStepUpAuthDialog(methods: [.code]) { _ in }
            }
        }
    }
#endif
