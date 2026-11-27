import Foundation
import SwiftUI

/// What every row and the details sheet of one ``MonarchWebhookDeliveries`` share.
struct MonarchWebhookDeliveriesContext {
    let registry: MonarchStatusRegistry
    let text: MonarchWebhookDeliveriesText
    let now: Date
    let locale: Locale
    let calendar: Calendar
    let showsDestination: Bool
}

/// One delivery in the log: a button that opens its details, read as one sentence.
struct MonarchWebhookDeliveryRow: View {
    let delivery: MonarchWebhookDelivery
    let context: MonarchWebhookDeliveriesContext
    let canRedeliver: Bool
    let onOpen: @MainActor () -> Void
    let onRedeliver: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            openButton
            // As on the web, Redeliver is visible on the row, not only in the details and the context menu.
            if canRedeliver, let onRedeliver {
                MonarchButton(
                    context.text(\.redeliver, "redeliver"),
                    systemImage: MonarchWebhookDeliveriesTokens.redeliverSymbol,
                    variant: .secondary,
                    size: .sm,
                    action: { onRedeliver() }
                )
                .padding(.bottom, MonarchWebhookDeliveriesTokens.rowPadding)
            }
        }
    }

    /// The delivery as label and value rows, as on the web: Status, Event with its ID and a copy button,
    /// Response, Attempts and When. Tapping the facts opens the details; there is no disclosure chevron.
    private var openButton: some View {
        let text = context.text
        return VStack(alignment: .leading, spacing: MonarchWebhookDeliveriesTokens.rowSpacing) {
            fact(text(\.status, "status")) {
                MonarchStatusBadge(delivery.status, registry: context.registry, size: .sm)
            }
            fact(text(\.event, "event")) {
                VStack(alignment: .leading, spacing: MonarchWebhookDeliveriesTokens.rowLineSpacing) {
                    Text(delivery.eventType)
                        .monarchTextStyle(.code)
                        .foregroundStyle(theme.colors.textDefault)
                    if let id = delivery.eventID {
                        HStack(spacing: MonarchWebhookDeliveriesTokens.rowLineSpacing) {
                            Text(id)
                                .monarchTextStyle(.codeSmall)
                                .foregroundStyle(theme.colors[role: MonarchWebhookDeliveriesTokens.muted])
                            MonarchCopyButton(
                                value: id,
                                target: text(\.eventIDTarget, "eventIDTarget"),
                                variant: .ghost,
                                isIconOnly: true
                            )
                        }
                    }
                }
            }
            if context.showsDestination, let destination = delivery.destination {
                fact(text(\.destination, "destination")) { caption(destination) }
            }
            let outcome = MonarchWebhookDeliveriesTokens.outcome(delivery, text: text, locale: context.locale)
            if outcome != nil || delivery.error != nil {
                fact(text(\.response, "response")) {
                    VStack(alignment: .leading, spacing: MonarchWebhookDeliveriesTokens.rowLineSpacing) {
                        if let outcome { value(outcome, muted: true) }
                        if let error = delivery.error {
                            Text(error)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors[role: MonarchWebhookDeliveriesTokens.errorRole])
                        }
                    }
                }
            }
            fact(text(\.attemptsLabel, "attemptsLabel")) {
                value(delivery.attempts.formatted(.number.locale(context.locale)))
            }
            fact(text(\.when, "when")) {
                caption(
                    MonarchWebhookDeliveriesTokens.when(
                        delivery,
                        now: context.now,
                        text: text,
                        locale: context.locale,
                        calendar: context.calendar
                    )
                )
            }
        }
        .padding(.vertical, MonarchWebhookDeliveriesTokens.rowPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture(perform: onOpen)
        .accessibilityElement(children: .ignore)
        .accessibilityAddTraits(.isButton)
        .accessibilityLabel(
            MonarchWebhookDeliveriesTokens.rowLabel(
                delivery,
                registry: context.registry,
                now: context.now,
                text: text,
                locale: context.locale,
                calendar: context.calendar,
                showsDestination: context.showsDestination
            )
        )
        .accessibilityHint(text(\.detailsHint, "detailsHint"))
        .accessibilityActions {
            if canRedeliver, let onRedeliver {
                Button(text(\.redeliver, "redeliver"), action: { onRedeliver() })
            }
        }
        .contextMenu {
            if canRedeliver, let onRedeliver {
                Button(
                    text(\.redeliver, "redeliver"),
                    systemImage: MonarchWebhookDeliveriesTokens.redeliverSymbol,
                    action: { onRedeliver() }
                )
            }
        }
    }

    private func fact<Content: View>(_ label: String, @ViewBuilder content: () -> Content) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchWebhookDeliveriesTokens.rowSpacing) {
            Text(label)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchWebhookDeliveriesTokens.muted])
                .frame(width: MonarchWebhookDeliveriesTokens.factLabelWidth, alignment: .leading)
            content()
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func value(_ string: String, muted: Bool = false) -> some View {
        Text(string)
            .monarchTextStyle(.bodySmall)
            .foregroundStyle(
                muted ? theme.colors[role: MonarchWebhookDeliveriesTokens.muted] : theme.colors.textDefault
            )
    }

    private func caption(_ string: String) -> some View {
        Text(string)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchWebhookDeliveriesTokens.muted])
    }
}
