package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.intelligence.aiusermessageedit.MonarchAiUserMessageEdit
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AiUserMessageEdit. */
object MonarchAiUserMessageEditSection : CataloguePage {
    override val title = "AiUserMessageEdit"

    @Composable
    override fun Content() {
        var prompt by rememberSaveable { mutableStateOf("Summarise the quarterly report.") }
        var editing by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Edit and resend")
        if (editing) {
            MonarchAiUserMessageEdit(
                original = prompt,
                onSubmit = {
                    prompt = it
                    editing = false
                },
                onCancel = { editing = false },
                focusOnAppear = false,
            )
        } else {
            MonarchText(text = prompt)
            MonarchButton(text = "Edit", onClick = { editing = true }, size = MonarchButtonSize.Sm)
        }
        CatalogueCaption("Disabled")
        MonarchAiUserMessageEdit(original = "Draft a reply.", onSubmit = {}, onCancel = {}, enabled = false)
    }
}
