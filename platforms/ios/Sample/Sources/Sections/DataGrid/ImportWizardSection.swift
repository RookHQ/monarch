import MonarchUI
import SwiftUI
import TabularData
import UniformTypeIdentifiers

/// Recipe: upload a CSV, map its columns, preview the rows and import, in a sheet.
struct ImportWizardSection: CataloguePage {
    static let title = "Import wizard (sheet recipe)"

    private struct Table {
        let fileName: String
        let headers: [String]
        let rows: [[String: String]]
    }

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var step = 0
    @State private var table: Table?
    @State private var readError: String?
    @State private var mapping: [String: String] = [:]
    @State private var progress: Double?
    @State private var imported = 0
    @State private var result = "Nothing imported yet"

    private let steps = [
        MonarchStepperStep(id: "upload", label: "Upload"),
        MonarchStepperStep(id: "map", label: "Map columns"),
        MonarchStepperStep(id: "preview", label: "Preview"),
        MonarchStepperStep(id: "results", label: "Results"),
    ]
    private let fields = [
        MonarchColumnMappingField(id: "email", label: "Email", isRequired: true, aliases: ["E-mail", "Mail"]),
        MonarchColumnMappingField(id: "name", label: "Name", isRequired: true, aliases: ["Full name"]),
        MonarchColumnMappingField(id: "company", label: "Company"),
    ]
    private static let sampleFile = Table(
        fileName: "contacts.csv",
        headers: ["E-mail", "Full name", "Organisation"],
        rows: [
            ["E-mail": "ada@example.com", "Full name": "Ada Moss", "Organisation": "Moss & Co"],
            ["E-mail": "bea@example.com", "Full name": "", "Organisation": "Lund AB"],
            ["E-mail": "cal@example.com", "Full name": "Cal Reyes", "Organisation": ""],
        ]
    )

    var body: some View {
        Text(result).monarchTextStyle(.bodyStrong)
        MonarchButton("Import contacts", systemImage: "square.and.arrow.down", variant: .primary) {
            reset()
            isOpen = true
        }
        .sheet(isPresented: $isOpen) {
            NavigationStack {
                ScrollView {
                    VStack(alignment: .leading, spacing: theme.spacing.space4) {
                        MonarchStepper(steps, current: step, furthest: step)
                        Text(steps[step].label)
                            .monarchTextStyle(.heading)
                            .accessibilityAddTraits(.isHeader)
                        stepContent
                    }
                    .padding(theme.spacing.space4)
                }
                .safeAreaInset(edge: .bottom) { bottomBar }
                .background(theme.colors.bgSurface)
                .navigationTitle("Import contacts")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { isOpen = false }
                            .disabled(progress != nil)
                    }
                }
            }
            .interactiveDismissDisabled(step > 0)
            .presentationBackground(theme.colors.bgSurface)
        }
    }

    // MARK: - Steps

    @ViewBuilder private var stepContent: some View {
        switch step {
        case 0:
            Text("A CSV file whose first row holds the column names.").monarchTextStyle(.body)
            MonarchFileUpload(allowedContentTypes: [.commaSeparatedText], maxFiles: 1) { accepted, _ in
                if let file = accepted.first { read(file.url) }
            }
            MonarchButton("Use the sample file", variant: .ghost) { load(Self.sampleFile) }
            if let readError {
                MonarchInlineNotice(readError, tone: .danger)
            }
        case 1:
            if let table {
                Text("\(table.fileName) · \(table.rows.count) rows. Choose which column fills each field.")
                    .monarchTextStyle(.body)
                MonarchColumnMapping(sources(table), mapping: $mapping, fields: fields)
            }
        case 2:
            Text("Nothing has been saved yet. Rows with errors are skipped; the rest are imported.")
                .monarchTextStyle(.body)
            MonarchPreviewRows(
                "Preview of \(table?.fileName ?? "")",
                rows: previewRows,
                columns: fields.map { MonarchPreviewRowsColumn($0.label, id: $0.id) }
            )
            if let progress {
                ProgressView("Importing \(importable) rows…", value: progress)
                    .progressViewStyle(.monarchLinear)
            }
        default:
            MonarchInlineNotice("\(imported) created, \(previewRows.count - imported) skipped.", tone: .success)
        }
    }

    private var bottomBar: some View {
        HStack {
            if step > 0 && step < 3 {
                MonarchButton("Back") { move(to: step - 1) }
                    .disabled(progress != nil)
            }
            Spacer(minLength: 0)
            switch step {
            case 0:
                EmptyView()
            case 1:
                MonarchButton("Check rows", variant: .primary) { move(to: 2) }
                    .disabled(!MonarchColumnMapping.issues(mapping, fields: fields).missing.isEmpty)
            case 2:
                MonarchButton(
                    "Import \(importable) rows",
                    variant: .primary,
                    isLoading: progress != nil
                ) {
                    runImport()
                }
                .disabled(importable == 0)
            default:
                MonarchButton("Done", variant: .primary) { isOpen = false }
            }
        }
        .padding(theme.spacing.space4)
        .background(theme.colors.bgSurface)
    }

    // MARK: - Data

    private var previewRows: [MonarchPreviewRow] {
        guard let table else { return [] }
        return table.rows.enumerated().map { index, raw in
            var values: [String: String] = [:]
            for field in fields {
                if let column = mapping[field.id] { values[field.id] = raw[column] ?? "" }
            }
            let errors = fields.filter { $0.isRequired && (values[$0.id] ?? "").isEmpty }
                .map { MonarchPreviewRowIssue("\($0.label) is required", field: $0.label) }
            return MonarchPreviewRow(row: index + 1, values: values, errors: errors)
        }
    }

    private var importable: Int {
        previewRows.filter { $0.resolvedStatus != .error }.count
    }

    private func sources(_ table: Table) -> [MonarchColumnMappingSource] {
        table.headers.map { header in
            MonarchColumnMappingSource(header, samples: table.rows.prefix(3).compactMap { $0[header] })
        }
    }

    private func read(_ url: URL) {
        let isScoped = url.startAccessingSecurityScopedResource()
        defer { if isScoped { url.stopAccessingSecurityScopedResource() } }
        do {
            let frame = try DataFrame(contentsOfCSVFile: url)
            let headers = frame.columns.map(\.name)
            let rows = frame.rows.map { row in
                Dictionary(uniqueKeysWithValues: headers.map { ($0, row[$0].map { "\($0)" } ?? "") })
            }
            guard !rows.isEmpty else {
                readError = "This file has no data rows under the header row."
                return
            }
            load(Table(fileName: url.lastPathComponent, headers: headers, rows: rows))
        } catch {
            readError = "This file could not be read. Check that it is a CSV file and try again."
        }
    }

    private func load(_ table: Table) {
        readError = nil
        self.table = table
        mapping = MonarchColumnMapping.mapping(from: MonarchColumnMapping.suggest(sources(table), fields: fields))
        move(to: 1)
    }

    private func runImport() {
        let total = importable
        progress = 0
        Task { @MainActor in
            for done in 1...max(total, 1) {
                try? await Task.sleep(for: .milliseconds(300))
                progress = Double(done) / Double(max(total, 1))
            }
            imported = total
            progress = nil
            result = "Imported \(total) contacts"
            move(to: 3)
        }
    }

    private func move(to index: Int) {
        step = index
        AccessibilityNotification.Announcement("Step \(index + 1) of \(steps.count), \(steps[index].label)").post()
    }

    private func reset() {
        step = 0
        table = nil
        readError = nil
        mapping = [:]
        progress = nil
        imported = 0
    }
}
