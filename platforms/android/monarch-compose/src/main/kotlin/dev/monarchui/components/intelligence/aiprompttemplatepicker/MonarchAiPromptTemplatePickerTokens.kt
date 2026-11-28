package dev.monarchui.components.intelligence.aiprompttemplatepicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSearchText
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiPromptTemplatePicker. */
internal object MonarchAiPromptTemplatePickerTokens {
    val Padding: Dp = MonarchTokens.Space.space4.dp
    val SectionGap: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val BlockPadding: Dp = MonarchTokens.Space.space3.dp
    val RowGap: Dp = MonarchTokens.Space.space0_5.dp

    /** A `{{name}}` blank; the name is the first group. */
    val Blank: Regex = Regex("\\{\\{\\s*([\\w.\\-]+)\\s*\\}\\}")
    private val Separators = setOf('_', '.', '-')

    /** A label from a blank's name: `customer_name` and `customerName` become "Customer name". */
    fun humanize(name: String): String {
        val words = StringBuilder()
        var previous: Char? = null
        name.forEach { char ->
            if (char in Separators) {
                words.append(' ')
            } else {
                if (char.isUpperCase() && previous?.isLowerCase() == true) words.append(' ')
                words.append(char)
            }
            previous = char
        }
        val text = words.split(' ').filter { it.isNotEmpty() }.joinToString(" ").lowercase()
        return text.replaceFirstChar { it.uppercaseChar() }.ifEmpty { name }
    }

    /** Whether a template matches the search in its title, description, body or keywords. */
    fun matches(template: MonarchAiPromptTemplate, query: String): Boolean {
        val needle = MonarchSearchText.normalize(query.trim())
        if (needle.isEmpty()) return true
        val fields = listOf(template.title, template.description.orEmpty(), template.body) + template.keywords
        return fields.any { MonarchSearchText.normalize(it).contains(needle) }
    }

    /** The templates in [category] (all when `null`) that match [query]. */
    fun filtered(
        templates: List<MonarchAiPromptTemplate>,
        query: String,
        category: String?,
    ): List<MonarchAiPromptTemplate> =
        templates.filter { (category == null || it.category == category) && matches(it, query) }

    /** The values the fields start with: each variable's default, or empty. */
    fun initialValues(variables: List<MonarchAiPromptTemplateVariable>): Map<String, String> =
        variables.associate { it.name to it.defaultValue.orEmpty() }

    /** The names of required variables whose value is empty or white space, in form order. */
    fun missing(variables: List<MonarchAiPromptTemplateVariable>, values: Map<String, String>): List<String> =
        variables.filter { it.required && values[it.name].isNullOrBlank() }.map { it.name }

    /** The trimmed value of every variable, as handed to the app. */
    fun submitted(variables: List<MonarchAiPromptTemplateVariable>, values: Map<String, String>): Map<String, String> =
        variables.associate { it.name to values[it.name].orEmpty().trim() }

    /** The body with each blank's trimmed value, or `[Label]` while it is empty. */
    fun preview(body: String, variables: List<MonarchAiPromptTemplateVariable>, values: Map<String, String>): String {
        val labels = variables.associate { it.name to it.label }
        return Blank.replace(body) { match ->
            val name = match.groupValues[1]
            values[name].orEmpty().trim().ifEmpty { "[" + (labels[name] ?: name) + "]" }
        }
    }

    /** The field label, with ", Optional" for a field that may stay empty. */
    fun fieldLabel(variable: MonarchAiPromptTemplateVariable, optional: String): String =
        if (variable.required) variable.label else "${variable.label}, $optional"

    /** The fill and border of the template and preview blocks. */
    fun blockColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.bgSubtle, content = colors.textDefault, border = colors.borderSubtle)

    /** The colour of the section headings. */
    fun headingColor(colors: MonarchColors): Color = colors.textMuted
}
