package dev.monarchui.components.files.blockeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.collaboration.richtexteditor.MonarchRichTextEditorTokens
import dev.monarchui.components.files.blockeditor.BlockEditorModel as Model
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorBlock as Block
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorTextStyle as Style
import dev.monarchui.components.files.calloutblock.MonarchCalloutBlock
import dev.monarchui.components.files.embedblock.MonarchEmbedBlock
import dev.monarchui.components.files.tableblock.MonarchTableBlock
import dev.monarchui.components.files.toggleblock.MonarchToggleBlock
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.navigation.link.MonarchLinkDefaults
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/** The editable form of a block. */
@Suppress("LongParameterList")
@Composable
internal fun BlockEditorEditContent(
    block: Block,
    index: Int,
    document: List<Block>,
    state: MonarchBlockEditorState,
    actions: BlockEditorActions,
    config: BlockEditorConfig,
) {
    val name = blockEditorName(block, index, document, config.texts)
    val empty = Model.textOf(block)?.isEmpty() == true
    val showPlaceholder = empty && (document.size == 1 || (state.hasFocus && state.focusedBlockId == block.id))
    when (block) {
        is Block.Text -> BlockEditorTextBlock(block, index, document, name, showPlaceholder, state, actions, config)
        is Block.Callout -> MonarchCalloutBlock(tone = block.tone) {
            Column {
                if (config.editable) BlockEditorToneRow(block, actions, config)
                BlockEditorTextField(
                    block = block,
                    name = name,
                    textStyle = MonarchTheme.typography.body.copy(color = MonarchTheme.colors.textDefault),
                    state = state,
                    actions = actions,
                    config = config,
                    placeholder = name.takeIf { showPlaceholder },
                )
            }
        }
        is Block.Toggle -> BlockEditorToggle(block, name, showPlaceholder, state, actions, config)
        is Block.Table -> MonarchTableBlock(
            data = block.data,
            onDataChange = { data -> actions.update(block.copy(data = data)) },
            label = name,
            enabled = config.editable,
            labels = config.labels.table,
            onDelete = { actions.delete(block.id) },
        )
        is Block.Embed -> BlockEditorEmbed(block, state, actions, config)
    }
}

@Suppress("LongParameterList")
@Composable
private fun BlockEditorTextBlock(
    block: Block.Text,
    index: Int,
    document: List<Block>,
    name: String,
    showPlaceholder: Boolean,
    state: MonarchBlockEditorState,
    actions: BlockEditorActions,
    config: BlockEditorConfig,
) {
    val colors = MonarchTheme.colors
    val tokens = MonarchBlockEditorTokens
    val base = tokens.textStyle(block.style, MonarchTheme.typography)
    val color = when {
        block.style == Style.Quote -> tokens.quote(colors, config.enabled)
        block.style == Style.Checklist && block.checked -> tokens.marker(colors, config.enabled)
        else -> tokens.text(colors, config.enabled)
    }
    val style = base.copy(
        color = color,
        textDecoration = if (block.style == Style.Checklist && block.checked) TextDecoration.LineThrough else null,
    )
    val placeholder = when {
        !showPlaceholder -> null
        block.style == Style.Paragraph -> config.texts.placeholder
        else -> name
    }
    BlockEditorTextFrame(block, index, document, config, actions) {
        BlockEditorTextField(
            block = block,
            name = name,
            textStyle = style,
            state = state,
            actions = actions,
            config = config,
            placeholder = placeholder,
            heading = block.style.isHeading,
        )
    }
}

/** The marker, bar or fill around the text of a text block. */
@Composable
internal fun BlockEditorTextFrame(
    block: Block.Text,
    index: Int,
    document: List<Block>,
    config: BlockEditorConfig,
    actions: BlockEditorActions? = null,
    text: @Composable () -> Unit,
) {
    val colors = MonarchTheme.colors
    val tokens = MonarchBlockEditorTokens
    when (block.style) {
        Style.BulletedList, Style.NumberedList -> Row(horizontalArrangement = Arrangement.spacedBy(tokens.PartGap)) {
            val marker = if (block.style == Style.BulletedList) BULLET else "${Model.number(document, index)}."
            Box(
                modifier = Modifier
                    .heightIn(min = tokens.MinBlockHeight)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = marker, style = MonarchTheme.typography.body, color = tokens.marker(colors, config.enabled))
            }
            Box(Modifier.weight(1f)) { text() }
        }
        Style.Checklist -> Row(verticalAlignment = Alignment.Top) {
            MonarchCheckbox(
                checked = block.checked,
                onCheckedChange = if (config.editable && actions != null) {
                    { checked -> actions.setChecked(block.id, checked) }
                } else {
                    null
                },
                modifier = Modifier.semantics {
                    contentDescription = block.text.ifBlank { config.texts.name(block.type) }
                    if (config.readOnly) toggleableState = ToggleableState(block.checked)
                },
                enabled = config.enabled,
            )
            Box(Modifier.weight(1f)) { text() }
        }
        Style.Quote -> Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(tokens.PartGap),
        ) {
            Box(
                Modifier
                    .width(tokens.QuoteBar)
                    .fillMaxHeight()
                    .background(tokens.quoteBar(colors), MonarchTheme.shapes.sm),
            )
            Box(Modifier.weight(1f)) { text() }
        }
        Style.Code -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.codeFill(colors), MonarchTheme.shapes.md)
                .border(MonarchShapeDefaults.BorderWidth, tokens.inset(colors), MonarchTheme.shapes.md)
                .padding(horizontal = tokens.InsetPadding),
        ) { text() }
        else -> text()
    }
}

private const val BULLET = "•"

