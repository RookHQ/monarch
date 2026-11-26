package com.rookhq.monarch.components.files.blockeditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuItem
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.components.files.blockeditor.MonarchBlockEditorBlockType as Type

/** The strings of a MonarchBlockEditor with the caller's replacements applied. */
@Suppress("LongParameterList")
internal class BlockEditorTexts(
    val placeholder: String,
    val names: Map<Type, String>,
    val descriptions: Map<Type, String>,
    val groupText: String,
    val groupLists: String,
    val groupBlocks: String,
    val handle: (String) -> String,
    val moveUp: String,
    val moveDown: String,
    val duplicate: String,
    val delete: String,
    val turnInto: String,
    val tone: String,
    val tones: Map<MonarchTone, String>,
    val moved: (String, Int, Int) -> String,
    val toggleShow: String,
    val toggleHide: String,
    val toggleContent: String,
    val embedLink: String,
    val embedAdd: String,
) {
    /** The name of a kind of block. */
    fun name(type: Type): String = names.getValue(type)
}

/** The entries of the / menu: the built-in blocks that are offered, in the web's order, then the app's. */
internal object BlockEditorSlash {
    /** The built-in entries with the web's ids and keywords. */
    private val Entries: List<Triple<String, Type, List<String>>> = listOf(
        Triple("text", Type.Paragraph, listOf("paragraph", "p")),
        Triple("heading-1", Type.Heading1, listOf("h1", "title")),
        Triple("heading-2", Type.Heading2, listOf("h2", "title")),
        Triple("heading-3", Type.Heading3, listOf("h3", "title")),
        Triple("quote", Type.Quote, listOf("blockquote")),
        Triple("code", Type.Code, listOf("pre")),
        Triple("bullet-list", Type.BulletedList, listOf("ul")),
        Triple("ordered-list", Type.NumberedList, listOf("ol")),
        Triple("task-list", Type.Checklist, listOf("todo", "checkbox")),
        Triple("callout", Type.Callout, listOf("note", "tip", "warning")),
        Triple("toggle", Type.Toggle, listOf("details", "collapse")),
        Triple("table", Type.Table, listOf("grid")),
        Triple("embed", Type.Embed, listOf("video", "link")),
    )

    /** The block type of a built-in entry, or `null` for an app's entry. */
    fun typeOf(id: String): Type? = Entries.firstOrNull { it.first == id }?.second

    /** The entries for the [offered] types, then [extra]. */
    fun items(
        texts: BlockEditorTexts,
        offered: Set<Type>,
        extra: List<MonarchSlashMenuItem>,
    ): List<MonarchSlashMenuItem> =
        Entries.filter { it.second in offered }.map { (id, type, keywords) ->
            MonarchSlashMenuItem(
                id = id,
                label = texts.name(type),
                description = texts.descriptions[type],
                group = when (type) {
                    Type.BulletedList, Type.NumberedList, Type.Checklist -> texts.groupLists
                    Type.Callout, Type.Toggle, Type.Table, Type.Embed -> texts.groupBlocks
                    else -> texts.groupText
                },
                keywords = keywords,
            )
        } + extra
}

private const val Heading1Level = 1
private const val Heading2Level = 2
private const val Heading3Level = 3

