import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct OfflineError: LocalizedError {
    var errorDescription: String? { "Check your connection and try again." }
}

struct MonarchSwitchSettingsListSection: CataloguePage {
    static let title = "Switch settings list"

    @State private var weekly = true
    @State private var mentions = true
    @State private var news = false
    @State private var shareData = false
    @State private var failsNextSave = true

    var body: some View {
        MonarchSwitchSettingsList("Email notifications", description: "Each setting saves as soon as you change it.") {
            MonarchSwitchSettingsRow(
                "Weekly summary",
                isOn: $weekly,
                description: "A summary of activity, sent on Mondays."
            ) {
                _ in try await Task.sleep(for: .milliseconds(800))
            }
            MonarchSwitchSettingsRow("Mentions", isOn: $mentions) { _ in
                try await Task.sleep(for: .milliseconds(800))
            }
            MonarchSwitchSettingsRow("Product news", isOn: $news) { _ in
                try await Task.sleep(for: .milliseconds(800))
            }
        }

        CatalogueCaption("The first save fails; Retry succeeds")
        MonarchSwitchSettingsList("Privacy") {
            MonarchSwitchSettingsRow("Share usage data", isOn: $shareData, description: "Helps us find problems.") {
                _ in
                try await Task.sleep(for: .milliseconds(800))
                if failsNextSave {
                    failsNextSave = false
                    throw OfflineError()
                }
            }
        }
    }
}
