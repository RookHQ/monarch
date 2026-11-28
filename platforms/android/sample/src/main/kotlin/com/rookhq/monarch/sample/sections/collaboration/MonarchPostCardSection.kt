package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.collaboration.postcard.MonarchPostCard
import com.rookhq.monarch.components.collaboration.postcard.MonarchPostCardTag
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of PostCard. */
object MonarchPostCardSection : CataloguePage {
    override val title = "PostCard"

    private const val ThreeHours = 3 * 3_600_000L
    private const val Reactions = 12
    private const val Comments = 4

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var saved by rememberSaveable { mutableStateOf(false) }
        var opened by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("A question with an accepted answer")
        MonarchPostCard(
            title = "How do you plan a release with two teams?",
            author = "Ana Lima",
            onClick = { opened++ },
            pinned = true,
            answered = true,
            excerpt = "We ship every two weeks and keep missing the window when the web and mobile teams both " +
                "change the API. How do other teams line up their releases?",
            publishedAt = now - ThreeHours,
            tags = listOf(MonarchPostCardTag("Planning"), MonarchPostCardTag("In review", MonarchTone.Warning)),
            reactionCount = Reactions,
            commentCount = Comments,
            actions = {
                MonarchButton(
                    text = if (saved) "Saved" else "Save",
                    onClick = { saved = !saved },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
        CatalogueCaption("Title only")
        MonarchPostCard(title = "Welcome to the community", author = "Ben Okafor", onClick = { opened++ })
        MonarchText(text = "Posts opened: $opened")
    }
}
