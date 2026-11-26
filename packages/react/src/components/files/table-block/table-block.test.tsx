import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import type { Editor, JSONContent } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import { TableBlock, TableBlockExtension } from './index.js';

// Tiptap 3's StarterKit adds an empty paragraph after a trailing block; the
// assertions below compare only what this extension writes.
const KIT = StarterKit.configure({ trailingNode: false });

let current: Editor | null = null;

function Harness({ content, editable = true }: { content: string; editable?: boolean }) {
  const editor = useEditor({
    immediatelyRender: false,
    editable,
    extensions: [KIT, TableBlockExtension],
    content,
    editorProps: { attributes: { 'aria-label': 'Document', role: 'textbox' } },
  });
  current = editor;
  return <EditorContent editor={editor} />;
}

const ready = async () => {
  await waitFor(() => expect(current).not.toBeNull());
  await screen.findByRole('textbox', { name: 'Document' });
  return current as Editor;
};

const cellText = (editor: Editor) => editor.state.selection.$from.parent.textContent;
const rows = (editor: Editor): JSONContent[] =>
  editor.getJSON().content?.find((n) => n.type === 'table')?.content ?? [];

const SMALL =
  '<table><tbody><tr><th><p>Name</p></th><th><p>Role</p></th></tr><tr><td><p>Ana</p></td><td><p>Lead</p></td></tr></tbody></table>';

describe('TableBlock', () => {
  it('wraps a table in a named, keyboard-scrollable region with a caption', async () => {
    render(
      <TableBlock caption="Team">
        <table>
          <tbody>
            <tr>
              <th scope="col">Name</th>
            </tr>
            <tr>
              <td>Ana</td>
            </tr>
          </tbody>
        </table>
      </TableBlock>,
    );
    const region = screen.getByRole('region', { name: 'Team' });
    expect(region).toHaveAttribute('tabindex', '0');
    expect(screen.getByText('Team')).toHaveClass('mn-table-block__caption');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    expect(renderToString(<TableBlock label="Prices" />)).toContain('aria-label="Prices"');
  });
});

describe('TableBlockExtension', () => {
  it('inserts a table with a header row and puts the caret in the first cell', async () => {
    render(<Harness content="<p></p>" />);
    const editor = await ready();
    act(() => {
      editor.commands.insertTable({ rows: 2, cols: 2 });
    });
    expect(editor.getHTML()).toBe(
      '<table class="mn-table-block__table"><tbody><tr><th scope="col"><p></p></th><th scope="col"><p></p></th></tr><tr><td><p></p></td><td><p></p></td></tr></tbody></table>',
    );
    expect(editor.state.selection.$from.node(-1).type.name).toBe('tableHeader');
  });

  it('round-trips stored HTML, spans included', async () => {
    render(
      <Harness content='<table><tbody><tr><td colspan="2"><p>Wide</p></td></tr><tr><td><p>a</p></td><td><p>b</p></td></tr></tbody></table>' />,
    );
    const editor = await ready();
    expect(editor.getHTML()).toContain('<td colspan="2"><p>Wide</p></td>');
  });

  it('moves between cells with Tab and Shift+Tab, and Tab in the last cell adds a row', async () => {
    const user = userEvent.setup();
    render(<Harness content={SMALL} />);
    const editor = await ready();
    await user.click(screen.getByText('Name'));
    act(() => {
      editor.commands.setTextSelection(4);
    });
    expect(cellText(editor)).toBe('Name');
    await user.keyboard('{Tab}');
    expect(cellText(editor)).toBe('Role');
    await user.keyboard('{Shift>}{Tab}{/Shift}');
    expect(cellText(editor)).toBe('Name');
    await user.keyboard('{Tab}{Tab}{Tab}');
    expect(cellText(editor)).toBe('Lead');
    await user.keyboard('{Tab}');
    expect(rows(editor)).toHaveLength(3);
    expect(cellText(editor)).toBe('');
  });

  it('shows a named APG toolbar while the caret is in the table, and its controls edit it', async () => {
    const user = userEvent.setup();
    render(<Harness content={`<p>Intro</p>${SMALL}`} />);
    const editor = await ready();
    expect(screen.queryByRole('toolbar', { name: 'Table' })).toBeNull();
    await user.click(screen.getByText('Ana'));
    act(() => {
      editor.commands.setTextSelection(editor.getText().indexOf('Ana') + 10);
    });
    const toolbar = await screen.findByRole('toolbar', { name: 'Table' });
    const header = screen.getByRole('button', { name: 'Header row' });
    expect(header).toHaveAttribute('aria-pressed', 'true');
    // Before columns are added: an added header cell is empty until someone types in it.
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Add row below' }));
    expect(rows(editor)).toHaveLength(3);
    await user.click(screen.getByRole('button', { name: 'Add column after' }));
    expect(rows(editor)[0]?.content).toHaveLength(3);
    // Roving tabindex with arrows, Home and End.
    const tools = Array.from(toolbar.querySelectorAll('button'));
    expect(tools.filter((b) => b.tabIndex === 0)).toHaveLength(1);
    tools[0]?.focus();
    await user.keyboard('{End}');
    expect(screen.getByRole('button', { name: 'Delete table' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(tools[0]).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Delete table' }));
    expect(editor.getJSON().content?.some((n) => n.type === 'table')).toBe(false);
  });

  it('moves focus to the controls with Alt+F10 and back with Escape', async () => {
    const user = userEvent.setup();
    render(<Harness content={SMALL} />);
    const editor = await ready();
    await user.click(screen.getByText('Ana'));
    act(() => {
      editor.commands.setTextSelection(editor.state.doc.resolve(1).end() + 6);
    });
    await screen.findByRole('toolbar', { name: 'Table' });
    await user.keyboard('{Alt>}{F10}{/Alt}');
    expect(screen.getByRole('button', { name: 'Add row below' })).toHaveFocus();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.getByRole('textbox')).toHaveFocus());
  });

  it('has no controls when read-only', async () => {
    render(<Harness editable={false} content={SMALL} />);
    await waitFor(() => expect(current).not.toBeNull());
    await screen.findByText('Ana');
    expect(screen.queryByRole('toolbar')).toBeNull();
  });
});
