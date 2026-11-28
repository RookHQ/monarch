import { RichTextEditor, ToggleBlock, ToggleBlockExtension } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './toggle-block.fixture.json';

const EXTENSIONS = [ToggleBlockExtension];

export default {
  id: 'toggle-block',
  title: 'ToggleBlock',
  summary:
    'A collapsible section of a document, as a React disclosure and as a Tiptap node whose open state is saved.',
  sections: {
    purpose: {
      summary:
        'Folds detail (answers, long steps, reference material) under a one-line summary so a document stays scannable. Needed by: Rook Dossier (ToggleBlock, ToggleSummary, ToggleContent in src/lib/tiptap/blocks.ts and src/components/document/nodeViews/ToggleBlockNodeView.tsx).',
      useWhen: [
        'A document has optional detail that most readers skip.',
        'Authors should create collapsible sections themselves in an editor (`ToggleBlockExtension`).',
      ],
      avoidWhen: [
        'Several related panels where only one should be open: use Accordion.',
        'App chrome that shows or hides settings: use Collapsible.',
      ],
    },
    designPrinciples: {
      items: [
        'An APG disclosure: a real button with `aria-expanded`, never a clickable div.',
        'Collapsed content stays in the document: the node hides it with CSS so search, undo and selection keep working, and the open state is saved with the document.',
        'Built from plain Tiptap nodes, not a paid extension, so the HTML is readable without Monarch.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`ToggleBlock` renders `.mn-toggle-block[data-open]` > `__head` > `__trigger` (button with `__chevron` and `__summary`) and `__body` (with `id` for `aria-controls`).',
        '`ToggleBlockExtension` is the node `toggleBlock` (content `toggleSummary toggleContent`, attribute `open`) and adds `ToggleSummaryNode` and `ToggleContentNode`. HTML: `<div data-type="toggle-block" data-open="true"><div data-type="toggle-summary">…</div><div data-type="toggle-content">…</div></div>`.',
        'Command: `setToggleBlock()` turns the paragraph at the caret into a toggle.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`ToggleBlockExtension.configure({ messages })` translates the button names and the summary placeholder.',
        'Migrating from Dossier: `div.toggle-block[open]` with `div.toggle-summary` and `div.toggle-content` becomes the `data-type` form above; map `open` to `data-open`.',
      ],
    },
    variants: {
      items: [],
      notes: ['No variants. Open or closed is state, not a variant.'],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-open="true"',
          description: 'Content visible; chevron rotated.',
        },
        {
          name: 'Closed',
          attribute: 'data-open="false"',
          description: 'Only the summary is visible.',
        },
        {
          name: 'Empty summary (editor)',
          attribute: 'data-empty-summary',
          description: 'Shows `messages.summaryPlaceholder` in the summary line.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the button (or the summary, for the React component) toggles the content.',
        'In an editor the summary is editable text; the chevron button toggles without moving the caret.',
        'Enter at the summary opens the toggle and moves the caret to its first block.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space (button)', action: 'Shows or hides the content.' },
        { keys: 'Enter (in the summary, editor)', action: 'Opens the toggle and moves into it.' },
      ],
    },
    accessibility: {
      role: 'button with aria-expanded (APG disclosure)',
      name: 'The summary. In the editor: "Hide section: <summary>" / "Show section: <summary>" from `messages`.',
      items: [
        '`aria-controls` points at the content in the React component.',
        'The chevron is decorative and mirrors in right-to-left layouts.',
      ],
    },
    responsive: {
      items: ['The summary wraps; the button grows to 44px on coarse pointers.'],
    },
    theme: {
      items: [
        '`--mn-color-bg-muted` hover, `--mn-color-focus-ring`, `--mn-color-text-subtle` placeholder.',
        'The chevron rotation is removed under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'On a page',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <ToggleBlock summary={fixture.data.summary}>
                <p>{fixture.data.body}</p>
              </ToggleBlock>
            </div>
          ),
        },
        {
          title: 'In an editor',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <RichTextEditor
                aria-label="Help article"
                extensions={EXTENSIONS}
                defaultValue='<div data-type="toggle-block" data-open="true"><div data-type="toggle-summary">Before you start</div><div data-type="toggle-content"><p>Export your current settings.</p></div></div>'
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: ['Write the summary as the question or topic the content answers.'],
      dont: ['Do not hide information everyone needs, such as a required step.'],
    },
    related: {
      items: [
        { id: 'accordion', reason: 'A set of panels in app UI.' },
        { id: 'collapsible', reason: 'Show and hide app chrome.' },
        { id: 'block-editor', reason: 'Offers the toggle in its slash menu.' },
      ],
    },
    dependencies: {
      packages: ['react', '@tiptap/core', '@tiptap/pm', '@tiptap/react'],
      components: [],
    },
    testing: {
      unit: [
        'Disclosure button named by the summary, `aria-expanded`, `aria-controls`, Enter and Space, controlled mode.',
        '`setToggleBlock` and HTML round-trip; collapsed content kept in the document.',
        'Node-view button names and state; Enter in the summary opens and moves into the content; placeholder.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
