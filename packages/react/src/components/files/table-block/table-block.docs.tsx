import { RichTextEditor, TableBlock, TableBlockExtension } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const EXTENSIONS = [TableBlockExtension];

export default {
  id: 'table-block',
  title: 'TableBlock',
  summary:
    'A table inside a document: a scrollable frame for static tables and a Tiptap table with Tab navigation and row and column controls.',
  sections: {
    purpose: {
      summary:
        'Lets authors put a small grid of text in a document and edit it with the keyboard, without extra dependencies. Needed by: Rook Dossier (uses @tiptap/extension-table, TableRow, TableHeader and TableCell in src/lib/tiptap/kit.ts).',
      useWhen: [
        'A document needs a few rows and columns of text: a comparison, a schedule, a glossary.',
        'Authors should create and edit tables in an editor (`TableBlockExtension`).',
      ],
      avoidWhen: [
        'Data from the app that people sort, filter or page: use DataTable or DataGrid.',
        'Side-by-side blocks of prose: use ColumnsBlock.',
      ],
    },
    designPrinciples: {
      items: [
        'Built on prosemirror-tables (shipped inside `@tiptap/pm`), so there is no extra package and the schema is the one Tiptap’s own table uses: `table`, `tableRow`, `tableHeader`, `tableCell`.',
        'Plain HTML out: `<table><tbody><tr><th scope="col">…` that any page can render and style.',
        'Wide tables scroll inside their column instead of widening the page.',
        'Controls appear only while the caret is in the table, so reading stays quiet.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`TableBlock` renders `.mn-table-block` > optional `__caption` and `__scroll` (a named, focusable region) around the `<table>`.',
        '`TableBlockExtension` adds `TableRowNode`, `TableHeaderNode` and `TableCellNode` and the prosemirror-tables editing plugin. Commands: `insertTable({ rows, cols, withHeaderRow })`, `addRowAfter`, `addColumnAfter`, `deleteRow`, `deleteColumn`, `deleteTable`, `toggleHeaderRow`, `goToNextCell(±1)`.',
        'Do not add `@tiptap/extension-table` to the same editor: the node and command names are the same.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`TableBlockExtension.configure({ messages })` translates the controls.',
        '`insertTable` clamps to 1–50 rows and 1–20 columns.',
        'Migrating from `@tiptap/extension-table`: the JSON and HTML are compatible; replace the four extensions with `TableBlockExtension`. Column resizing is not included.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'withHeaderRow (insertTable)',
          values: ['true', 'false'],
          description:
            'First row of `<th scope="col">` header cells, toggled later with the Header row control.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Active',
          attribute: 'data-active',
          description: 'The caret is in the table; the controls toolbar shows.',
        },
        {
          name: 'Header row',
          attribute: 'aria-pressed on "Header row"',
          description: 'The first row is header cells.',
        },
        {
          name: 'Cell selection',
          attribute: '.selectedCell',
          description: 'Several cells selected by dragging or Shift+arrows.',
        },
      ],
    },
    interaction: {
      items: [
        '`insertTable` replaces an empty paragraph or inserts after the current block, caret in the first cell.',
        'Tab moves to the next cell; in the last cell it adds a row. Shift+Tab moves back.',
        'Controls add a row below, add a column after, delete the row or column, toggle the header row, or delete the table.',
        'Dragging across cells selects them; Backspace clears the selected cells.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab (in a cell)',
          action: 'Next or previous cell; Tab in the last cell adds a row.',
        },
        { keys: 'Alt+F10 (in a cell)', action: 'Moves focus to the table controls.' },
        {
          keys: 'ArrowLeft / ArrowRight, Home / End (controls)',
          action: 'Moves between controls.',
        },
        { keys: 'Escape (controls)', action: 'Returns focus to the cell.' },
        { keys: 'Shift+Arrow keys', action: 'Extends a cell selection.' },
      ],
      notes: [
        'Tab is used for cells only inside a table; elsewhere it leaves the editor as usual.',
      ],
    },
    accessibility: {
      role: 'table; the controls are an APG toolbar; the static frame is a named region',
      name: 'Controls: `messages.toolbar` ("Table"). Static frame: `caption` or `label`.',
      items: [
        'Header cells carry `scope="col"`.',
        'The static scroll region is focusable so keyboard users can scroll a wide table.',
        'Control buttons have visible text; Delete table is styled as a danger action.',
      ],
    },
    responsive: {
      items: [
        'The table keeps its natural width and scrolls sideways inside its column at 390px.',
        'Controls wrap and grow to 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        '`--mn-color-border-default` cell borders, `--mn-color-bg-subtle` header cells, `--mn-color-accent-subtle` cell selection.',
        'Forced colours use Highlight for selected cells.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Static table',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <TableBlock caption="Support hours">
                <table>
                  <tbody>
                    <tr>
                      <th scope="col">Day</th>
                      <th scope="col">Hours</th>
                    </tr>
                    <tr>
                      <td>Monday to Friday</td>
                      <td>08:00–18:00</td>
                    </tr>
                    <tr>
                      <td>Saturday</td>
                      <td>10:00–14:00</td>
                    </tr>
                  </tbody>
                </table>
              </TableBlock>
            </div>
          ),
        },
        {
          title: 'In an editor',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <RichTextEditor
                aria-label="Schedule"
                extensions={EXTENSIONS}
                defaultValue="<table><tbody><tr><th><p>Week</p></th><th><p>Focus</p></th></tr><tr><td><p>1</p></td><td><p>Setup</p></td></tr></tbody></table>"
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give tables a header row.',
        'Keep document tables small; link to a data view for more.',
      ],
      dont: ['Do not use tables for layout.', 'Do not combine with `@tiptap/extension-table`.'],
    },
    related: {
      items: [
        { id: 'data-table', reason: 'App data with sorting and selection.' },
        { id: 'columns-block', reason: 'Prose side by side.' },
        { id: 'block-editor', reason: 'Offers tables in its slash menu.' },
      ],
    },
    dependencies: {
      packages: ['react', '@tiptap/core', '@tiptap/pm', '@tiptap/react'],
      components: [],
    },
    testing: {
      unit: [
        'Static frame: named focusable region and caption.',
        '`insertTable` output and caret; HTML round-trip with spans.',
        'Tab, Shift+Tab, Tab in the last cell adds a row.',
        'Toolbar shows only inside the table; add row and column, header row pressed state, delete table; roving tabindex; Alt+F10 and Escape; no controls when read-only.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
