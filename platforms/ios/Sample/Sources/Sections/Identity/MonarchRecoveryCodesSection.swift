import MonarchUI
import SwiftUI

struct MonarchRecoveryCodesSection: CataloguePage {
    static let title = "Recovery codes"

    @State private var codes = Self.makeCodes()
    @State private var status = "Not saved yet."

    var body: some View {
        CatalogueCaption("Setup: save, then acknowledge")
        Text(status).monarchTextStyle(.caption)
        MonarchRecoveryCodes(
            codes,
            heading: "Example account recovery codes",
            onRegenerate: {
                try? await Task.sleep(for: .seconds(1))
                codes = Self.makeCodes()
            },
            onAcknowledge: { status = "Acknowledged." }
        )

        CatalogueCaption("Settings: some used, hidden at first")
        MonarchRecoveryCodes(
            [
                "4f7k-2m9q", MonarchRecoveryCode("8d3r-6t1w", isUsed: true), "7n4h-3j8k",
                MonarchRecoveryCode("2p6s-5l0z", isUsed: true), "9a1e-4u7y", "3g8b-1q6r",
            ],
            startsRevealed: false
        )
    }

    private static func makeCodes() -> [MonarchRecoveryCode] {
        let characters = Array("abcdefghjkmnpqrstuvwxyz23456789")
        return (0..<10).map { _ in
            let part = { String((0..<4).map { _ in characters.randomElement() ?? "a" }) }
            return MonarchRecoveryCode("\(part())-\(part())")
        }
    }
}
