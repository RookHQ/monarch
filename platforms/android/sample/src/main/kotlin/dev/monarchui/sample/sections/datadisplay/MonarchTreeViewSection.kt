package dev.monarchui.sample.sections.datadisplay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.treeview.MonarchTreeView
import dev.monarchui.components.datadisplay.treeview.MonarchTreeViewItem
import dev.monarchui.components.datadisplay.treeview.MonarchTreeViewSelectionMode
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of TreeView: departments with teams; Mobile loads its children on first open. */
object MonarchTreeViewSection : CataloguePage {
    override val title = "TreeView"

    private const val LOAD_MILLIS = 1200L
    private const val LIST_HEIGHT_STEPS = 4

    /** A chevron that points to the end; it mirrors in right-to-left layouts. */
    private val Chevron: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chevron",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = true,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.6f, 7.4f)
                lineTo(10f, 6f)
                lineTo(16f, 12f)
                lineTo(10f, 18f)
                lineTo(8.6f, 16.6f)
                lineTo(13.2f, 12f)
                close()
            }
        }.build()
    }

    private val mobileTeams = listOf(
        MonarchTreeViewItem(id = "ios", label = "iOS"),
        MonarchTreeViewItem(id = "android", label = "Android"),
    )

    private fun tree(mobileLoaded: Boolean) = listOf(
        MonarchTreeViewItem(
            id = "eng",
            label = "Engineering",
            description = "Department · 12 people",
            children = listOf(
                MonarchTreeViewItem(id = "web", label = "Web"),
                MonarchTreeViewItem(
                    id = "mobile",
                    label = "Mobile",
                    children = if (mobileLoaded) mobileTeams else null,
                    hasChildren = true,
                ),
            ),
        ),
        MonarchTreeViewItem(
            id = "sales",
            label = "Sales",
            description = "Department · 5 people",
            children = listOf(MonarchTreeViewItem(id = "emea", label = "EMEA")),
        ),
        MonarchTreeViewItem(id = "legal", label = "Legal", enabled = false),
    )

    @Composable
    override fun Content() {
        var expanded by rememberSaveable { mutableStateOf(setOf("eng")) }
        var selected by rememberSaveable { mutableStateOf(setOf<String>()) }
        var loading by rememberSaveable { mutableStateOf(setOf<String>()) }
        var mobileLoaded by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(loading) {
            if ("mobile" in loading) {
                delay(LOAD_MILLIS)
                mobileLoaded = true
                loading = loading - "mobile"
            }
        }
        CatalogueCaption("Single selection, children loaded on first open")
        MonarchTreeView(
            items = tree(mobileLoaded),
            expanded = expanded,
            onExpandedChange = { expanded = it },
            expandIcon = {
                MonarchIcon(Chevron, contentDescription = null, size = MonarchIconSize.Sm)
            },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS),
            selectionMode = MonarchTreeViewSelectionMode.Single,
            selected = selected,
            loading = loading,
            onSelectedChange = { selected = it },
            onLoadChildren = { loading = loading + it.id },
        )
        MonarchText(
            text = if (selected.isEmpty()) "Nothing selected." else "Selected: ${selected.joinToString()}",
            color = MonarchTheme.colors.textMuted,
        )
    }
}
