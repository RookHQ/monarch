/// Replacements for the built-in strings of ``MonarchInvoiceView``, for
/// example the labels of the invoice's own language.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchInvoiceViewLabels: Sendable {
    /// The title when the view has none. Built in: "Invoice".
    public var title: String?
    /// The heading over the issuer. Built in: "From".
    public var from: String?
    /// The heading over the billed party. Built in: "Bill to".
    public var to: String?
    /// The issue date label. Built in: "Issue date".
    public var issueDate: String?
    /// The due date label. Built in: "Due date".
    public var dueDate: String?
    /// The heading over the lines. Built in: "Line items".
    public var lineItems: String?
    /// The description column. Built in: "Description".
    public var description: String?
    /// The quantity column. Built in: "Qty".
    public var quantity: String?
    /// The unit price column. Built in: "Unit price".
    public var unitPrice: String?
    /// The amount column. Built in: "Amount".
    public var amount: String?
    /// Built in: "Subtotal".
    public var subtotal: String?
    /// Built in: "Discount".
    public var discount: String?
    /// The tax row without a rate. Built in: "Tax".
    public var tax: String?
    /// The tax row with the formatted rate ("18%"). Built in: "Tax (<rate>)".
    public var taxWithRate: (@Sendable (String) -> String)?
    /// Built in: "Total".
    public var total: String?
    /// Built in: "Amount paid".
    public var amountPaid: String?
    /// Built in: "Amount due".
    public var amountDue: String?
    /// Shown when there are no lines. Built in: "No line items."
    public var noItems: String?
    /// The heading over the notes. Built in: "Notes".
    public var notes: String?
    /// The print button. Built in: "Print".
    public var print: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        from: String? = nil,
        to: String? = nil,
        issueDate: String? = nil,
        dueDate: String? = nil,
        lineItems: String? = nil,
        description: String? = nil,
        quantity: String? = nil,
        unitPrice: String? = nil,
        amount: String? = nil,
        subtotal: String? = nil,
        discount: String? = nil,
        tax: String? = nil,
        taxWithRate: (@Sendable (String) -> String)? = nil,
        total: String? = nil,
        amountPaid: String? = nil,
        amountDue: String? = nil,
        noItems: String? = nil,
        notes: String? = nil,
        print: String? = nil
    ) {
        self.title = title
        self.from = from
        self.to = to
        self.issueDate = issueDate
        self.dueDate = dueDate
        self.lineItems = lineItems
        self.description = description
        self.quantity = quantity
        self.unitPrice = unitPrice
        self.amount = amount
        self.subtotal = subtotal
        self.discount = discount
        self.tax = tax
        self.taxWithRate = taxWithRate
        self.total = total
        self.amountPaid = amountPaid
        self.amountDue = amountDue
        self.noItems = noItems
        self.notes = notes
        self.print = print
    }
}
