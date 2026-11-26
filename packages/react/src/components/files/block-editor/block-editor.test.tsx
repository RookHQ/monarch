import { createRef } from 'react';
import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Editor } from '@tiptap/core';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import { BlockEditor, type BlockEditorHandle } from './index.js';

async function setup(ui: React.ReactElement) {
  const user = userEvent.setup();
  let editor: Editor | null = null;
  const onReady = (e: Editor | null) => {
    editor = e;
  };
  const utils = render(
    <BlockEditor aria-label="Document" onEditorReady={onReady} {...(ui.props as object)} />,
  );
  const box = await screen.findByRole('textbox', { name: 'Document' });
  await waitFor(() => expect(editor).not.toBeNull());
  return { user, box, editor: () => editor as unknown as Editor, ...utils };
}

const typeSlash = async (
  user: ReturnType<typeof userEvent.setup>,
  box: HTMLElement,
  text: string,
) => {
  await user.click(box);
  await user.keyboard(text);
};

describe('BlockEditor', () => {
  it('renders a labelled document with a formatting toolbar and a placeholder', async () => {
    await setup(<BlockEditor aria-label="Document" />);
    expect(screen.getByRole('toolbar', { name: 'Formatting' })).toBeInTheDocument();
    expect(document.querySelector('[data-placeholder]')).toHaveAttribute(
      'data-placeholder',
      'Write, or type / for blocks',
    );
    await expectNoA11yViolations();
  });

  it('opens the slash menu on /, filters it, and inserts a heading with Enter', async () => {
    const onValueChange = vi.fn();
    const { user, box } = await setup(
      <BlockEditor aria-label="Document" onValueChange={onValueChange} />,
    );
    await typeSlash(user, box, '/head');
    const list = await screen.findByRole('listbox', { name: 'Insert block' });
    expect(
      within(list)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(expect.arrayContaining([expect.stringMatching(/^Heading 1/)]));
    expect(box).toHaveAttribute('aria-controls', list.id);
    expect(box.getAttribute('aria-activedescendant')).toMatch(/heading-1/);
    await user.keyboard('{ArrowDown}');
    expect(box.getAttribute('aria-activedescendant')).toMatch(/heading-2/);
    await expectNoA11yViolations();
    await user.keyboard('{Enter}');
    await waitFor(() => expect(screen.queryByRole('listbox')).toBeNull());
    await user.keyboard('Plan');
    expect(onValueChange).toHaveBeenLastCalledWith('<h2>Plan</h2>', expect.anything());
  });

  it('closes the slash menu with Escape and keeps the typed text', async () => {
    const { user, box, editor } = await setup(<BlockEditor aria-label="Document" />);
    await typeSlash(user, box, '/ta');
    await screen.findByRole('listbox');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('listbox')).toBeNull());
    expect(editor().getText()).toBe('/ta');
    expect(box).not.toHaveAttribute('aria-controls');
  });

  it('inserts every custom block from the slash menu', async () => {
    const { user, box, editor } = await setup(<BlockEditor aria-label="Document" format="json" />);
    const inserted: (string | undefined)[] = [];
    for (const name of ['Callout', 'Toggle', 'Two columns', 'Table', 'Embed']) {
      act(() => {
        editor().commands.setContent('<p></p>');
      });
      await user.click(box);
      act(() => {
        editor().commands.focus('start');
      });
      await user.keyboard('/');
      const list = await screen.findByRole('listbox');
      await user.click(within(list).getByRole('option', { name: new RegExp(`^${name}`) }));
      await waitFor(() => expect(screen.queryByRole('listbox')).toBeNull());
      inserted.push(editor().getJSON().content?.[0]?.type);
    }
    expect(inserted).toEqual(['callout', 'toggleBlock', 'columns', 'table', 'embed']);
    expect(box).toBeInTheDocument();
  });

  it('leaves out blocks that are not enabled', async () => {
    const { user, box } = await setup(<BlockEditor aria-label="Document" blocks={['callout']} />);
    await typeSlash(user, box, '/');
    const list = await screen.findByRole('listbox');
    expect(within(list).queryByRole('option', { name: /^Table/ })).toBeNull();
    expect(within(list).getByRole('option', { name: /^Callout/ })).toBeInTheDocument();
    expect(within(list).queryByRole('option', { name: /^Image/ })).toBeNull();
  });

  it('runs product slash items', async () => {
    const run = vi.fn((e: Editor) => e.commands.insertContent('Signed.'));
    const { user, box, editor } = await setup(
      <BlockEditor
        aria-label="Document"
        slashItems={[{ id: 'sign', label: 'Signature', group: 'Custom', run }]}
      />,
    );
    await typeSlash(user, box, '/sig{Enter}');
    expect(run).toHaveBeenCalled();
    expect(editor().getText()).toBe('Signed.');
  });

  it('moves blocks with Alt+Shift+arrows and from the block handle menu', async () => {
    const ref = createRef<BlockEditorHandle>();
    const { user, editor } = await setup(
      <BlockEditor
        aria-label="Document"
        editorRef={ref}
        defaultValue="<p>First</p><p>Second</p><p>Third</p>"
      />,
    );
    await user.click(screen.getByText('Second'));
    act(() => {
      editor().commands.setTextSelection(9);
    });
    await user.keyboard('{Alt>}{Shift>}{ArrowUp}{/Shift}{/Alt}');
    expect(ref.current?.getHTML()).toBe('<p>Second</p><p>First</p><p>Third</p>');
    await user.keyboard('{Alt>}{Shift>}{ArrowDown}{ArrowDown}{/Shift}{/Alt}');
    expect(ref.current?.getHTML()).toBe('<p>First</p><p>Third</p><p>Second</p>');
    const handle = await screen.findByRole('button', { name: 'Block actions: Paragraph' });
    await user.click(handle);
    await user.click(await screen.findByRole('menuitem', { name: 'Duplicate' }));
    expect(ref.current?.getHTML()).toBe('<p>First</p><p>Third</p><p>Second</p><p>Second</p>');
    await user.click(await screen.findByRole('button', { name: /^Block actions/ }));
    await user.click(await screen.findByRole('menuitem', { name: 'Delete block' }));
    await waitFor(() => expect(editor().state.doc.childCount).toBe(3));
    await user.click(screen.getByText('Third'));
    await user.click(await screen.findByRole('button', { name: /^Block actions/ }));
    expect(await screen.findByRole('menuitem', { name: 'Move up' })).toBeInTheDocument();
  });

  it('formats from the toolbar and emits JSON', async () => {
    const onValueChange = vi.fn();
    const { user, editor } = await setup(
      <BlockEditor
        aria-label="Document"
        format="json"
        defaultValue="<p>Hello</p>"
        onValueChange={onValueChange}
      />,
    );
    act(() => {
      editor().commands.selectAll();
    });
    await user.click(screen.getByRole('button', { name: /^Bold/ }));
    expect(onValueChange).toHaveBeenLastCalledWith(
      {
        type: 'doc',
        content: [
          {
            type: 'paragraph',
            content: [{ type: 'text', text: 'Hello', marks: [{ type: 'bold' }] }],
          },
        ],
      },
      expect.anything(),
    );
  });

  it('is read-only without toolbar, handles or slash menu', async () => {
    render(<BlockEditor aria-label="Document" readOnly defaultValue="<h2>Notes</h2>" />);
    expect(await screen.findByText('Notes')).toBeInTheDocument();
    expect(screen.queryByRole('toolbar')).toBeNull();
    expect(screen.queryByRole('button', { name: /^Block actions/ })).toBeNull();
    expect(document.querySelector('.mn-block-editor')).toHaveAttribute('data-readonly', 'true');
  });

  it('renders on the server', () => {
    const html = renderToString(<BlockEditor aria-label="Document" />);
    expect(html).toContain('mn-block-editor');
    expect(html).toContain('aria-busy="true"');
  });
});
