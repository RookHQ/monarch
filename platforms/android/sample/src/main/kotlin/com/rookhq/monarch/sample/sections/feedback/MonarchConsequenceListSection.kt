package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequence
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceList
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of ConsequenceList. */
object MonarchConsequenceListSection : CataloguePage {
    override val title = "ConsequenceList"

    private val items = listOf(
        MonarchConsequence(MonarchConsequenceKind.Removed, "tasks will be deleted", count = 1284),
        MonarchConsequence(MonarchConsequenceKind.Changed, "Members lose access immediately"),
        MonarchConsequence(
            MonarchConsequenceKind.Warning,
            "This cannot be undone",
            detail = "Export the project first if you need a copy.",
        ),
        MonarchConsequence(MonarchConsequenceKind.Kept, "Invoices stay in billing"),
    )

    @Composable
    override fun Content() {
        var grouped by rememberSaveable { mutableStateOf(false) }
        var loading by rememberSaveable { mutableStateOf(false) }
        MonarchSwitchRow(checked = grouped, onCheckedChange = { grouped = it }, label = "Group by kind")
        MonarchSwitchRow(checked = loading, onCheckedChange = { loading = it }, label = "Loading")
        MonarchConsequenceList(
            items = items,
            grouped = grouped,
            loading = loading,
            heading = "What happens",
            icon = { MonarchIcon(SampleIcons.Warning, contentDescription = null, size = MonarchIconSize.Sm) },
        )
    }
}
