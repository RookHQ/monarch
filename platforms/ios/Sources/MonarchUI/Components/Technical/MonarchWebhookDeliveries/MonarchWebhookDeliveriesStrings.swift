import Foundation

/// Built-in strings of ``MonarchWebhookDeliveries``, from the `MonarchWebhookDeliveries` strings table.
enum MonarchWebhookDeliveriesStrings {
    private static let table = "MonarchWebhookDeliveries"

    /// The string for a key without placeholders, such as `redeliver`.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.webhookDeliveries.\(key)", table: table)
    }

    /// The string for a key with text placeholders, such as "Next attempt in 5 minutes".
    static func string(_ key: String, _ texts: String...) -> String {
        String(format: string(key), arguments: texts)
    }

    /// The string for a key with a number placeholder, such as "HTTP 500".
    static func string(_ key: String, number: Int) -> String {
        MonarchStrings.string("monarch.webhookDeliveries.\(key)", table: table, number)
    }
}
