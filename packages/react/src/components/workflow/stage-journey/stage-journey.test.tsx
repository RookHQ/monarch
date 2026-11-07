import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { StageHistory, StageJourney, type JourneyStage, type JourneyTransition } from './index.js';

const NOW = Date.UTC(2026, 0, 12, 10, 0);
const DAY = 86_400_000;

const STAGES: JourneyStage[] = [
  { id: 'draft', label: 'Draft', group: 'Prepare' },
  { id: 'review', label: 'Review', group: 'Prepare' },
  {
    id: 'filed',
    label: 'Filed',
    group: 'Prosecute',
    description: 'Waiting for the office to respond.',
  },
  { id: 'granted', label: 'Granted', group: 'Maintain' },
];
const MOVES: JourneyTransition[] = [
  { id: 'granted', label: 'Granted' },
  { id: 'examined', label: 'Examined', needsInput: true },
  { id: 'withdrawn', label: 'Withdrawn', kind: 'exit' },
  { id: 'review', label: 'Review', kind: 'back' },
];

describe('StageJourney', () => {
  it('shows stages grouped by phase with done and current states', async () => {
    render(
      <StageJourney
        stages={STAGES}
        current="filed"
        enteredAt={NOW - 3 * DAY}
        now={NOW}
        locale="en"
        data-testid="j"
      />,
    );
    const root = screen.getByTestId('j');
    expect(root).toHaveClass('mn-stage-journey');
    expect(screen.getByRole('region', { name: 'Now: Filed' })).toBe(root);
    const strip = screen.getByRole('group', { name: 'Stages' });
    expect(strip).toHaveAttribute('tabindex', '0');
    expect(within(strip).getByText('Prepare')).toBeInTheDocument();
    const items = within(strip)
      .getAllByRole('listitem')
      .filter((li) => li.classList.contains('mn-stage-journey__stage'));
    expect(items.map((i) => i.getAttribute('data-status'))).toEqual([
      'done',
      'done',
      'current',
      'upcoming',
    ]);
    expect(items[0]).toHaveTextContent('Draft (done)');
    expect(items[2]).toHaveAttribute('aria-current', 'step');
    expect(items[2]).toHaveTextContent('Filed (current stage)');
    expect(root).toHaveTextContent('since 3 days ago');
    expect(root).toHaveTextContent('Waiting for the office to respond.');
    await expectNoA11yViolations();
  });

  it('offers forward, exit and back moves and calls onTransition', async () => {
    const onTransition = vi.fn();
    render(
      <StageJourney
        stages={STAGES}
        current="filed"
        transitions={MOVES}
        onTransition={onTransition}
      />,
    );
    const group = screen.getByRole('group', { name: 'Next steps' });
    const buttons = within(group).getAllByRole('button');
    expect(buttons.map((b) => b.getAttribute('aria-label') ?? b.textContent)).toEqual([
      'Move to Granted',
      'Move to Examined (asks for details)',
      'Move to Withdrawn',
      'Back to Review',
    ]);
    expect(buttons[0]).toHaveAttribute('data-variant', 'primary');
    expect(buttons[2]).toHaveAttribute('data-variant', 'ghost');
    await userEvent.click(buttons[0]!);
    expect(onTransition).toHaveBeenCalledWith(MOVES[0]);
    await userEvent.click(screen.getByRole('button', { name: 'Back to Review' }));
    expect(onTransition).toHaveBeenLastCalledWith(MOVES[3]);
    await expectNoA11yViolations();
  });

  it('is busy while a move runs, shows a failure, and announces the new stage', async () => {
    let reject!: (e: Error) => void;
    const onTransition = vi.fn(() => new Promise<void>((_, r) => (reject = r)));
    const { rerender } = render(
      <StageJourney
        stages={STAGES}
        current="filed"
        transitions={MOVES}
        onTransition={onTransition}
      />,
    );
    const user = userEvent.setup();
    const granted = screen.getByRole('button', { name: 'Move to Granted' });
    await user.click(granted);
    expect(granted).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Move to Withdrawn' })).toBeDisabled();
    await act(async () => reject(new Error('Someone else moved it')));
    expect(screen.getByRole('alert')).toHaveTextContent('Someone else moved it');
    rerender(
      <StageJourney
        stages={STAGES}
        current="granted"
        terminal="success"
        onTransition={onTransition}
      />,
    );
    expect(screen.queryByRole('alert')).toBeNull();
    expect(screen.getByRole('status')).toHaveTextContent('Moved to Granted');
    expect(screen.getByText('Now: Granted')).toHaveTextContent('Now: Granted (Completed)');
  });

  it('renders the strip only, and translated strings', () => {
    render(
      <StageJourney
        stages={STAGES}
        current="review"
        variant="strip"
        transitions={MOVES}
        onTransition={() => {}}
        messages={{ stages: 'Etappen', current: 'aktuell' }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Etappen' })).toBeInTheDocument();
    expect(screen.getByText('Review')).toHaveTextContent('Review (aktuell)');
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <StageJourney
        stages={STAGES}
        current="filed"
        enteredAt={NOW - DAY}
        now={NOW}
        transitions={MOVES}
        onTransition={() => {}}
      />,
    );
    expect(html).toContain('mn-stage-journey');
    expect(html).toContain('aria-current="step"');
  });
});

describe('StageHistory', () => {
  const entries = [
    {
      id: 'h3',
      from: 'Filed',
      to: 'Review',
      at: NOW - 3600_000,
      actor: 'Sam Lee',
      backward: true,
      note: 'Missing drawings',
    },
    {
      id: 'h2',
      from: 'Review',
      to: 'Filed',
      at: NOW - DAY,
      actor: 'Automation',
      meta: '2 deadlines added',
    },
    { id: 'h1', to: 'Draft', at: NOW - 5 * DAY },
  ];

  it('lists moves with from, to, when, who and why', async () => {
    render(<StageHistory aria-label="Stage history" entries={entries} now={NOW} locale="en" />);
    const items = screen.getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('Filed → to Review (moved back)1 hour ago');
    expect(items[0]).toHaveTextContent('Sam Lee');
    expect(items[0]).toHaveTextContent('Missing drawings');
    expect(items[0]).toHaveAttribute('data-backward');
    expect(items[1]).toHaveTextContent('Automation · 2 deadlines added');
    expect(items[2]).toHaveTextContent('Started at Draft');
    await expectNoA11yViolations();
  });

  it('shows an empty message and renders on the server', () => {
    render(<StageHistory entries={[]} />);
    expect(screen.getByText('No stage moves yet.')).toBeInTheDocument();
    expect(renderToString(<StageHistory entries={entries} now={NOW} />)).toContain(
      'mn-stage-history',
    );
  });
});
