/// Built-in strings of the line items editor, from the `MonarchLineItemsEditor` strings table.
enum MonarchLineItemsEditorStrings {
    private static let table = "MonarchLineItemsEditor"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.lineItemsEditor." + key, table: table)
    }

    private static func format(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// The heading.
    static var label: String { string("label") }
    /// The heading of a row.
    static func item(_ row: Int) -> String { format("item", row) }
    /// The description field.
    static var description: String { string("description") }
    /// The quantity field.
    static var quantity: String { string("quantity") }
    /// The unit price field.
    static func unitPrice(_ currencyCode: String) -> String { format("unitPrice", currencyCode) }
    /// A row's amount.
    static var amount: String { string("amount") }
    /// The description placeholder.
    static var descriptionPlaceholder: String { string("descriptionPlaceholder") }
    /// The add button.
    static var addItem: String { string("addItem") }
    /// The swipe action.
    static var remove: String { string("remove") }
    /// The remove button of a row.
    static func removeItem(_ row: Int, description: String) -> String {
        let trimmed = description.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? format("removeItem", row) : format("removeItemNamed", row, trimmed)
    }
    /// The empty message.
    static var empty: String { string("empty") }
    /// The added announcement.
    static func added(_ row: Int) -> String { format("added", row) }
    /// The removed announcement.
    static func removed(_ row: Int) -> String { format("removed", row) }
    /// The subtotal.
    static var subtotal: String { string("subtotal") }
    /// The discount row, with its percentage when there is one.
    static func discount(_ rate: String) -> String { rate.isEmpty ? string("discount") : format("discountRate", rate) }
    /// The discount type picker.
    static var discountType: String { string("discountType") }
    /// The percentage choice.
    static var percentOption: String { format("percentOption") }
    /// The tax row, with its rate when there is one.
    static func tax(_ rate: String) -> String { rate.isEmpty ? string("tax") : format("taxWithRate", rate) }
    /// The tax rate control.
    static var taxRate: String { string("taxRate") }
    /// The total.
    static var total: String { string("total") }
    /// Spoken for a required list.
    static var required: String { string("required") }
    /// A row of a sent document.
    static func quantityTimesPrice(_ quantity: String, _ price: String) -> String {
        format("quantityTimesPrice", quantity, price)
    }
}
