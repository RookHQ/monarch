import CoreImage
import MonarchUI
import SwiftUI

/// Recipe: turn on two-step verification in three steps.
struct TwoFactorSetupSection: CataloguePage {
    static let title = "Two-factor setup (recipe)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL
    @State private var step = 0
    @State private var code = ""
    @State private var codeError: String?
    @State private var isVerifying = false
    @State private var result = "Two-step verification is off"

    private let secret = "JBSW Y3DP EHPK 3PXP"
    private let otpauth = URL(string: "otpauth://totp/Rook:ana@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Rook")
    private let codes: [MonarchRecoveryCode] = [
        "4f7k-2m9q", "8d3r-6t1w", "7n4h-3j8k", "2p6s-5l0z", "9a1e-4u7y", "3g8b-1q6r",
    ]
    private let steps = [
        MonarchStepperStep(id: "scan", label: "Connect your app"),
        MonarchStepperStep(id: "verify", label: "Enter a code"),
        MonarchStepperStep(id: "recovery", label: "Save recovery codes"),
    ]

    var body: some View {
        CatalogueCaption("The code 123456 is accepted")
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            MonarchStepper(steps, current: step)
            switch step {
            case 0: connect
            case 1: verify
            default: recovery
            }
        }
        Text(result).monarchTextStyle(.caption)
    }

    private var connect: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            Text("Set up two-step verification")
                .monarchTextStyle(.title)
                .accessibilityAddTraits(.isHeader)
            Text("Add your account to an authenticator app. On this phone, open the app directly.")
                .monarchTextStyle(.body)
            if let otpauth {
                MonarchButton("Open in authenticator app", systemImage: "key", variant: .primary, fullWidth: true) {
                    openURL(otpauth)
                }
            }
            MonarchCopyField("Setup key", value: secret)
            if let otpauth, let qr = qrCode(otpauth.absoluteString) {
                Text("On another device, scan this code instead.").monarchTextStyle(.caption)
                qr
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: theme.spacing.space24 * 2)
                    .accessibilityLabel("QR code for your authenticator app")
                    .accessibilityAddTraits(.isImage)
            }
            MonarchButton("Next", variant: .secondary) { step = 1 }
        }
    }

    private var verify: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            Text("Enter the code from your app")
                .monarchTextStyle(.title)
                .accessibilityAddTraits(.isHeader)
            MonarchOtpPinInput(
                "6-digit code",
                text: $code,
                helper: "The code changes every 30 seconds.",
                error: codeError
            ) { _ in
                verifyCode()
            }
            HStack(spacing: theme.spacing.space3) {
                MonarchButton("Back", variant: .ghost) { step = 0 }
                MonarchButton("Verify", variant: .primary, isLoading: isVerifying, action: verifyCode)
            }
        }
    }

    private var recovery: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            Text("Save your recovery codes")
                .monarchTextStyle(.title)
                .accessibilityAddTraits(.isHeader)
            MonarchRecoveryCodes(
                codes,
                onAcknowledge: {
                    result = "Two-step verification is on"
                    step = 0
                    code = ""
                }
            )
        }
    }

    private func verifyCode() {
        guard code.count == 6 else {
            codeError = "Enter all 6 digits."
            return
        }
        isVerifying = true
        Task {
            try? await Task.sleep(for: .milliseconds(600))
            isVerifying = false
            if code == "123456" {
                codeError = nil
                step = 2
            } else {
                codeError = "That code didn’t work. Check the time on your device and try the newest code."
            }
        }
    }

    /// The QR code of a text, drawn by Core Image's built-in generator.
    private func qrCode(_ text: String) -> Image? {
        guard let filter = CIFilter(name: "CIQRCodeGenerator") else { return nil }
        filter.setValue(Data(text.utf8), forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")
        guard let output = filter.outputImage,
            let image = CIContext().createCGImage(output, from: output.extent)
        else { return nil }
        return Image(decorative: image, scale: 1)
    }
}
