import MonarchUI
import SwiftUI

struct MonarchAiDisclaimerSection: CataloguePage {
    static let title = "AI disclaimer"

    @State private var risk = MonarchAiDisclaimerRisk.low

    var body: some View {
        Picker("Risk", selection: $risk) {
            Text("Low").tag(MonarchAiDisclaimerRisk.low)
            Text("Medium").tag(MonarchAiDisclaimerRisk.medium)
            Text("High").tag(MonarchAiDisclaimerRisk.high)
        }
        .pickerStyle(.segmented)
        MonarchAiDisclaimer(risk: risk)

        CatalogueCaption("Every risk")
        ForEach(MonarchAiDisclaimerRisk.allCases, id: \.self) { risk in
            MonarchAiDisclaimer(risk: risk)
        }

        CatalogueCaption("Custom wording")
        MonarchAiDisclaimer("Answers come from your team's documents only.", risk: .medium)
    }
}
