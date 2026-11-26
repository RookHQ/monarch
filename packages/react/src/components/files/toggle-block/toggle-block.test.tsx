import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Editor } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import { ToggleBlock, ToggleBlockExtension } from './index.js';

// Tiptap 3's StarterKit adds an empty paragraph after a trailing block; the
// assertions below compare only what this extension writes.
const KIT = StarterKit.configure({ trailingNode: false });

let current: Editor | null = null;

function Harness({ content }: { content: string }) {
  const editor = useEditor({
    immediatelyRender: false,
    extensions: [KIT, ToggleBlockExtension],
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

describe('ToggleBlock', () => {
  it('is an APG disclosure: the summary names a button that shows and hides the content', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(
      <ToggleBlock summary="Release notes" onOpenChange={onOpenChange}>
        <p>Version 2 adds export.</p>
      </ToggleBlock>,
    );
    const button = screen.getByRole('button', { name: 'Release notes' });
    expect(button).toHaveAttribute('aria-expanded', 'false');
    const content = document.getElementById(button.getAttribute('aria-controls') ?? '');
    expect(content).not.toBeVisible();
    button.focus();
    await user.keyboard('{Enter}');
    expect(button).toHaveAttribute('aria-expanded', 'true');
    expect(content).toBeVisible();
    expect(onOpenChange).toHaveBeenLastCalledWith(true);
    await user.keyboard(' ');
    expect(button).toHaveAttribute('aria-expanded', 'false');
    await expectNoA11yViolations();
  });

  it('can be controlled', async () => {
    const user = userEvent.setup();
    render(
      <ToggleBlock summary="Fixed" open>
        Body
      </ToggleBlock>,
    );
    await user.click(screen.getByRole('button', { name: 'Fixed' }));
    expect(screen.getByRole('button', { name: 'Fixed' })).toHaveAttribute('aria-expanded', 'true');
  });

  it('renders on the server', () => {
    const html = renderToString(<ToggleBlock summary="Server">Body</ToggleBlock>);
    expect(html).toContain('mn-toggle-block');
    expect(html).toContain('aria-expanded="false"');
  });
});

describe('ToggleBlockExtension', () => {
  it('turns a paragraph into an open toggle and round-trips through HTML', async () => {
    render(<Harness content="<p>Details</p>" />);
    const editor = await ready();
    act(() => {
      editor.commands.setTextSelection(3);
      editor.commands.setToggleBlock();
    });
    expect(editor.getHTML()).toBe(
      '<div data-open="true" data-type="toggle-block"><div data-type="toggle-summary" class="mn-toggle-block__summary-text">Details</div><div data-type="toggle-content" class="mn-toggle-block__body"><p></p></div></div>',
    );
    // The caret lands at the end of the summary.
    expect(editor.state.selection.$from.parent.type.name).toBe('toggleSummary');
  });

  it('collapses with a named button and keeps the collapsed content in the document', async () => {
    const user = userEvent.setup();
    render(
      <Harness content='<div data-type="toggle-block" data-open="true"><div data-type="toggle-summary">FAQ</div><div data-type="toggle-content"><p>Answer</p></div></div>' />,
    );
    const editor = await ready();
    const button = await screen.findByRole('button', { name: 'Hide section: FAQ' });
    expect(button).toHaveAttribute('aria-expanded', 'true');
    await user.click(button);
    const collapsed = await screen.findByRole('button', { name: 'Show section: FAQ' });
    expect(collapsed).toHaveAttribute('aria-expanded', 'false');
    expect(editor.getHTML()).toContain('data-open="false"');
    expect(editor.getText()).toContain('Answer');
    await expectNoA11yViolations();
  });

  it('opens and moves into the content when Enter is pressed in the summary', async () => {
    const user = userEvent.setup();
    render(
      <Harness content='<div data-type="toggle-block" data-open="false"><div data-type="toggle-summary">Steps</div><div data-type="toggle-content"><p>One</p></div></div>' />,
    );
    const editor = await ready();
    await user.click(screen.getByText('Steps'));
    act(() => {
      editor.commands.setTextSelection(3);
    });
    await user.keyboard('{Enter}');
    expect(editor.getJSON().content?.[0]?.attrs).toEqual({ open: true });
    expect(editor.state.selection.$from.parent.textContent).toBe('One');
  });

  it('shows a placeholder for an empty summary', async () => {
    render(
      <Harness content='<div data-type="toggle-block"><div data-type="toggle-summary"></div><div data-type="toggle-content"><p>x</p></div></div>' />,
    );
    await ready();
    expect(await screen.findByText('Toggle title')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByRole('button', { name: 'Hide section' })).toBeInTheDocument();
  });
});
