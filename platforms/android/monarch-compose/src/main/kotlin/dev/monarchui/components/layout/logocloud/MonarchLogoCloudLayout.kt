package dev.monarchui.components.layout.logocloud

/** How a [MonarchLogoCloud] places its logos. */
public enum class MonarchLogoCloudLayout {
    /** A centred row that wraps onto more lines. */
    Row,

    /** Equal bordered cells, as many per row as fit at least 144dp wide. */
    Grid,
}
