import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Editor } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import {
  EDITOR_TOOLBAR_INLINE_ITEMS,
  EditorToolbar,
  type EditorToolbarItem,
  type EditorToolbarProps,
} from './index.js';

let current: Editor | null = null;

function Harness({
  content = '<p>Hello world</p>',
  withLink = true,
  ...props
}: Partial<EditorToolbarProps> & { content?: string; withLink?: boolean }) {
  const editor = useEditor({
    immediatelyRender: false,
    extensions: [
      // No trailing empty paragraph, so the HTML assertions see only the edits.
      StarterKit.configure({
        link: withLink ? { openOnClick: false } : false,
        trailingNode: false,
      }),
    ],
    content,
    editorProps: { attributes: { 'aria-label': 'Body', role: 'textbox' } },
  });
  current = editor;
  return (
    <div>
      <EditorToolbar editor={editor} {...props} />
      <EditorContent editor={editor} />
    </div>
  );
}

const ready = async () => {
  await screen.findByRole('textbox', { name: 'Body' });
  await waitFor(() => expect(current).not.toBeNull());
  return current as Editor;
};

const tools = () => Array.from(screen.getByRole('toolbar').querySelectorAll('button'));

describe('EditorToolbar', () => {
  it('renders a named toolbar with every default control and one Tab stop', async () => {
    render(<Harness />);
    await ready();
    const toolbar = screen.getByRole('toolbar', { name: 'Formatting' });
    expect(toolbar).toHaveAttribute('aria-orientation', 'horizontal');
    expect(tools().map((b) => b.getAttribute('aria-label'))).toEqual([
      'Undo',
      'Redo',
      'Text',
      'Heading 1',
      'Heading 2',
      'Heading 3',
      'Bold',
      'Italic',
      'Strikethrough',
      'Inline code',
      'Link',
      'Bulleted list',
      'Numbered list',
      'Quote',
      'Code block',
    ]);
    expect(tools().filter((b) => b.tabIndex === 0)).toHaveLength(1);
    expect(screen.getAllByRole('separator')).toHaveLength(4);
    await expectNoA11yViolations();
  });

  it('skips controls whose node or mark is not in the schema', async () => {
    render(<Harness withLink={false} items={['bold', 'link', 'taskList', '|', 'italic']} />);
    await ready();
    expect(tools().map((b) => b.getAttribute('aria-label'))).toEqual(['Bold', 'Italic']);
  });

  it('moves with arrow keys, Home and End, wrapping (roving tabindex)', async () => {
    const user = userEvent.setup();
    render(<Harness items={EDITOR_TOOLBAR_INLINE_ITEMS} />);
    await ready();
    const bold = screen.getByRole('button', { name: 'Bold' });
    act(() => bold.focus());
    await user.keyboard('{ArrowRight}');
    const italic = screen.getByRole('button', { name: 'Italic' });
    expect(italic).toHaveFocus();
    expect(italic.tabIndex).toBe(0);
    expect(bold.tabIndex).toBe(-1);
    await user.keyboard('{End}');
    expect(screen.getByRole('button', { name: 'Link' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(bold).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('button', { name: 'Link' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(bold).toHaveFocus();
  });

  it('reverses the arrow keys in a right-to-left layout', async () => {
    const user = userEvent.setup();
    render(
      <div dir="rtl" style={{ direction: 'rtl' }}>
        <Harness items={['bold', 'italic', 'strike']} style={{ direction: 'rtl' }} />
      </div>,
    );
    await ready();
    act(() => screen.getByRole('button', { name: 'Bold' }).focus());
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('button', { name: 'Italic' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Bold' })).toHaveFocus();
  });

  it('toggles marks and blocks and reflects the caret in aria-pressed', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const editor = await ready();
    act(() => {
      editor.commands.selectAll();
    });
    const bold = screen.getByRole('button', { name: 'Bold' });
    expect(bold).toHaveAttribute('aria-pressed', 'false');
    await user.click(bold);
    expect(bold).toHaveAttribute('aria-pressed', 'true');
    expect(editor.getHTML()).toBe('<p><strong>Hello world</strong></p>');
    await user.click(screen.getByRole('button', { name: 'Heading 2' }));
    expect(editor.getHTML()).toBe('<h2><strong>Hello world</strong></h2>');
    expect(screen.getByRole('button', { name: 'Heading 2' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    await user.click(screen.getByRole('button', { name: 'Text' }));
    expect(editor.getHTML()).toBe('<p><strong>Hello world</strong></p>');
    await user.click(screen.getByRole('button', { name: 'Bulleted list' }));
    expect(editor.getHTML()).toBe('<ul><li><p><strong>Hello world</strong></p></li></ul>');
  });

  it('disables undo until there is something to undo, then undoes and redoes', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const editor = await ready();
    const undo = screen.getByRole('button', { name: 'Undo' });
    expect(undo).toHaveAttribute('aria-disabled', 'true');
    await user.click(undo);
    act(() => {
      editor.chain().focus('end').insertContent('!').run();
    });
    expect(undo).not.toHaveAttribute('aria-disabled');
    await user.click(undo);
    expect(editor.getText()).toBe('Hello world');
    await user.click(screen.getByRole('button', { name: 'Redo' }));
    expect(editor.getText()).toBe('Hello world!');
  });

  it('runs custom items with their own pressed and disabled state', async () => {
    const user = userEvent.setup();
    const run = vi.fn();
    const items: EditorToolbarItem[] = [
      'bold',
      '|',
      {
        id: 'comment',
        label: 'Comment',
        icon: <span>C</span>,
        isDisabled: (ed) => ed.state.selection.empty,
        run,
      },
      { id: 'flag', label: 'Flag', icon: <span>F</span>, isActive: () => true, run },
    ];
    render(<Harness items={items} />);
    const editor = await ready();
    const comment = screen.getByRole('button', { name: 'Comment' });
    expect(comment).not.toHaveAttribute('aria-pressed');
    expect(comment).toHaveAttribute('data-tool', 'comment');
    expect(screen.getByRole('button', { name: 'Flag' })).toHaveAttribute('aria-pressed', 'true');
    act(() => {
      editor.commands.setTextSelection(1);
    });
    expect(comment).toHaveAttribute('aria-disabled', 'true');
    await user.click(comment);
    expect(run).not.toHaveBeenCalled();
    act(() => {
      editor.commands.setTextSelection({ from: 1, to: 6 });
    });
    await user.click(comment);
    expect(run).toHaveBeenCalledWith(editor);
  });

  it('applies a link from the popover, refusing unsafe addresses', async () => {
    const user = userEvent.setup();
    render(<Harness items={['link']} />);
    const editor = await ready();
    act(() => {
      editor.commands.setTextSelection({ from: 1, to: 6 });
    });
    await user.click(screen.getByRole('button', { name: 'Link' }));
    const input = await screen.findByLabelText('Link address');
    await user.type(input, 'javascript:alert(1){Enter}');
    expect(await screen.findByText(/Enter a web address/)).toBeInTheDocument();
    await user.clear(input);
    await user.type(input, 'example.com{Enter}');
    await waitFor(() => expect(screen.queryByLabelText('Link address')).toBeNull());
    expect(editor.getHTML()).toContain('href="https://example.com"');
    expect(screen.getByRole('button', { name: 'Link' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('can be opened from outside with linkOpen (a ⌘/Ctrl+K shortcut)', async () => {
    const onLinkOpenChange = vi.fn();
    const user = userEvent.setup();
    render(<Harness items={['link']} linkOpen onLinkOpenChange={onLinkOpenChange} />);
    await ready();
    const input = await screen.findByLabelText('Link address');
    await user.type(input, '{Escape}');
    expect(onLinkOpenChange).toHaveBeenLastCalledWith(false);
  });

  it('is inert but focusable when disabled, and translates through messages', async () => {
    const user = userEvent.setup();
    render(
      <Harness disabled items={['bold']} messages={{ label: 'Mise en forme', bold: 'Gras' }} />,
    );
    const editor = await ready();
    act(() => {
      editor.commands.selectAll();
    });
    const toolbar = screen.getByRole('toolbar', { name: 'Mise en forme' });
    expect(toolbar).toHaveAttribute('aria-disabled', 'true');
    const bold = screen.getByRole('button', { name: 'Gras' });
    expect(bold).toHaveAttribute('aria-disabled', 'true');
    await user.click(bold);
    expect(editor.getHTML()).toBe('<p>Hello world</p>');
  });

  it('shows shortcuts in the tooltip and aria-keyshortcuts after mount', async () => {
    render(<Harness items={['bold']} />);
    await ready();
    const bold = screen.getByRole('button', { name: 'Bold' });
    expect(bold.getAttribute('aria-keyshortcuts')).toMatch(/^(Meta|Control)\+B$/);
    expect(bold.title).toMatch(/^Bold \((⌘B|Ctrl\+B)\)$/);
  });

  it('renders inert controls on the server and before the editor loads', () => {
    const html = renderToString(<EditorToolbar editor={null} items={['bold', '|', 'italic']} />);
    expect(html).toContain('mn-editor-toolbar');
    expect(html).toContain('role="toolbar"');
    expect(html).toContain('aria-disabled="true"');
    expect(html).not.toContain('aria-keyshortcuts');
  });
});
