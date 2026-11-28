import MonarchUI
import SwiftUI

/// Native adoption: the system focus engine, with `@FocusState` and `onKeyPress`.
struct RovingFocusGroupSection: CataloguePage {
    static let title = "Roving focus group (focus system)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.layoutDirection) private var layoutDirection
    @FocusState private var field: String?
    @FocusState private var swatch: Int?
    @State private var name = ""
    @State private var email = ""

    private let swatches = 3

    var body: some View {
        CatalogueCaption("Focus is read and moved in code with @FocusState")
        MonarchTextField("Name", text: $name)
            .focused($field, equals: "name")
        MonarchTextField("Email", text: $email)
            .focused($field, equals: "email")
        MonarchButton("Go to Email") { field = "email" }
        Text("Focused field: \(field ?? "none")")
            .monarchTextStyle(.caption)

        CatalogueCaption("Connect a keyboard: Tab to a swatch, then use the left and right arrow keys")
        HStack(spacing: theme.spacing.space2) {
            ForEach(0..<swatches, id: \.self) { index in
                Text("Swatch \(index + 1)")
                    .monarchTextStyle(.label)
                    .padding(theme.spacing.space3)
                    .background(theme.colors.bgMuted, in: RoundedRectangle(cornerRadius: theme.radius.md))
                    .focusable()
                    .focused($swatch, equals: index)
                    .monarchFocusRing(swatch == index, in: RoundedRectangle(cornerRadius: theme.radius.md))
            }
        }
        .onKeyPress(.rightArrow) { move(layoutDirection == .rightToLeft ? -1 : 1) }
        .onKeyPress(.leftArrow) { move(layoutDirection == .rightToLeft ? 1 : -1) }
    }

    private func move(_ step: Int) -> KeyPress.Result {
        guard let current = swatch, (0..<swatches).contains(current + step) else { return .ignored }
        swatch = current + step
        return .handled
    }
}
