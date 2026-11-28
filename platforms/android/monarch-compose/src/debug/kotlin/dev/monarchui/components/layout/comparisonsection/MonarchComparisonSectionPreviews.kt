package dev.monarchui.components.layout.comparisonsection

import androidx.compose.runtime.Composable
import dev.monarchui.components.billing.pricingtable.MonarchPricingFeatureValue
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchComparisonSectionPreview() {
    PreviewFrame {
        MonarchComparisonSection(
            columns = listOf(
                MonarchComparisonColumn("product", "Product A", highlighted = true, badge = "This product"),
                MonarchComparisonColumn("sheets", "Spreadsheets"),
            ),
            rows = listOf(
                MonarchComparisonRow(
                    "history",
                    "Change history",
                    mapOf(
                        "product" to MonarchPricingFeatureValue.Included,
                        "sheets" to MonarchPricingFeatureValue.Text("Per file"),
                    ),
                ),
            ),
            title = "How it compares",
            eyebrow = "Compare",
            bestFor = listOf(MonarchComparisonBestFor("product", "Product A suits", "Teams that share work.", true)),
        )
    }
}
