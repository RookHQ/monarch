@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiHandoffSection: CataloguePage {
    static let title = "AI handoff"

    private let research = MonarchAiAgentRef(id: "research", name: "Research agent", codename: "Ra")

    var body: some View {
        CatalogueCaption("Agent to agent")
        MonarchAiHandoff(
            from: research,
            to: .agent(MonarchAiAgentRef(id: "writer", name: "Writing agent", codename: "Wa")),
            reason: "Sources are ready to draft from",
            date: Date().addingTimeInterval(-180)
        )

        CatalogueCaption("Agent to a person")
        MonarchAiHandoff(
            from: research,
            to: .person(name: "Sam Lee"),
            reason: "Needs a person to approve the refund",
            date: Date().addingTimeInterval(-3_600)
        )

        CatalogueCaption("Without reason or time")
        MonarchAiHandoff(from: research, to: .person(name: "Sam Lee"))
    }
}
