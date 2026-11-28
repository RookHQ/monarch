import MonarchUI
import SwiftUI

struct MonarchColumnMappingSection: CataloguePage {
    static let title = "Column mapping"

    private static let sources = [
        MonarchColumnMappingSource("E-mail Address", samples: ["ada@example.com", "bea@example.com"]),
        MonarchColumnMappingSource("Full name", samples: ["Ada Moss", "Bea Lund", "Cal Ortiz"]),
        MonarchColumnMappingSource("Phone", samples: ["+44 7700 900123"]),
        MonarchColumnMappingSource("Organisation", samples: ["Rook", "Atlas Ltd"]),
        MonarchColumnMappingSource("Notes"),
    ]

    private static let fields = [
        MonarchColumnMappingField(id: "email", label: "Email", isRequired: true, aliases: ["E-mail", "Mail"]),
        MonarchColumnMappingField(id: "name", label: "Name", isRequired: true, aliases: ["Full name"]),
        MonarchColumnMappingField(id: "phone", label: "Phone", description: "International format, +44 7700 900123"),
        MonarchColumnMappingField(id: "company", label: "Company", isRequired: true),
    ]

    @State private var mapping = MonarchColumnMapping.mapping(
        from: MonarchColumnMapping.suggest(Self.sources, fields: Self.fields)
    )
    @State private var showsIssues = true

    var body: some View {
        CatalogueCaption("Options")
        Toggle("Show issues", isOn: $showsIssues)
            .monarchTextStyle(.body)

        CatalogueCaption("Map the file's columns to fields")
        MonarchColumnMapping(Self.sources, mapping: $mapping, fields: Self.fields, showsIssues: showsIssues)
            .frame(minHeight: 560)
        let issues = MonarchColumnMapping.issues(mapping, fields: Self.fields)
        Text(issues.missing.isEmpty && issues.duplicates.isEmpty ? "Ready to import" : "Not ready yet")
            .monarchTextStyle(.caption)
    }
}
