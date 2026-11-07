import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiToolResultCard, type AiToolResultKind } from './index.js';

describe('AiToolResultCard', () => {
  it('renders kind, title, description and meta; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiToolResultCard
        ref={ref}
        data-testid="root"
        className="x"
        kind="record-created"
        title="Northwind account"
        description="Added to Accounts"
        meta={[
          { label: 'Owner', value: 'Sam Lee' },
          { label: 'Stage', value: 'Prospect' },
        ]}
        href="/accounts/42"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-tool-result-card', 'x');
    expect(root).toHaveAttribute('data-kind', 'record-created');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Northwind account' })).toBe(root);
    expect(screen.getByText('Record created')).toBeInTheDocument();
    expect(screen.getByText('Added to Accounts')).toBeInTheDocument();
    expect(screen.getByText('Owner')).toBeInTheDocument();
    const link = screen.getByRole('link', { name: 'Open' });
    expect(link).toHaveAttribute('href', '/accounts/42');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    expect(link).not.toHaveAttribute('target');
    expect(link).toHaveAccessibleDescription('Northwind account');
    await expectNoA11yViolations();
  });

  it.each<[AiToolResultKind, string]>([
    ['email-sent', 'Email sent'],
    ['event-booked', 'Event booked'],
    ['ticket-filed', 'Ticket filed'],
    ['generic', 'Done'],
  ])('labels the %s kind', (kind, label) => {
    render(<AiToolResultCard kind={kind} title="Item" />);
    expect(screen.getByText(label)).toBeInTheDocument();
  });

  it('drops unsafe links and opens external ones in a new tab', () => {
    const { rerender } = render(<AiToolResultCard title="T" href="javascript:alert(1)" />);
    expect(screen.queryByRole('link')).toBeNull();
    rerender(<AiToolResultCard title="T" href="https://example.test/x" external />);
    expect(screen.getByRole('link', { name: 'Open' })).toHaveAttribute('target', '_blank');
  });

  it('undoes, then shows and announces Undone and keeps focus in the card', async () => {
    const onUndo = vi.fn(() => Promise.resolve());
    render(<AiToolResultCard title="Weekly update" kind="email-sent" onUndo={onUndo} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Undo' }));
    expect(onUndo).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Undone'));
    expect(screen.queryByRole('button', { name: 'Undo' })).toBeNull();
    expect(screen.getByRole('group')).toHaveAttribute('data-undone');
    expect(screen.getByRole('group')).toHaveFocus();
  });

  it('reports a failed undo and lets the person try again', async () => {
    const user = userEvent.setup();
    const onUndo = vi.fn(() => Promise.reject(new Error('nope')));
    render(<AiToolResultCard title="T" onUndo={onUndo} />);
    await user.click(screen.getByRole('button', { name: 'Undo' }));
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('Couldn’t undo. Try again.'),
    );
    await user.click(screen.getByRole('button', { name: 'Undo' }));
    expect(onUndo).toHaveBeenCalledTimes(2);
  });

  it('accepts message overrides', () => {
    render(
      <AiToolResultCard
        title="T"
        href="/x"
        messages={{ open: 'Öffnen', kinds: { generic: 'Erledigt' } }}
      />,
    );
    expect(screen.getByRole('link', { name: 'Öffnen' })).toBeInTheDocument();
    expect(screen.getByText('Erledigt')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<AiToolResultCard title="T" kind="ticket-filed" />)).toContain(
      'mn-ai-tool-result-card',
    );
  });
});
