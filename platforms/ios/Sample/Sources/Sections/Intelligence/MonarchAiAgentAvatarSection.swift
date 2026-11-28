import MonarchUI
import SwiftUI

struct MonarchAiAgentAvatarSection: CataloguePage {
    static let title = "AI agent avatar"

    @Environment(\.monarchTheme) private var theme
    @State private var state: MonarchAiAgentState = .working

    var body: some View {
        CatalogueCaption("Pick a state")
        Picker("State", selection: $state) {
            ForEach(MonarchAiAgentState.allCases, id: \.self) { state in
                Text(state.rawValue.capitalized).tag(state)
            }
        }
        .pickerStyle(.segmented)
        MonarchAiAgentAvatar(
            MonarchAiAgentRef(id: "research", name: "Research agent", codename: "Ra"),
            state: state,
            size: .lg,
            showsState: true
        )

        CatalogueCaption("Sizes")
        HStack(spacing: theme.spacing.space4) {
            ForEach(MonarchAvatarSize.allCases, id: \.self) { size in
                MonarchAiAgentAvatar(MonarchAiAgentRef(name: "Billing agent"), state: .idle, size: size)
            }
        }

        CatalogueCaption("Beside its name")
        HStack(spacing: theme.spacing.space2) {
            MonarchAiAgentAvatar(MonarchAiAgentRef(name: "Support agent"), state: .waiting, isDecorative: true)
            Text("Support agent").monarchTextStyle(.body)
        }
    }
}
