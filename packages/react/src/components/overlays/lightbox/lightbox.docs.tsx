import { useState } from 'react';
import { Button, Lightbox, type LightboxItem } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PHOTOS = [
  {
    alt: 'Planning board with three columns of notes',
    caption: 'Sprint planning board',
    fill: '#94a3b8',
  },
  { alt: 'Pencil sketch of a settings screen', caption: 'Settings sketch', fill: '#a3b18a' },
  { alt: 'Plain sample image in a warm grey', fill: '#c9ada7' },
];

/** Demo images: solid SVGs served as `blob:` URLs, which Lightbox accepts. */
function sampleItems(): LightboxItem[] {
  if (typeof URL === 'undefined' || typeof URL.createObjectURL !== 'function') return [];
  return PHOTOS.map(({ fill, ...item }) => ({
    ...item,
    width: 640,
    height: 400,
    src: URL.createObjectURL(
      new Blob(
        [
          `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="400"><rect width="640" height="400" fill="${fill}"/></svg>`,
        ],
        { type: 'image/svg+xml' },
      ),
    ),
  }));
}

function Gallery() {
  const [open, setOpen] = useState(false);
  const [index, setIndex] = useState(0);
  const [items] = useState(sampleItems);
  return (
    <>
      {items.map((item, i) => (
        <Button
          key={item.alt}
          onClick={() => {
            setIndex(i);
            setOpen(true);
          }}
        >
          View image {i + 1}
        </Button>
      ))}
      <Lightbox
        items={items}
        open={open}
        onOpenChange={setOpen}
        index={index}
        onIndexChange={setIndex}
      />
    </>
  );
}

