package com.rookhq.monarch.components.layout.marketingsection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A frame around a product illustration: a rounded, bordered container with a window bar that shows three dots and
 * an optional address, and the illustration below. TalkBack reads [label] for the whole frame and skips its
 * content, which is a picture of the product rather than a working screen.
 *
 * @param label what the illustration shows, read by TalkBack, such as "Share dialog with a copyable link".
 * @param modifier the modifier of the frame.
 * @param title the address or window title in the bar, such as "example.rookhq.com/share".
 * @param content the illustration.
 */
@Composable
public fun MonarchProductFrame(
    label: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.bgSurface, shape)
            .border(MonarchShapeDefaults.BorderWidth, colors.borderDefault, shape)
            .clearAndSetSemantics { contentDescription = label },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSubtle)
                .padding(MonarchMarketingSectionTokens.FrameBarPadding),
            horizontalArrangement = Arrangement.spacedBy(MonarchMarketingSectionTokens.FrameBarPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(FrameDots) {
                Box(Modifier.size(MonarchMarketingSectionTokens.FrameDot).background(colors.borderStrong, CircleShape))
            }
            if (title != null) {
                MonarchText(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.bgSurface, MonarchTheme.shapes.full)
                        .padding(horizontal = MonarchMarketingSectionTokens.FrameBarPadding),
                    style = MonarchTextStyle.Caption,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MonarchDivider()
        Box(Modifier.fillMaxWidth().semantics { }, content = content)
    }
}

private const val FrameDots = 3
