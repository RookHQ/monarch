import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { fileMatchesAccept } from '../../files/file-upload/index.js';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiPromptInput,
  findTrigger,
  usePromptDraft,
  type AiPromptInputProps,
  type AiPromptTriggerState,
} from './index.js';

const box = () => screen.getByRole('textbox', { name: 'Message the assistant' });

function Input(props: Partial<AiPromptInputProps>) {
  return <AiPromptInput label="Message the assistant" onSubmit={() => {}} {...props} />;
}

function file(name: string, type: string) {
  return new File(['x'], name, { type });
}

/** jsdom has no DataTransfer; this is the subset the input reads. */
function dataTransfer(files: File[], text = '') {
  return {
    files,
    types: files.length ? ['Files'] : ['text/plain'],
    getData: (format: string) => (format === 'text/plain' ? text : ''),
    dropEffect: 'none',
  };
}

afterEach(() => {
  try {
    window.sessionStorage.clear();
  } catch {
    /* ignore */
  }
});

describe('AiPromptInput', () => {
  it('has an accessible name, a send-key hint and no axe violations', async () => {
    render(<Input data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-ai-prompt-input');
    expect(box()).toHaveAccessibleDescription('Enter to send, Shift+Enter for a new line.');
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('sends on Enter, adds a line on Shift+Enter, and clears', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<Input onSubmit={onSubmit} />);
    await user.click(box());
    await user.keyboard('Hello{Shift>}{Enter}{/Shift}there');
    expect(onSubmit).not.toHaveBeenCalled();
    expect(box()).toHaveValue('Hello\nthere');
    await user.keyboard('{Enter}');
    expect(onSubmit).toHaveBeenCalledWith('Hello\nthere', { files: [] });
    expect(box()).toHaveValue('');
  });

  it('does not send empty or whitespace-only prompts', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<Input onSubmit={onSubmit} />);
    await user.click(box());
    await user.keyboard('   {Enter}');
    await user.click(screen.getByRole('button', { name: 'Send' }));
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('with submitOn="mod-enter", Enter adds a line and Ctrl/⌘+Enter sends', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<Input onSubmit={onSubmit} submitOn="mod-enter" />);
    await user.click(box());
    await user.keyboard('one{Enter}two');
    expect(onSubmit).not.toHaveBeenCalled();
    await user.keyboard('{Control>}{Enter}{/Control}');
    expect(onSubmit).toHaveBeenCalledWith('one\ntwo', { files: [] });
    await user.keyboard('three{Meta>}{Enter}{/Meta}');
    expect(onSubmit).toHaveBeenLastCalledWith('three', { files: [] });
  });

  it('never sends while an IME is composing, including Safari keyCode 229', () => {
    const onSubmit = vi.fn();
    render(<Input onSubmit={onSubmit} defaultValue="にほん" />);
    const el = box();
    fireEvent.compositionStart(el);
    fireEvent.keyDown(el, { key: 'Enter', isComposing: true });
    fireEvent.keyDown(el, { key: 'Enter' });
    fireEvent.compositionEnd(el);
    expect(onSubmit).not.toHaveBeenCalled();
    // Safari: the committing Enter arrives after compositionend with keyCode 229.
    fireEvent.keyDown(el, { key: 'Enter', keyCode: 229 });
    expect(onSubmit).not.toHaveBeenCalled();
    fireEvent.keyDown(el, { key: 'Enter', keyCode: 13 });
    expect(onSubmit).toHaveBeenCalledWith('にほん', { files: [] });
  });

  it('turns Send into Stop while busy; Stop and Escape call onStop', async () => {
    const onStop = vi.fn();
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(<Input onSubmit={onSubmit} onStop={onStop} status="idle" />);
    await user.click(box());
    await user.keyboard('Hi{Enter}');
    expect(onSubmit).toHaveBeenCalledTimes(1);
    rerender(<Input onSubmit={onSubmit} onStop={onStop} status="streaming" />);
    expect(screen.queryByRole('button', { name: 'Send' })).not.toBeInTheDocument();
    const stop = screen.getByRole('button', { name: 'Stop' });
    expect(box()).toHaveFocus();
    expect(box()).toHaveAccessibleDescription(
      'Enter to send, Shift+Enter for a new line. Escape stops the response.',
    );
    // Typing the next prompt is allowed; Enter does not send while busy.
    await user.keyboard('Next{Enter}');
    expect(onSubmit).toHaveBeenCalledTimes(1);
    await user.keyboard('{Escape}');
    expect(onStop).toHaveBeenCalledTimes(1);
    await user.click(stop);
    expect(onStop).toHaveBeenCalledTimes(2);
    expect(box()).toHaveFocus();
    rerender(<Input onSubmit={onSubmit} onStop={onStop} busy={false} status="streaming" />);
    expect(screen.getByRole('button', { name: 'Send' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('treats status "submitted" and the busy prop as busy', () => {
    const { rerender } = render(<Input status="submitted" />);
    expect(screen.getByRole('button', { name: 'Stop' })).toBeInTheDocument();
    rerender(<Input busy />);
    expect(screen.getByRole('button', { name: 'Stop' })).toBeInTheDocument();
    rerender(<Input status="awaiting-approval" />);
    expect(screen.getByRole('button', { name: 'Send' })).toBeInTheDocument();
  });

  it('shows the disabled reason and links it to the textarea', async () => {
    render(<Input disabled disabledReason="Connect a provider to start" />);
    expect(box()).toBeDisabled();
    expect(box()).toHaveAccessibleDescription('Connect a provider to start');
    expect(screen.getByText('Connect a provider to start')).toBeVisible();
    await expectNoA11yViolations();
  });

  it('works controlled', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    function Controlled() {
      const [value, setValue] = useState('Draft');
      return (
        <>
          <Input value={value} onValueChange={setValue} onSubmit={onSubmit} />
          <output>{value}</output>
        </>
      );
    }
    render(<Controlled />);
    await user.click(box());
    await user.keyboard(' more');
    expect(document.querySelector('output')).toHaveTextContent('Draft more');
    await user.keyboard('{Enter}');
    expect(onSubmit).toHaveBeenCalledWith('Draft more', { files: [] });
    expect(document.querySelector('output')).toHaveTextContent('');
  });

  it('restores the prompt when onSubmit rejects', async () => {
    const user = userEvent.setup();
    render(<Input onSubmit={() => Promise.reject(new Error('offline'))} />);
    await user.click(box());
    await user.keyboard('Keep me{Enter}');
    await waitFor(() => expect(box()).toHaveValue('Keep me'));
  });

  describe('attachments', () => {
    it('attaches pasted files and passes them to onSubmit', async () => {
      const onSubmit = vi.fn();
      const onAttach = vi.fn();
      render(<Input onSubmit={onSubmit} onAttach={onAttach} />);
      const image = file('chart.png', 'image/png');
      fireEvent.paste(box(), { clipboardData: dataTransfer([image]) });
      expect(onAttach).toHaveBeenCalledWith([image], 'paste');
      const list = screen.getByRole('list', { name: 'Attachments' });
      expect(list).toHaveTextContent('chart.png');
      expect(screen.getByRole('status')).toHaveTextContent('1 file attached');
      await expectNoA11yViolations();
      fireEvent.keyDown(box(), { key: 'Enter' });
      expect(onSubmit).toHaveBeenCalledWith('', { files: [image] });
      expect(screen.queryByRole('list', { name: 'Attachments' })).not.toBeInTheDocument();
    });

    it('ignores paste of plain text, and filters files by accept', () => {
      render(<Input accept="image/*" />);
      fireEvent.paste(box(), { clipboardData: dataTransfer([], 'hello') });
      fireEvent.paste(box(), {
        clipboardData: dataTransfer([file('notes.pdf', 'application/pdf')]),
      });
      expect(screen.queryByRole('list', { name: 'Attachments' })).not.toBeInTheDocument();
    });

    it('shows a drop state and attaches dropped files', () => {
      render(<Input onAttach={() => {}} data-testid="root" />);
      const root = screen.getByTestId('root');
      const report = file('report.csv', 'text/csv');
      fireEvent.dragEnter(root, { dataTransfer: dataTransfer([report]) });
      expect(root).toHaveAttribute('data-dragging', 'true');
      expect(screen.getByText('Drop files to attach')).toBeInTheDocument();
      fireEvent.drop(root, { dataTransfer: dataTransfer([report]) });
      expect(root).not.toHaveAttribute('data-dragging');
      expect(screen.getByRole('list', { name: 'Attachments' })).toHaveTextContent('report.csv');
      expect(box()).toHaveFocus();
    });

    it('lets onAttach reject files', () => {
      render(<Input onAttach={(files) => files.filter((f) => f.size > 100)} />);
      fireEvent.paste(box(), { clipboardData: dataTransfer([file('tiny.txt', 'text/plain')]) });
      expect(screen.queryByRole('list', { name: 'Attachments' })).not.toBeInTheDocument();
    });

    it('removes a chip and keeps focus in the list', async () => {
      const user = userEvent.setup();
      render(<Input onAttach={() => {}} />);
      fireEvent.paste(box(), {
        clipboardData: dataTransfer([file('a.txt', 'text/plain'), file('b.txt', 'text/plain')]),
      });
      await user.click(screen.getByRole('button', { name: 'Remove a.txt' }));
      expect(screen.getByRole('button', { name: 'Remove b.txt' })).toHaveFocus();
      expect(screen.getByRole('status')).toHaveTextContent('a.txt removed');
      await user.keyboard('{Enter}');
      expect(box()).toHaveFocus();
    });

    it('opens the picker from the attach button', async () => {
      const user = userEvent.setup();
      const onAttach = vi.fn();
      const { container } = render(<Input onAttach={onAttach} accept=".pdf" />);
      const input = container.querySelector<HTMLInputElement>('input[type="file"]')!;
      expect(input).toHaveAttribute('accept', '.pdf');
      const click = vi.spyOn(input, 'click');
      await user.click(screen.getByRole('button', { name: 'Attach files' }));
      expect(click).toHaveBeenCalled();
      await user.upload(input, file('brief.pdf', 'application/pdf'));
      expect(onAttach).toHaveBeenCalledWith([expect.any(File)], 'picker');
    });

    it('supports a custom renderAttachments', () => {
      render(
        <Input onAttach={() => {}} renderAttachments={(list) => <p>{list.length} in tray</p>} />,
      );
      fireEvent.paste(box(), { clipboardData: dataTransfer([file('a.txt', 'text/plain')]) });
      expect(screen.getByText('1 in tray')).toBeInTheDocument();
    });
  });

  describe('triggers', () => {
    it('finds a trigger at the start of a word only', () => {
      const t = [{ char: '/' }, { char: '@' }];
      expect(findTrigger('/sum', 4, t)).toMatchObject({ char: '/', query: 'sum', start: 0 });
      expect(findTrigger('hi @al', 6, t)).toMatchObject({ char: '@', query: 'al', start: 3 });
      expect(findTrigger('a/b', 3, t)).toBeNull();
      expect(findTrigger('mail@x', 6, t)).toBeNull();
      expect(findTrigger('/sum now', 8, t)).toBeNull();
    });

    it('reports the query, renders the menu, and inserts a choice', async () => {
      const user = userEvent.setup();
      const onTrigger = vi.fn();
      const onDismiss = vi.fn();
      let state: AiPromptTriggerState | null = null;
      render(
        <Input
          triggers={[{ char: '/', onTrigger, onDismiss }]}
          renderTriggerMenu={(s) => {
            state = s;
            return <div>Commands for “{s.query}”</div>;
          }}
        />,
      );
      await user.click(box());
      await user.keyboard('/');
      expect(onTrigger).toHaveBeenLastCalledWith('', expect.any(Object));
      await user.keyboard('su');
      expect(onTrigger).toHaveBeenLastCalledWith('su', expect.any(Object));
      expect(screen.getByText('Commands for “su”')).toBeInTheDocument();
      expect(state!.inputId).toBe(box().id);
      act(() => state!.insert('/summarize '));
      expect(box()).toHaveValue('/summarize ');
      expect(onDismiss).toHaveBeenCalled();
      expect(screen.queryByText(/Commands for/)).not.toBeInTheDocument();
    });

    it('closes on Escape before stopping, and on a space', async () => {
      const user = userEvent.setup();
      const onStop = vi.fn();
      const onDismiss = vi.fn();
      render(
        <Input
          busy
          onStop={onStop}
          triggers={[{ char: '@', onDismiss }]}
          renderTriggerMenu={() => <div>People</div>}
        />,
      );
      await user.click(box());
      await user.keyboard('@ma');
      expect(screen.getByText('People')).toBeInTheDocument();
      await user.keyboard('{Escape}');
      expect(screen.queryByText('People')).not.toBeInTheDocument();
      expect(onStop).not.toHaveBeenCalled();
      // Stays closed while still inside the same trigger.
      await user.keyboard('x');
      expect(screen.queryByText('People')).not.toBeInTheDocument();
      await user.keyboard(' @');
      expect(screen.getByText('People')).toBeInTheDocument();
      await user.keyboard(' ');
      expect(screen.queryByText('People')).not.toBeInTheDocument();
      expect(onDismiss).toHaveBeenCalledTimes(2);
      await user.keyboard('{Escape}');
      expect(onStop).toHaveBeenCalledTimes(1);
    });

    it("runs the caller's onKeyDown first so a menu can take Enter", async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();
      render(
        <Input
          onSubmit={onSubmit}
          textareaProps={{
            role: 'combobox',
            'aria-expanded': true,
            'aria-controls': 'menu',
            onKeyDown: (e) => {
              if (e.key === 'Enter') e.preventDefault();
            },
          }}
        />,
      );
      const el = screen.getByRole('combobox', { name: 'Message the assistant' });
      expect(el).toHaveAttribute('aria-controls', 'menu');
      await user.click(el);
      await user.keyboard('/x{Enter}');
      expect(onSubmit).not.toHaveBeenCalled();
    });
  });

  describe('counter', () => {
    it('shows the count, warns near the limit and announces only on crossing', async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();
      render(<Input maxLength={10} onSubmit={onSubmit} data-testid="root" />);
      const root = screen.getByTestId('root');
      const status = screen.getByRole('status');
      expect(screen.getByText('0 / 10')).toBeInTheDocument();
      expect(box()).toHaveAccessibleDescription(
        'Enter to send, Shift+Enter for a new line. 0 of 10 characters',
      );
      await user.click(box());
      await user.keyboard('12345678');
      expect(root).toHaveAttribute('data-limit', 'ok');
      expect(status).toHaveTextContent('');
      await user.keyboard('9');
      expect(root).toHaveAttribute('data-limit', 'warning');
      expect(status).toHaveTextContent('1 character left');
      await user.keyboard('0');
      expect(status).toHaveTextContent('1 character left');
      await user.keyboard('ab');
      expect(root).toHaveAttribute('data-limit', 'over');
      expect(status).toHaveTextContent('1 character over the limit');
      expect(box()).toHaveAttribute('aria-invalid', 'true');
      await user.keyboard('{Enter}');
      expect(onSubmit).not.toHaveBeenCalled();
      await user.keyboard('{Backspace}{Backspace}');
      expect(root).toHaveAttribute('data-limit', 'warning');
      await user.keyboard('{Enter}');
      expect(onSubmit).toHaveBeenCalledWith('1234567890', { files: [] });
    });

    it('shows a token estimate', async () => {
      const user = userEvent.setup();
      render(<Input countTokens={(t) => Math.ceil(t.length / 4)} />);
      expect(screen.getByText('≈ 0 tokens')).toBeInTheDocument();
      await user.click(box());
      await user.keyboard('abcdefgh');
      expect(screen.getByText('≈ 2 tokens')).toBeInTheDocument();
    });
  });

  it('renders toolbar and footer slots and a visible label', () => {
    render(
      <Input
        showLabel
        toolbar={<button type="button">Model</button>}
        footer="Answers can be wrong. Check important facts."
      />,
    );
    expect(screen.getByText('Message the assistant')).not.toHaveClass('mn-visually-hidden');
    expect(screen.getByRole('button', { name: 'Model' })).toBeInTheDocument();
    expect(screen.getByText('Answers can be wrong. Check important facts.')).toBeInTheDocument();
  });

  it('grows with its content up to maxRows', () => {
    render(<Input minRows={2} maxRows={4} defaultValue="one" />);
    const el = box();
    expect(el).toHaveAttribute('rows', '2');
    expect(el.style.height).not.toBe('');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiPromptInput label="Message" onSubmit={() => {}} maxLength={100} onAttach={() => {}} />,
    );
    expect(html).toContain('mn-ai-prompt-input');
    expect(html).toContain('0 / 100');
  });
});

