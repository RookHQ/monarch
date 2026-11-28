import MonarchTokens
import SwiftUI

/// Merges two or more duplicate records into one: the person picks the
/// record to keep, then the value to keep for every field where the records
/// differ. Fields that already match are listed once.
///
/// Present it in a sheet:
///
/// ```swift
/// .sheet(isPresented: $isMerging) {
///     MonarchMergeDialog(
///         records: duplicates,
///         fields: [MonarchMergeField(id: "email", label: "Email")],
///         consequences: [MonarchConsequence(.changed, "12 invoices move to the kept record")]
///     ) { selection in
///         try await api.merge(selection)
///     }
/// }
/// ```
///
/// `onMerge` runs when the person confirms. While it runs, the dialog is
/// busy and cannot be swiped away; when it returns the sheet closes; when
/// it throws, the error is shown and every choice stays as it was.
///
/// Accessibility: each choice is a standard inline `Picker`, so VoiceOver
/// reads every option with its selected state. A value option reads its
/// value and its source ("hello@acme.com, from Acme Ltd"). A failure is
/// announced.
public struct MonarchMergeDialog: View {
    private let records: [MonarchMergeRecord]
    private let fields: [MonarchMergeField]
    private let consequences: [MonarchConsequence]
    private let blockers: [String]
    private let labels: MonarchMergeDialogLabels
    private let onCancel: (@MainActor () -> Void)?
    private let onMerge: @MainActor (MonarchMergeSelection) async throws -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var primaryID: String
    @State private var picked: [String: String] = [:]
    @State private var isMerging = false
    @State private var failure: String?

    /// A merge dialog.
    ///
    /// - Parameters:
    ///   - records: The records to merge, two or more.
    ///   - fields: The fields whose surviving value can be picked, in display order.
    ///   - defaultPrimaryID: The record kept by default. The first record when `nil`.
    ///   - consequences: What else happens, such as invoices that move to the kept record.
    ///   - blockers: Problems that stop the merge. They are shown as a warning and the merge button is disabled.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCancel: Runs when Cancel is pressed, before the dialog dismisses itself.
    ///   - onMerge: Performs the merge. Throw to show an error and keep the dialog open.
    public init(
        records: [MonarchMergeRecord],
        fields: [MonarchMergeField],
        defaultPrimaryID: String? = nil,
        consequences: [MonarchConsequence] = [],
        blockers: [String] = [],
        labels: MonarchMergeDialogLabels = MonarchMergeDialogLabels(),
        onCancel: (@MainActor () -> Void)? = nil,
        onMerge: @escaping @MainActor (MonarchMergeSelection) async throws -> Void
    ) {
        self.records = records
        self.fields = fields
        self.consequences = consequences
        self.blockers = blockers
        self.labels = labels
        self.onCancel = onCancel
        self.onMerge = onMerge
        self._primaryID = State(initialValue: defaultPrimaryID ?? records.first?.id ?? "")
    }

    private var resolvedPrimaryID: String {
        MonarchMergeDialogTokens.primaryID(primaryID, records: records) ?? primaryID
    }

    private var isBlocked: Bool {
        MonarchMergeDialogTokens.isBlocked(recordCount: records.count, blockers: blockers)
    }

