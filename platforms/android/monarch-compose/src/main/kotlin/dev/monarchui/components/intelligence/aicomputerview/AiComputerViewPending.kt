package dev.monarchui.components.intelligence.aicomputerview

import kotlinx.coroutines.Job

/** New actions waiting to be announced, and whether an announcement was made too recently. */
internal class AiComputerViewPending {
    val seen: MutableSet<String> = mutableSetOf()
    var started: Boolean = false
    var count: Int = 0
    var latest: String = ""
    var cooling: Job? = null
}
