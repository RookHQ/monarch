package dev.monarchui.components.billing.lineitemseditor

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchLineItemsEditor`. Every `null` uses the built-in string
 * named after it in `strings_lineitemseditor.xml`.
 *
 * @property label the heading of the editor: "Line items".
 * @property item the header of a row, from its 1-based number: "Item 2".
 * @property description the label of the description field: "Description".
 * @property quantity the label of the quantity field: "Quantity".
 * @property unitPrice the label of the unit price field, from the currency code: "Unit price (USD)".
 * @property amount the name of a row's amount when read: "Amount".
 * @property descriptionPlaceholder the placeholder of an empty description: "Item or service".
 * @property addItem the button that adds a row: "Add item".
 * @property remove the visible text of a row's remove button: "Remove".
 * @property removeItem the spoken name of a row's remove button, from its number and description: "Remove item 2:
 * Design hours" ("Remove item 2" without a description).
 * @property empty shown when there are no rows: "No items yet. Add one to start.".
 * @property added announced after a row is added, from its number: "Added item 3".
 * @property removed announced after a row is removed, from its number: "Removed item 2".
 * @property subtotal the subtotal row: "Subtotal".
 * @property discount the discount row and the label of its field: "Discount".
 * @property discountType the spoken name of the percent or amount choice: "Discount type".
 * @property percentOption the percent choice of the discount type: "%".
 * @property tax the tax row, from the written rate or an empty string: "Tax (18%)", "Tax".
 * @property taxRate the label of the tax rate field or menu: "Tax rate".
 * @property total the total row: "Total".
 * @property required said after the heading of a required editor: "Required".
 * @property quantityTimesPrice a read-only row's figures, from the written quantity and unit price: "12 × $95.00".
 */
@Immutable
public class MonarchLineItemsEditorLabels(
    public val label: String? = null,
    public val item: ((number: Int) -> String)? = null,
    public val description: String? = null,
    public val quantity: String? = null,
    public val unitPrice: ((currencyCode: String) -> String)? = null,
    public val amount: String? = null,
    public val descriptionPlaceholder: String? = null,
    public val addItem: String? = null,
    public val remove: String? = null,
    public val removeItem: ((number: Int, description: String) -> String)? = null,
    public val empty: String? = null,
    public val added: ((number: Int) -> String)? = null,
    public val removed: ((number: Int) -> String)? = null,
    public val subtotal: String? = null,
    public val discount: String? = null,
    public val discountType: String? = null,
    public val percentOption: String? = null,
    public val tax: ((rate: String) -> String)? = null,
    public val taxRate: String? = null,
    public val total: String? = null,
    public val required: String? = null,
    public val quantityTimesPrice: ((quantity: String, unitPrice: String) -> String)? = null,
)
