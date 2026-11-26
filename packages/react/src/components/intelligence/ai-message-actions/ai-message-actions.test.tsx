import { afterEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiMessageActions, type AiFeedback, type AiMessageActionsProps } from './index.js';

function All(props: Partial<AiMessageActionsProps>) {
  const [feedback, setFeedback] = useState<AiFeedback | undefined>(props.feedback);
  return (
    <AiMessageActions
      messageText="The answer."
      onCopy={() => {}}
      onRegenerate={() => {}}
      onShare={() => {}}
      feedback={feedback}
      onFeedback={setFeedback}
      {...props}
    />
  );
}

describe('AiMessageActions', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('is a labelled toolbar showing only the actions that have callbacks', async () => {
    render(<AiMessageActions data-testid="root" onRegenerate={() => {}} />);
    const bar = screen.getByRole('toolbar', { name: 'Message actions' });
    expect(bar).toHaveClass('mn-ai-message-actions');
    expect(screen.getAllByRole('button')).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Regenerate' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('is one tab stop with arrow keys, Home and End moving between buttons', async () => {
    render(
      <>
        <All />
        <button type="button">After</button>
      </>,
    );
    const user = userEvent.setup();
    await user.tab();
    const copy = screen.getByRole('button', { name: 'Copy' });
    expect(copy).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Regenerate' })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('button', { name: 'Share' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(copy).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('button', { name: 'Share' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(copy).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'After' })).toHaveFocus();
    await user.tab({ shift: true });
    expect(copy).toHaveFocus();
  });

  it('thumbs reflect feedback with aria-pressed and toggle off when pressed again', async () => {
    const onFeedback = vi.fn();
    const { rerender } = render(<AiMessageActions feedback="up" onFeedback={onFeedback} />);
    const up = screen.getByRole('button', { name: 'Good response' });
    const down = screen.getByRole('button', { name: 'Bad response' });
    expect(up).toHaveAttribute('aria-pressed', 'true');
    expect(down).toHaveAttribute('aria-pressed', 'false');
    const user = userEvent.setup();
    await user.click(up);
    expect(onFeedback).toHaveBeenLastCalledWith(undefined);
    await user.click(down);
    expect(onFeedback).toHaveBeenLastCalledWith('down');
    rerender(<AiMessageActions feedback="down" onFeedback={onFeedback} />);
    expect(screen.getByRole('button', { name: 'Bad response' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('copies the message text and confirms with "Copied"', async () => {
    const onCopy = vi.fn().mockResolvedValue(undefined);
    render(<AiMessageActions messageText="Hello there." onCopy={onCopy} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Copy' }));
    expect(onCopy).toHaveBeenCalledWith('Hello there.');
    expect(await screen.findByRole('button', { name: 'Copied' })).toBeInTheDocument();
  });

  it('reads aloud with speech synthesis and toggles to Stop', async () => {
    const speak = vi.fn();
    const cancel = vi.fn();
    class Utterance {
      text: string;
      onend: (() => void) | null = null;
      onerror: (() => void) | null = null;
      constructor(text: string) {
        this.text = text;
      }
    }
    vi.stubGlobal('speechSynthesis', { speak, cancel });
    vi.stubGlobal('SpeechSynthesisUtterance', Utterance);
    const onReadAloud = vi.fn();
    render(<AiMessageActions messageText="Read me." onReadAloud={onReadAloud} />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Read aloud' }));
    expect(speak).toHaveBeenCalledOnce();
    expect((speak.mock.calls[0]![0] as Utterance).text).toBe('Read me.');
    expect(onReadAloud).toHaveBeenLastCalledWith(true);
    await user.click(screen.getByRole('button', { name: 'Stop reading' }));
    expect(cancel).toHaveBeenCalled();
    expect(onReadAloud).toHaveBeenLastCalledWith(false);
    // Speech ending on its own flips the button back as well.
    await user.click(screen.getByRole('button', { name: 'Read aloud' }));
    act(() => (speak.mock.calls[1]![0] as Utterance).onend?.());
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Read aloud' })).toBeInTheDocument(),
    );
  });

  it('hides Read aloud when speech synthesis is missing, and includes the more slot', () => {
    render(
      <AiMessageActions
        onReadAloud={() => {}}
        onEdit={() => {}}
        more={<button type="button">More</button>}
      />,
    );
    expect(screen.queryByRole('button', { name: 'Read aloud' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'More' })).toHaveAttribute('tabindex', '-1');
  });

  it('takes translated labels', () => {
    render(
      <AiMessageActions onShare={() => {}} messages={{ share: 'Partager', toolbar: 'Actions' }} />,
    );
    expect(screen.getByRole('toolbar', { name: 'Actions' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Partager' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<All />)).toContain('mn-ai-message-actions');
  });
});
