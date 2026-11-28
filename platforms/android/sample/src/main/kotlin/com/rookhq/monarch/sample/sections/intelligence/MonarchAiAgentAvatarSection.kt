package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

private val Agent = MonarchAiAgentRef(name = "Research agent", id = "research", codename = "RA")

/** The catalogue page of AiAgentAvatar. */
object MonarchAiAgentAvatarSection : CataloguePage {
    override val title = "AiAgentAvatar"

    @Composable
    override fun Content() {
        var state by rememberSaveable { mutableStateOf(MonarchAiAgentState.Working) }
        CatalogueCaption("Pick a state")
        MonarchSegmentedControl {
            MonarchAiAgentState.entries.forEachIndexed { index, option ->
                Segment(
                    selected = state == option,
                    onClick = { state = option },
                ) { Text(option.name) }
            }
        }
        MonarchAiAgentAvatar(agent = Agent, state = state, size = MonarchAvatarSize.Lg, showState = true)
        CatalogueCaption("Sizes")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            MonarchAvatarSize.entries.forEach { size ->
                MonarchAiAgentAvatar(agent = Agent, state = state, size = size)
            }
        }
    }
}
