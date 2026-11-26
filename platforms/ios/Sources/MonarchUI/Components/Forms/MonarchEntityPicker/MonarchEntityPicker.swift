import SwiftUI

/// A field for picking a person, a record or another entity from a list
/// that can be searched on a server, shown with avatars.
///
/// ```swift
/// MonarchEntityPicker(
///     "Owner",
///     selection: $ownerID,
///     entities: recentPeople,
///     clearOptionTitle: "Unassigned",
///     search: { query in try await api.people(matching: query) }
/// )
/// ```
///
/// The field shows the chosen entity's avatar and name. Tapping it opens a
/// sheet with a standard `List` and `.searchable` field. Without `search`,
/// typing filters `entities` by name, hint and keywords. With `search`, the
/// typed text is sent after `debounce`; a newer text cancels the older
/// search, and an answer that arrives late is dropped. `entities` are shown
/// until the first search runs. A failed search shows an error with Try
/// again. With a `selection` of several ids, rows add and remove entities
/// and the sheet stays open until Done; the field shows "Ana Silva +2".
/// Pass `selectedEntities` so chosen ids that are not in the current list
/// still show their names.
///
/// Accessibility: the field is a button labelled with `label`; its value is
/// every chosen name (or "None") and its hint is the error, the help text
/// and "Required". Avatars are decorative, because the name is written
/// beside them. The clear button is a separate button named "Clear". In the
/// sheet each row is a button read with its name, description and hint;
/// chosen rows have the selected trait and a check mark. Rows and the field
/// are at least 44 points high.
public struct MonarchEntityPicker: View {
    private enum Selection {
        case single(Binding<String?>)
        case multiple(Binding<[String]>)
    }

    private let label: String
    private let selectionBinding: Selection
    private let entities: [MonarchEntityPickerEntity]
    private let selectedEntities: [MonarchEntityPickerEntity]
    private let prompt: String?
    private let footerTitle: String?
    private let onFooter: (@MainActor () -> Void)?
    private let helper: String?
    private let error: String?
    private let clearOptionTitle: String?
    private let isRequired: Bool
    private let isLoading: Bool
    private let minSearchLength: Int
    private let debounce: Duration
    private let labels: MonarchEntityPickerLabels
    private let search: (@Sendable (String) async throws -> [MonarchEntityPickerEntity])?
    private let onCreate: (@MainActor (String) async -> MonarchEntityPickerEntity?)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @State private var isPresented = false
    @Environment(\.monarchStartsPresented) private var startsPresented
    @State private var query = ""
    @State private var remembered: [String: MonarchEntityPickerEntity] = [:]

    /// A picker for one entity.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. It names the field and the sheet.
    ///   - selection: The id of the chosen entity, or `nil`.
    ///   - entities: The entities to pick from. With `search`, shown until the first search runs.
    ///   - selectedEntities: Entities for chosen ids that may not be in `entities`, for their names.
    ///   - prompt: Shown in the field while nothing is chosen. "Select…" by default.
    ///   - helper: Help text under the field.
    ///   - error: An error message. Shown with an icon and announced.
    ///   - clearOptionTitle: Adds a first row that clears the selection, such as "Unassigned".
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isLoading: Shows "Loading…" while `entities` is empty.
    ///   - minSearchLength: The shortest text `search` runs for.
    ///   - debounce: How long to wait after typing before `search` runs.
    ///   - labels: Replacements for the built-in strings.
    ///   - search: Loads the entities that match the typed text. Throw to show an error with Try again.
    ///   - onCreate: Offers "Create “text”" when no shown name equals the typed text. Return the new entity to choose it.
    ///   - footerTitle: A button pinned under the list, such as "Manage projects".
    ///   - onFooter: Runs when the footer button is tapped.
    public init(
        _ label: String,
        selection: Binding<String?>,
        entities: [MonarchEntityPickerEntity],
        selectedEntities: [MonarchEntityPickerEntity] = [],
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        clearOptionTitle: String? = nil,
        isRequired: Bool = false,
        isLoading: Bool = false,
        minSearchLength: Int = 0,
        debounce: Duration = .milliseconds(250),
        labels: MonarchEntityPickerLabels = MonarchEntityPickerLabels(),
        search: (@Sendable (String) async throws -> [MonarchEntityPickerEntity])? = nil,
        onCreate: (@MainActor (String) async -> MonarchEntityPickerEntity?)? = nil,
        footerTitle: String? = nil,
        onFooter: (@MainActor () -> Void)? = nil
    ) {
        self.label = label
        self.selectionBinding = .single(selection)
        self.entities = entities
        self.selectedEntities = selectedEntities
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.clearOptionTitle = clearOptionTitle
        self.isRequired = isRequired
        self.isLoading = isLoading
        self.minSearchLength = minSearchLength
        self.debounce = debounce
        self.labels = labels
        self.search = search
        self.onCreate = onCreate
        self.footerTitle = footerTitle
        self.onFooter = onFooter
    }

