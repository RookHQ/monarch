package com.rookhq.monarch.components.layout.logocloud

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchLogoCloudPreview() {
    PreviewFrame {
        MonarchLogoCloud(
            logos = listOf("Company A", "Company B", "Company C", "Company D").map { MonarchLogoCloudLogo(it) },
            title = "Placeholder organisations",
        )
        MonarchLogoCloud(
            logos = listOf("Company A", "Company B", "Company C").map { MonarchLogoCloudLogo(it, link = it) },
            title = "Integrations",
            layout = MonarchLogoCloudLayout.Grid,
            size = MonarchLogoCloudSize.Sm,
            onLogoClick = {},
        )
    }
}
