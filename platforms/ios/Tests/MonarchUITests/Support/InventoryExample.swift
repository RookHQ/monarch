import SwiftUI

/// One compiled example from an inventory file. The generated files under
/// `Generated/Examples` create these; nothing is written by hand.
struct InventoryExample: Sendable {
    /// The inventory id the example belongs to.
    let id: String
    /// The example's title in the inventory.
    let title: String
    /// True when the example opens a presentation (a sheet, alert, dialog, popover) that the preview host shows open
    /// (`previewPresented` in the inventory file).
    var presented = false
    /// True when the preview host also opens the example's menu (`previewMenu` in the inventory file).
    var opensMenu = false
    /// True when the preview host activates the example's first button (`previewTap` in the inventory file).
    var taps = false
    /// True when the preview host runs the example's own action once (`previewAction` in the inventory file); the
    /// capture stays the example's column.
    var acts = false
    /// Builds the example on the main actor.
    let make: @MainActor @Sendable () -> AnyView
}

/// Whether examples start with their presentation open. Always false in the app and the tests; the preview host sets it
/// before it builds an example that has `presented`, so the documentation image shows the open sheet, not its trigger.
enum PreviewMode {
    @MainActor static var presented = false
}
