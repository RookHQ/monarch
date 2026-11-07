import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiConversationHeader } from './index.js';

function Controlled({ onRename }: { onRename?: (t: string) => void | Promise<void> }) {
  const [title, setTitle] = useState('Quarterly plan review');
  return (
    <AiConversationHeader
      title={title}
      onRename={async (next) => {
        await onRename?.(next);
        setTitle(next);
      }}
    />
  );
}

describe('AiConversationHeader', () => {
  it('renders the title, agent, model and actions', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiConversationHeader
        ref={ref}
        data-testid="root"
        className="extra"
        title="Quarterly plan review"
        agent={{ name: 'Assistant' }}
        model="Balanced model"
        actions={<button type="button">Share</button>}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-conversation-header', 'extra');
    expect(ref.current).toBe(root);
    expect(
      screen.getByRole('heading', { level: 2, name: 'Quarterly plan review' }),
    ).toBeInTheDocument();
    expect(root).toHaveTextContent('AI assistant: Assistant');
    expect(root).toHaveTextContent('Model: Balanced model');
    expect(screen.getByRole('group', { name: 'Conversation actions' })).toContainElement(
      screen.getByRole('button', { name: 'Share' }),
    );
    expect(screen.queryByRole('button', { name: 'Rename conversation' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('shows "New conversation" for an empty title and a busy placeholder while naming', () => {
    const { rerender } = render(<AiConversationHeader title="" onRename={() => {}} />);
    expect(screen.getByRole('heading', { name: 'New conversation' })).toBeInTheDocument();
    rerender(<AiConversationHeader title="" naming onRename={() => {}} />);
    const heading = screen.getByRole('heading', { name: 'Naming conversation…' });
    expect(heading).toHaveAttribute('aria-busy', 'true');
    expect(screen.queryByRole('button', { name: 'Rename conversation' })).toBeNull();
  });

  it('renames with Enter and returns focus to the Rename button', async () => {
    const onRename = vi.fn();
    const user = userEvent.setup();
    render(<Controlled onRename={onRename} />);
    await user.click(screen.getByRole('button', { name: 'Rename conversation' }));
    const input = screen.getByRole('textbox', { name: 'Conversation title' });
    expect(input).toHaveFocus();
    expect(input).toHaveValue('Quarterly plan review');
    expect(input).toHaveAccessibleDescription('Enter to save, Escape to cancel.');
    await user.clear(input);
    await user.type(input, '  Q3 plan  {Enter}');
    expect(onRename).toHaveBeenCalledWith('Q3 plan');
    expect(await screen.findByRole('heading', { name: 'Q3 plan' })).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Rename conversation' })).toHaveFocus(),
    );
  });

  it('cancels with Escape without calling onRename', async () => {
    const onRename = vi.fn();
    const user = userEvent.setup();
    render(<AiConversationHeader title="Plan" onRename={onRename} />);
    await user.click(screen.getByRole('button', { name: 'Rename conversation' }));
    await user.keyboard('Other{Escape}');
    expect(onRename).not.toHaveBeenCalled();
    expect(screen.getByRole('heading', { name: 'Plan' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Rename conversation' })).toHaveFocus();
  });

  it('does not rename to an empty or unchanged title, and saves on blur', async () => {
    const onRename = vi.fn();
    const user = userEvent.setup();
    render(
      <>
        <AiConversationHeader title="Plan" onRename={onRename} />
        <button type="button">Elsewhere</button>
      </>,
    );
    await user.click(screen.getByRole('button', { name: 'Rename conversation' }));
    await user.keyboard('{Enter}');
    await user.click(screen.getByRole('button', { name: 'Rename conversation' }));
    await user.clear(screen.getByRole('textbox'));
    await user.keyboard('{Enter}');
    expect(onRename).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Rename conversation' }));
    await user.type(screen.getByRole('textbox'), ' notes');
    await user.click(screen.getByRole('button', { name: 'Elsewhere' }));
    expect(onRename).toHaveBeenCalledWith('Plan notes');
  });

  it('keeps the editor open with an error when the rename fails', async () => {
    const user = userEvent.setup();
    render(
      <AiConversationHeader title="Plan" onRename={() => Promise.reject(new Error('nope'))} />,
    );
    await user.click(screen.getByRole('button', { name: 'Rename conversation' }));
    await user.type(screen.getByRole('textbox'), '!{Enter}');
    expect(await screen.findByRole('alert')).toHaveTextContent('Couldn’t rename. Try again.');
    const input = screen.getByRole('textbox', { name: 'Conversation title' });
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveValue('Plan!');
  });

  it('supports heading levels and messages', () => {
    render(
      <AiConversationHeader
        title=""
        headingLevel={1}
        onRename={() => {}}
        messages={{ untitled: 'Neues Gespräch', rename: 'Umbenennen' }}
      />,
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Neues Gespräch' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Umbenennen' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiConversationHeader title="Plan" onRename={() => {}} />);
    expect(html).toContain('mn-ai-conversation-header');
    expect(html).toContain('Plan');
  });
});
