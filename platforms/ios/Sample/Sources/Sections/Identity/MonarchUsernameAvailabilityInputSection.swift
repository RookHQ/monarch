import MonarchUI
import SwiftUI

struct MonarchUsernameAvailabilityInputSection: CataloguePage {
    static let title = "Username availability input"

    @State private var handle = ""
    @State private var status = MonarchUsernameAvailabilityStatus.idle

    var body: some View {
        MonarchUsernameAvailabilityInput(
            "Handle",
            text: $handle,
            prompt: "your-name",
            helper: "Try \"admin\" (taken) or \"offline\" (the check fails).",
            prefix: "rook.com/",
            transform: { $0.lowercased() },
            onStatusChange: { newStatus, _ in status = newStatus },
            validate: { value in
                // A stand-in for a request to the app's own service.
                try await Task.sleep(for: .milliseconds(600))
                if value == "offline" { throw URLError(.notConnectedToInternet) }
                if value.count < 3 {
                    return MonarchUsernameAvailabilityResult(isValid: false, message: "Use at least 3 characters.")
                }
                return MonarchUsernameAvailabilityResult(isValid: value != "admin")
            }
        )
        MonarchButton("Create account", variant: .primary) {}
            .disabled(status != .valid)

        CatalogueCaption("Disabled")
        MonarchUsernameAvailabilityInput("Handle", text: .constant(""), prefix: "rook.com/") { _ in nil }
            .disabled(true)
    }
}
