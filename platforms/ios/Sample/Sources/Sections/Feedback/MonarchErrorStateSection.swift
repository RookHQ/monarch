import MonarchUI
import SwiftUI

struct MonarchErrorStateSection: CataloguePage {
    static let title = "ErrorState"

    @State private var isRetrying = false
    @State private var attempts = 0

    var body: some View {
        CatalogueCaption("Server error with a request id")
        MonarchErrorState(
            "Invoices didn't load",
            message: "Something went wrong on our side. Tried \(attempts) times.",
            kind: .server,
            requestID: "req_8f2k1c9d",
            isRetrying: isRetrying,
            actionTitle: "Contact support",
            onRetry: retry,
            action: { attempts = 0 }
        )
        CatalogueCaption("Kinds, small")
        ForEach(MonarchErrorStateKind.allCases, id: \.self) { kind in
            MonarchErrorState(String(describing: kind), kind: kind, size: .sm, onRetry: retry)
        }
    }

    private func retry() {
        isRetrying = true
        attempts += 1
        Task {
            try? await Task.sleep(for: .seconds(1))
            isRetrying = false
        }
    }
}
