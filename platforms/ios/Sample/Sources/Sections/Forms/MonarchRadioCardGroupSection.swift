import MonarchUI
import SwiftUI

struct MonarchRadioCardGroupSection: CataloguePage {
    static let title = "Radio card group"

    @State private var plan: String? = "team"
    @State private var region: Int?

    var body: some View {
        MonarchRadioCardGroup(
            "Plan",
            selection: $plan,
            options: [
                MonarchRadioCardOption("starter", title: "Starter", description: "For one person.", meta: "$0"),
                MonarchRadioCardOption(
                    "team",
                    title: "Team",
                    description: "Up to 10 people, shared projects and roles.",
                    systemImage: "person.2",
                    meta: "$20"
                ),
                MonarchRadioCardOption(
                    "business",
                    title: "Business",
                    description: "Unlimited people and an audit log.",
                    systemImage: "building.2",
                    meta: "$60"
                ),
            ]
        )

        CatalogueCaption("Two columns on a regular width, one card disabled")
        MonarchRadioCardGroup(
            "Region",
            selection: $region,
            options: [
                MonarchRadioCardOption(1, title: "Europe"),
                MonarchRadioCardOption(2, title: "North America"),
                MonarchRadioCardOption(3, title: "Asia Pacific", description: "Not available yet.", isDisabled: true),
            ],
            columns: 2
        )

        CatalogueCaption("Disabled")
        MonarchRadioCardGroup(
            "Plan",
            selection: .constant("starter"),
            options: [MonarchRadioCardOption("starter", title: "Starter", meta: "$0")]
        )
        .disabled(true)
    }
}
