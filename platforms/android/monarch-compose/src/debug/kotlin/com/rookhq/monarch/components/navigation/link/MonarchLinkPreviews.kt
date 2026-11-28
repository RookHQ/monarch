package com.rookhq.monarch.components.navigation.link

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchLinkPreview() {
    PreviewFrame {
        MonarchLinkVariant.entries.forEach { variant ->
            MonarchLink(text = "${variant.name} link", onClick = {}, variant = variant)
        }
        MonarchLink(text = "Disabled link", onClick = {}, enabled = false)
        MonarchLink(
            text = "Terms of service",
            onClick = {},
            variant = MonarchLinkVariant.Standalone,
            external = true,
            trailingIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchText(
            text = buildAnnotatedString {
                append("Read the ")
                withLink(LinkAnnotation.Url("https://example.com/terms", MonarchLinkDefaults.styles())) {
                    append("terms of service")
                }
                append(" before you continue.")
            },
        )
    }
}