    /// A picker for several entities, kept in the order they were picked.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised. It names the field and the sheet.
    ///   - selection: The ids of the chosen entities.
    ///   - entities: The entities to pick from. With `search`, shown until the first search runs.
    ///   - selectedEntities: Entities for chosen ids that may not be in `entities`, for their names.
    ///   - prompt: Shown in the field while nothing is chosen. "Select…" by default.
    ///   - helper: Help text under the field.
    ///   - error: An error message. Shown with an icon and announced.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isLoading: Shows "Loading…" while `entities` is empty.
    ///   - minSearchLength: The shortest text `search` runs for.
    ///   - debounce: How long to wait after typing before `search` runs.
    ///   - labels: Replacements for the built-in strings.
    ///   - search: Loads the entities that match the typed text. Throw to show an error with Try again.
    ///   - onCreate: Offers "Create “text”" when no shown name equals the typed text. Return the new entity to add it.
    ///   - footerTitle: A button pinned under the list, such as "Manage projects".
    ///   - onFooter: Runs when the footer button is tapped.
    public init(
        _ label: String,
        selection: Binding<[String]>,
        entities: [MonarchEntityPickerEntity],
        selectedEntities: [MonarchEntityPickerEntity] = [],
        prompt: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        isRequired: Bool = false,
        isLoading: Bool = false,
        minSearchLength: Int = 0,
        debounce: Duration = .milliseconds(250),
        labels: MonarchEntityPickerLabels = MonarchEntityPickerLabels(),
        search: (@Sendable (String) async throws -> [MonarchEntityPickerEntity])? = nil,
        onCreate: (@MainActor (String) async -> MonarchEntityPickerEntity?)? = nil,
        footerTitle: String? = nil,
        onFooter: (@MainActor () -> Void)? = nil
    ) {
        self.label = label
        self.selectionBinding = .multiple(selection)
        self.entities = entities
        self.selectedEntities = selectedEntities
        self.prompt = prompt
        self.helper = helper
        self.error = error
        self.clearOptionTitle = nil
        self.isRequired = isRequired
        self.isLoading = isLoading
        self.minSearchLength = minSearchLength
        self.debounce = debounce
        self.labels = labels
        self.search = search
        self.onCreate = onCreate
        self.footerTitle = footerTitle
        self.onFooter = onFooter
    }

    private var isMultiple: Bool {
        if case .multiple = selectionBinding { return true }
        return false
    }

    private var selectedIDs: [String] {
        switch selectionBinding {
        case .single(let binding): binding.wrappedValue.map { [$0] } ?? []
        case .multiple(let binding): binding.wrappedValue
        }
    }

    private var known: [String: MonarchEntityPickerEntity] {
        var known = remembered
        for entity in selectedEntities + entities { known[entity.id] = entity }
        return known
    }

