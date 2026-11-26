import SwiftUI

// The navigation chrome of a searchable list in a sheet: the combobox, the
// space and workspace switchers, the entity and multi-select pickers.

extension View {
    /// An inline navigation title on iOS; other platforms have one style.
    @ViewBuilder
    func monarchInlineNavigationTitle() -> some View {
        #if os(iOS)
            navigationBarTitleDisplayMode(.inline)
        #else
            self
        #endif
    }

    /// The search field of a sheet list, always shown in the navigation bar on iOS.
    @ViewBuilder
    func monarchSheetSearchable(text: Binding<String>, prompt: String) -> some View {
        #if os(iOS)
            searchable(text: text, placement: .navigationBarDrawer(displayMode: .always), prompt: Text(prompt))
        #else
            searchable(text: text, prompt: Text(prompt))
        #endif
    }
}
