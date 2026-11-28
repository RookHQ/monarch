import MonarchUI
import SwiftUI

struct MonarchDomainProgressSection: CataloguePage {
    static let title = "Domain progress"

    @State private var stage = MonarchDomainStage.dns
    @State private var hasProblem = false
    @State private var checkedAt = Date()

    private var problems: [MonarchDomainProblem] {
        hasProblem
            ? [
                MonarchDomainProblem(
                    "Your domain points somewhere else. Remove the A record for book, then add the CNAME."
                )
            ]
            : []
    }

    var body: some View {
        MonarchDomainProgress(
            "book.example.com",
            stage: stage,
            problems: problems,
            dnsFound: stage == .dns
                ? MonarchDomainDnsSnapshot(a: hasProblem ? ["203.0.113.10"] : [])
                : MonarchDomainDnsSnapshot(cnames: ["domains.example.net"]),
            checkedAt: checkedAt,
            notify: .email
        )
        Picker("Stage", selection: $stage) {
            ForEach(MonarchDomainStage.allCases, id: \.self) { stage in
                Text(stage.rawValue).tag(stage)
            }
        }
        .pickerStyle(.segmented)
        Toggle("Blocking problem", isOn: $hasProblem)
            .monarchTextStyle(.body)
        MonarchButton("Check again", variant: .secondary) { checkedAt = Date() }

        CatalogueCaption("Certificate pending, shown quietly")
        MonarchDomainProgress(
            "shop.example.org",
            stage: .certificate,
            problems: [MonarchDomainProblem("The certificate authority has not answered yet.", severity: .info)],
            isRechecking: false
        )

        CatalogueCaption("Live")
        MonarchDomainProgress("book.example.com", stage: .live)
    }
}
