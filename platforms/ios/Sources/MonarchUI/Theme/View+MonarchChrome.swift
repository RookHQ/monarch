import SwiftUI

extension ToolbarContent {
    /// Draws a toolbar item flat: on iOS 26 and later the system puts every
    /// bar item in a floating glass pill with a shadow; this removes the pill
    /// so a text action reads as plain text in the bar (PREMIUM rule 7).
    /// Earlier systems already draw bar items flat, so nothing changes there.
    ///
    /// ```swift
    /// .toolbar {
    ///     ToolbarItem(placement: .confirmationAction) { Button("Save") { save() } }
    ///         .monarchFlat()
    /// }
    /// ```
    @ToolbarContentBuilder
    public func monarchFlat() -> some ToolbarContent {
        if #available(iOS 26.0, macOS 26.0, *) {
            self.sharedBackgroundVisibility(.hidden)
        } else {
            self
        }
    }
}

extension View {
    /// Adds a search field to the screen, shown under the navigation title and
    /// kept in the bar, so it never floats over the rows.
    ///
    /// On iOS 26 the system puts a bare `searchable` field in a floating
    /// glass pill at the bottom of the screen, over the last rows of a list.
    /// This places it in the navigation bar, as on earlier systems. On other
    /// platforms it is `searchable` with the automatic placement.
    ///
    /// - Parameters:
    ///   - text: The query.
    ///   - prompt: The placeholder of the field.
    /// - Returns: The view with a search field.
    public func monarchSearchable(text: Binding<String>, prompt: Text) -> some View {
        #if os(iOS)
            searchable(text: text, placement: .navigationBarDrawer(displayMode: .always), prompt: prompt)
        #else
            searchable(text: text, prompt: prompt)
        #endif
    }

    /// Adds a search field to the screen; see ``monarchSearchable(text:prompt:)``.
    public func monarchSearchable(text: Binding<String>, prompt: LocalizedStringKey) -> some View {
        monarchSearchable(text: text, prompt: Text(prompt))
    }

    /// Adds a search field to the screen with a verbatim prompt; see ``monarchSearchable(text:prompt:)``.
    public func monarchSearchable(text: Binding<String>, prompt: String) -> some View {
        monarchSearchable(text: text, prompt: Text(verbatim: prompt))
    }
}
