import MonarchUI
import SwiftUI

/// Recipe: MonarchErrorState filling a whole screen.
struct ErrorPageSection: CataloguePage {
    static let title = "Error page (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var kind = MonarchErrorStateKind.notFound
    @State private var isRetrying = false

    var body: some View {
        Picker("Kind", selection: $kind) {
            ForEach(MonarchErrorStateKind.allCases, id: \.self) { kind in
                Text(String(describing: kind)).tag(kind)
            }
        }
        MonarchErrorState(
            title(kind),
            message: "Check the address, or go back to the dashboard.",
            kind: kind,
            size: .lg,
            requestID: kind == .server ? "req_7f3a91" : nil,
            isRetrying: isRetrying,
            actionTitle: "Go to dashboard",
            onRetry: {
                isRetrying = true
                Task {
                    try? await Task.sleep(for: .seconds(1))
                    isRetrying = false
                }
            },
            action: {}
        )
        .frame(maxWidth: .infinity, minHeight: 480)
        .background(theme.colors.bgCanvas)
    }

    private func title(_ kind: MonarchErrorStateKind) -> String {
        switch kind {
        case .notFound: "This page doesn't exist"
        case .forbidden: "You don't have access to this page"
        case .offline: "You're offline"
        case .server: "Something went wrong on our side"
        case .maintenance: "We're doing maintenance"
        case .error: "This page didn't load"
        }
    }
}
