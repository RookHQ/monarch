import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import type { Editor } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import { ColumnsBlock, ColumnsBlockExtension, clampColumnsRatio } from './index.js';

// Tiptap 3's StarterKit adds an empty paragraph after a trailing block; the
// assertions below compare only what this extension writes.
const KIT = StarterKit.configure({ trailingNode: false });

let current: Editor | null = null;

function Harness({ content, editable = true }: { content: string; editable?: boolean }) {
  const editor = useEditor({
    immediatelyRender: false,
    editable,
    extensions: [KIT, ColumnsBlockExtension],
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

const TWO =
  '<div data-type="columns" data-ratio="0.5"><div data-type="column"><p>Left</p></div><div data-type="column"><p>Right</p></div></div>';

describe('ColumnsBlock', () => {
  it('wraps each child in a column and sets the ratio variable', async () => {
    render(
      <ColumnsBlock ratio={0.3} data-testid="c">
        <p>One</p>
        <p>Two</p>
      </ColumnsBlock>,
    );
    const c = screen.getByTestId('c');
    expect(c).toHaveAttribute('data-columns', '2');
    expect(c.style.getPropertyValue('--mn-columns-block-ratio')).toBe('0.3');
    expect(c.querySelectorAll('.mn-columns-block__column')).toHaveLength(2);
    await expectNoA11yViolations();
  });

  it('clamps ratios', () => {
    expect(clampColumnsRatio(0.05)).toBe(0.2);
    expect(clampColumnsRatio(2)).toBe(0.8);
    expect(clampColumnsRatio('abc')).toBe(0.5);
    expect(clampColumnsRatio('0.333333')).toBe(0.333);
  });

  it('renders on the server', () => {
    expect(
      renderToString(
        <ColumnsBlock>
          <p>a</p>
          <p>b</p>
          <p>c</p>
        </ColumnsBlock>,
      ),
    ).toContain('data-columns="3"');
  });
});

describe('ColumnsBlockExtension', () => {
  it('replaces an empty paragraph with columns and puts the caret in the first one', async () => {
    render(<Harness content="<p></p>" />);
    const editor = await ready();
    act(() => {
      editor.commands.insertColumns({ count: 3 });
    });
    expect(editor.getHTML()).toBe(
      '<div data-ratio="0.5" data-type="columns" class="mn-columns-block"><div data-type="column" class="mn-columns-block__column"><p></p></div><div data-type="column" class="mn-columns-block__column"><p></p></div><div data-type="column" class="mn-columns-block__column"><p></p></div></div>',
    );
    expect(editor.state.selection.$from.node(-1).type.name).toBe('column');
    expect(editor.state.selection.$from.index(1)).toBe(0);
  });

  it('inserts after a non-empty block and parses stored HTML with a clamped ratio', async () => {
    render(
      <Harness content='<p>Intro</p><div data-type="columns" data-ratio="9"><div data-type="column"><p>A</p></div><div data-type="column"><p>B</p></div></div>' />,
    );
    const editor = await ready();
    expect(editor.getJSON().content?.[1]).toMatchObject({ type: 'columns', attrs: { ratio: 0.8 } });
    act(() => {
      editor.commands.setTextSelection(2);
      editor.commands.insertColumns();
    });
    expect(editor.getJSON().content?.map((n) => n.type)).toEqual([
      'paragraph',
      'columns',
      'columns',
    ]);
  });

  it('resizes two columns from a keyboard-operable window splitter', async () => {
    const user = userEvent.setup();
    render(<Harness content={TWO} />);
    const editor = await ready();
    const handle = await screen.findByRole('separator', { name: 'Resize columns' });
    expect(handle).toHaveAttribute('aria-valuenow', '50');
    expect(handle).toHaveAttribute('aria-valuetext', 'First column 50%');
    handle.focus();
    await user.keyboard('{ArrowRight}');
    await waitFor(() => expect(editor.getJSON().content?.[0]?.attrs).toEqual({ ratio: 0.55 }));
    await user.keyboard('{End}');
    await waitFor(() =>
      expect(screen.getByRole('separator')).toHaveAttribute('aria-valuenow', '80'),
    );
    await user.keyboard('{Home}');
    await waitFor(() =>
      expect(screen.getByRole('separator')).toHaveAttribute('aria-valuenow', '20'),
    );
    await user.keyboard('{Enter}');
    await waitFor(() =>
      expect(screen.getByRole('separator')).toHaveAttribute('aria-valuenow', '50'),
    );
    await expectNoA11yViolations();
  });

  it('has no handle when read-only or with three columns', async () => {
    render(<Harness editable={false} content={TWO} />);
    await waitFor(() => expect(current).not.toBeNull());
    await screen.findByText('Left');
    expect(screen.queryByRole('separator')).toBeNull();
  });
});
