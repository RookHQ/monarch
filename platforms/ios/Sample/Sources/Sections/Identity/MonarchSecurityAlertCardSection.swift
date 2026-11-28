import MonarchUI
import SwiftUI

struct MonarchSecurityAlertCardSection: CataloguePage {
    static let title = "Security alert card"

    private static func sample() -> [MonarchSecurityAlert] {
        let now = Date()
        return [
            MonarchSecurityAlert(
                id: "blocked",
                title: "Sign-in blocked from an unknown device",
                description: "Someone entered your password from a new location. We blocked the sign-in.",
                severity: .critical,
                date: now.addingTimeInterval(-120),
                actionTitle: "Change password"
            ),
            MonarchSecurityAlert(
                id: "firefox",
                title: "New sign-in from Firefox on Linux",
                description: "Berlin, Germany",
                severity: .medium,
                date: now.addingTimeInterval(-3_600),
                actionTitle: "Review sessions",
                actionURL: URL(string: "https://example.com/sessions")
            ),
            MonarchSecurityAlert(
                id: "password",
                title: "Password changed",
                severity: .info,
                date: now.addingTimeInterval(-86_400),
                isRead: true
            ),
        ]
    }

    @State private var alerts = Self.sample()
    @State private var state = MonarchSecurityAlertCardState.ready
    @State private var lastAction = "No action yet."

    var body: some View {
        Picker("State", selection: $state) {
            Text("Ready").tag(MonarchSecurityAlertCardState.ready)
            Text("Loading").tag(MonarchSecurityAlertCardState.loading)
            Text("Failed").tag(MonarchSecurityAlertCardState.failed)
        }
        .pickerStyle(.segmented)
        Text(lastAction).monarchTextStyle(.caption)
        MonarchSecurityAlertCard(
            alerts,
            state: state,
            onAction: { alert in lastAction = "Action on: \(alert.title)" },
            onMarkRead: { id in
                if let index = alerts.firstIndex(where: { $0.id == id }) { alerts[index].isRead = true }
            },
            onMarkAllRead: {
                for index in alerts.indices { alerts[index].isRead = true }
            },
            onDismiss: { id in alerts.removeAll { $0.id == id } },
            onRetry: { state = .ready }
        )
        MonarchButton("Reset", variant: .ghost, size: .sm) {
            alerts = Self.sample()
            state = .ready
        }
    }
}
