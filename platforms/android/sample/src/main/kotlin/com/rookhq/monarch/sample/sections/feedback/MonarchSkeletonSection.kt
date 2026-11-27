package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeletonShape
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Skeleton. */
object MonarchSkeletonSection : CataloguePage {
    override val title = "Skeleton"

    private const val SHORT_LINE = 0.6f

    @Composable
    override fun Content() {
        var loading by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("A contact card")
        MonarchCard(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
                if (loading) {
                    MonarchSkeleton(shape = MonarchSkeletonShape.Circle)
                    Column(
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "Loading contact"
                        },
                        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                    ) {
                        MonarchSkeleton(shape = MonarchSkeletonShape.Text)
                        MonarchSkeleton(modifier = Modifier.fillMaxWidth(SHORT_LINE), shape = MonarchSkeletonShape.Text)
                    }
                } else {
                    Column {
                        MonarchText(text = "Ada Lovelace", style = MonarchTextStyle.BodyStrong)
                        MonarchText(text = "Joined in March, 12 open tickets", style = MonarchTextStyle.Caption)
                    }
                }
            }
        }
        MonarchButton(
            text = if (loading) "Show the content" else "Show the placeholder",
            onClick = { loading = !loading },
            size = MonarchButtonSize.Sm,
        )
        CatalogueCaption("Shapes")
        MonarchSkeletonShape.entries.forEach { shape -> MonarchSkeleton(shape = shape) }
    }
}
