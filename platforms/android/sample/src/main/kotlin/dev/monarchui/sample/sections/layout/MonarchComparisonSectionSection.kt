package dev.monarchui.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.billing.pricingtable.MonarchPricingFeatureValue
import dev.monarchui.components.layout.comparisonsection.MonarchComparisonBestFor
import dev.monarchui.components.layout.comparisonsection.MonarchComparisonColumn
import dev.monarchui.components.layout.comparisonsection.MonarchComparisonRow
import dev.monarchui.components.layout.comparisonsection.MonarchComparisonSection
import dev.monarchui.sample.CataloguePage

/** The catalogue page of ComparisonSection. The action changes its label once tapped. */
object MonarchComparisonSectionSection : CataloguePage {
    override val title = "ComparisonSection"

    private val included = MonarchPricingFeatureValue.Included
    private val notIncluded = MonarchPricingFeatureValue.NotIncluded
    private val perFile = MonarchPricingFeatureValue.Text("Per file")

    @Composable
    override fun Content() {
        var tried by rememberSaveable { mutableStateOf(false) }
        MonarchComparisonSection(
            columns = listOf(
                MonarchComparisonColumn("product", "Product A", highlighted = true, badge = "This product"),
                MonarchComparisonColumn("sheets", "Spreadsheets"),
                MonarchComparisonColumn("email", "Email threads"),
            ),
            rows = listOf(
                MonarchComparisonRow(
                    "history",
                    "Change history",
                    mapOf("product" to included, "sheets" to perFile, "email" to notIncluded),
                ),
                MonarchComparisonRow(
                    "assign",
                    "Assign an owner",
                    mapOf("product" to included, "sheets" to notIncluded, "email" to notIncluded),
                ),
                MonarchComparisonRow(
                    "roles",
                    "Roles and permissions",
                    mapOf("product" to included, "sheets" to perFile, "email" to notIncluded),
                    group = "Access",
                ),
                MonarchComparisonRow(
                    "export",
                    "Export to CSV",
                    mapOf("product" to included, "sheets" to included, "email" to notIncluded),
                    group = "Access",
                ),
            ),
            title = "How it compares",
            eyebrow = "Compare",
            description = "Neutral sample rows for documentation.",
            bestFor = listOf(
                MonarchComparisonBestFor(
                    "product",
                    "Product A suits",
                    "Teams that share and hand over work every day.",
                    true,
                ),
                MonarchComparisonBestFor("sheets", "Spreadsheets suit", "One-off lists kept by one person."),
            ),
            note = "Sample note: say when the comparison was checked and against which public sources, " +
                "and credit trademarks to their owners.",
        ) {
            MonarchButton(
                text = if (tried) "Trying Product A" else "Try Product A",
                onClick = { tried = true },
                variant = MonarchButtonVariant.Primary,
            )
        }
    }
}