@Composable
internal fun blockEditorTexts(labels: MonarchBlockEditorLabels): BlockEditorTexts {
    val builtInNames = mapOf(
        Type.Paragraph to stringResource(R.string.monarch_blockeditor_paragraph),
        Type.Heading1 to stringResource(R.string.monarch_blockeditor_heading, Heading1Level),
        Type.Heading2 to stringResource(R.string.monarch_blockeditor_heading, Heading2Level),
        Type.Heading3 to stringResource(R.string.monarch_blockeditor_heading, Heading3Level),
        Type.BulletedList to stringResource(R.string.monarch_blockeditor_bulleted_list),
        Type.NumberedList to stringResource(R.string.monarch_blockeditor_numbered_list),
        Type.Checklist to stringResource(R.string.monarch_blockeditor_checklist),
        Type.Quote to stringResource(R.string.monarch_blockeditor_quote),
        Type.Code to stringResource(R.string.monarch_blockeditor_code),
        Type.Callout to stringResource(R.string.monarch_blockeditor_callout),
        Type.Toggle to stringResource(R.string.monarch_blockeditor_toggle),
        Type.Table to stringResource(R.string.monarch_blockeditor_table),
        Type.Embed to stringResource(R.string.monarch_blockeditor_embed),
    )
    val builtInDescriptions = mapOf(
        Type.Paragraph to stringResource(R.string.monarch_blockeditor_paragraph_description),
        Type.Heading1 to stringResource(R.string.monarch_blockeditor_heading1_description),
        Type.Heading2 to stringResource(R.string.monarch_blockeditor_heading2_description),
        Type.Heading3 to stringResource(R.string.monarch_blockeditor_heading3_description),
        Type.BulletedList to stringResource(R.string.monarch_blockeditor_bulleted_list_description),
        Type.NumberedList to stringResource(R.string.monarch_blockeditor_numbered_list_description),
        Type.Checklist to stringResource(R.string.monarch_blockeditor_checklist_description),
        Type.Quote to stringResource(R.string.monarch_blockeditor_quote_description),
        Type.Code to stringResource(R.string.monarch_blockeditor_code_description),
        Type.Callout to stringResource(R.string.monarch_blockeditor_callout_description),
        Type.Toggle to stringResource(R.string.monarch_blockeditor_toggle_description),
        Type.Table to stringResource(R.string.monarch_blockeditor_table_description),
        Type.Embed to stringResource(R.string.monarch_blockeditor_embed_description),
    )
    val handle = stringResource(R.string.monarch_blockeditor_handle)
    val moved = stringResource(R.string.monarch_blockeditor_moved)
    return BlockEditorTexts(
        placeholder = monarchString(labels.placeholder, R.string.monarch_blockeditor_placeholder),
        names = builtInNames + labels.names,
        descriptions = builtInDescriptions + labels.descriptions,
        groupText = monarchString(labels.groupText, R.string.monarch_blockeditor_group_text),
        groupLists = monarchString(labels.groupLists, R.string.monarch_blockeditor_group_lists),
        groupBlocks = monarchString(labels.groupBlocks, R.string.monarch_blockeditor_group_blocks),
        handle = labels.handle ?: { handle.format(it) },
        moveUp = monarchString(labels.moveUp, R.string.monarch_blockeditor_move_up),
        moveDown = monarchString(labels.moveDown, R.string.monarch_blockeditor_move_down),
        duplicate = monarchString(labels.duplicate, R.string.monarch_blockeditor_duplicate),
        delete = monarchString(labels.delete, R.string.monarch_blockeditor_delete),
        turnInto = monarchString(labels.turnInto, R.string.monarch_blockeditor_turn_into),
        tone = monarchString(labels.tone, R.string.monarch_blockeditor_tone),
        tones = mapOf(
            MonarchTone.Neutral to monarchString(labels.neutralTone, R.string.monarch_blockeditor_tone_neutral),
            MonarchTone.Info to stringResource(R.string.monarch_internal_tone_info),
            MonarchTone.Success to stringResource(R.string.monarch_internal_tone_success),
            MonarchTone.Warning to stringResource(R.string.monarch_internal_tone_warning),
            MonarchTone.Danger to stringResource(R.string.monarch_internal_tone_danger),
        ),
        moved = labels.moved ?: { block, position, total -> moved.format(block, position, total) },
        toggleShow = monarchString(labels.toggleShow, R.string.monarch_blockeditor_toggle_show),
        toggleHide = monarchString(labels.toggleHide, R.string.monarch_blockeditor_toggle_hide),
        toggleContent = monarchString(labels.toggleContent, R.string.monarch_blockeditor_toggle_content),
        embedLink = monarchString(labels.embedLink, R.string.monarch_blockeditor_embed_link),
        embedAdd = monarchString(labels.embedAdd, R.string.monarch_blockeditor_embed_add),
    )
}
