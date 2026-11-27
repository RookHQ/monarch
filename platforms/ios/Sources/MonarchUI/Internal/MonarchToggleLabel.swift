import SwiftUI

/// The label of a custom toggle style, left out when the toggle is inside
/// `labelsHidden()`, as the system styles do. A row that puts the checkbox
/// next to its own text (a table row, a file card) hides the label this way
/// and keeps it as the spoken name.
///
/// `labelsVisibility` exists from iOS 18. On iOS 17 the label is always shown.
struct MonarchToggleLabel<Label: View>: View {
    let label: Label

    var body: some View {
        if #available(iOS 18.0, macOS 15.0, *) {
            MonarchVisibleLabel(label: label)
        } else {
            label
        }
    }
}

@available(iOS 18.0, macOS 15.0, *)
private struct MonarchVisibleLabel<Label: View>: View {
    let label: Label
    @Environment(\.labelsVisibility) private var visibility

    var body: some View {
        if visibility != .hidden {
            label
        }
    }
}