describe('fileMatchesAccept', () => {
  it('matches extensions, wildcards and exact types', () => {
    expect(fileMatchesAccept(file('a.PDF', 'application/pdf'), '.pdf')).toBe(true);
    expect(fileMatchesAccept(file('a.png', 'image/png'), 'image/*')).toBe(true);
    expect(fileMatchesAccept(file('a.txt', 'text/plain'), 'image/*, .pdf')).toBe(false);
    expect(fileMatchesAccept(file('a.txt', 'text/plain'), undefined)).toBe(true);
  });
});

describe('usePromptDraft', () => {
  it('reads, writes and clears sessionStorage', () => {
    window.sessionStorage.setItem('mn-prompt-draft:chat-1', 'Saved text');
    const { result } = renderHook(() => usePromptDraft('chat-1'));
    expect(result.current[0]).toBe('Saved text');
    act(() => result.current[1]('Changed'));
    expect(result.current[0]).toBe('Changed');
    expect(window.sessionStorage.getItem('mn-prompt-draft:chat-1')).toBe('Changed');
    act(() => result.current[2]());
    expect(result.current[0]).toBe('');
    expect(window.sessionStorage.getItem('mn-prompt-draft:chat-1')).toBeNull();
  });

  it('survives storage errors and persists nothing without a key', () => {
    const spy = vi
      .spyOn(Object.getPrototypeOf(window.sessionStorage) as Storage, 'setItem')
      .mockImplementation(() => {
        throw new Error('QuotaExceededError');
      });
    const { result } = renderHook(() => usePromptDraft('chat-2'));
    act(() => result.current[1]('Still here'));
    expect(result.current[0]).toBe('Still here');
    spy.mockRestore();
    const none = renderHook(() => usePromptDraft(null));
    act(() => none.result.current[1]('x'));
    expect(window.sessionStorage.length).toBe(0);
  });

  it('drives the input as a controlled value and clears after sending', async () => {
    const user = userEvent.setup();
    function Drafted() {
      const [draft, setDraft] = usePromptDraft('chat-3');
      return <Input value={draft} onValueChange={setDraft} />;
    }
    const { unmount } = render(<Drafted />);
    await user.click(box());
    await user.keyboard('Half a thought');
    unmount();
    render(<Drafted />);
    expect(box()).toHaveValue('Half a thought');
    await user.click(box());
    await user.keyboard('{Enter}');
    expect(window.sessionStorage.getItem('mn-prompt-draft:chat-3')).toBeNull();
  });

  it('is SSR-safe', () => {
    function Drafted() {
      const [draft, setDraft] = usePromptDraft('chat-4');
      return <Input value={draft} onValueChange={setDraft} />;
    }
    expect(renderToString(<Drafted />)).toContain('mn-ai-prompt-input');
  });
});
