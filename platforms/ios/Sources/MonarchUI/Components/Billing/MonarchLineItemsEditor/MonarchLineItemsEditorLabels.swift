/// Replacements for the built-in strings of ``MonarchLineItemsEditor``.
/// Every value is optional; leave one out to keep the built-in English text.
@_spi(Experimental) public struct MonarchLineItemsEditorLabels: Sendable {
    /// The heading of the rows. Built in: "Line items".
    public var label: String
    /// The heading of a row, from its number: "Item 1".
    public var item: @Sendable (_ row: Int) -> String
    /// The label of the description field. Built in: "Description".
    public var description: String
    /// The label of the quantity field. Built in: "Quantity".
    public var quantity: String
    /// The label of the unit price field, from the currency code: "Unit price (USD)".
    public var unitPrice: @Sendable (_ currencyCode: String) -> String
    /// The name of a row's amount. Built in: "Amount".
    public var amount: String
    /// The placeholder of an empty description. Built in: "Item or service".
    public var descriptionPlaceholder: String
    /// The add button. Built in: "Add item".
    public var addItem: String
    /// The swipe action that removes a row. Built in: "Remove".
    public var remove: String
    /// The spoken name of a row's remove button: "Remove item 2: Design hours".
    public var removeItem: @Sendable (_ row: Int, _ description: String) -> String
    /// Shown when there are no rows. Built in: "No items yet. Add one to start."
    public var empty: String
    /// Announced after a row is added: "Item 3 added".
    public var added: @Sendable (_ row: Int) -> String
    /// Announced after a row is removed: "Item 2 removed".
    public var removed: @Sendable (_ row: Int) -> String
    /// The name of the subtotal. Built in: "Subtotal".
    public var subtotal: String
    /// The discount row, from its formatted percentage or an empty string: "Discount (5%)".
    public var discount: @Sendable (_ rate: String) -> String
    /// The name of the discount type picker. Built in: "Discount type".
    public var discountType: String
    /// The percentage choice of the discount type picker. Built in: "%".
    public var percentOption: String
    /// The tax row, from its formatted rate or an empty string: "Tax (18%)".
    public var tax: @Sendable (_ rate: String) -> String
    /// The label of the tax rate field or picker. Built in: "Tax rate".
    public var taxRate: String
    /// The name of the total. Built in: "Total".
    public var total: String
    /// Spoken after the heading when the list is required. Built in: "Required".
    public var required: String
    /// A row of a read-only document, from the quantity and the unit price: "12 × $95.00".
    public var quantityTimesPrice: @Sendable (_ quantity: String, _ unitPrice: String) -> String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - label: Replaces "Line items".
    ///   - item: Replaces "Item …".
    ///   - description: Replaces "Description".
    ///   - quantity: Replaces "Quantity".
    ///   - unitPrice: Replaces "Unit price (…)".
    ///   - amount: Replaces "Amount".
    ///   - descriptionPlaceholder: Replaces "Item or service".
    ///   - addItem: Replaces "Add item".
    ///   - remove: Replaces "Remove".
    ///   - removeItem: Replaces "Remove item …".
    ///   - empty: Replaces "No items yet. Add one to start."
    ///   - added: Replaces "Item … added".
    ///   - removed: Replaces "Item … removed".
    ///   - subtotal: Replaces "Subtotal".
    ///   - discount: Replaces "Discount" and "Discount (…)".
    ///   - discountType: Replaces "Discount type".
    ///   - percentOption: Replaces "%".
    ///   - tax: Replaces "Tax" and "Tax (…)".
    ///   - taxRate: Replaces "Tax rate".
    ///   - total: Replaces "Total".
    ///   - required: Replaces "Required".
    ///   - quantityTimesPrice: Replaces "… × …".
    public init(
        label: String? = nil,
        item: (@Sendable (_ row: Int) -> String)? = nil,
        description: String? = nil,
        quantity: String? = nil,
        unitPrice: (@Sendable (_ currencyCode: String) -> String)? = nil,
        amount: String? = nil,
        descriptionPlaceholder: String? = nil,
        addItem: String? = nil,
        remove: String? = nil,
        removeItem: (@Sendable (_ row: Int, _ description: String) -> String)? = nil,
        empty: String? = nil,
        added: (@Sendable (_ row: Int) -> String)? = nil,
        removed: (@Sendable (_ row: Int) -> String)? = nil,
        subtotal: String? = nil,
        discount: (@Sendable (_ rate: String) -> String)? = nil,
        discountType: String? = nil,
        percentOption: String? = nil,
        tax: (@Sendable (_ rate: String) -> String)? = nil,
        taxRate: String? = nil,
        total: String? = nil,
        required: String? = nil,
        quantityTimesPrice: (@Sendable (_ quantity: String, _ unitPrice: String) -> String)? = nil
    ) {
        typealias Strings = MonarchLineItemsEditorStrings
        self.label = label ?? Strings.label
        self.item = item ?? { Strings.item($0) }
        self.description = description ?? Strings.description
        self.quantity = quantity ?? Strings.quantity
        self.unitPrice = unitPrice ?? { Strings.unitPrice($0) }
        self.amount = amount ?? Strings.amount
        self.descriptionPlaceholder = descriptionPlaceholder ?? Strings.descriptionPlaceholder
        self.addItem = addItem ?? Strings.addItem
        self.remove = remove ?? Strings.remove
        self.removeItem = removeItem ?? { Strings.removeItem($0, description: $1) }
        self.empty = empty ?? Strings.empty
        self.added = added ?? { Strings.added($0) }
        self.removed = removed ?? { Strings.removed($0) }
        self.subtotal = subtotal ?? Strings.subtotal
        self.discount = discount ?? { Strings.discount($0) }
        self.discountType = discountType ?? Strings.discountType
        self.percentOption = percentOption ?? Strings.percentOption
        self.tax = tax ?? { Strings.tax($0) }
        self.taxRate = taxRate ?? Strings.taxRate
        self.total = total ?? Strings.total
        self.required = required ?? Strings.required
        self.quantityTimesPrice = quantityTimesPrice ?? { Strings.quantityTimesPrice($0, $1) }
    }
}