export default {
  id: 'lightbox',
  title: 'Lightbox',
  summary: 'Full-screen image and video viewer with arrows, zoom, captions and required alt text.',
  sections: {
    purpose: {
      summary:
        'Opens images and videos at full size over the page, with previous and next, zoom, captions, a position count and an optional download link. Needed by: Rook Chat (`src/components/chat/messages/Attachments.tsx` builds its own `Lightbox` on Dialog with arrow keys and a download link).',
      useWhen: [
        'Thumbnails in a message, gallery or record that people open to see in detail.',
        'Several images people step through in order.',
      ],
      avoidWhen: [
        'Previewing a document or other file type: use FilePreview.',
        'An image that is already shown at a readable size: no viewer needed.',
      ],
    },
    designPrinciples: {
      items: [
        'Alt text is required by the type: every item says what it shows, and the alt is announced when stepping through.',
        'A modal dialog: focus is trapped, Escape closes it, and focus returns to the thumbnail.',
        'Zoom has buttons, keys and a visible level.',
        'Only safe addresses load: `src` and `downloadUrl` pass `isSafeImageSrc` (http(s), same-origin paths, `blob:`).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'One component. Give `trigger` for a single opener, or control `open` and `index` from your thumbnails.',
        '`LightboxItem` is `{ src, alt, caption?, kind?, downloadUrl?, tracks?, width?, height? }`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Rook Chat’s local Lightbox: pass the images as `items` with `alt` and `downloadUrl`, and replace `index`/`onIndex`/`onClose` with `index`, `onIndexChange` and `onOpenChange`.',
        'The dialog is named by `messages.title` ("Media viewer"); the position and current alt text are a polite live region.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'LightboxItem kind',
          values: ['image', 'video'],
          description:
            'Images can be zoomed; videos play in the native player with `alt` as its name.',
        },
        {
          prop: 'loop',
          values: ['true', 'false'],
          description: 'Wrap from the last item to the first. Default `false`.',
        },
        {
          prop: 'maxZoom',
          values: ['number'],
          description: 'Largest zoom factor, in steps of 50%. Default 4.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open / closed',
          attribute: 'data-state="open|closed"',
          description: 'From Radix Dialog.',
        },
        {
          name: 'Zoomed',
          attribute: 'data-zoomed',
          description:
            'The image is larger than the stage; the stage scrolls and is focusable so arrow keys pan.',
        },
        {
          name: 'First or last item',
          attribute: 'aria-disabled="true" (previous / next)',
          description: 'Without `loop` the button stays focusable but does nothing.',
        },
        {
          name: 'Blocked',
          description: 'An unsafe `src` shows `messages.blocked` instead of loading.',
        },
      ],
    },
    interaction: {
      items: [
        'Previous and next buttons, or the arrow keys, change the item and call `onIndexChange`. Stepping resets zoom.',
        'Zoom out, the level button (resets) and zoom in change the zoom between 100% and `maxZoom`.',
        'Videos get caption tracks from the item’s `tracks` (unsafe track URLs are dropped).',
        'The download link appears when the item has a safe `downloadUrl`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'ArrowRight / ArrowLeft',
          action: 'Next / previous item (mirrored in right-to-left). Pans instead while zoomed.',
        },
        { keys: 'Home / End', action: 'First / last item.' },
        { keys: '+ / =', action: 'Zoom in.' },
        { keys: '-', action: 'Zoom out.' },
        { keys: '0', action: 'Reset zoom.' },
        { keys: 'Escape', action: 'Close and return focus to the trigger.' },
        { keys: 'Tab / Shift+Tab', action: 'Move between controls; focus stays inside.' },
      ],
      notes: ['Keys pressed on a video go to the native player.'],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: '`messages.title` (visually hidden title).',
      items: [
        'Each image has its `alt`; a video gets it as `aria-label`.',
        'The position and current alt text are announced politely on every step.',
        'Icon buttons have names from `messages` and their shortcuts in `aria-keyshortcuts`.',
        'The zoom level is visible text; the reset button is named "Reset zoom".',
      ],
    },
    responsive: {
      items: [
        'Fills the screen and keeps clear of notches and the home bar.',
        'Below 640px the previous and next buttons move under the image so it gets the full width.',
        'On coarse pointers every button is at least 44px.',
        'Arrow icons and keys mirror in right-to-left layouts.',
      ],
    },
    theme: {
      items: [
        'The backdrop is the shared `mn-overlay` scrim; controls and captions sit on `--mn-color-bg-raised` with `--mn-shadow-md`.',
        'The open animation uses `--mn-duration-base`; reduced motion removes it.',
        'In forced colours controls get `CanvasText` borders and `ButtonText` icons.',
      ],
    },
    examples: {
      items: [{ title: 'Gallery of three images', render: () => <Gallery /> }],
    },
    doDont: {
      do: [
        'Write alt text that says what the image shows, not its file name.',
        'Pass `width` and `height` when you know them, so the layout does not jump.',
        'Give videos caption `tracks`.',
        'Offer `downloadUrl` when people may need the original file.',
      ],
      dont: [
        'Don’t put essential information only in the image; repeat it in the caption or page.',
        'Don’t use Lightbox for documents; use FilePreview.',
        'Don’t pass untrusted `data:` URLs; they are blocked.',
      ],
    },
    related: {
      items: [
        { id: 'dialog', reason: 'The modal pattern Lightbox follows.' },
        { id: 'file-preview', reason: 'Previews documents and other file types.' },
        { id: 'attachment-chip', reason: 'The attachment a Lightbox is often opened from.' },
      ],
    },
    dependencies: { packages: ['@radix-ui/react-dialog', 'react'], components: [] },
    testing: {
      unit: [
        'Opens as a named modal with image, caption and announced position; Escape returns focus.',
        'Traps focus.',
        'Moves between items with buttons, arrows, Home and End; download link; video with alt as its name and safe caption tracks only.',
        'Wraps with `loop` and mirrors arrows in right-to-left.',
        'Zooms with buttons and keys, clamps to `maxZoom`, pans instead of navigating while zoomed, resets on a new item.',
        'Blocks unsafe `src` and `downloadUrl`; translates labels.',
        'Works controlled.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
