import SwiftUI

/// The layout shared by ``MonarchEmptyState`` and ``MonarchErrorState``: the
/// system `ContentUnavailableView` with Monarch's type, colours and sizes.
struct MonarchEmptyStateLayout<Actions: View>: View {
    private let title: String
    private let message: String?
    private let symbol: String
    private let iconRole: MonarchColorRole
    private let tileRole: MonarchColorRole
    private let size: MonarchEmptyStateSize
    private let hasActions: Bool
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric private var iconSize: CGFloat
    @ScaledMetric private var tileSize: CGFloat

    init(
        title: String,
        message: String?,
        symbol: String,
        iconRole: MonarchColorRole,
        tileRole: MonarchColorRole = \.bgMuted,
        size: MonarchEmptyStateSize,
        hasActions: Bool,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.message = message
        self.symbol = symbol
        self.iconRole = iconRole
        self.tileRole = tileRole
        self.size = size
        self.hasActions = hasActions
        self.actions = actions()
        _iconSize = ScaledMetric(wrappedValue: MonarchEmptyStateTokens.iconSize(size), relativeTo: .body)
        _tileSize = ScaledMetric(wrappedValue: MonarchEmptyStateTokens.tileSize(size), relativeTo: .body)
    }

    var body: some View {
        let fillsHeight = MonarchEmptyStateTokens.fillsHeight(size)
        // The web anatomy, drawn here rather than by `ContentUnavailableView`,
        // which scales any icon up to its own large size: a 20 point glyph on
        // a 40 point tile, the title, the message, then the actions.
        VStack(spacing: MonarchEmptyStateTokens.textSpacing) {
            Image(systemName: symbol)
                .font(.system(size: iconSize))
                .foregroundStyle(theme.colors[role: iconRole])
                .frame(width: tileSize, height: tileSize)
                .background(theme.colors[role: tileRole], in: MonarchShapes.container)
                .padding(.bottom, MonarchEmptyStateTokens.tileBottomSpacing)
                .accessibilityHidden(true)
            Text(title)
                .monarchTextStyle(MonarchEmptyStateTokens.titleStyle(size))
                .foregroundStyle(theme.colors[role: MonarchEmptyStateTokens.title])
                .accessibilityAddTraits(.isHeader)
            if let message {
                Text(message)
                    .monarchTextStyle(MonarchEmptyStateTokens.messageStyle(size))
                    .foregroundStyle(theme.colors[role: MonarchEmptyStateTokens.message])
            }
            // Side by side, as on the web; a button that does not fit drops to the next line, so titles stay whole.
            if hasActions {
                MonarchFlowLayout(
                    spacing: MonarchEmptyStateTokens.actionSpacing,
                    alignment: .center,
                    justification: .center
                ) {
                    actions
                }
                .padding(.top, MonarchEmptyStateTokens.actionsTopSpacing)
            }
        }
        .multilineTextAlignment(.center)
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: MonarchEmptyStateTokens.maxWidth)
        .padding(.vertical, MonarchEmptyStateTokens.verticalPadding(size))
        .padding(.horizontal, MonarchEmptyStateTokens.horizontalPadding)
        .frame(maxWidth: .infinity, maxHeight: fillsHeight ? .infinity : nil)
        .accessibilityElement(children: .contain)
    }
}
