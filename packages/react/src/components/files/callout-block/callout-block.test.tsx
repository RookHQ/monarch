import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import type { Editor } from '@tiptap/core';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import { CalloutBlock, CalloutBlockExtension } from './index.js';

// Tiptap 3's StarterKit adds an empty paragraph after a trailing block; the
// assertions below compare only what this extension writes.
const KIT = StarterKit.configure({ trailingNode: false });

let current: Editor | null = null;

function Harness({ content, editable = true }: { content: string; editable?: boolean }) {
  const editor = useEditor({
    immediatelyRender: false,
    editable,
    extensions: [
      KIT,
      CalloutBlockExtension.configure({ messages: { tones: { danger: 'Critical' } } }),
    ],
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

describe('CalloutBlock', () => {
  it('renders content with a tone and a decorative icon, and no live-region role', async () => {
    render(
      <CalloutBlock tone="warning" data-testid="c">
        <p>Back up the workspace before you import.</p>
      </CalloutBlock>,
    );
    const c = screen.getByTestId('c');
    expect(c).toHaveClass('mn-callout-block');
    expect(c).toHaveAttribute('data-tone', 'warning');
    expect(c).not.toHaveAttribute('role');
    expect(c.querySelector('.mn-callout-block__icon')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('falls back to neutral for an unknown tone and hides the icon with icon={null}', () => {
    render(
      // @ts-expect-error -- stored data can hold anything
      <CalloutBlock tone="purple" icon={null} data-testid="c">
        Text
      </CalloutBlock>,
    );
    const c = screen.getByTestId('c');
    expect(c).toHaveAttribute('data-tone', 'neutral');
    expect(c.querySelector('.mn-callout-block__icon')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<CalloutBlock tone="info">Server</CalloutBlock>);
    expect(html).toContain('mn-callout-block');
    expect(html).toContain('data-tone="info"');
  });
});

describe('CalloutBlockExtension', () => {
  it('wraps the current block with setCallout and round-trips through HTML', async () => {
    render(<Harness content="<p>Remember the deadline.</p>" />);
    const editor = await ready();
    act(() => {
      editor.commands.setTextSelection(3);
      editor.commands.setCallout({ tone: 'info' });
    });
    expect(editor.getHTML()).toBe(
      '<div data-tone="info" data-type="callout"><p>Remember the deadline.</p></div>',
    );
    act(() => {
      editor.commands.setCalloutTone('success');
    });
    expect(editor.getJSON().content?.[0]).toMatchObject({
      type: 'callout',
      attrs: { tone: 'success' },
    });
    act(() => {
      editor.commands.unsetCallout();
    });
    expect(editor.getHTML()).toBe('<p>Remember the deadline.</p>');
  });

  it('parses stored HTML and rejects unknown tones', async () => {
    render(
      <Harness content='<div data-type="callout" data-tone="javascript:x"><p>Stored</p></div>' />,
    );
    const editor = await ready();
    expect(editor.getJSON().content?.[0]).toMatchObject({
      type: 'callout',
      attrs: { tone: 'neutral' },
    });
  });

  it('offers a named tone picker (radio group) that changes the tone by keyboard', async () => {
    const user = userEvent.setup();
    render(<Harness content='<div data-type="callout" data-tone="neutral"><p>Note</p></div>' />);
    const editor = await ready();
    const group = await screen.findByRole('radiogroup', { name: 'Callout style' });
    const note = screen.getByRole('radio', { name: 'Note' });
    expect(note).toBeChecked();
    expect(screen.getByRole('radio', { name: 'Critical' })).toBeInTheDocument();
    note.focus();
    await user.keyboard('{ArrowRight}');
    await waitFor(() =>
      expect(editor.getJSON().content?.[0]).toMatchObject({ attrs: { tone: 'info' } }),
    );
    expect(group.closest('.mn-callout-block')).toHaveAttribute('data-tone', 'info');
    await expectNoA11yViolations();
  });

  it('shows no picker when the editor is read-only', async () => {
    render(
      <Harness
        editable={false}
        content='<div data-type="callout" data-tone="danger"><p>Read me</p></div>'
      />,
    );
    await waitFor(() => expect(current).not.toBeNull());
    await screen.findByText('Read me');
    expect(screen.queryByRole('radiogroup')).toBeNull();
    expect(document.querySelector('.mn-callout-block')).toHaveAttribute('data-tone', 'danger');
  });

  it('lifts the content out with Backspace at the start of the callout', async () => {
    const user = userEvent.setup();
    render(<Harness content='<div data-type="callout"><p>Lift</p></div>' />);
    const editor = await ready();
    await user.click(screen.getByText('Lift'));
    act(() => {
      editor.commands.setTextSelection(2);
    });
    await user.keyboard('{Backspace}');
    expect(editor.getHTML()).toBe('<p>Lift</p>');
  });
});
