/// Built-in strings of the receipt preview, from the `MonarchReceiptPreview` strings table.
enum MonarchReceiptPreviewStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.receiptPreview.\(key)", table: "MonarchReceiptPreview")
    }

    /// The button title.
    static var view: String { string("view") }
    /// Shown when there is no receipt.
    static var none: String { string("none") }
    /// Spoken while the file loads.
    static var loading: String { string("loading") }
    /// Shown when loading fails.
    static var loadError: String { string("loadError") }
}
