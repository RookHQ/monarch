import SwiftUI

/// The decorative icon of a relation's target: its file type, or a text document.
struct MonarchDocumentRelationsTargetIcon: View {
    let target: MonarchDocumentRelationTarget

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        if let fileName = target.fileName {
            MonarchFileTypeIcon(fileName: fileName, size: .sm, isDecorative: true)
        } else {
            MonarchIcon(systemName: MonarchDocumentRelationsTokens.documentSymbol)
                .foregroundStyle(theme.colors[role: MonarchDocumentRelationsTokens.mutedColor])
        }
    }
}
