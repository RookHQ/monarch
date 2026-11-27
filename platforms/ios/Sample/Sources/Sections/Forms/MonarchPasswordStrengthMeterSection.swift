import MonarchUI
import SwiftUI

struct MonarchPasswordStrengthMeterSection: CataloguePage {
    static let title = "Password strength meter"

    @State private var password = ""

    var body: some View {
        MonarchPasswordInput("New password", text: $password, purpose: .new, isRevealed: .constant(true))
        MonarchPasswordStrengthMeter(password: password)

        CatalogueCaption("Without the list of rules")
        MonarchPasswordStrengthMeter(password: password, showsRules: false)

        CatalogueCaption("Custom rule")
        MonarchPasswordStrengthMeter(
            password: password,
            rules: [
                MonarchPasswordRule(id: "words", label: "Four words or more") {
                    $0.split(separator: " ").count >= 4
                }
            ]
        )
    }
}
