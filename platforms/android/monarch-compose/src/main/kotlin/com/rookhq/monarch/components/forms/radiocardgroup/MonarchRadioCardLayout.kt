package com.rookhq.monarch.components.forms.radiocardgroup

/** How the cards of a `MonarchRadioCardGroup` are laid out. */
public enum class MonarchRadioCardLayout {
    /** A row per card: icon, title, description and meta, with the radio mark at the end. */
    Row,

    /**
     * Centred tiles: the icon above the title and the description, all centred, with no radio mark. The accent
     * edge and fill (and TalkBack's "selected") show the choice. The tiles keep their columns on a phone.
     */
    Tile,
}