/** The tone of a callout as icon buttons, the current one pressed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockEditorToneRow(block: Block.Callout, actions: BlockEditorActions, config: BlockEditorConfig) {
    // On a narrow callout the buttons wrap to a second row, so every one stays in view at 48dp.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        MonarchBlockEditorTokens.CalloutTones.forEach { tone ->
            MonarchIconButton(
                label = config.texts.tones.getValue(tone),
                checked = tone == block.tone,
                onCheckedChange = { actions.setTone(block.id, tone) },
                modifier = Modifier.size(MonarchTouchTarget.Minimum),
                enabled = config.enabled,
                size = MonarchButtonSize.Sm,
            ) { MonarchBlockEditorTokens.toneIcon(tone)() }
        }
    }
}

@Composable
private fun BlockEditorToggle(
    block: Block.Toggle,
    name: String,
    showPlaceholder: Boolean,
    state: MonarchBlockEditorState,
    actions: BlockEditorActions,
    config: BlockEditorConfig,
) {
    val texts = config.texts
    val colors = MonarchTheme.colors
    val tokens = MonarchBlockEditorTokens
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonarchIconButton(
                label = if (block.expanded) texts.toggleHide else texts.toggleShow,
                onClick = { actions.setExpanded(block.id, !block.expanded) },
                enabled = config.editable,
                size = MonarchButtonSize.Sm,
            ) { if (block.expanded) MonarchDefaultIcons.ChevronDown() else MonarchDefaultIcons.ChevronRight() }
            BlockEditorTextField(
                block = block,
                name = name,
                textStyle = MonarchTheme.typography.bodyStrong.copy(color = tokens.text(colors, config.enabled)),
                state = state,
                actions = actions,
                config = config,
                modifier = Modifier.weight(1f),
                placeholder = name.takeIf { showPlaceholder },
            )
        }
        if (block.expanded) {
            BasicTextField(
                value = block.body,
                onValueChange = { actions.setBody(block.id, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.MinBlockHeight)
                    .border(MonarchShapeDefaults.BorderWidth, tokens.inset(colors), MonarchTheme.shapes.md)
                    .padding(horizontal = tokens.InsetPadding)
                    .semantics { contentDescription = texts.toggleContent },
                enabled = config.enabled,
                readOnly = !config.editable,
                textStyle = MonarchTheme.typography.body.copy(color = tokens.text(colors, config.enabled)),
                cursorBrush = SolidColor(tokens.cursor(colors)),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = tokens.MinBlockHeight),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (block.body.isEmpty()) {
                            Text(
                                text = texts.toggleContent,
                                style = MonarchTheme.typography.body,
                                color = tokens.placeholder(colors),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun BlockEditorEmbed(
    block: Block.Embed,
    state: MonarchBlockEditorState,
    actions: BlockEditorActions,
    config: BlockEditorConfig,
) {
    if (block.url.isNotBlank()) {
        MonarchEmbedBlock(
            url = block.url,
            caption = block.caption,
            providers = config.providers,
            onOpenLink = config.onOpenLink,
            labels = config.labels.embed,
        )
        return
    }
    var draft by rememberSaveable(block.id) { mutableStateOf("") }
    val requester = remember { FocusRequester() }
    val request = state.focusRequest
    LaunchedEffect(request) {
        if (request != null && request.id == block.id) {
            requester.requestFocus()
            state.focusRequest = null
        }
    }
    val commit = { if (draft.isNotBlank()) actions.update(block.copy(url = draft.trim())) }
    Column(verticalArrangement = Arrangement.spacedBy(MonarchBlockEditorTokens.PartGap)) {
        MonarchTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth().focusRequester(requester),
            label = config.texts.embedLink,
            enabled = config.editable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
        )
        MonarchButton(
            text = config.texts.embedAdd,
            onClick = commit,
            enabled = config.editable && draft.isNotBlank(),
        )
    }
}

/** The read form of a block: text, a callout, a toggle that opens and closes, a table or an embed. */
@Composable
internal fun BlockEditorReadContent(block: Block, index: Int, document: List<Block>, config: BlockEditorConfig) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val links = MonarchLinkDefaults.styles()
    val annotated = Model.textOf(block)?.let { text ->
        MonarchRichTextEditorTokens.annotated(text, Model.spansOf(block), typography, colors, links)
    }
    when (block) {
        is Block.Text -> {
            val base = MonarchBlockEditorTokens.textStyle(block.style, typography)
            val quote = block.style == Style.Quote
            val color = if (quote) MonarchBlockEditorTokens.quote(colors, true) else colors.textDefault
            BlockEditorTextFrame(block, index, document, config) {
                Box(
                    modifier = Modifier.heightIn(min = MonarchBlockEditorTokens.MinBlockHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = annotated ?: return@Box,
                        modifier = Modifier.semantics { if (block.style.isHeading) heading() },
                        style = base.copy(color = color),
                    )
                }
            }
        }
        is Block.Callout -> MonarchCalloutBlock(tone = block.tone) { Text(annotated ?: return@MonarchCalloutBlock) }
        is Block.Toggle -> MonarchToggleBlock(summary = block.text, initiallyExpanded = block.expanded) {
            Text(text = block.body, style = typography.body, color = colors.textDefault)
        }
        is Block.Table -> MonarchTableBlock(
            data = block.data,
            label = config.texts.name(block.type),
            labels = config.labels.table,
        )
        is Block.Embed -> if (block.url.isNotBlank()) {
            MonarchEmbedBlock(
                url = block.url,
                caption = block.caption,
                providers = config.providers,
                onOpenLink = config.onOpenLink,
                labels = config.labels.embed,
            )
        }
    }
}
