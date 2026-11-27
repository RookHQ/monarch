package com.rookhq.monarch.sample.sections.layout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerOrientation
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The catalogue page of SplitInboxLayout.
 *
 * Apps use the Material ListDetailPaneScaffold with its extra pane, as the
 * inventory example shows. The catalogue app does not depend on the adaptive
 * artifacts, so this page shows the same behaviour with a Row and
 * BackHandler: three panes from 840dp, two from 600dp, one pane at a time
 * below, with back returning one pane.
 */
object SplitInboxLayoutSection : CataloguePage {
    override val title = "SplitInboxLayout"

    private const val HEIGHT_STEPS = 3
    private const val SIDE_SHARE = 0.3f
    private const val TWO_PANES = 2
    private val MediumWidth = 600.dp
    private val ExpandedWidth = 840.dp

    private val conversations = listOf(
        "Printer offline on floor 3",
        "Invoice shows the wrong address",
        "Reset two-factor",
    )

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf<String?>(null) }
        var context by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Three panes from 840dp, two from 600dp, one below")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS)) {
            val width = maxWidth
            val single = width < MediumWidth
            BackHandler(enabled = single && (context || open != null)) {
                if (context) context = false else open = null
            }
            val list: @Composable (Modifier) -> Unit = { modifier -> Conversations(modifier, open) { open = it } }
            val detail: @Composable (Modifier) -> Unit = { modifier -> Detail(modifier, open) { context = !context } }
            val details: @Composable (Modifier) -> Unit = { modifier -> Context(modifier, open) }
            val panes = visiblePanes(width, open != null, context)
            Row {
                panes.forEachIndexed { index, pane ->
                    if (index > 0) Divider()
                    val modifier = if (pane == Pane.Detail || panes.size == 1) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.width(width * SIDE_SHARE)
                    }
                    when (pane) {
                        Pane.List -> list(modifier)
                        Pane.Detail -> detail(modifier)
                        Pane.Context -> details(modifier)
                    }
                }
            }
        }
    }

    private enum class Pane { List, Detail, Context }

    /** The panes that fit [width]: the deepest open pane alone on a phone, the last two or three otherwise. */
    private fun visiblePanes(width: Dp, open: Boolean, context: Boolean): List<Pane> {
        val all = listOfNotNull(Pane.List, Pane.Detail, Pane.Context.takeIf { context })
        return when {
            width >= ExpandedWidth -> all
            width >= MediumWidth -> all.takeLast(TWO_PANES)
            context -> listOf(Pane.Context)
            open -> listOf(Pane.Detail)
            else -> listOf(Pane.List)
        }
    }

    @Composable
    private fun Divider() {
        MonarchDivider(Modifier.fillMaxHeight(), orientation = MonarchDividerOrientation.Vertical)
    }

    @Composable
    private fun Conversations(modifier: Modifier, open: String?, onOpen: (String) -> Unit) {
        LazyColumn(modifier = modifier) {
            items(items = conversations, key = { it }) { conversation ->
                ListItem(
                    headlineContent = { Text(conversation) },
                    modifier = Modifier.selectable(selected = conversation == open, onClick = { onOpen(conversation) }),
                    colors = ListItemDefaults.colors(
                        containerColor = if (conversation == open) {
                            MonarchTheme.colors.accentSubtle
                        } else {
                            MonarchTheme.colors.bgCanvas
                        },
                    ),
                )
            }
        }
    }

    @Composable
    private fun Detail(modifier: Modifier, open: String?, onToggleContext: () -> Unit) {
        Column(modifier = modifier.padding(MonarchTheme.spacing.space4)) {
            if (open == null) {
                MonarchEmptyState(title = "Select a conversation")
            } else {
                MonarchText(text = open, style = MonarchTextStyle.Heading)
                MonarchButton(text = "Details", onClick = onToggleContext, variant = MonarchButtonVariant.Ghost)
            }
        }
    }

    @Composable
    private fun Context(modifier: Modifier, open: String?) {
        Column(modifier = modifier.padding(MonarchTheme.spacing.space4)) {
            MonarchText(text = "Ticket details", style = MonarchTextStyle.Heading)
            MonarchText(text = open ?: "No conversation open")
        }
    }
}
