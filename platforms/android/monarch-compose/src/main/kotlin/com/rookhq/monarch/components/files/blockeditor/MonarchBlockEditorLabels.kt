package com.rookhq.monarch.components.files.blockeditor

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuLabels
import com.rookhq.monarch.components.files.editortoolbar.MonarchEditorToolbarLabels
import com.rookhq.monarch.components.files.embedblock.MonarchEmbedBlockLabels
import com.rookhq.monarch.components.files.tableblock.MonarchTableBlockLabels

/**
 * Replacements for the built-in strings of a `MonarchBlockEditor`. Every
 * `null`, and every block type missing from a map, keeps the built-in string
 * `monarch_blockeditor_<key>`.
 *
 * @property placeholder shown in an empty paragraph: "Write, or type / for blocks".
 * @property names the name of each kind of block, in the / menu, the "Turn into" menu and for TalkBack:
 * "Text", "Heading 1", "Bulleted list", "Callout"…
 * @property descriptions the second line of each entry of the / menu: "Plain paragraph", "Page title"…
 * @property groupText the heading of the text entries of the / menu: "Text".
 * @property groupLists the heading of the list entries: "Lists".
 * @property groupBlocks the heading of the other blocks: "Blocks".
 * @property handle the spoken name of a block's handle from the block's name: "Block actions: Heading 2".
 * @property moveUp the block menu item "Move up", also the handle's TalkBack action.
 * @property moveDown the block menu item "Move down", also the handle's TalkBack action.
 * @property duplicate the block menu item "Duplicate".
 * @property delete the block menu item "Delete block".
 * @property turnInto the heading over the kinds a block can become: "Turn into".
 * @property tone the heading over a callout's tones: "Tone".
 * @property neutralTone the name of the neutral tone: "Neutral". The other tones use the callout's names.
 * @property moved announced after a move, from the block's name, its new position and the number of blocks:
 * "Heading 2 moved to position 3 of 7."
 * @property toggleShow the button that shows a toggle's content: "Show".
 * @property toggleHide the button that hides it: "Hide".
 * @property toggleContent the spoken name and placeholder of a toggle's content: "Content".
 * @property embedLink the label of the field of a new embed: "Link to embed".
 * @property embedAdd the button that embeds the link: "Embed".
 * @property toolbar the strings of the formatting toolbar.
 * @property slashMenu the strings of the / menu.
 * @property table the strings of table blocks.
 * @property embed the strings of embed blocks.
 */
@Immutable
public class MonarchBlockEditorLabels(
    public val placeholder: String? = null,
    public val names: Map<MonarchBlockEditorBlockType, String> = emptyMap(),
    public val descriptions: Map<MonarchBlockEditorBlockType, String> = emptyMap(),
    public val groupText: String? = null,
    public val groupLists: String? = null,
    public val groupBlocks: String? = null,
    public val handle: ((block: String) -> String)? = null,
    public val moveUp: String? = null,
    public val moveDown: String? = null,
    public val duplicate: String? = null,
    public val delete: String? = null,
    public val turnInto: String? = null,
    public val tone: String? = null,
    public val neutralTone: String? = null,
    public val moved: ((block: String, position: Int, total: Int) -> String)? = null,
    public val toggleShow: String? = null,
    public val toggleHide: String? = null,
    public val toggleContent: String? = null,
    public val embedLink: String? = null,
    public val embedAdd: String? = null,
    public val toolbar: MonarchEditorToolbarLabels = MonarchEditorToolbarLabels(),
    public val slashMenu: MonarchSlashMenuLabels = MonarchSlashMenuLabels(),
    public val table: MonarchTableBlockLabels = MonarchTableBlockLabels(),
    public val embed: MonarchEmbedBlockLabels = MonarchEmbedBlockLabels(),
)
