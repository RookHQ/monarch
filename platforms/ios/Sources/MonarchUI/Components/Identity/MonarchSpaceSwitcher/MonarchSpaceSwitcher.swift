import MonarchTokens
import SwiftUI

/// Chooses which Space of the workspace the app shows: "All spaces" or one
/// Space. A button with the current Space opens a sheet with a searchable
/// list, the current choice checked, and New Space and Manage Spaces.
///
/// ```swift
/// MonarchSpaceSwitcher(
///     selection: $spaceID,
///     spaces: spaces,
///     manageURL: URL(string: "https://id.example.com/spaces"),
///     onCreate: { showNewSpace = true }
/// )
/// ```
///
/// `selection` is a Space id, or `nil` for "All spaces". The app supplies
/// the Spaces and stores the choice; the switcher never fetches. A selected
/// id that is no longer in `spaces` reads as "All spaces". With no Spaces
/// and nothing loading the switcher shows nothing, unless `hidesWhenEmpty`
/// is `false`. Choosing closes the sheet. New Space closes the sheet, then
/// runs `onCreate`; Manage Spaces opens `manageURL` with the environment's
/// `openURL`.
///
/// Accessibility: the button is named "Space" and its value is the current
/// Space, also when only the icon shows. Each row is a button named by the
/// Space and its description; the current one has the selected trait and a
/// check mark. The search field is the system's.
public struct MonarchSpaceSwitcher: View {
    @Binding private var selection: String?
    private let spaces: [MonarchSpaceOption]
    private let size: MonarchButtonSize
    private let allowsAll: Bool
    private let isLoading: Bool
    private let isIconOnly: Bool
    private let hidesWhenEmpty: Bool
    private let manageURL: URL?
    private let labels: MonarchSpaceSwitcherLabels
    private let onCreate: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var isPresented = false
    @Environment(\.monarchStartsPresented) private var startsPresented

    private typealias Tokens = MonarchSpaceSwitcherTokens

    /// A Space switcher.
    ///
    /// - Parameters:
    ///   - selection: The chosen Space id, `nil` for "All spaces".
    ///   - spaces: The Spaces this person can open, in display order.
    ///   - size: The height and padding of the button.
    ///   - allowsAll: Offers "All spaces" first. Without it a Space must stay chosen.
    ///   - isLoading: The Spaces are still loading; the list says so.
    ///   - isIconOnly: Shows only the icon and the chevron, for a narrow bar. The name is still spoken.
    ///   - hidesWhenEmpty: Shows nothing when there are no Spaces and nothing is loading.
    ///   - manageURL: Adds "Manage Spaces", which opens this URL.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCreate: Adds "New Space", which closes the sheet and runs this.
    public init(
        selection: Binding<String?>,
        spaces: [MonarchSpaceOption],
        size: MonarchButtonSize = .md,
        allowsAll: Bool = true,
        isLoading: Bool = false,
        isIconOnly: Bool = false,
        hidesWhenEmpty: Bool = true,
        manageURL: URL? = nil,
        labels: MonarchSpaceSwitcherLabels = MonarchSpaceSwitcherLabels(),
        onCreate: (@MainActor () -> Void)? = nil
    ) {
        _selection = selection
        self.spaces = spaces
        self.size = size
        self.allowsAll = allowsAll
        self.isLoading = isLoading
        self.isIconOnly = isIconOnly
        self.hidesWhenEmpty = hidesWhenEmpty
        self.manageURL = manageURL
        self.labels = labels
        self.onCreate = onCreate
    }

    /// The content of the view.
    public var body: some View {
        if Tokens.isVisible(count: spaces.count, isLoading: isLoading, hidesWhenEmpty: hidesWhenEmpty) {
            trigger
        }
    }

    private var trigger: some View {
        let current = Tokens.current(selection, in: spaces)
        let name = Tokens.triggerName(current, allowsAll: allowsAll, labels: labels)
        return Button {
            isPresented = true
        } label: {
            HStack(spacing: Tokens.spacing) {
                MonarchIcon(systemName: Tokens.triggerSymbol(current))
                if !isIconOnly {
                    VStack(alignment: .leading, spacing: 0) {
                        Text(labels.label ?? MonarchSpaceSwitcherStrings.label)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                        Text(name)
                            .lineLimit(1)
                    }
                }
                MonarchIcon(systemName: Tokens.chevronSymbol, size: .sm)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .buttonStyle(.monarch(.ghost, size: size))
        .overlay(
            RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                .strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth)
                .allowsHitTesting(false)
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.label ?? MonarchSpaceSwitcherStrings.label)
        .accessibilityValue(name)
        .accessibilityAddTraits(.isButton)
        .onAppear { if startsPresented { isPresented = true } }
        .sheet(isPresented: $isPresented) {
            MonarchSpaceSwitcherList(
                spaces: spaces,
                selection: current?.id,
                allowsAll: allowsAll,
                isLoading: isLoading,
                canCreate: onCreate != nil,
                manageURL: manageURL,
                labels: labels,
                onChoose: choose,
                onCreate: create,
                onCancel: { isPresented = false }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
    }

    private func choose(_ id: String?) {
        guard Tokens.accepts(id, allowsAll: allowsAll) else { return }
        isPresented = false
        selection = id
    }

    private func create() {
        isPresented = false
        onCreate?()
    }
}

#if DEBUG
    #Preview("MonarchSpaceSwitcher") {
        let spaces = [
            MonarchSpaceOption(id: "eng", name: "Engineering", description: "18 members", systemImage: "hammer"),
            MonarchSpaceOption(id: "mkt", name: "Marketing", description: "9 members"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSpaceSwitcher(selection: .constant(nil), spaces: spaces)
                MonarchSpaceSwitcher(selection: .constant("eng"), spaces: spaces, size: .sm)
                MonarchSpaceSwitcher(selection: .constant("mkt"), spaces: spaces, isIconOnly: true)
                MonarchSpaceSwitcherList(
                    spaces: spaces,
                    selection: "eng",
                    allowsAll: true,
                    isLoading: false,
                    canCreate: true,
                    manageURL: URL(string: "https://example.com/spaces"),
                    labels: MonarchSpaceSwitcherLabels(),
                    onChoose: { _ in },
                    onCreate: {},
                    onCancel: {}
                )
            }
        }
    }
#endif
