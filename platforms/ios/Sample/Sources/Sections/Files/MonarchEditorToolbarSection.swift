import MonarchUI
import SwiftUI

struct MonarchEditorToolbarSection: CataloguePage {
    static let title = "Editor toolbar"

    @State private var active: Set<MonarchEditorTool> = [.paragraph]
    @State private var log = "Press a tool."
    @State private var text = "Tap here to show the toolbar above the keyboard."

    var body: some View {
        CatalogueCaption("Above the editor")
        MonarchEditorToolbar(
            active: active,
            disabled: [.redo],
            onLink: { url in
                if let url {
                    active.insert(.link)
                    log = "Link: \(url.absoluteString)"
                } else {
                    active.remove(.link)
                    log = "Link removed."
                }
            },
            action: apply
        )
        Text(log).monarchTextStyle(.caption)

        CatalogueCaption("Above the keyboard")
        TextField("Notes", text: $text, axis: .vertical)
            .monarchTextStyle(.body)
            .toolbar {
                ToolbarItemGroup(placement: .keyboard) {
                    MonarchEditorToolbar(groups: MonarchEditorTool.inlineGroups, active: active, action: apply)
                }
            }

        CatalogueCaption("Disabled while loading")
        MonarchEditorToolbar(groups: MonarchEditorTool.inlineGroups) { _ in }
            .disabled(true)
    }

    private func apply(_ tool: MonarchEditorTool) {
        switch tool {
        case .undo, .redo, .custom:
            log = "\(tool)"
        case .paragraph, .heading:
            active = active.filter {
                if case .heading = $0 { return false }
                return $0 != .paragraph
            }
            active.insert(tool)
            log = "Text style: \(tool)"
        default:
            if active.contains(tool) { active.remove(tool) } else { active.insert(tool) }
            log = "Toggled \(tool)"
        }
    }
}
