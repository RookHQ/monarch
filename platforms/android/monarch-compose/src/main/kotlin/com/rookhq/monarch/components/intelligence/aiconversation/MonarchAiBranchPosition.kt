package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * Which of a message's alternatives is shown. A message has alternatives
 * after a reply was regenerated or a prompt was edited and resent.
 *
 * @property index the alternative that is shown, counted from 0.
 * @property count how many alternatives exist.
 */
public data class MonarchAiBranchPosition(public val index: Int, public val count: Int)