    private var activeError: String? { MonarchFormFieldTokens.activeError(error) }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchComboboxTokens.stackSpacing) {
            labelRow
            field
            supportingText
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchAnnouncesError(activeError, labels: labels.field)
        .onAppear { if startsPresented { isPresented = true } }
        .onChange(of: isPresented) { _, presented in
            if !presented { query = "" }
        }
        .sheet(isPresented: $isPresented) {
            MonarchEntityPickerList(
                title: label,
                isMultiple: isMultiple,
                selection: selectedIDs,
                entities: entities,
                query: $query,
                clearOptionTitle: clearOptionTitle,
                isLoading: isLoading,
                minSearchLength: minSearchLength,
                debounce: debounce,
                labels: labels,
                search: search,
                canCreate: onCreate != nil,
                footerTitle: footerTitle,
                onFooter: onFooter,
                onChoose: choose,
                onCreate: create,
                onClose: { isPresented = false }
            )
            .environment(\.monarchTheme, theme)
        }
    }

    private var labelRow: some View {
        MonarchFieldLabelRow(label: label, isRequired: isRequired)
    }

    private var field: some View {
        let shape = MonarchShapes.control
        let border = MonarchTextFieldTokens.border(hasError: activeError != nil, isEnabled: isEnabled)
        let background = MonarchTextFieldTokens.background(isEnabled: isEnabled)
        let known = known
        let ids = selectedIDs
        let names = ids.map { MonarchEntityPickerTokens.name(of: $0, known: known) }
        let summary = MonarchEntityPickerTokens.summary(names: names, labels: labels)
        return HStack(spacing: MonarchComboboxTokens.spacing) {
            Button {
                isPresented = true
            } label: {
                HStack(spacing: MonarchComboboxTokens.spacing) {
                    if let first = ids.first {
                        MonarchEntityPickerAvatar(
                            entity: known[first] ?? MonarchEntityPickerEntity(id: first, name: names[0]),
                            size: MonarchEntityPickerTokens.fieldAvatarSize
                        )
                    }
                    Text(summary ?? prompt ?? labels.select ?? MonarchEntityPickerStrings.select)
                        .monarchTextStyle(.body)
                        .foregroundStyle(
                            theme.colors[
                                role: MonarchComboboxTokens.valueColor(
                                    hasSelection: summary != nil,
                                    isEnabled: isEnabled
                                )
                            ]
                        )
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    MonarchIcon(systemName: MonarchComboboxTokens.chevronSymbol, size: .sm)
                        .foregroundStyle(theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: isEnabled)])
                }
                .padding(.vertical, MonarchComboboxTokens.verticalPadding)
                .frame(minHeight: MonarchComboboxTokens.minHeight)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label)
            .accessibilityValue(MonarchEntityPickerTokens.accessibilityValue(names: names, labels: labels))
            .accessibilityHint(
                MonarchFormFieldTokens.accessibilityHint(
                    helper: helper,
                    error: activeError,
                    isRequired: isRequired,
                    labels: labels.field
                )
            )
            if !ids.isEmpty {
                Button {
                    clear()
                } label: {
                    MonarchIcon(
                        systemName: MonarchComboboxTokens.clearSymbol,
                        label: labels.clear ?? MonarchEntityPickerStrings.clear,
                        size: .sm
                    )
                    .foregroundStyle(theme.colors[role: MonarchComboboxTokens.secondaryColor(isEnabled: isEnabled)])
                    .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, MonarchComboboxTokens.horizontalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchComboboxTokens.minHeight, alignment: .leading)
        .background(shape.fill(theme.colors[role: background]))
        .overlay(shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchShapes.borderWidth))
    }

    private var supportingText: some View {
        MonarchFieldSupportingText(helper: helper, error: activeError)
    }

    private func choose(_ entity: MonarchEntityPickerEntity?) {
        if let entity { remembered[entity.id] = entity }
        switch selectionBinding {
        case .single(let binding):
            binding.wrappedValue = entity?.id
            isPresented = false
        case .multiple(let binding):
            guard let entity else { return }
            binding.wrappedValue = MonarchEntityPickerTokens.toggled(binding.wrappedValue, id: entity.id)
        }
    }

    private func clear() {
        switch selectionBinding {
        case .single(let binding): binding.wrappedValue = nil
        case .multiple(let binding): binding.wrappedValue = []
        }
    }

    private func create(_ text: String) async {
        guard let onCreate else { return }
        guard let entity = await onCreate(text.trimmingCharacters(in: .whitespacesAndNewlines)) else { return }
        remembered[entity.id] = entity
        switch selectionBinding {
        case .single(let binding):
            binding.wrappedValue = entity.id
            isPresented = false
        case .multiple(let binding):
            if !binding.wrappedValue.contains(entity.id) { binding.wrappedValue.append(entity.id) }
            query = ""
        }
    }
}

#if DEBUG
    #Preview("MonarchEntityPicker") {
        let people = [
            MonarchEntityPickerEntity(id: "ana", name: "Ana Silva", description: "Design", hint: "ana@example.com"),
            MonarchEntityPickerEntity(id: "sam", name: "Sam Lee", description: "Engineering"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchEntityPicker("Owner", selection: .constant("ana"), entities: people)
                MonarchEntityPicker("Reviewers", selection: .constant(["ana", "sam"]), entities: people)
                MonarchEntityPicker(
                    "Owner",
                    selection: .constant(nil),
                    entities: people,
                    error: "Choose an owner.",
                    isRequired: true
                )
            }
        }
    }
#endif
