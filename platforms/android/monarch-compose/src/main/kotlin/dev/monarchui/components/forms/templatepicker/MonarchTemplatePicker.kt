package dev.monarchui.components.forms.templatepicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchDefaultIcons
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateKind
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.forms.radio.MonarchRadio
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardGroupTokens
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * A gallery of templates to start from, with category filters, search and an
 * optional "start from scratch" card first.
 *
 * Built by Monarch on a `LazyVerticalGrid` of selectable Material 3 `Surface`
 * cards with a `RadioButton` mark, inside a `selectableGroup`, so TalkBack
 * reads each card as a radio button and the choice is shown by the mark and a
 * heavier border as well as colour (the same card colours as
 * `MonarchRadioCardGroup`). The filters are `MonarchTag` toggles in a
 * scrolling row, the search field is `MonarchSearchBar`, and no results show
 * a `MonarchEmptyState`. When the category or the search changes, the number
 * of templates shown is announced through a polite live region. Cards are at
 * least 144dp wide, scaled with the font size, so the largest text shows one
 * column.
 *
 * The grid scrolls on its own: give it a bounded height, such as a sheet or
 * the rest of a screen.
 *
 * @param label the name of the gallery, read by TalkBack. It is not shown; the screen's title usually says it.
 * @param templates the templates in order.
 * @param selected the id of the chosen template or of the blank card, or `null` for none yet.
 * @param onSelect called with the id the person chose.
 * @param modifier the modifier of the gallery.
 * @param categories the category filters, shown with "All" first when given.
 * @param initialCategory the category filter selected at first. `null` shows all.
 * @param query the search text. Every word must appear in the name, the description or the keywords.
 * @param onQueryChange shows the search field at the top and is called as the person types. Leave it `null` when
 * the screen has its own search field and passes [query].
 * @param blank a "start from scratch" card shown first whatever the filters.
 * @param enabled whether any card or filter can be used. A template can also be disabled on its own.
 * @param state the scroll state of the grid.
 * @param labels replacements for the built-in strings.
 * @param showPreview shows a large preview of the chosen template, with its name and description, under the grid,
 * as the web does. It uses the template's `preview` slot, scaled up.
 */
@Composable
@Suppress("CyclomaticComplexMethod")
public fun MonarchTemplatePicker(
    label: String,
    templates: List<MonarchTemplatePickerItem>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<MonarchTemplatePickerCategory> = emptyList(),
    initialCategory: String? = null,
    query: String = "",
    onQueryChange: ((String) -> Unit)? = null,
    blank: MonarchTemplatePickerBlank? = null,
    enabled: Boolean = true,
    state: LazyGridState = rememberLazyGridState(),
    labels: MonarchTemplatePickerLabels = MonarchTemplatePickerLabels(),
    showPreview: Boolean = false,
) {
    val tokens = MonarchTemplatePickerTokens
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    val shown = tokens.filter(templates, category, query)
    val filtering = category != null || query.isNotBlank()
    val results = labels.results?.invoke(shown.size)
        ?: pluralStringResource(R.plurals.monarch_templatepicker_results, shown.size, shown.size)
    val minWidth = tokens.minCardWidth(LocalDensity.current.fontScale)
    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minWidth),
            modifier = Modifier
                .semantics {
                    contentDescription = label
                    isTraversalGroup = true
                }
                .selectableGroup(),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            if (onQueryChange != null) {
                item(key = "monarch-templatepicker-search", span = { GridItemSpan(maxLineSpan) }) {
                    MonarchSearchBar(
                        query = query,
                        onQueryChange = onQueryChange,
                        onSearch = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        placeholder = monarchString(labels.search, R.string.monarch_templatepicker_search),
                    )
                }
            }
            if (categories.isNotEmpty()) {
                item(key = "monarch-templatepicker-categories", span = { GridItemSpan(maxLineSpan) }) {
                    CategoryFilters(categories, category, { category = it }, enabled, labels)
                }
            }
            if (blank != null) {
                item(key = "monarch-templatepicker-blank:${blank.id}") {
                    TemplateCard(
                        name = blank.name ?: monarchString(labels.blank, R.string.monarch_templatepicker_blank),
                        description = blank.description,
                        badge = null,
                        preview = blank.preview,
                        selected = selected == blank.id,
                        enabled = enabled,
                        onClick = { onSelect(blank.id) },
                    )
                }
            }
            items(shown, key = { it.id }) { template ->
                TemplateCard(
                    name = template.name,
                    description = template.description,
                    badge = template.badge,
                    preview = template.preview,
                    selected = selected == template.id,
                    enabled = enabled && template.enabled,
                    onClick = { onSelect(template.id) },
                )
            }
            if (showPreview) {
                val chosen = shown.firstOrNull { it.id == selected }
                    ?.let { TemplateChoice(it.name, it.description, it.preview) }
                    ?: blank?.takeIf { it.id == selected }?.let { TemplateChoice(it.name, it.description, it.preview) }
                val preview = chosen?.preview
                if (chosen != null && preview != null) {
                    item(key = "monarch-templatepicker-preview", span = { GridItemSpan(maxLineSpan) }) {
                        TemplatePreviewPanel(
                            name = chosen.name ?: monarchString(labels.blank, R.string.monarch_templatepicker_blank),
                            description = chosen.description,
                            preview = preview,
                            enabled = enabled,
                        )
                    }
                }
            }
            if (shown.isEmpty()) {
                item(key = "monarch-templatepicker-empty", span = { GridItemSpan(maxLineSpan) }) {
                    MonarchEmptyState(
                        title = if (query.isNotBlank()) {
                            labels.noResults?.invoke(query)
                                ?: stringResource(R.string.monarch_templatepicker_no_results, query)
                        } else {
                            monarchString(labels.emptyCategory, R.string.monarch_templatepicker_empty_category)
                        },
                        kind = MonarchEmptyStateKind.Filtered,
                        size = MonarchEmptyStateSize.Sm,
                        onClearFilters = if (enabled) {
                            {
                                category = null
                                onQueryChange?.invoke("")
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        // Always present, so the number of templates is announced when the filters change.
        Box(
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (filtering) contentDescription = results
            },
        )
    }
}

/** The large preview of the chosen template: its preview scaled up, then its name and description. */
@Composable
private fun TemplatePreviewPanel(
    name: String,
    description: String?,
    preview: @Composable () -> Unit,
    enabled: Boolean,
) {
    val tokens = MonarchTemplatePickerTokens
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(MonarchTokens.Space.px.dp, colors.borderSubtle, MonarchShapeDefaults.Container)
            .padding(tokens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.CardGap),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((MonarchTokens.Space.space24 * 2).dp)
                .clip(MonarchShapeDefaults.Control)
                .background(tokens.previewColor(enabled, colors))
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f / PreviewScale)
                    .graphicsLayer {
                        scaleX = PreviewScale
                        scaleY = PreviewScale
                    },
            ) { preview() }
        }
        Text(text = name, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
        if (description != null) {
            Text(
                text = description,
                style = MonarchTheme.typography.caption,
                color = tokens.descriptionColor(enabled, colors),
            )
        }
    }
}

