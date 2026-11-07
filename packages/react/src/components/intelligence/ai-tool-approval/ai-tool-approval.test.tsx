import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { mockTransport, useAiConversation, type AiToolCallPart } from '../ai-conversation/index.js';
import { AiToolApproval } from './index.js';

const part = (overrides: Partial<AiToolCallPart> = {}): AiToolCallPart => ({
  type: 'tool-call',
  id: 'mail-1',
  name: 'send_email',
  input: { to: ['a@example.test', 'b@example.test'], subject: 'Weekly update' },
  state: 'pending-approval',
  ...overrides,
});

describe('AiToolApproval', () => {
  it('renders the gate with readable arguments; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiToolApproval
        ref={ref}
        data-testid="root"
        className="x"
        part={part()}
        onApprove={() => {}}
        onDeny={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-tool-approval', 'x');
    expect(ref.current).toBe(root);
    expect(root).toHaveAttribute('data-risk', 'medium');
    expect(root).toHaveAttribute('data-state', 'pending');
    expect(screen.getByRole('group', { name: 'Send email' })).toBe(root);
    expect(screen.getByText('To')).toBeInTheDocument();
    expect(screen.getByText('a@example.test, b@example.test')).toBeInTheDocument();
    expect(screen.getByText('Medium risk')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('announces assertively when it appears', async () => {
    render(
      <AiToolApproval
        part={part()}
        describe={() => 'Send email to 2 people'}
        onApprove={() => {}}
        onDeny={() => {}}
      />,
    );
    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Approval needed: Send email to 2 people',
      ),
    );
  });

  it('approves without input when the arguments were not edited', async () => {
    const onApprove = vi.fn();
    render(<AiToolApproval part={part()} onApprove={onApprove} onDeny={() => {}} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Approve' }));
    expect(onApprove).toHaveBeenCalledWith();
    expect(screen.getByText('Approved')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Approve' })).toBeNull();
  });

  it('approves edited arguments and rejects invalid JSON', async () => {
    const user = userEvent.setup();
    const onApprove = vi.fn();
    render(<AiToolApproval part={part()} onApprove={onApprove} onDeny={() => {}} />);
    await user.click(screen.getByRole('button', { name: 'Edit arguments' }));
    const editor = screen.getByRole('textbox', { name: 'Arguments (JSON)' });
    expect(editor).toHaveFocus();
    await user.clear(editor);
    await user.paste('{"to": [');
    await user.click(screen.getByRole('button', { name: 'Approve' }));
    expect(onApprove).not.toHaveBeenCalled();
    expect(screen.getByText('Fix the JSON before approving.')).toBeInTheDocument();
    expect(editor).toHaveAttribute('aria-invalid', 'true');
    expect(editor).toHaveFocus();
    await user.clear(editor);
    await user.paste('{"to": ["c@example.test"]}');
    await user.click(screen.getByRole('button', { name: 'Approve' }));
    expect(onApprove).toHaveBeenCalledWith({ to: ['c@example.test'] });
  });

  it('denies with an optional reason', async () => {
    const user = userEvent.setup();
    const onDeny = vi.fn();
    const { rerender } = render(
      <AiToolApproval part={part()} onApprove={() => {}} onDeny={onDeny} />,
    );
    await user.click(screen.getByRole('button', { name: 'Add a reason' }));
    const reason = screen.getByRole('textbox', { name: 'Reason (optional)' });
    expect(reason).toHaveFocus();
    await user.type(reason, 'Wrong recipients');
    await user.click(screen.getByRole('button', { name: 'Deny' }));
    expect(onDeny).toHaveBeenCalledWith('Wrong recipients');
    expect(screen.getByText('Denied')).toBeInTheDocument();

    onDeny.mockClear();
    rerender(<AiToolApproval part={part({ id: 'mail-2' })} onApprove={() => {}} onDeny={onDeny} />);
    await user.click(screen.getByRole('button', { name: 'Deny' }));
    expect(onDeny).toHaveBeenCalledWith();
  });

  it('calls onAlwaysAllow before approving when ticked, never for destructive', async () => {
    const user = userEvent.setup();
    const calls: string[] = [];
    const { rerender } = render(
      <AiToolApproval
        part={part()}
        onAlwaysAllow={(name) => calls.push(`always:${name}`)}
        onApprove={() => calls.push('approve')}
        onDeny={() => {}}
      />,
    );
    await user.click(screen.getByRole('checkbox', { name: 'Always allow this tool' }));
    await user.click(screen.getByRole('button', { name: 'Approve' }));
    expect(calls).toEqual(['always:send_email', 'approve']);
    rerender(
      <AiToolApproval
        part={part({ id: 'other' })}
        risk="destructive"
        onAlwaysAllow={() => {}}
        onApprove={() => {}}
        onDeny={() => {}}
      />,
    );
    expect(screen.queryByRole('checkbox')).toBeNull();
  });

  it('destructive: danger tone, consequence line, focus on Deny', async () => {
    render(
      <AiToolApproval
        part={part({ name: 'delete_records', input: { ids: [1, 2, 3] } })}
        risk="destructive"
        focusOnMount
        describe={() => 'Delete 3 records'}
        onApprove={() => {}}
        onDeny={() => {}}
      />,
    );
    const group = screen.getByRole('group', { name: 'Delete 3 records' });
    expect(group).toHaveAttribute('data-risk', 'destructive');
    expect(group).toHaveAccessibleDescription('This can’t be undone once it runs.');
    expect(screen.getByRole('button', { name: 'Approve' })).toHaveAttribute(
      'data-variant',
      'danger',
    );
    expect(screen.getByRole('button', { name: 'Deny' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('focusOnMount puts focus on Approve for low risk, and never takes focus by default', () => {
    const { unmount } = render(
      <AiToolApproval
        part={part()}
        risk="low"
        focusOnMount
        onApprove={() => {}}
        onDeny={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: 'Approve' })).toHaveFocus();
    unmount();
    render(<AiToolApproval part={part()} onApprove={() => {}} onDeny={() => {}} />);
    expect(document.body).toHaveFocus();
  });

  it('keeps focus in the card after deciding', async () => {
    render(<AiToolApproval part={part()} onApprove={() => {}} onDeny={() => {}} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Approve' }));
    expect(screen.getByRole('group')).toHaveFocus();
  });

  it('shows the outcome for a call that is no longer pending', () => {
    const { rerender } = render(
      <AiToolApproval part={part({ state: 'running' })} onApprove={() => {}} onDeny={() => {}} />,
    );
    expect(screen.getByText('Approved')).toBeInTheDocument();
    rerender(
      <AiToolApproval part={part({ state: 'cancelled' })} onApprove={() => {}} onDeny={() => {}} />,
    );
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
  });

  it('continues the reply through useAiConversation and mockTransport', async () => {
    const transport = mockTransport({
      delayMs: 0,
      reply: [
        { type: 'text-delta', text: 'I will send it. ' },
        { type: 'part', part: part() },
      ],
    });
    let start: () => void = () => {};
    function Harness() {
      const conv = useAiConversation({ transport });
      start = () => conv.send('Email the team');
      const tool = conv.messages
        .flatMap((m) => m.parts)
        .find((p): p is AiToolCallPart => p.type === 'tool-call');
      return (
        <div>
          <p data-testid="status">{conv.status}</p>
          <p data-testid="text">
            {conv.messages
              .filter((m) => m.role === 'assistant')
              .flatMap((m) => m.parts)
              .map((p) => (p.type === 'text' ? p.text : ''))
              .join('')}
          </p>
          {tool ? (
            <AiToolApproval
              part={tool}
              onApprove={(input) => conv.approveTool(tool.id, input)}
              onDeny={(reason) => conv.denyTool(tool.id, reason)}
            />
          ) : null}
        </div>
      );
    }
    render(<Harness />);
    act(() => start());
    await waitFor(() =>
      expect(screen.getByTestId('status')).toHaveTextContent('awaiting-approval'),
    );
    await userEvent.setup().click(screen.getByRole('button', { name: 'Approve' }));
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('idle'));
    expect(screen.getByTestId('text')).toHaveTextContent('I will send it. Done.');
    expect(screen.getByText('Approved')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiToolApproval part={part()} onApprove={() => {}} onDeny={() => {}} />,
    );
    expect(html).toContain('mn-ai-tool-approval');
    expect(html).toContain('Send email');
  });
});
