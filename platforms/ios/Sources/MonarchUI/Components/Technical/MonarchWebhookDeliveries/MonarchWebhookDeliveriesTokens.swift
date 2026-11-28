import Foundation
import MonarchTokens
import SwiftUI

/// The resolved strings of ``MonarchWebhookDeliveries``: the app's replacements, else the built-in ones.
struct MonarchWebhookDeliveriesText {
    let labels: MonarchWebhookDeliveriesLabels

    /// A plain string: the replacement at `keyPath`, else the built-in string with the same key.
    func callAsFunction(_ keyPath: KeyPath<MonarchWebhookDeliveriesLabels, String?>, _ key: String) -> String {
        labels[keyPath: keyPath] ?? MonarchWebhookDeliveriesStrings.string(key)
    }

    func httpStatus(_ code: Int) -> String {
        labels.httpStatus?(code) ?? MonarchWebhookDeliveriesStrings.string("httpStatus", number: code)
    }

    func duration(_ milliseconds: Double, locale: Locale) -> String {
        if let custom = labels.duration { return custom(milliseconds) }
        let number = Int(milliseconds.rounded()).formatted(.number.locale(locale))
        return MonarchWebhookDeliveriesStrings.string("duration", number)
    }

    func nextAttempt(_ relative: String) -> String {
        labels.nextAttempt?(relative) ?? MonarchWebhookDeliveriesStrings.string("nextAttempt", relative)
    }

    func attempts(_ count: Int, locale: Locale) -> String {
        if let custom = labels.attempts { return custom(count) }
        let key = count == 1 ? "attempts.one" : "attempts.other"
        return MonarchWebhookDeliveriesStrings.string(key, count.formatted(.number.locale(locale)))
    }

    func count(_ count: Int, status: String, locale: Locale) -> String {
        if let custom = labels.count { return custom(count, status) }
        return MonarchWebhookDeliveriesStrings.string(
            "count",
            count.formatted(.number.locale(locale)),
            status.lowercased(with: locale)
        )
    }
}

/// Component tokens and rules for ``MonarchWebhookDeliveries``.
enum MonarchWebhookDeliveriesTokens {
    /// The gap between the filter bar, the counts and the list.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the filters.
    static let filterSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the lines of a row.
    static let rowLineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the parts of a row line.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding above and below a row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space3
    /// The padding of the details sheet.
    static let sheetPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the facts of the details sheet.
    static let factSpacing: CGFloat = MonarchTokens.Space.space4
    /// The row's trailing symbol.
    static let disclosureSymbol = "chevron.forward"
    /// The width of a fact's label in a row.
    static let factLabelWidth: CGFloat = MonarchTokens.Space.space20
    /// The redeliver symbol.
    static let redeliverSymbol = "arrow.clockwise"
    /// The role of secondary row text.
    static var muted: MonarchColorRole { \.textMuted }
    /// The role of a delivery's error.
    static var errorRole: MonarchColorRole { \.dangerStrong }
    /// The role of the row separators.
    static var separator: MonarchColorRole { \.borderSubtle }

    /// The built-in statuses, in the order the status filter lists them.
    static let builtInOrder = ["pending", "sending", "succeeded", "retrying", "failed", "gone"]

    /// The built-in statuses: label, tone and a distinct symbol each.
    static var builtInRegistry: MonarchStatusRegistry {
        let looks: [String: (MonarchTone, String)] = [
            "pending": (.neutral, "clock"),
            "sending": (.info, "paperplane.fill"),
            "succeeded": (.success, "checkmark.circle.fill"),
            "retrying": (.warning, "arrow.clockwise.circle.fill"),
            "failed": (.danger, "xmark.octagon.fill"),
            "gone": (.danger, "questionmark.circle.fill"),
        ]
        var definitions: [String: MonarchStatusDefinition] = [:]
        for (key, look) in looks {
            definitions[key] = MonarchStatusDefinition(
                MonarchWebhookDeliveriesStrings.string("state.\(key)"),
                tone: look.0,
                systemImage: look.1
            )
        }
        return MonarchStatusRegistry(definitions)
    }

    /// The built-in statuses with the app's merged over them.
    static func registry(_ statuses: MonarchStatusRegistry?) -> MonarchStatusRegistry {
        guard let statuses else { return builtInRegistry }
        return builtInRegistry.merging(statuses)
    }

