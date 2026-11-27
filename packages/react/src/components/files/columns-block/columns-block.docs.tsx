import { ColumnsBlock, ColumnsBlockExtension, RichTextEditor } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './columns-block.fixture.json';

const EXTENSIONS = [ColumnsBlockExtension];

export default {
  id: 'columns-block',
  title: 'ColumnsBlock',
  summary:
    'Two or three side-by-side columns in a document, with a keyboard-resizable split, as a component and a Tiptap node.',
  sections: {
    purpose: {
      summary:
        'Puts blocks side by side inside a document (a comparison, an image beside its notes) and stacks them on narrow screens. Needed by: Rook Dossier (Columns and Column in src/lib/tiptap/blocks.ts, src/components/document/nodeViews/ColumnsNodeView.tsx).',
      useWhen: [
        'Two or three short pieces of content read better next to each other.',
        'Authors should lay out a document themselves in an editor (`ColumnsBlockExtension`).',
      ],
      avoidWhen: [
        'Page layout around the document (sidebars, panels): use SplitLayout or the shell.',
        'Tabular data: use a table.',
      ],
    },
    designPrinciples: {
      items: [
        'One number for the split: the first column’s share (`ratio`, 0.2–0.8) is stored, so the columns can never disagree.',
        'Stacking is pure CSS (a container query), so the stored document is the same on every screen.',
        'Resizing is a window splitter with a keyboard path; a pointer drag is one undo step.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`ColumnsBlock` renders `.mn-columns-block[data-columns]` > `__grid` > `__column` per child. The ratio is the CSS variable `--mn-columns-block-ratio`.',
        '`ColumnsBlockExtension` is the node `columns` (content `column column column?`, attribute `ratio`) and adds `ColumnNode`. HTML: `<div data-type="columns" data-ratio="0.5"><div data-type="column">…</div>…</div>`.',
        'Commands: `insertColumns({ count: 2 | 3 })`, `setColumnsRatio(ratio)`. Helper: `clampColumnsRatio(value)`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`ColumnsBlockExtension.configure({ messages })` translates the handle name and value text.',
        'Migrating from Dossier: `div.doc-columns[data-ratio]` with `div.doc-column` children becomes the `data-type` form above; the ratio keeps its meaning.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'number of columns',
          values: ['2', '3'],
          description: 'Two columns can be resized; three are always equal.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Columns', attribute: 'data-columns', description: 'How many columns there are.' },
        {
          name: 'Editing',
          attribute: 'data-editor',
          description:
            'Node view in an editor: dashed column outlines on hover and a resize handle.',
        },
        {
          name: 'Stacked',
          description: 'Under a 30rem container the columns stack and the handle is hidden.',
        },
      ],
    },
    interaction: {
      items: [
        '`insertColumns` replaces an empty paragraph at the caret, or adds the columns after the current block, and puts the caret in the first column.',
        'Dragging the handle between two columns resizes them; the new ratio is saved on release.',
        'Each column holds any blocks, including lists and images.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the resize handle.' },
        {
          keys: 'ArrowLeft / ArrowRight',
          action: 'Narrows or widens the first column by 5% (mirrored in right-to-left).',
        },
        { keys: 'Home / End', action: 'Smallest (20%) or largest (80%) first column.' },
        { keys: 'Enter', action: 'Resets to equal columns.' },
      ],
    },
    accessibility: {
      role: 'separator (focusable, APG window splitter)',
      name: '`messages.resize` ("Resize columns"); `aria-valuenow` 20–80 and `aria-valuetext` "First column 55%".',
      items: [
        'Columns are read in source order, the same order they stack in.',
        'The handle is hidden when read-only and when the columns are stacked.',
      ],
    },
    responsive: {
      items: [
        'Columns stack into one under a 30rem container width (not the viewport), so they also stack in narrow side panels.',
        'The handle grows to 44px wide on coarse pointers.',
      ],
    },
    theme: {
      items: [
        '`--mn-color-border-default` and `--mn-color-accent-solid` for the handle, `--mn-color-border-subtle` for column outlines.',
        'Forced colours draw the handle in system colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'On a page',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 640 }}>
              <ColumnsBlock ratio={fixture.data.ratio}>
                <p>
                  <strong>{fixture.data.columns[0]?.lead}</strong>
                  {fixture.data.columns[0]?.text}
                </p>
                <p>
                  <strong>{fixture.data.columns[1]?.lead}</strong>
                  {fixture.data.columns[1]?.text}
                </p>
              </ColumnsBlock>
            </div>
          ),
        },
        {
          title: 'In an editor',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 640 }}>
              <RichTextEditor
                aria-label="Comparison"
                extensions={EXTENSIONS}
                defaultValue='<div data-type="columns" data-ratio="0.5"><div data-type="column"><p>Option A</p></div><div data-type="column"><p>Option B</p></div></div>'
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: ['Keep columns short and parallel.'],
      dont: ['Do not nest columns inside columns.', 'Do not use columns for page layout.'],
    },
    related: {
      items: [
        { id: 'split-layout', reason: 'Page-level panes.' },
        { id: 'table-block', reason: 'Rows and columns of data.' },
        { id: 'block-editor', reason: 'Offers columns in its slash menu.' },
      ],
    },
    dependencies: {
      packages: ['react', '@tiptap/core', '@tiptap/pm', '@tiptap/react'],
      components: [],
    },
    testing: {
      unit: [
        'Columns per child, ratio variable, ratio clamping.',
        '`insertColumns` replaces an empty paragraph or inserts after, caret in the first column; stored ratio clamped.',
        'Window splitter name, value text, Arrow, Home, End and Enter; hidden when read-only.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
