import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DecisionCard, type DecisionOption } from './index.js';

const OPTIONS: DecisionOption[] = [
  { value: 'keep', label: 'Keep', tone: 'success' },
  { value: 'review', label: 'Review', description: 'Decide later', tone: 'warning' },
  {
    value: 'lapse',
    label: 'Let lapse',
    tone: 'danger',
    note: 'Reminders stop; one final notice is sent.',
    reasonRequired: true,
  },
];

const NOW = new Date('2026-05-10T12:00:00Z');

describe('DecisionCard', () => {
  it('renders a named region with a radio group, the saved choice and who decided', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <DecisionCard
        ref={ref}
        title="Renewal decision"
        options={OPTIONS}
        value="keep"
        reason="Still in use"
        decidedAt={new Date('2026-05-09T12:00:00Z')}
        decidedBy="Alex Morgan"
        now={NOW}
        context={<p>Used by 3 services</p>}
        onDecide={() => {}}
      />,
    );
    expect(ref.current).toHaveClass('mn-decision-card');
    expect(screen.getByRole('region', { name: 'Renewal decision' })).toBe(ref.current);
    expect(screen.getByRole('group', { name: 'Decision' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Keep' })).toBeChecked();
    expect(screen.getByRole('radio', { name: 'Review' })).toHaveAccessibleDescription(
      'Decide later',
    );
    expect(screen.getByRole('radio', { name: 'No decision' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: /Reason/ })).toHaveValue('Still in use');
    expect(screen.getByText(/by Alex Morgan/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save decision' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('moves with arrow keys, shows the note and requires a reason where asked', async () => {
    const user = userEvent.setup();
    const onDecide = vi.fn();
    render(<DecisionCard options={OPTIONS} value="review" onDecide={onDecide} />);
    await user.click(screen.getByRole('radio', { name: 'Review' }));
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: 'Let lapse' })).toBeChecked();
    expect(screen.getByText('Reminders stop; one final notice is sent.')).toBeInTheDocument();
    const reason = screen.getByRole('textbox', { name: /Reason/ });
    expect(reason).toBeRequired();
    await user.click(screen.getByRole('button', { name: 'Save decision' }));
    expect(onDecide).not.toHaveBeenCalled();
    expect(reason).toHaveAccessibleDescription(/Add a reason for this decision./);
    expect(reason).toHaveAttribute('aria-invalid', 'true');
    await user.type(reason, '  Not needed  ');
    await user.click(screen.getByRole('button', { name: 'Save decision' }));
    expect(onDecide).toHaveBeenCalledWith({ value: 'lapse', reason: 'Not needed' });
    expect(await screen.findByText('Decision saved.')).toHaveAttribute('role', 'status');
  });

  it('clears the decision with No decision and shows busy then error', async () => {
    const user = userEvent.setup();
    let reject: (e: Error) => void = () => {};
    const onDecide = vi.fn(
      () =>
        new Promise((_, r) => {
          reject = r;
        }),
    );
    render(<DecisionCard options={OPTIONS} value="keep" onDecide={onDecide} />);
    await user.click(screen.getByRole('radio', { name: 'No decision' }));
    await user.click(screen.getByRole('button', { name: 'Save decision' }));
    expect(onDecide).toHaveBeenCalledWith({ value: null, reason: null });
    expect(screen.getByRole('radio', { name: 'Keep' })).toBeDisabled();
    reject(new Error('Saving is paused.'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Saving is paused.');
    expect(screen.getByRole('radio', { name: 'No decision' })).toBeChecked();
  });

  it('resets when the saved decision changes', () => {
    const { rerender } = render(
      <DecisionCard options={OPTIONS} value="keep" onDecide={() => {}} />,
    );
    rerender(<DecisionCard options={OPTIONS} value="review" reason="Later" onDecide={() => {}} />);
    expect(screen.getByRole('radio', { name: 'Review' })).toBeChecked();
    expect(screen.getByRole('textbox', { name: /Reason/ })).toHaveValue('Later');
  });

  it('shows a read-only badge and reason', async () => {
    render(
      <DecisionCard
        title="Renewal decision"
        options={OPTIONS}
        value="lapse"
        reason="Replaced"
        readOnly
        onDecide={() => {}}
        allowNone={false}
      />,
    );
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    expect(screen.getByText('Let lapse')).toBeInTheDocument();
    expect(screen.getByText('Replaced')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('says when nothing is decided and accepts custom strings', async () => {
    const user = userEvent.setup();
    render(
      <DecisionCard
        options={OPTIONS}
        value={null}
        messages={{ undecided: 'Open', save: 'Record' }}
      />,
    );
    expect(screen.getByText('Open')).toBeInTheDocument();
    const { unmount } = render(
      <DecisionCard
        options={OPTIONS}
        value={null}
        onDecide={() => {}}
        messages={{ save: 'Record' }}
      />,
    );
    expect(screen.getByRole('radio', { name: 'No decision' })).toBeChecked();
    await user.click(screen.getByRole('radio', { name: 'Keep' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Record' })).toBeEnabled());
    unmount();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <DecisionCard
        title="Decision"
        options={OPTIONS}
        value="keep"
        now={NOW}
        onDecide={() => {}}
      />,
    );
    expect(html).toContain('mn-decision-card');
    expect(html).toContain('type="radio"');
  });
});
