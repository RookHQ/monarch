package com.rookhq.monarch.screenshots.showcase

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.navigation.tabs.MonarchTabs
import com.rookhq.monarch.foundation.MonarchTone

internal val navigationShowcases: Map<String, @Composable () -> Unit> = mapOf(
    "tabs" to {
        ShowcaseColumn {
            var first by remember { mutableIntStateOf(0) }
            MonarchTabs(selectedIndex = first, onSelectedIndexChange = { first = it }, panel = { Text("Board view") }) {
                tab("Board")
                tab("List")
                tab("Timeline", enabled = false)
            }
            MonarchTabs(selectedIndex = 0, onSelectedIndexChange = {}, panel = { Text("Open requests") }) {
                tab("Open", count = "6", countTone = MonarchTone.Accent)
                tab("Waiting", count = "3", countTone = MonarchTone.Warning)
                tab("Closed", count = "15")
            }
            MonarchTabs(selectedIndex = 1, onSelectedIndexChange = {}) {
                listOf("Overview", "Activity", "Files", "Members", "Billing").forEach { tab(it) }
            }
        }
    },
)