@Composable
private fun CategoryFilters(
    categories: List<MonarchTemplatePickerCategory>,
    category: String?,
    onCategoryChange: (String?) -> Unit,
    enabled: Boolean,
    labels: MonarchTemplatePickerLabels,
) {
    val name = monarchString(labels.categories, R.string.monarch_templatepicker_categories)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = name
                isTraversalGroup = true
            }
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MonarchTemplatePickerTokens.FilterGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchTag(
            text = monarchString(labels.allCategories, R.string.monarch_templatepicker_all_categories),
            selected = category == null,
            onSelectedChange = { onCategoryChange(null) },
            enabled = enabled,
        )
        categories.forEach { item ->
            MonarchTag(
                text = item.label,
                selected = category == item.id,
                onSelectedChange = { on -> onCategoryChange(if (on) item.id else null) },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun TemplateCard(
    name: String,
    description: String?,
    badge: String?,
    preview: (@Composable () -> Unit)?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MonarchTemplatePickerTokens
    val cards = MonarchRadioCardGroupTokens
    val colors = MonarchTheme.colors
    val card = cards.card(selected, enabled, colors)
    val shape = MonarchShapeDefaults.Container
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .monarchFocusRing(shape)
            .semantics { role = Role.RadioButton },
        enabled = enabled,
        shape = shape,
        color = card.container,
        contentColor = card.content,
        border = BorderStroke(cards.borderWidth(selected), card.border ?: card.container),
    ) {
        Column(
            modifier = Modifier.padding(tokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.CardGap),
        ) {
            if (preview != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = tokens.PreviewHeight)
                            .clip(MonarchShapeDefaults.Control)
                            .background(tokens.previewColor(enabled, colors))
                            .clearAndSetSemantics { },
                        contentAlignment = Alignment.Center,
                    ) { preview() }
                    if (selected) {
                        // The whole card is the control; the check only shows the state.
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(tokens.CardGap)
                                .size(CheckSize)
                                .background(colors.accentSolid, CircleShape)
                                .clearAndSetSemantics { },
                            contentAlignment = Alignment.Center,
                        ) {
                            CompositionLocalProvider(LocalContentColor provides colors.accentOnSolid) {
                                Box(Modifier.size(CheckGlyphSize)) { MonarchDefaultIcons.Check() }
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.CardGap), verticalAlignment = Alignment.Top) {
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    style = MonarchTheme.typography.bodyStrong,
                    color = card.content,
                )
                // The whole card is the control; the radio mark only shows the state of a card without a preview.
                if (preview == null) MonarchRadio(selected = selected, onClick = null, enabled = enabled)
            }
            if (badge != null) MonarchBadge(text = badge)
            if (description != null) {
                Text(
                    text = description,
                    style = MonarchTheme.typography.caption,
                    color = tokens.descriptionColor(enabled, colors),
                )
            }
        }
    }
}

private val CheckSize = MonarchTokens.Space.space6.dp
private val CheckGlyphSize = MonarchTokens.Space.space4.dp

private const val PreviewScale = 2.5f

private class TemplateChoice(val name: String?, val description: String?, val preview: (@Composable () -> Unit)?)
