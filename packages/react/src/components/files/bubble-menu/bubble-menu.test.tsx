import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Editor } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import { BubbleMenu, defaultBubbleMenuShouldShow, type BubbleMenuProps } from './index.js';

let current: Editor | null = null;

function Harness({
  content = '<p>Hello world</p><pre><code>let a = 1</code></pre>',
  ...props
}: Partial<BubbleMenuProps> & { content?: string }) {
  const editor = useEditor({
    immediatelyRender: false,
    extensions: [StarterKit.configure({ link: { openOnClick: false } })],
    content,
    editorProps: { attributes: { 'aria-label': 'Body', role: 'textbox' } },
  });
  current = editor;
  return (
    <div>
      <EditorContent editor={editor} />
      <BubbleMenu editor={editor} {...props} />
    </div>
  );
}

const ready = async () => {
  await screen.findByRole('textbox', { name: 'Body' });
  await waitFor(() => expect(current).not.toBeNull());
  return current as Editor;
};

const select = (editor: Editor, from: number, to: number) =>
  act(() => {
    editor.chain().focus().setTextSelection({ from, to }).run();
  });

describe('BubbleMenu', () => {
  it('stays hidden until text is selected, then shows the inline toolbar without taking focus', async () => {
    render(<Harness />);
    const editor = await ready();
    act(() => {
      editor.commands.focus('start');
    });
    expect(screen.queryByRole('toolbar')).toBeNull();
    select(editor, 1, 6);
    const toolbar = await screen.findByRole('toolbar', { name: 'Formatting' });
    expect(toolbar.closest('.mn-bubble-menu')).not.toBeNull();
    expect(
      Array.from(toolbar.querySelectorAll('button')).map((b) => b.getAttribute('aria-label')),
    ).toEqual(['Bold', 'Italic', 'Strikethrough', 'Inline code', 'Link']);
    expect(screen.getByRole('textbox', { name: 'Body' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('hides when the selection collapses or the editor loses focus', async () => {
    render(
      <>
        <Harness />
        <button type="button">Elsewhere</button>
      </>,
    );
    const editor = await ready();
    select(editor, 1, 6);
    await screen.findByRole('toolbar');
    act(() => {
      editor.commands.setTextSelection(3);
    });
    expect(screen.queryByRole('toolbar')).toBeNull();
    select(editor, 1, 6);
    await screen.findByRole('toolbar');
    act(() => screen.getByRole('button', { name: 'Elsewhere' }).focus());
    await waitFor(() => expect(screen.queryByRole('toolbar')).toBeNull());
  });

  it('formats the selection from its buttons', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const editor = await ready();
    select(editor, 1, 6);
    await user.click(await screen.findByRole('button', { name: 'Bold' }));
    expect(editor.getHTML()).toContain('<strong>Hello</strong> world');
    expect(screen.getByRole('button', { name: 'Bold' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('moves focus in with Alt+F10 and back to the text with Escape, keeping the selection', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const editor = await ready();
    select(editor, 1, 6);
    await screen.findByRole('toolbar');
    await user.keyboard('{Alt>}{F10}{/Alt}');
    const bold = screen.getByRole('button', { name: 'Bold' });
    expect(bold).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Italic' })).toHaveFocus();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Body' })).toHaveFocus());
    expect(editor.state.selection.from).toBe(1);
    expect(editor.state.selection.to).toBe(6);
    expect(screen.getByRole('toolbar')).toBeInTheDocument();
  });

  it('Escape in the text dismisses it until the selection changes', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const editor = await ready();
    select(editor, 1, 6);
    await screen.findByRole('toolbar');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('toolbar')).toBeNull());
    select(editor, 1, 4);
    expect(await screen.findByRole('toolbar')).toBeInTheDocument();
  });

  it('does not show in a code block or when read-only', async () => {
    render(<Harness />);
    const editor = await ready();
    const codeStart = editor.state.doc.firstChild!.nodeSize + 1;
    select(editor, codeStart, codeStart + 3);
    expect(screen.queryByRole('toolbar')).toBeNull();
    expect(defaultBubbleMenuShouldShow(editor)).toBe(false);
    act(() => editor.setEditable(false));
    select(editor, 1, 6);
    expect(screen.queryByRole('toolbar')).toBeNull();
  });

  it('wraps custom children in a named group and honours shouldShow', async () => {
    const user = userEvent.setup();
    const onComment = vi.fn();
    const shouldShow = vi.fn((ed: Editor) => !ed.state.selection.empty);
    render(
      <Harness shouldShow={shouldShow} messages={{ label: 'Comment on selection' }}>
        <button type="button" onClick={onComment}>
          Comment
        </button>
      </Harness>,
    );
    const editor = await ready();
    select(editor, 1, 6);
    const group = await screen.findByRole('group', { name: 'Comment on selection' });
    expect(group).toHaveClass('mn-bubble-menu');
    await user.click(screen.getByRole('button', { name: 'Comment' }));
    expect(onComment).toHaveBeenCalled();
    expect(shouldShow).toHaveBeenCalledWith(editor);
  });

  it('opens the link form from the menu and keeps the menu while it is open', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const editor = await ready();
    select(editor, 1, 6);
    await user.click(await screen.findByRole('button', { name: 'Link' }));
    const input = await screen.findByLabelText('Link address');
    expect(input).toHaveFocus();
    expect(screen.getByRole('toolbar')).toBeInTheDocument();
    await user.type(input, 'example.com{Enter}');
    await waitFor(() => expect(editor.getHTML()).toContain('href="https://example.com"'));
  });

  it('renders nothing on the server', () => {
    expect(renderToString(<BubbleMenu editor={null} />)).toBe('');
  });
});
