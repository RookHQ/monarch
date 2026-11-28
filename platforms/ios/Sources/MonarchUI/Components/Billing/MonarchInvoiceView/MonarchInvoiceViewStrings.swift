/// Built-in strings of the invoice view, from the `MonarchInvoiceView` strings table.
enum MonarchInvoiceViewStrings {
    private static let table = "MonarchInvoiceView"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.invoiceView.\(key)", table: table)
    }

    /// The default title.
    static var title: String { string("title") }
    /// The heading over the issuer.
    static var from: String { string("from") }
    /// The heading over the billed party.
    static var to: String { string("to") }
    /// The issue date label.
    static var issueDate: String { string("issueDate") }
    /// The due date label.
    static var dueDate: String { string("dueDate") }
    /// The heading over the lines.
    static var lineItems: String { string("lineItems") }
    /// The description column.
    static var description: String { string("description") }
    /// The quantity column.
    static var quantity: String { string("quantity") }
    /// The unit price column.
    static var unitPrice: String { string("unitPrice") }
    /// The amount column.
    static var amount: String { string("amount") }
    /// The subtotal row.
    static var subtotal: String { string("subtotal") }
    /// The discount row.
    static var discount: String { string("discount") }
    /// The tax row without a rate.
    static var tax: String { string("tax") }
    /// The tax row with a formatted rate.
    static func taxWithRate(_ rate: String) -> String {
        MonarchStrings.string("monarch.invoiceView.taxWithRate", table: table, rate)
    }
    /// The total row.
    static var total: String { string("total") }
    /// The amount paid row.
    static var amountPaid: String { string("amountPaid") }
    /// The amount due row.
    static var amountDue: String { string("amountDue") }
    /// Shown when there are no lines.
    static var noItems: String { string("noItems") }
    /// The heading over the notes.
    static var notes: String { string("notes") }
    /// The print button.
    static var print: String { string("print") }
}
