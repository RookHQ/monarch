import { EmbedBlock, EmbedBlockExtension, RichTextEditor } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const EXTENSIONS = [EmbedBlockExtension];

export default {
  id: 'embed-block',
  title: 'EmbedBlock',
  summary:
    'Shows a video, design or demo from an allow-listed site in a sandboxed frame, and any other link as a card; also a Tiptap node.',
  sections: {
    purpose: {
      summary:
        'Lets documents include a video or design someone pasted a link to, without letting a pasted link put arbitrary pages or scripts in the app. Needed by: Rook Dossier (EmbedBlock and resolveEmbed in src/lib/tiptap/blocks.ts, src/components/document/nodeViews/EmbedNodeView.tsx).',
      useWhen: [
        'People paste YouTube, Vimeo, Loom, Figma or CodePen links into documents.',
        'Authors should add embeds in an editor (`EmbedBlockExtension`).',
      ],
      avoidWhen: [
        'A link inside a sentence: use a normal link.',
        'A preview with title and image from your own backend: use LinkCard.',
        'Your own app’s pages: render them as components, not frames.',
      ],
    },
    designPrinciples: {
      items: [
        'Allow-list, not block-list: only https URLs on exact hostnames of a known provider are framed, and only at the provider’s own embed address built from a validated id. `youtube.com.example.net`, `http:`, ports and credentials are never framed.',
        'Every frame is sandboxed (`allow-scripts allow-same-origin allow-presentation allow-popups` by default), lazy, and sends only the origin as referrer. `allow-same-origin` is safe here because the frame is always on the provider’s origin, never the app’s; it lets players keep their own settings.',
        'Stored HTML never contains a frame: the node saves a plain link, and the frame is rebuilt from the allow-list when the document is shown, so editing raw HTML cannot add one.',
        'Everything else degrades to a link card; unsafe URLs (`javascript:`, `data:`) show a short message and no link.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`EmbedBlock` renders `figure.mn-embed-block[data-kind="frame" | "link" | "invalid"]` with `__frame > iframe`, `__card` or `__invalid`, then `figcaption.__caption`.',
        '`resolveEmbed(url, providers?)` returns `{ kind: "frame", src, provider }`, `{ kind: "link", host }` or `{ kind: "invalid" }`. `DEFAULT_EMBED_PROVIDERS` lists YouTube (privacy-enhanced domain), Vimeo, Loom, Figma and CodePen.',
        '`EmbedBlockExtension` is the atom node `embed` (attributes `url`, `caption`); command `insertEmbed({ url, caption })`. HTML: `<div data-type="embed" data-url="…"><a href="…">…</a></div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'An `EmbedProvider` is `{ id, name, hosts, toEmbedSrc(url), sandbox?, allow?, aspectRatio? }`. `toEmbedSrc` must validate ids and return an https address; return null to fall back to a link card.',
        '`EmbedBlockExtension.configure({ providers, messages })` sets the allow-list and strings for the editor.',
        'Migrating from Dossier: `div.doc-embed` with `url` and `caption` maps to `data-url` and `data-caption`; GitHub and other unframeable hosts already become link cards.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'kind (derived from url)',
          values: ['frame', 'link', 'invalid'],
          description: 'Allow-listed frame, link card, or a message for unusable URLs.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Kind', attribute: 'data-kind', description: 'What the URL resolved to.' },
        {
          name: 'Empty (editor)',
          description: 'A new embed shows a labelled URL field and an Embed button.',
        },
        {
          name: 'Selected (editor)',
          attribute: 'data-selected',
          description: 'Focus-ring outline when the node is selected.',
        },
        {
          name: 'Invalid input (editor)',
          description: 'The field shows an error and nothing is inserted.',
        },
      ],
    },
    interaction: {
      items: [
        'In the editor, paste a link and press Enter or Embed. Unusable addresses are refused with a message.',
        'Once embedded, the caption field edits the caption under the frame.',
        'The node can be dragged and deleted like any block (select it and press Backspace).',
        'The link card opens in a new tab.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter (URL field)', action: 'Embeds the link.' },
        { keys: 'Tab', action: 'Moves through the field, the button, the frame and the caption.' },
      ],
      notes: ['Keys inside the frame belong to the provider’s player.'],
    },
    accessibility: {
      role: 'figure containing an iframe or a link',
      name: 'The frame’s `title` is the caption, `title`, or "<Provider> content".',
      items: [
        'The link card tells screen readers it opens in a new tab.',
        'Editor fields are labelled with Field and FieldLabel; errors are linked to the field.',
      ],
    },
    responsive: {
      items: [
        'The frame keeps its provider’s aspect ratio (16:9, or 4:3 for Figma and CodePen) at any width.',
        'Long URLs are truncated in the card; the form wraps at 390px; touch targets are 44px.',
      ],
    },
    theme: {
      items: [
        '`--mn-color-bg-muted` behind frames, `--mn-color-border-*` borders, `--mn-color-text-link` URLs.',
        'Forced colours draw system borders.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Frame and link card',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 16, width: '100%', maxWidth: 560 }}>
              <EmbedBlock url="https://vimeo.com/76979871" caption="Walkthrough" />
              <EmbedBlock url="https://example.com/handbook/onboarding" />
            </div>
          ),
        },
        {
          title: 'In an editor',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <RichTextEditor
                aria-label="Notes"
                extensions={EXTENSIONS}
                defaultValue='<p>Paste a link below.</p><div data-type="embed"></div>'
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Add providers with exact hostnames and strict id checks in `toEmbedSrc`.',
        'Give a caption so the frame has a meaningful title.',
      ],
      dont: [
        'Do not add the app’s own origin as a provider.',
        'Do not store or render embed codes (`<iframe>` HTML) people paste.',
      ],
    },
    related: {
      items: [
        { id: 'link-card', reason: 'A rich preview of a link from your own backend.' },
        { id: 'block-editor', reason: 'Offers the embed in its slash menu.' },
      ],
    },
    dependencies: {
      packages: ['react', '@tiptap/core', '@tiptap/react'],
      components: ['button', 'form-field', 'text-field'],
    },
    testing: {
      unit: [
        '`resolveEmbed` for each provider, lookalike hosts, http, ports, credentials, bad ids, `javascript:`, `data:`, relative and malformed URLs, custom provider lists.',
        'Frame attributes: sandbox, referrer policy, lazy, title; link card rel and target; message for unsafe URLs.',
        'Editor: labelled URL field, refused input, embed on Enter, caption field, HTML holds a link and never a frame, unsafe stored URLs dropped.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
