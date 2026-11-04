import { createRef, useState } from 'react';
import { renderToString } from 'react-dom/server';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import '../../../../test/prosemirror.js';
import type { RichTextEditorHandle } from '../rich-text-editor/index.js';
import { MessageComposer, type MessageComposerMessage } from './message-composer.js';

const ready = () => screen.findByRole('textbox');

async function typeInto(user: ReturnType<typeof userEvent.setup>, text: string) {
  const box = await ready();
  await user.click(box);
  await user.keyboard(text);
  return box;
}

afterEach(() => vi.useRealTimers());

describe('MessageComposer', () => {
  it('sends on Enter, adds a line on Shift+Enter and clears after sending', async () => {
    const onSend = vi.fn();
    const user = userEvent.setup();
    render(<MessageComposer label="Message #design" onSend={onSend} />);
    const box = await typeInto(user, 'Hello{Shift>}{Enter}{/Shift}there');
    expect(onSend).not.toHaveBeenCalled();
    await user.keyboard('{Enter}');
    expect(onSend).toHaveBeenCalledTimes(1);
    const msg = onSend.mock.calls[0]![0] as MessageComposerMessage;
    expect(msg.html).toBe('<p>Hello<br>there</p>');
    expect(msg.text).toBe('Hello\nthere');
    expect(msg.json).toMatchObject({ type: 'doc' });
    expect(box).toHaveTextContent('');
  });

  it('sends with Mod+Enter only when sendOn is mod-enter', async () => {
    const onSend = vi.fn();
    const user = userEvent.setup();
    render(<MessageComposer label="Reply" sendOn="mod-enter" onSend={onSend} />);
    await typeInto(user, 'one{Enter}two');
    expect(onSend).not.toHaveBeenCalled();
    expect(screen.getByText(/\+Enter to send/)).toBeInTheDocument();
    await user.keyboard('{Control>}{Enter}{/Control}');
    expect(onSend).toHaveBeenCalledWith(expect.objectContaining({ html: '<p>one</p><p>two</p>' }));
  });

  it('keeps Enter as a new line inside a code block', async () => {
    const onSend = vi.fn();
    const ref = createRef<RichTextEditorHandle>();
    const user = userEvent.setup();
    render(<MessageComposer label="Message" onSend={onSend} editorRef={ref} />);
    await typeInto(user, '```');
    await user.keyboard(' const a = 1;{Enter}');
    expect(onSend).not.toHaveBeenCalled();
    expect(ref.current?.editor?.isActive('codeBlock')).toBe(true);
  });

  it('disables Send while empty, and shows a pending state for a promise', async () => {
    let resolve!: () => void;
    const onSend = vi.fn(() => new Promise<void>((r) => (resolve = r)));
    const user = userEvent.setup();
    render(<MessageComposer label="Message" onSend={onSend} />);
    await ready();
    const send = screen.getByRole('button', { name: 'Send' });
    expect(send).toBeDisabled();
    await typeInto(user, 'Ship it');
    expect(send).toBeEnabled();
    await user.click(send);
    const busy = screen.getByRole('button', { name: /Sending/ });
    expect(busy).toHaveAttribute('aria-busy', 'true');
    // A second Enter while pending does not send again.
    await user.keyboard('{Enter}');
    expect(onSend).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('textbox')).toHaveTextContent('Ship it');
    await act(async () => resolve());
    await waitFor(() => expect(screen.getByRole('textbox')).toHaveTextContent(''));
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
  });

  it('keeps the text when sending fails', async () => {
    const onSend = vi.fn(() => Promise.reject(new Error('offline')));
    const user = userEvent.setup();
    render(<MessageComposer label="Message" onSend={onSend} />);
    await typeInto(user, 'Draft{Enter}');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Send' })).toBeEnabled());
    expect(screen.getByRole('textbox')).toHaveTextContent('Draft');
  });

  it('allows sending only attachments when hasAttachments is set', async () => {
    const onSend = vi.fn();
    const user = userEvent.setup();
    render(
      <MessageComposer
        label="Message"
        onSend={onSend}
        hasAttachments
        attachments={<span>brief.pdf</span>}
      />,
    );
    await ready();
    expect(screen.getByText('brief.pdf')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Send' }));
    expect(onSend).toHaveBeenCalledWith(expect.objectContaining({ html: '', json: null }));
  });

  it('hands picked and pasted files to onFiles through the attach button', async () => {
    const onFiles = vi.fn();
    const user = userEvent.setup();
    const { container } = render(
      <MessageComposer label="Message" onSend={vi.fn()} onFiles={onFiles} fileAccept="image/*" />,
    );
    await ready();
    const attach = screen.getByRole('button', { name: 'Attach files' });
    const input = container.querySelector<HTMLInputElement>(
      '.mn-message-composer > input[type="file"]',
    )!;
    expect(input).toHaveAttribute('accept', 'image/*');
    const click = vi.spyOn(input, 'click');
    await user.click(attach);
    expect(click).toHaveBeenCalled();
    await user.upload(input, new File(['x'], 'photo.png', { type: 'image/png' }));
    expect(onFiles).toHaveBeenCalledWith([expect.any(File)], 'picker');
  });

  it('inserts an emoji from the product picker slot', async () => {
    const user = userEvent.setup();
    const ref = createRef<RichTextEditorHandle>();
    render(
      <MessageComposer
        label="Message"
        onSend={vi.fn()}
        editorRef={ref}
        emoji={(insert) => (
          <button type="button" onClick={() => insert('🎉')}>
            Emoji
          </button>
        )}
      />,
    );
    await ready();
    await user.click(screen.getByRole('button', { name: 'Emoji' }));
    expect(ref.current?.getText()).toBe('🎉');
  });

  it('debounces draft persistence and clears the draft after sending', async () => {
    const onDraftChange = vi.fn();
    const user = userEvent.setup();
    render(
      <MessageComposer
        label="Message"
        onSend={vi.fn()}
        onDraftChange={onDraftChange}
        draftDebounceMs={400}
        draftStatus="saved"
      />,
    );
    await typeInto(user, 'Hi');
    expect(onDraftChange).not.toHaveBeenCalled();
    await waitFor(() => expect(onDraftChange).toHaveBeenLastCalledWith('<p>Hi</p>'), {
      timeout: 3000,
    });
    // Debounced: one save for the burst of keystrokes, not one per key.
    expect(onDraftChange).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('status')).toHaveTextContent('Draft saved');
    await user.keyboard('{Enter}');
    expect(onDraftChange).toHaveBeenLastCalledWith('');
  });

  it('fires typing callbacks at most once per interval and stops on send', async () => {
    const onTyping = vi.fn();
    const onTypingStop = vi.fn();
    const user = userEvent.setup();
    render(
      <MessageComposer
        label="Message"
        onSend={vi.fn()}
        onTyping={onTyping}
        onTypingStop={onTypingStop}
        typingIntervalMs={60_000}
      />,
    );
    await typeInto(user, 'abc');
    expect(onTyping).toHaveBeenCalledTimes(1);
    await user.keyboard('{Enter}');
    expect(onTypingStop).toHaveBeenCalledTimes(1);
  });

  it('edits an existing message: Save label, Escape cancels', async () => {
    const onSend = vi.fn();
    const onCancelEdit = vi.fn();
    const user = userEvent.setup();
    render(
      <MessageComposer
        label="Edit message"
        editing
        defaultValue="<p>Old text</p>"
        onSend={onSend}
        onCancelEdit={onCancelEdit}
      />,
    );
    const box = await ready();
    await waitFor(() => expect(box).toHaveFocus());
    expect(screen.getByText('Editing message')).toBeInTheDocument();
    expect(box).toHaveAccessibleDescription('Enter to save, Escape to cancel');
    expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();
    await user.keyboard('{Escape}');
    expect(onCancelEdit).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancelEdit).toHaveBeenCalledTimes(2);
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSend).toHaveBeenCalledWith(expect.objectContaining({ html: '<p>Old text</p>' }));
  });

  it('shows a reply-to preview that can be dismissed', async () => {
    const onDismissReply = vi.fn();
    const user = userEvent.setup();
    render(
      <MessageComposer
        label="Message"
        onSend={vi.fn()}
        replyTo={{ author: 'Ana Silva', excerpt: 'Can we move the review?' }}
        onDismissReply={onDismissReply}
      />,
    );
    await ready();
    expect(screen.getByText('Replying to Ana Silva')).toBeInTheDocument();
    expect(screen.getByText('Can we move the review?')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Stop replying' }));
    expect(onDismissReply).toHaveBeenCalled();
  });

  it('calls onEditLast on Arrow Up in an empty box', async () => {
    const onEditLast = vi.fn();
    const user = userEvent.setup();
    render(<MessageComposer label="Message" onSend={vi.fn()} onEditLast={onEditLast} />);
    await typeInto(user, '{ArrowUp}');
    expect(onEditLast).toHaveBeenCalledTimes(1);
  });

  it('is disabled with a reason linked to the text box', async () => {
    render(
      <MessageComposer
        label="Message"
        onSend={vi.fn()}
        disabled
        disabledReason="Only admins can post in this channel."
      />,
    );
    const box = await ready();
    const reason = screen.getByText('Only admins can post in this channel.');
    expect(box).toHaveAttribute('aria-disabled', 'true');
    expect(box.getAttribute('aria-describedby')).toContain(reason.id);
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
  });

  it('switches between product-supplied modes (public reply / internal note)', async () => {
    const onSend = vi.fn();
    const onModeChange = vi.fn();
    const user = userEvent.setup();
    const { container } = render(
      <MessageComposer
        label="Reply"
        onSend={onSend}
        onModeChange={onModeChange}
        modes={[
          { value: 'public', label: 'Reply', placeholder: 'Reply to the customer' },
          {
            value: 'note',
            label: 'Internal note',
            tone: 'note',
            placeholder: 'Only your team sees notes',
            sendLabel: 'Add note',
          },
        ]}
      />,
    );
    const box = await ready();
    expect(screen.getByRole('radiogroup', { name: 'Message type' })).toBeInTheDocument();
    expect(box.querySelector('p')).toHaveAttribute('data-placeholder', 'Reply to the customer');
    await user.click(screen.getByRole('radio', { name: 'Internal note' }));
    expect(onModeChange).toHaveBeenCalledWith('note');
    expect(container.firstElementChild).toHaveAttribute('data-tone', 'note');
    await waitFor(() =>
      expect(screen.getByRole('textbox').querySelector('p')).toHaveAttribute(
        'data-placeholder',
        'Only your team sees notes',
      ),
    );
    await typeInto(user, 'Refund approved');
    await user.click(screen.getByRole('button', { name: 'Add note' }));
    expect(onSend).toHaveBeenCalledWith(expect.objectContaining({ mode: 'note' }));
  });

  it('works controlled', async () => {
    function Controlled() {
      const [value, setValue] = useState('<p>Restored draft</p>');
      return (
        <>
          <MessageComposer
            label="Message"
            value={value}
            onValueChange={setValue}
            onSend={vi.fn()}
          />
          <output data-testid="v">{value}</output>
        </>
      );
    }
    const user = userEvent.setup();
    render(<Controlled />);
    const box = await ready();
    expect(box).toHaveTextContent('Restored draft');
    await user.click(box);
    await user.keyboard('{Enter}');
    await waitFor(() => expect(screen.getByTestId('v')).toHaveTextContent(''));
  });

  it('has no axe violations', async () => {
    const { container } = render(
      <MessageComposer
        label="Reply"
        onSend={vi.fn()}
        onFiles={vi.fn()}
        replyTo={{ author: 'Ana Silva', excerpt: 'Thanks!' }}
        onDismissReply={vi.fn()}
        modes={[
          { value: 'public', label: 'Reply' },
          { value: 'note', label: 'Internal note', tone: 'note' },
        ]}
        maxLength={500}
        draftStatus="saving"
      />,
    );
    await ready();
    await expectNoA11yViolations(container);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <MessageComposer label="Message" placeholder="Write a message" onSend={() => {}} />,
    );
    expect(html).toContain('mn-message-composer');
    expect(html).toContain('Write a message');
    expect(html).toContain('Send');
  });
});
