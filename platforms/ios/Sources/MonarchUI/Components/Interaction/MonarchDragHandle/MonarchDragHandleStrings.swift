/// Built-in strings of the drag handle, from the `MonarchDragHandle` strings table.
enum MonarchDragHandleStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.dragHandle." + key, table: "MonarchDragHandle")
    }

    /// The action that moves the item one place earlier in a vertical order.
    static var moveUp: String { string("moveUp") }
    /// The action that moves the item one place later in a vertical order.
    static var moveDown: String { string("moveDown") }
    /// The action that moves the item one place earlier in a horizontal order.
    static var moveBackward: String { string("moveBackward") }
    /// The action that moves the item one place later in a horizontal order.
    static var moveForward: String { string("moveForward") }
    /// Spoken as the value while the item is picked up.
    static var grabbed: String { string("grabbed") }
    /// Spoken as the value just after the item is dropped.
    static var dropped: String { string("dropped") }
}
