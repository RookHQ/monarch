import SwiftUI

/// The add link form of ``MonarchLinkedDocsPanel``: a sheet with the web
/// address, an optional title, and Add once the address is valid.
struct MonarchLinkedDocsPanelLinkForm: View {
    let labels: MonarchLinkedDocsPanelLabels
    let onAdd: @MainActor (URL, String) async throws -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss
    @State private var address = ""
    @State private var title = ""
    @State private var hasTriedAdding = false
    @State private var isAdding = false
    @State private var failure: String?

    private typealias Strings = MonarchLinkedDocsPanelStrings

    var body: some View {
        let url = MonarchLinkedDocsPanelTokens.parseLinkURL(address)
        NavigationStack {
            Form {
                Group {
                    Section {
                        addressField
                        if hasTriedAdding, url == nil {
                            Label(labels.urlInvalid ?? Strings.urlInvalid, systemImage: "exclamationmark.circle")
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors.dangerStrong)
                        }
                        TextField(labels.titleLabel ?? Strings.titleLabel, text: $title)
                            .monarchTextStyle(.body)
                    }
                    if let failure {
                        Section {
                            MonarchInlineNotice(failure, tone: .danger, variant: .plain)
                        }
                    }

                }
                .monarchRowsOnSurface()
            }
            .monarchCanvas()
            .navigationTitle(labels.addLink ?? Strings.addLink)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(labels.cancel ?? Strings.cancel) { dismiss() }
                        .disabled(isAdding)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isAdding {
                        ProgressView()
                    } else {
                        Button(labels.confirmAdd ?? Strings.confirmAdd) { add(url) }
                    }
                }
            }
        }
        .interactiveDismissDisabled(isAdding)
    }

    @ViewBuilder private var addressField: some View {
        let field = TextField(labels.urlLabel ?? Strings.urlLabel, text: $address)
            .monarchTextStyle(.body)
            .autocorrectionDisabled()
            .onSubmit { add(MonarchLinkedDocsPanelTokens.parseLinkURL(address)) }
        #if os(iOS)
            field
                .keyboardType(.URL)
                .textContentType(.URL)
                .textInputAutocapitalization(.never)
        #else
            field
        #endif
    }

    private func add(_ url: URL?) {
        hasTriedAdding = true
        guard let url, !isAdding else {
            if url == nil {
                AccessibilityNotification.Announcement(labels.urlInvalid ?? Strings.urlInvalid).post()
            }
            return
        }
        failure = nil
        isAdding = true
        let linkTitle = MonarchLinkedDocsPanelTokens.linkTitle(title, url: url)
        Task { @MainActor in
            do {
                try await onAdd(url, linkTitle)
                isAdding = false
                dismiss()
            } catch {
                isAdding = false
                let message = labels.linkFailed ?? Strings.linkFailed
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }
}
