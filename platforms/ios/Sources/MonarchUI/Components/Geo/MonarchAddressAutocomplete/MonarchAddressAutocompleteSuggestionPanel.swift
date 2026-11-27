import SwiftUI

/// The suggestion list of ``MonarchAddressAutocomplete``, drawn under the field as the web draws its
/// dropdown: a bordered panel of suggestions with the manual-entry row last. It runs the debounced search
/// for the text of the field above it.
struct MonarchAddressAutocompleteSuggestionPanel: View {
    /// The search text and the retry count, so Try again reruns the same text.
    private struct SearchKey: Equatable {
        let query: String
        let attempt: Int
    }

    let query: String
    let minQueryLength: Int
    let debounce: Duration
    let allowsManualEntry: Bool
    let labels: MonarchAddressAutocompleteLabels
    let search: @Sendable (String) async throws -> [MonarchAddressAutocompleteSuggestion]
    let onPick: @MainActor (MonarchAddressAutocompleteSuggestion) -> Void
    let onManual: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var searcher = MonarchDebouncedSearch<MonarchAddressAutocompleteSuggestion>()
    @State private var attempt = 0

    var body: some View {
        let content = content
        VStack(alignment: .leading, spacing: 0) {
            switch content {
            case .suggestions:
                ForEach(searcher.results) { suggestion in row(suggestion) }
            case .searching:
                HStack(spacing: MonarchAddressAutocompleteTokens.spacing) {
                    ProgressView()
                    Text(message(content)).monarchTextStyle(.body)
                }
                .foregroundStyle(theme.colors.textMuted)
                .padding(MonarchAddressAutocompleteTokens.panelPadding)
                .accessibilityElement(children: .combine)
            case .failed:
                MonarchInlineErrorRetry(message(content)) { attempt += 1 }
                    .padding(MonarchAddressAutocompleteTokens.panelPadding)
            case .tooShort, .empty:
                Text(message(content))
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                    .padding(MonarchAddressAutocompleteTokens.panelPadding)
            case .idle:
                EmptyView()
            }
            if allowsManualEntry {
                Button(action: onManual) {
                    Text(labels.enterManually ?? MonarchAddressAutocompleteStrings.enterManually)
                        .monarchTextStyle(.body, weight: .medium)
                        .foregroundStyle(theme.colors.textLink)
                        .frame(
                            maxWidth: .infinity,
                            minHeight: MonarchAccessibility.minimumHitTarget,
                            alignment: .leading
                        )
                        .padding(.horizontal, MonarchAddressAutocompleteTokens.panelPadding)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonarchShapes.control.fill(theme.colors.bgSurface))
        .overlay(
            MonarchShapes.control.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth)
        )
        .task(id: SearchKey(query: query, attempt: attempt)) {
            await searcher.run(query, minLength: minQueryLength, debounce: debounce, search: search)
            if let message = MonarchAddressAutocompleteTokens.announcement(content, labels: labels) {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    private var content: MonarchAddressAutocompleteContent {
        MonarchAddressAutocompleteTokens.content(phase: searcher.phase, results: searcher.results)
    }

    private func message(_ content: MonarchAddressAutocompleteContent) -> String {
        MonarchAddressAutocompleteTokens.message(content, minQueryLength: minQueryLength, labels: labels) ?? ""
    }

    private func row(_ suggestion: MonarchAddressAutocompleteSuggestion) -> some View {
        Button {
            onPick(suggestion)
        } label: {
            VStack(alignment: .leading, spacing: MonarchComboboxTokens.textSpacing) {
                Text(suggestion.label)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                if let description = suggestion.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
            .padding(.horizontal, MonarchAddressAutocompleteTokens.panelPadding)
            .padding(.vertical, MonarchAddressAutocompleteTokens.spacing)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchAddressAutocompleteTokens.accessibilityLabel(suggestion))
        .accessibilityAddTraits(.isButton)
    }
}
