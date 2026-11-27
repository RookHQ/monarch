import MonarchUI
import SwiftUI

struct MonarchTabularNumberStyleSection: CataloguePage {
    static let title = "Tabular number"

    @State private var isTabular = true
    @State private var startedAt = Date.now

    @Environment(\.monarchTheme) private var theme

    private let amounts: [Double?] = [1_111.11, 9_048.5, nil, 20, 777.7]

    var body: some View {
        CatalogueCaption("A column of amounts, one of them missing")
        VStack(alignment: .trailing, spacing: theme.spacing.space1) {
            ForEach(amounts.indices, id: \.self) { index in
                Text(monarchTabular: amounts[index], format: .number.precision(.fractionLength(2)))
                    .foregroundStyle(amounts[index] == nil ? theme.colors.textSubtle : theme.colors.textDefault)
            }
        }
        .monarchTextStyle(.body)

        CatalogueCaption("Percent and currency")
        Text(monarchTabular: 0.125, format: .percent).monarchTextStyle(.body)
        Text(monarchTabular: Decimal(1_299.5), format: .currency(code: "EUR")).monarchTextStyle(.body)

        CatalogueCaption("A value that counts, with and without the style")
        Toggle("Fixed-width digits", isOn: $isTabular)
        if isTabular {
            Text(startedAt, style: .timer)
                .monarchTextStyle(.title)
                .monarchTabularNumber()
        } else {
            Text(startedAt, style: .timer)
                .monarchTextStyle(.title)
        }
    }
}
