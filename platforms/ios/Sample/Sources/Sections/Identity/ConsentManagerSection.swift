import AppTrackingTransparency
import MonarchUI
import SwiftUI

/// Native adoption: App Tracking Transparency for tracking across apps, and
/// MonarchConsentPreferences for the app's own choices.
struct ConsentManagerSection: CataloguePage {
    static let title = "Consent manager (App Tracking Transparency)"

    @Environment(\.monarchTheme) private var theme
    @State private var status = ATTrackingManager.trackingAuthorizationStatus
    @State private var showsChoices = false
    @State private var choices: [String: Bool] = [:]

    private let categories = [
        MonarchConsentPreferencesCategory(
            id: "necessary",
            label: "Strictly necessary",
            description: "Sign-in and security.",
            isRequired: true
        ),
        MonarchConsentPreferencesCategory(
            id: "analytics",
            label: "Analytics",
            description: "How the app is used, inside this app only."
        ),
    ]

    /// The request needs a usage description in Info.plist; the catalogue may not have one.
    private var hasUsageDescription: Bool {
        Bundle.main.object(forInfoDictionaryKey: "NSUserTrackingUsageDescription") != nil
    }

    private var statusText: String {
        switch status {
        case .notDetermined: "Not asked yet"
        case .restricted: "Restricted on this device"
        case .denied: "Not allowed"
        case .authorized: "Allowed"
        @unknown default: "Unknown"
        }
    }

    var body: some View {
        CatalogueCaption("Tracking across apps")
        Text("Status: \(statusText)").monarchTextStyle(.body)
        MonarchButton("Ask to track", variant: .primary) {
            Task { status = await ATTrackingManager.requestTrackingAuthorization() }
        }
        .disabled(!hasUsageDescription || status != .notDetermined)
        if !hasUsageDescription {
            Text("This app has no NSUserTrackingUsageDescription, so it does not ask.")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }

        CatalogueCaption("In-app choices")
        MonarchButton("Privacy choices") { showsChoices = true }
        Text("Analytics: \(choices["analytics"] == true ? "on" : "off")").monarchTextStyle(.caption)
            .sheet(isPresented: $showsChoices) {
                ScrollView {
                    MonarchConsentPreferences(categories: categories, choices: choices) { _, newChoices in
                        choices = newChoices
                        showsChoices = false
                    }
                    .padding()
                }
            }
    }
}