    /// The content of the view.
    public var body: some View {
        let split = MonarchMergeDialogTokens.split(fields: fields, records: records)
        VStack(spacing: 0) {
            header
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space5) {
                    recordSection
                    if !split.differing.isEmpty {
                        valuesSection(split.differing)
                    }
                    if !split.same.isEmpty {
                        sameSection(split.same)
                    }
                    if !consequences.isEmpty {
                        VStack(alignment: .leading, spacing: theme.spacing.space2) {
                            sectionHeader(labels.consequences ?? MonarchMergeDialogStrings.consequences)
                            MonarchConsequenceList(consequences)
                        }
                    }
                    if !blockers.isEmpty {
                        MonarchBanner(message: blockers.joined(separator: "\n"), tone: .warning)
                    }
                    if let failure {
                        MonarchBanner(message: failure, tone: .danger)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, theme.spacing.space5)
                .padding(.bottom, theme.spacing.space5)
            }
            .disabled(isMerging)
            footer
        }
        .background(theme.colors[role: MonarchMergeDialogTokens.row])
        .foregroundStyle(theme.colors[role: MonarchMergeDialogTokens.text])
        .presentationBackground(theme.colors[role: MonarchMergeDialogTokens.row])
        .interactiveDismissDisabled(isMerging)
    }

    // MARK: Parts

    /// The title and the close button, as the web dialog's header.
    private var header: some View {
        HStack(alignment: .top, spacing: theme.spacing.space2) {
            Text(labels.title ?? MonarchMergeDialogStrings.title)
                .monarchTextStyle(.heading)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 0)
            Button {
                onCancel?()
                dismiss()
            } label: {
                MonarchIcon(systemName: "xmark", size: .sm)
                    .foregroundStyle(theme.colors[role: MonarchMergeDialogTokens.muted])
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(isMerging)
            .accessibilityLabel(labels.cancel ?? MonarchMergeDialogStrings.cancel)
        }
        .padding(.leading, theme.spacing.space5)
        .padding(.trailing, theme.spacing.space2)
        .padding(.top, theme.spacing.space3)
    }

    /// Cancel and the merge button, on the muted bar under a hairline.
    private var footer: some View {
        HStack(spacing: theme.spacing.space2) {
            Spacer(minLength: 0)
            MonarchButton(labels.cancel ?? MonarchMergeDialogStrings.cancel) {
                onCancel?()
                dismiss()
            }
            .disabled(isMerging)
            MonarchButton(
                MonarchMergeDialogTokens.confirmTitle(count: records.count, labels: labels),
                variant: .primary,
                isLoading: isMerging
            ) {
                merge()
            }
            .disabled(isBlocked)
        }
        .padding(.horizontal, theme.spacing.space5)
        .padding(.vertical, theme.spacing.space3)
        .background(theme.colors.bgSubtle)
        .overlay(alignment: .top) {
            Rectangle().fill(theme.colors.borderSubtle).frame(height: 1)
        }
    }

    private var columns: [GridItem] {
        let count = dynamicTypeSize.isAccessibilitySize ? 1 : 2
        return Array(repeating: GridItem(.flexible(), spacing: theme.spacing.space2, alignment: .top), count: count)
    }

    private var recordSection: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            Text(labels.description ?? MonarchMergeDialogStrings.description)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: MonarchMergeDialogTokens.muted])
            sectionHeader(labels.keep ?? MonarchMergeDialogStrings.keep)
            LazyVGrid(columns: columns, spacing: theme.spacing.space2) {
                ForEach(records) { record in
                    radioCard(
                        title: record.title,
                        caption: record.description,
                        isSelected: record.id == resolvedPrimaryID,
                        badge: record.id == resolvedPrimaryID ? labels.kept ?? MonarchMergeDialogStrings.kept : nil,
                        label: record.title
                    ) { primaryID = record.id }
                }
            }
        }
    }

    private func valuesSection(_ differing: [MonarchMergeField]) -> some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            sectionHeader(labels.values ?? MonarchMergeDialogStrings.values)
            ForEach(differing) { field in
                VStack(alignment: .leading, spacing: theme.spacing.space2) {
                    Text(field.label)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchMergeDialogTokens.muted])
                    LazyVGrid(columns: columns, spacing: theme.spacing.space2) {
                        ForEach(records) { record in
                            let value = record.values[field.id]
                            radioCard(
                                title: MonarchMergeDialogTokens.valueText(value, labels: labels),
                                caption: MonarchMergeDialogTokens.sourceText(record, labels: labels),
                                isSelected: choiceBinding(field.id).wrappedValue == record.id,
                                isEmptyValue: MonarchMergeDialogTokens.isEmpty(value),
                                label: MonarchMergeDialogTokens.optionLabel(
                                    value: value,
                                    record: record,
                                    labels: labels
                                )
                            ) { choiceBinding(field.id).wrappedValue = record.id }
                        }
                    }
                }
            }
        }
    }

    /// One radio card: a radio mark, the value, its source and, for the kept record, a badge.
    private func radioCard(
        title: String,
        caption: String?,
        isSelected: Bool,
        isEmptyValue: Bool = false,
        badge: String? = nil,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: theme.spacing.space2) {
                Image(systemName: isSelected ? "circle.inset.filled" : "circle")
                    .foregroundStyle(
                        isSelected ? theme.colors.accentSolid : theme.colors[role: MonarchMergeDialogTokens.muted]
                    )
                    .padding(.top, 2)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: MonarchMergeDialogTokens.lineSpacing) {
                    HStack(alignment: .firstTextBaseline, spacing: theme.spacing.space1_5) {
                        Text(title)
                            .monarchTextStyle(.label)
                            .italic(isEmptyValue)
                            .foregroundStyle(
                                theme.colors[
                                    role: isEmptyValue ? MonarchMergeDialogTokens.muted : MonarchMergeDialogTokens.text
                                ]
                            )
                        if let badge {
                            Spacer(minLength: 0)
                            MonarchBadge(badge, tone: .accent, variant: .solid, size: .sm)
                                .accessibilityHidden(true)
                        }
                    }
                    if let caption {
                        Text(caption)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: MonarchMergeDialogTokens.muted])
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(theme.spacing.space2_5)
            .frame(maxWidth: .infinity, minHeight: 44, alignment: .topLeading)
            .background(
                isSelected ? theme.colors.accentSubtle : theme.colors[role: MonarchMergeDialogTokens.row],
                in: MonarchShapes.control
            )
            .overlay(
                MonarchShapes.control.strokeBorder(
                    isSelected ? theme.colors.accentBorder : theme.colors.borderDefault,
                    lineWidth: MonarchShapes.borderWidth
                )
            )
            .contentShape(MonarchShapes.control)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }

    private func sameSection(_ same: [MonarchMergeField]) -> some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            sectionHeader(labels.same ?? MonarchMergeDialogStrings.same)
            ForEach(same) { field in
                let value = records.first { $0.id == resolvedPrimaryID }?.values[field.id]
                HStack(alignment: .firstTextBaseline, spacing: theme.spacing.space4) {
                    Text(field.label)
                        .foregroundStyle(theme.colors[role: MonarchMergeDialogTokens.muted])
                        .frame(width: theme.spacing.space24 * 0.7, alignment: .leading)
                    Text(MonarchMergeDialogTokens.valueText(value, labels: labels))
                }
                .monarchTextStyle(.body)
                .accessibilityElement(children: .combine)
            }
        }
    }

    private func sectionHeader(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.subheading)
            .foregroundStyle(theme.colors[role: MonarchMergeDialogTokens.text])
            .accessibilityAddTraits(.isHeader)
    }

    // MARK: Behaviour

    private func choiceBinding(_ field: String) -> Binding<String> {
        Binding(
            get: {
                MonarchMergeDialogTokens.choice(
                    field: field,
                    picked: picked,
                    records: records,
                    primaryID: resolvedPrimaryID
                )
            },
            set: { picked[field] = $0 }
        )
    }

    private func merge() {
        guard !isMerging, !isBlocked,
            let selection = MonarchMergeDialogTokens.selection(
                records: records,
                fields: fields,
                primaryID: primaryID,
                picked: picked
            )
        else { return }
        failure = nil
        isMerging = true
        Task {
            do {
                try await onMerge(selection)
                isMerging = false
                dismiss()
            } catch {
                let message = MonarchMergeDialogTokens.errorMessage(error, labels: labels)
                isMerging = false
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchMergeDialog") {
        MonarchPreviewMatrix {
            MonarchMergeDialog(
                records: [
                    MonarchMergeRecord(
                        id: "a",
                        title: "Acme Ltd",
                        description: "Created 3 Mar 2025",
                        values: ["email": "hello@acme.com", "phone": ""]
                    ),
                    MonarchMergeRecord(
                        id: "b",
                        title: "ACME Limited",
                        values: ["email": "info@acme.com", "phone": "+44 20 7946 0000"]
                    ),
                ],
                fields: [
                    MonarchMergeField(id: "email", label: "Email"),
                    MonarchMergeField(id: "phone", label: "Phone"),
                ]
            ) { _ in }
            .frame(height: MonarchTokens.Space.space24 * 6)
        }
    }
#endif
