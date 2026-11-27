import MonarchUI
import SwiftUI

/// Style coverage: standard disclosure groups with the Monarch style.
struct AccordionSection: CataloguePage {
    static let title = "Accordion (DisclosureGroup style)"

    @State private var expanded: String? = "billing"
    @State private var openSections: Set<String> = []

    private let faq = [
        ("billing", "How does billing work?", "You are billed at the start of each period for the seats you have."),
        ("cancel", "Can I cancel at any time?", "Yes. The plan stays active until the end of the period you paid for."),
        ("export", "Can I export my data?", "Export from Settings, Data. The archive includes attachments."),
    ]

    var body: some View {
        CatalogueCaption("One open at a time")
        VStack(alignment: .leading, spacing: 0) {
            ForEach(faq, id: \.0) { id, title, answer in
                DisclosureGroup(
                    title,
                    isExpanded: Binding(get: { expanded == id }, set: { expanded = $0 ? id : nil })
                ) {
                    Text(answer).monarchTextStyle(.body)
                }
                .accessibilityAddTraits(.isHeader)
                MonarchDivider()
            }
        }
        .disclosureGroupStyle(.monarch)
        CatalogueCaption("Any number open")
        VStack(alignment: .leading, spacing: 0) {
            ForEach(faq, id: \.0) { id, title, answer in
                DisclosureGroup(
                    title,
                    isExpanded: Binding(
                        get: { openSections.contains(id) },
                        set: { isOpen in
                            if isOpen { openSections.insert(id) } else { openSections.remove(id) }
                        }
                    )
                ) {
                    Text(answer).monarchTextStyle(.body)
                }
                MonarchDivider()
            }
        }
        .disclosureGroupStyle(.monarch)
    }
}
