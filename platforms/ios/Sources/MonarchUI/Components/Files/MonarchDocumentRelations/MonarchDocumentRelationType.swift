/// A kind of relation and its label, such as "Parent of", with the kind it
/// is from the other document ("Child of").
@_spi(Experimental) public struct MonarchDocumentRelationType: Identifiable, Sendable, Hashable {
    /// A stable value, such as `parent-of`.
    public let value: String
    /// The label, already localised.
    public let label: String
    /// The `value` of the type as seen from the target. Itself by default.
    public let inverse: String

    /// The value.
    public var id: String { value }

    /// A relation type.
    ///
    /// - Parameters:
    ///   - value: A stable value.
    ///   - label: The label.
    ///   - inverse: The type as seen from the target. `nil` for the same type.
    public init(value: String, label: String, inverse: String? = nil) {
        self.value = value
        self.label = label
        self.inverse = inverse ?? value
    }

    /// Related to, parent of and child of, supersedes and superseded by,
    /// implements and specified by, with built-in English labels from the
    /// component's strings table. The same values as the web.
    public static var defaults: [MonarchDocumentRelationType] {
        typealias Strings = MonarchDocumentRelationsStrings
        return [
            MonarchDocumentRelationType(value: "related-to", label: Strings.relatedTo),
            MonarchDocumentRelationType(value: "parent-of", label: Strings.parentOf, inverse: "child-of"),
            MonarchDocumentRelationType(value: "child-of", label: Strings.childOf, inverse: "parent-of"),
            MonarchDocumentRelationType(value: "supersedes", label: Strings.supersedes, inverse: "superseded-by"),
            MonarchDocumentRelationType(value: "superseded-by", label: Strings.supersededBy, inverse: "supersedes"),
            MonarchDocumentRelationType(value: "implements", label: Strings.implements, inverse: "specified-by"),
            MonarchDocumentRelationType(value: "specified-by", label: Strings.specifiedBy, inverse: "implements"),
        ]
    }
}
