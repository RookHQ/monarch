package dev.monarchui.components.forms.entitypicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.internal.MonarchSearchText
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Where a search of the entity picker stands. */
internal enum class EntityPickerPhase { Ready, TooShort, Searching, Failed }

/** One row of the entity picker's list. */
internal sealed interface EntityPickerRow {
    /** The row that clears the selection, such as "Unassigned". */
    data class Clear(val label: String) : EntityPickerRow

    /** The small heading above a run of entities of one group. */
    data class Group(val label: String) : EntityPickerRow

    /** The action row at the end of the list, such as "Manage projects". */
    data class Footer(val label: String) : EntityPickerRow

    /** An entity. */
    data class Entity(val entity: MonarchEntityPickerEntity) : EntityPickerRow

    /** The row that creates an entity from the typed text. */
    data class Create(val text: String, val label: String) : EntityPickerRow

    /** A message: loading, too short or no matches. */
    data class Message(val text: String) : EntityPickerRow

    /** A failed search, with Try again. */
    data class Failed(val text: String) : EntityPickerRow
}

/** The resolved messages of the entity picker's list. */
internal class EntityPickerMessages(
    val loading: String,
    val empty: String,
    val tooShort: String,
    val failed: String,
    val create: (String) -> String,
)

/** Everything the app passed to an entity picker, single or multiple. */
@Suppress("LongParameterList")
internal class EntityPickerConfig(
    val label: String,
    val entities: List<MonarchEntityPickerEntity>,
    val selected: List<String>,
    val multiple: Boolean,
    val selectedEntities: List<MonarchEntityPickerEntity>,
    val enabled: Boolean,
    val required: Boolean,
    val loading: Boolean,
    val placeholder: String?,
    val helperText: String?,
    val errorText: String?,
    val clearOptionLabel: String?,
    val minSearchLength: Int,
    val debounceMillis: Long,
    val search: (suspend (String) -> List<MonarchEntityPickerEntity>)?,
    val onCreate: (suspend (String) -> MonarchEntityPickerEntity?)?,
    val clearIcon: (@Composable () -> Unit)?,
    val labels: MonarchEntityPickerLabels,
    val initiallyOpen: Boolean = false,
    val footerLabel: String? = null,
    val onFooter: (() -> Unit)? = null,
)

/** How the choice of an entity picker changes and reads. */
internal object EntityPickerSelection {
    /** The selection after tapping [id] in a multiple-choice picker: removed when chosen, else appended. */
    fun toggle(selected: List<String>, id: String): List<String> =
        if (id in selected) selected - id else selected + id

    /** The field's text for the chosen names: the name, "First +N" for several, or `null` for none. */
    fun summary(names: List<String>, format: (String, Int) -> String): String? = when (names.size) {
        0 -> null
        1 -> names.first()
        else -> format(names.first(), names.size - 1)
    }
}

/** Component tokens and the pure rules of MonarchEntityPicker. */
internal object MonarchEntityPickerTokens {
    /** The least height of the field and of a row: the touch target. */
    val MinHeight: Dp = MonarchTouchTarget.Minimum

    /** The padding inside the field and of the sheet's content. */
    val FieldPadding: Dp = MonarchTokens.Space.space3.dp
    val SheetPadding: Dp = MonarchTokens.Space.space4.dp

    /** The gap between an avatar, the texts and a mark. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of the sheet. */
    val SheetGap: Dp = MonarchTokens.Space.space2.dp

    /** The vertical padding of a row and of a message. */
    val RowVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The default filter: the name, hint or keywords contain the query, ignoring case and accents. */
    fun matches(entity: MonarchEntityPickerEntity, query: String): Boolean {
        val needle = MonarchSearchText.normalize(query.trim())
        return listOfNotNull(entity.name, entity.hint, entity.keywords)
            .any { MonarchSearchText.normalize(it).contains(needle) }
    }

    /** The phase before a search runs: too short when the trimmed text is under the minimum, else ready. */
    fun phase(query: String, minSearchLength: Int): EntityPickerPhase {
        val trimmed = query.trim()
        return if (trimmed.isNotEmpty() && trimmed.length < minSearchLength) {
            EntityPickerPhase.TooShort
        } else {
            EntityPickerPhase.Ready
        }
    }

    /**
     * The rows of the list: the clear row while nothing is typed, then a message for a short, running or failed
     * search, or the [shown] entities, the Create row when nothing shown has the typed name, and Loading or No
     * matches when nothing is shown.
     */
    @Suppress("LongParameterList")
    fun rows(
        shown: List<MonarchEntityPickerEntity>,
        query: String,
        phase: EntityPickerPhase,
        loading: Boolean,
        clearOptionLabel: String?,
        canCreate: Boolean,
        messages: EntityPickerMessages,
        footerLabel: String? = null,
    ): List<EntityPickerRow> {
        val trimmed = query.trim()
        val rows = mutableListOf<EntityPickerRow>()
        if (trimmed.isEmpty() && clearOptionLabel != null) rows += EntityPickerRow.Clear(clearOptionLabel)
        when (phase) {
            EntityPickerPhase.TooShort -> rows += EntityPickerRow.Message(messages.tooShort)
            EntityPickerPhase.Searching -> rows += EntityPickerRow.Message(messages.loading)
            EntityPickerPhase.Failed -> rows += EntityPickerRow.Failed(messages.failed)
            EntityPickerPhase.Ready -> rows += readyRows(shown, trimmed, loading, canCreate, messages)
        }
        if (footerLabel != null) rows += EntityPickerRow.Footer(footerLabel)
        return rows
    }

    /** The entities, the Create row when nothing shown has the typed name, then Loading or No matches. */
    private fun readyRows(
        shown: List<MonarchEntityPickerEntity>,
        trimmed: String,
        loading: Boolean,
        canCreate: Boolean,
        messages: EntityPickerMessages,
    ): List<EntityPickerRow> {
        val rows = mutableListOf<EntityPickerRow>()
        var group: String? = null
        shown.forEach {
            if (it.group != null && it.group != group) rows += EntityPickerRow.Group(it.group)
            group = it.group
            rows += EntityPickerRow.Entity(it)
        }
        val exact = shown.any { MonarchSearchText.normalize(it.name) == MonarchSearchText.normalize(trimmed) }
        val create = canCreate && trimmed.isNotEmpty() && !exact
        if (create) rows += EntityPickerRow.Create(trimmed, messages.create(trimmed))
        when {
            shown.isEmpty() && loading -> rows += EntityPickerRow.Message(messages.loading)
            shown.isEmpty() && !create -> rows += EntityPickerRow.Message(messages.empty)
        }
        return rows
    }

    /** The fill and border of the field: the danger border with an error, disabled colours when disabled. */
    fun fieldColors(error: Boolean, enabled: Boolean, colors: MonarchColors): MonarchSurfaceColors = when {
        !enabled -> MonarchSurfaceColors(colors.bgMuted, colors.textDisabled, colors.borderSubtle)
        error -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.dangerBorder)
        else -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderInput)
    }

    /** The colour of the field's text: the chosen names, or the subtle placeholder. */
    fun valueColor(chosen: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        chosen -> colors.textDefault
        else -> colors.textSubtle
    }

    /** The colour of a row's name. */
    fun labelColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of a description, a hint and a message. */
    fun secondaryColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The background of the sheet. */
    fun sheetColor(colors: MonarchColors): Color = colors.bgSurface
}
