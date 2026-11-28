import Foundation
import SwiftUI

/// The details sheet of one delivery: every fact as labeled content, the event ID with a copy
/// button, and Redeliver. The log-drawer recipe, built in.
struct MonarchWebhookDeliveryDetail: View {
    let delivery: MonarchWebhookDelivery
    let context: MonarchWebhookDeliveriesContext
    let canRedeliver: Bool
    let isRedelivering: Bool
    /// The outcome of the last redelivery from this screen: queued, failed, or none yet.
    let result: Bool?
    let onRedeliver: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let text = context.text
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: MonarchWebhookDeliveriesTokens.factSpacing) {
                    LabeledContent(text(\.status, "status")) {
                        MonarchStatusBadge(delivery.status, registry: context.registry, size: .sm)
                    }
                    LabeledContent(text(\.event, "event")) {
                        Text(delivery.eventType).monarchTextStyle(.code)
                    }
                    if let id = delivery.eventID {
                        HStack(alignment: .center, spacing: MonarchWebhookDeliveriesTokens.rowSpacing) {
                            LabeledContent(text(\.eventID, "eventID")) {
                                Text(id).monarchTextStyle(.code).textSelection(.enabled)
                            }
                            MonarchCopyButton(
                                value: id,
                                target: text(\.eventIDTarget, "eventIDTarget"),
                                variant: .ghost,
                                isIconOnly: true
                            )
                        }
                    }
                    if let destination = delivery.destination {
                        LabeledContent(text(\.destination, "destination"), value: destination)
                    }
                    if let outcome = MonarchWebhookDeliveriesTokens.outcome(
                        delivery,
                        text: text,
                        locale: context.locale
                    ) {
                        LabeledContent(text(\.response, "response"), value: outcome)
                    }
                    if let error = delivery.error {
                        LabeledContent(text(\.error, "error")) {
                            Text(error).foregroundStyle(theme.colors[role: MonarchWebhookDeliveriesTokens.errorRole])
                        }
                    }
                    LabeledContent(
                        text(\.attemptsLabel, "attemptsLabel"),
                        value: delivery.attempts.formatted(.number.locale(context.locale))
                    )
                    LabeledContent(
                        text(\.when, "when"),
                        value: MonarchWebhookDeliveriesTokens.when(
                            delivery,
                            now: context.now,
                            text: text,
                            locale: context.locale,
                            calendar: context.calendar
                        )
                    )
                    if canRedeliver, let onRedeliver {
                        MonarchButton(
                            text(\.redeliver, "redeliver"),
                            systemImage: MonarchWebhookDeliveriesTokens.redeliverSymbol,
                            variant: .primary,
                            isLoading: isRedelivering,
                            fullWidth: true,
                            action: { onRedeliver() }
                        )
                        if let result {
                            Text(
                                result
                                    ? text(\.redeliverQueued, "redeliverQueued")
                                    : text(\.redeliverFailed, "redeliverFailed")
                            )
                            .monarchTextStyle(.caption)
                            .foregroundStyle(
                                theme.colors[
                                    role: result
                                        ? MonarchWebhookDeliveriesTokens.muted
                                        : MonarchWebhookDeliveriesTokens.errorRole
                                ]
                            )
                        }
                    }
                }
                .labeledContentStyle(.monarch)
                .padding(MonarchWebhookDeliveriesTokens.sheetPadding)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .navigationTitle(text(\.detailsTitle, "detailsTitle"))
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(text(\.close, "close")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
