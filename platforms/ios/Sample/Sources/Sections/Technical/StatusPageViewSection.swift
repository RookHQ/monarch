import MonarchUI
import SwiftUI

/// Recipe: a status page as an in-app screen.
struct StatusPageViewSection: CataloguePage {
    static let title = "Status page (recipe)"

    private struct Service: Identifiable {
        let id: String
        let name: String
        let state: String
        let days: [MonarchUptimeBarSegment]
    }

    private struct Incident: Identifiable {
        let id: String
        let title: String
        let state: String
        let updates: [MonarchIncidentUpdate]
    }

    private static let start = Date(timeIntervalSinceReferenceDate: 800_000_000)

    private static func history(downDay: Int?, degradedDay: Int?) -> [MonarchUptimeBarSegment] {
        (1...30).map { day in
            let status: MonarchUptimeBarStatus =
                day == downDay ? .down : day == degradedDay ? .degraded : .operational
            return MonarchUptimeBarSegment(id: "\(day)", label: "Day \(day)", status: status)
        }
    }

    private static let services = [
        Service(id: "api", name: "API", state: "operational", days: history(downDay: nil, degradedDay: 12)),
        Service(id: "checkout", name: "Checkout", state: "degraded", days: history(downDay: 30, degradedDay: 29)),
        Service(id: "web", name: "Website", state: "operational", days: history(downDay: nil, degradedDay: nil)),
    ]

    private static let incidents = [
        Incident(
            id: "inc-1",
            title: "Checkout payments are slow",
            state: "degraded",
            updates: [
                MonarchIncidentUpdate(
                    id: "1",
                    stage: .investigating,
                    body: "Some card payments take over 10 seconds.",
                    time: start,
                    isAutomated: true
                ),
                MonarchIncidentUpdate(
                    id: "2",
                    stage: .identified,
                    body: "A payment provider is answering slowly. We are routing around it.",
                    time: start.addingTimeInterval(1_200),
                    author: "Ada"
                ),
            ]
        )
    ]

    /// The worst state among services: down, then partial, degraded, maintenance, operational.
    private static func overall(_ states: [String]) -> String {
        let order = ["down", "partial", "degraded", "maintenance", "operational"]
        return order.first { states.contains($0) } ?? "unknown"
    }

    private static func overallTitle(_ state: String) -> (String, MonarchTone) {
        switch state {
        case "down": ("Service disruption", .danger)
        case "partial": ("Partial system outage", .warning)
        case "degraded": ("Degraded performance", .warning)
        case "maintenance": ("Under maintenance", .info)
        case "operational": ("All systems operational", .success)
        default: ("Awaiting first checks", .neutral)
        }
    }

    @Environment(\.monarchTheme) private var theme
    @State private var email = ""
    @State private var subscribeMessage: String?
    @State private var isSending = false

    var body: some View {
        let state = Self.overall(Self.services.map(\.state))
        let (title, tone) = Self.overallTitle(state)
        MonarchCalloutBanner(title, message: "Updated 2 minutes ago", tone: tone)

        CatalogueCaption("Active incidents")
        ForEach(Self.incidents) { incident in
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                HStack(alignment: .firstTextBaseline) {
                    Text(incident.title).monarchTextStyle(.bodyStrong)
                    Spacer(minLength: 0)
                    MonarchStatusBadge(incident.state, size: .sm)
                }
                MonarchIncidentTimeline(incident.updates, density: .compact)
            }
        }

        CatalogueCaption("Services")
        ForEach(Self.services) { service in
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                HStack(alignment: .firstTextBaseline) {
                    Text(service.name).monarchTextStyle(.bodyStrong)
                    Spacer(minLength: 0)
                    MonarchStatusBadge(service.state, size: .sm)
                }
                MonarchUptimeBar(
                    service.name,
                    segments: service.days,
                    startLabel: "30 days ago",
                    endLabel: "Today",
                    size: .sm
                )
            }
        }

        CatalogueCaption("Subscribe to updates")
        MonarchTextField(
            "Email",
            text: $email,
            prompt: "you@example.com",
            helper: subscribeMessage ?? "Get an email when an incident is opened, updated or resolved."
        )
        .textContentType(.emailAddress)
        .keyboardType(.emailAddress)
        .textInputAutocapitalization(.never)
        MonarchButton("Subscribe", variant: .primary, isLoading: isSending, loadingLabel: "Sending…") {
            isSending = true
            Task {
                try? await Task.sleep(for: .seconds(1))
                isSending = false
                subscribeMessage =
                    "Check your inbox. If that address can be subscribed, a confirmation link is on its way."
            }
        }
        .disabled(email.isEmpty)
    }
}
