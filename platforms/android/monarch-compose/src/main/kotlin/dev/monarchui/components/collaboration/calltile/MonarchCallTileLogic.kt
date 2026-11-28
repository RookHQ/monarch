package dev.monarchui.components.collaboration.calltile

import dev.monarchui.components.identity.avatar.MonarchAvatarSize

/** The rules of MonarchCallTile: reactions, connection bars, buttons and what TalkBack reads. */
internal object MonarchCallTileLogic {
    /** How long a reaction stays, from when it was sent. */
    const val REACTION_MILLIS: Long = 3_000L

    private const val GOOD_BARS = 3
    private const val FAIR_BARS = 2
    private const val POOR_BARS = 1

    /** The avatar of a tile without video. */
    fun avatarSize(size: MonarchCallTileSize): MonarchAvatarSize = when (size) {
        MonarchCallTileSize.Sm -> MonarchAvatarSize.Md
        MonarchCallTileSize.Md, MonarchCallTileSize.Lg -> MonarchAvatarSize.Lg
    }

    /** Whether the tile offers the Pin and full-screen buttons: never in the filmstrip size. */
    fun showsActions(size: MonarchCallTileSize, pinnable: Boolean, fullScreen: Boolean): Boolean =
        size != MonarchCallTileSize.Sm && (pinnable || fullScreen)

    /** How many of the three connection bars are lit. */
    fun litBars(quality: MonarchCallQuality): Int = when (quality) {
        MonarchCallQuality.Good -> GOOD_BARS
        MonarchCallQuality.Fair -> FAIR_BARS
        MonarchCallQuality.Poor -> POOR_BARS
    }

    /** Whether [reaction] shows at [now]: from when it was sent, for three seconds. A future time does not show. */
    fun reactionVisible(reaction: MonarchCallReaction?, now: Long): Boolean {
        if (reaction == null) return false
        val age = now - reaction.sentAt
        return age in 0 until REACTION_MILLIS
    }

    /** How long [reaction] still shows at [now], or 0. */
    fun reactionRemaining(reaction: MonarchCallReaction, now: Long): Long =
        if (reactionVisible(reaction, now)) REACTION_MILLIS - (now - reaction.sentAt) else 0L

    /** The state TalkBack reads after the name: "Speaking, Muted, Hand raised". Empty parts are left out. */
    fun spokenState(parts: List<String?>): String = parts.filterNotNull().filter { it.isNotBlank() }.joinToString(", ")
}