    /// The statuses the filter offers: `filterStatuses` when given, else the built-in order then the
    /// app's own keys alphabetically.
    static func statusOptions(_ filterStatuses: [String]?, registry: MonarchStatusRegistry) -> [String] {
        if let filterStatuses { return filterStatuses }
        let extra = registry.definitions.keys.filter { !builtInOrder.contains($0) }.sorted()
        return builtInOrder + extra
    }

    /// Whether a delivery can be sent again: the app's rule, else every delivery that is not
    /// pending or sending.
    static func canRedeliver(
        _ delivery: MonarchWebhookDelivery,
        rule: ((MonarchWebhookDelivery) -> Bool)?
    ) -> Bool {
        if let rule { return rule(delivery) }
        return delivery.status != "pending" && delivery.status != "sending"
    }

    /// The response line: "HTTP 500 · 1,204 ms", or nothing when there is only an error, or
    /// "Not attempted yet", or "No response".
    static func outcome(_ delivery: MonarchWebhookDelivery, text: MonarchWebhookDeliveriesText, locale: Locale)
        -> String?
    {
        var parts: [String] = []
        if let code = delivery.responseCode { parts.append(text.httpStatus(code)) }
        if let ms = delivery.durationMilliseconds { parts.append(text.duration(ms, locale: locale)) }
        if !parts.isEmpty { return parts.joined(separator: " · ") }
        if delivery.error != nil { return nil }
        return delivery.attempts == 0 ? text(\.notAttempted, "notAttempted") : text(\.noResponse, "noResponse")
    }

    /// The time line: "Next attempt in 5 minutes" for a pending or retrying delivery with a next
    /// attempt, else when it was last attempted.
    static func when(
        _ delivery: MonarchWebhookDelivery,
        now: Date,
        text: MonarchWebhookDeliveriesText,
        locale: Locale,
        calendar: Calendar
    ) -> String {
        if let next = delivery.nextAttempt, delivery.status == "pending" || delivery.status == "retrying" {
            return text.nextAttempt(relative(next, now: now, locale: locale, calendar: calendar))
        }
        return relative(delivery.date, now: now, locale: locale, calendar: calendar)
    }

    /// "5 minutes ago" or "in 5 minutes" in the locale and calendar given.
    static func relative(_ date: Date, now: Date, locale: Locale, calendar: Calendar) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar, allowsFuture: true)
    }

    /// The counts line, "12 delivered · 3 retrying", in the order of the status filter; zero and
    /// unknown counts are kept as given.
    static func countLine(
        _ counts: [String: Int],
        order: [String],
        registry: MonarchStatusRegistry,
        text: MonarchWebhookDeliveriesText,
        locale: Locale
    ) -> String? {
        guard !counts.isEmpty else { return nil }
        let keys = order.filter { counts[$0] != nil } + counts.keys.filter { !order.contains($0) }.sorted()
        let parts = keys.compactMap { key in
            counts[key].map { text.count($0, status: registry.resolve(key).label, locale: locale) }
        }
        return parts.joined(separator: " · ")
    }

    /// What VoiceOver reads for a row: event, status, response, error, attempts and time.
    static func rowLabel(
        _ delivery: MonarchWebhookDelivery,
        registry: MonarchStatusRegistry,
        now: Date,
        text: MonarchWebhookDeliveriesText,
        locale: Locale,
        calendar: Calendar,
        showsDestination: Bool
    ) -> String {
        var parts = [delivery.eventType]
        if let id = delivery.eventID { parts.append(id) }
        parts.append(registry.resolve(delivery.status).label)
        if showsDestination, let destination = delivery.destination { parts.append(destination) }
        if let outcome = outcome(delivery, text: text, locale: locale) { parts.append(outcome) }
        if let error = delivery.error { parts.append(error) }
        parts.append(text.attempts(delivery.attempts, locale: locale))
        parts.append(when(delivery, now: now, text: text, locale: locale, calendar: calendar))
        return parts.joined(separator: ", ")
    }

    /// What the body shows.
    enum Content: Equatable {
        /// The first page is loading.
        case loading
        /// The list failed to load.
        case failed
        /// The filters hide every delivery.
        case filteredEmpty
        /// There are no deliveries.
        case empty
        /// The rows.
        case rows
    }

    /// Loading wins, then a load error, then emptiness, filtered or not.
    static func content(isLoading: Bool, hasError: Bool, rowCount: Int, isFiltered: Bool) -> Content {
        if isLoading { return .loading }
        if hasError { return .failed }
        if rowCount == 0 { return isFiltered ? .filteredEmpty : .empty }
        return .rows
    }
}
