import MonarchUI
import SwiftUI

struct MonarchAiPermissionScopeSection: CataloguePage {
    static let title = "AI permission scope"

    @Environment(\.monarchTheme) private var theme
    @State private var scopes = [
        MonarchAiPermissionScopeItem("Calendar", access: .read),
        MonarchAiPermissionScopeItem("Drive", resources: "Shared projects only", access: .write),
        MonarchAiPermissionScopeItem("Mail", access: .none),
    ]
    @State private var managed = false

    var body: some View {
        CatalogueCaption("Editable")
        MonarchAiPermissionScope(scopes: $scopes, onManage: { managed = true })
        if managed {
            Text("Permission settings opened")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }

        CatalogueCaption("Read only, the same values")
        MonarchAiPermissionScope(scopes)

        CatalogueCaption("Empty")
        MonarchAiPermissionScope([])
    }
}
