package dev.monarchui.sample.sections.workflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValue
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueItem
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.feedback.calloutbanner.MonarchCalloutBanner
import dev.monarchui.components.forms.inlineedittext.MonarchInlineEditText
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/**
 * The catalogue page of RecordHeader: the navigation bar carries the title,
 * back, previous and next and the More menu; the screen composes the avatar,
 * the editable title, status and badges, key facts, a state banner and tabs.
 */
object RecordHeaderSection : CataloguePage {
    override val title = "RecordHeader"

    private val records = listOf("Ada Lovelace", "Alan Turing", "Grace Hopper")
    private val tabs = listOf("Overview", "Activity", "Deals")

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        var index by rememberSaveable { mutableIntStateOf(0) }
        var names by rememberSaveable { mutableStateOf(records) }
        var menuOpen by rememberSaveable { mutableStateOf(false) }
        var archived by rememberSaveable { mutableStateOf(false) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val name = names[index]
        CatalogueCaption("Navigation bar")
        TopAppBar(
            title = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = {}) {
                    Icon(imageVector = SampleIcons.ArrowBack, contentDescription = "Back to contacts")
                }
            },
            actions = {
                IconButton(onClick = { index-- }, enabled = index > 0) {
                    Icon(SampleIcons.ChevronUp, contentDescription = "Previous contact")
                }
                IconButton(onClick = { index++ }, enabled = index < names.lastIndex) {
                    Icon(SampleIcons.ChevronDown, contentDescription = "Next contact")
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = SampleIcons.MoreVertical,
                            contentDescription = "More actions for $name",
                        )
                    }
                    MonarchMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        MonarchMenuItem(
                            text = if (archived) "Restore" else "Archive",
                            onClick = {
                                archived = !archived
                                menuOpen = false
                            },
                        )
                    }
                }
            },
        )
        CatalogueCaption("Header of the screen")
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchAvatar(image = null, name = name, size = MonarchAvatarSize.Lg, decorative = true)
                MonarchInlineEditText(
                    value = name,
                    onSave = { next -> names = names.toMutableList().also { it[index] = next } },
                    label = "Contact name",
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchStatusBadge(definition = MonarchStatusDefinition("Customer", MonarchTone.Success))
                MonarchBadge(text = "VIP", tone = MonarchTone.Info)
            }
            Text(text = "Contact ${index + 1} of ${names.size}", style = MonarchTheme.typography.caption)
            MonarchKeyValue(
                items = listOf(
                    MonarchKeyValueItem(label = "Owner", value = "Ben Okafor"),
                    MonarchKeyValueItem(label = "Plan", value = "Enterprise"),
                    MonarchKeyValueItem(label = "Phone", value = null),
                ),
            )
            if (archived) {
                MonarchCalloutBanner(
                    message = "This contact is archived. Restore it to edit.",
                    tone = MonarchTone.Warning,
                )
            }
            PrimaryTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
        }
    }
}
