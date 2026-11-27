import MonarchUI
import SwiftUI

struct MonarchOtpPinInputSection: CataloguePage {
    static let title = "OTP and PIN input"

    @State private var code = ""
    @State private var completed = ""
    @State private var pin = ""
    @State private var invite = ""

    var body: some View {
        MonarchOtpPinInput(
            "Verification code",
            text: $code,
            helper: completed.isEmpty ? "Type six digits. 000000 is rejected." : "Completed with \(completed).",
            error: code == "000000" ? "That code is not valid." : nil,
            groupSize: 3
        ) { value in
            completed = value
        }

        CatalogueCaption("Masked PIN, large")
        MonarchOtpPinInput("PIN", text: $pin, length: 4, size: .lg, isMasked: true)

        CatalogueCaption("Letters and digits")
        MonarchOtpPinInput("Invite code", text: $invite, length: 8, mode: .alphanumeric, groupSize: 4)

        CatalogueCaption("Disabled")
        MonarchOtpPinInput("Code", text: .constant("1234"))
            .disabled(true)
    }
}
