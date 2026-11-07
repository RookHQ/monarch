import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SlaIndicator, SlaTargetList, formatSlaDuration, resolveSlaState } from './index.js';

const NOW = Date.UTC(2026, 0, 12, 10, 0);
const MIN = 60_000;

afterEach(() => {
  vi.useRealTimers();
});

describe('formatSlaDuration and resolveSlaState', () => {
  it('formats spans compactly', () => {
    expect(formatSlaDuration(20_000)).toBe('<1m');
    expect(formatSlaDuration(45 * MIN)).toBe('45m');
    expect(formatSlaDuration(135 * MIN)).toBe('2h 15m');
    expect(formatSlaDuration(120 * MIN)).toBe('2h');
    expect(formatSlaDuration((3 * 1440 + 4 * 60) * MIN)).toBe('3d 4h');
    expect(formatSlaDuration(-45 * MIN)).toBe('45m');
  });

  it('derives breached and at-risk from the clock, never changing finished states', () => {
    expect(resolveSlaState('on-track', NOW - MIN, NOW)).toBe('breached');
    expect(resolveSlaState('at-risk', NOW - MIN, NOW)).toBe('breached');
    expect(resolveSlaState('on-track', NOW + 30 * MIN, NOW, 60 * MIN)).toBe('at-risk');
    expect(resolveSlaState('on-track', NOW + 90 * MIN, NOW, 60 * MIN)).toBe('on-track');
    expect(resolveSlaState('paused', NOW - MIN, NOW)).toBe('paused');
    expect(resolveSlaState('met', NOW - MIN, NOW)).toBe('met');
    expect(resolveSlaState('on-track', null, NOW)).toBe('on-track');
  });
});

describe('SlaIndicator', () => {
  it('shows time left with the state, target and due time in its text', async () => {
    render(
      <SlaIndicator
        data-testid="sla"
        state="on-track"
        dueAt={NOW + 135 * MIN}
        now={NOW}
        target="First response"
        locale="en-GB"
      />,
    );
    const el = screen.getByTestId('sla');
    expect(el).toHaveClass('mn-sla-indicator');
    expect(el).toHaveAttribute('data-state', 'on-track');
    expect(el).toHaveAttribute('data-variant', 'chip');
    expect(el).toHaveTextContent(/^First response: 2h 15m left \(On track\), due/);
    expect(el.querySelector('.mn-sla-indicator__icon svg')).toHaveAttribute('aria-hidden');
    await expectNoA11yViolations();
  });

  it('turns at-risk inside warnBefore and breached once overdue', () => {
    const { rerender } = render(
      <SlaIndicator
        data-testid="sla"
        state="on-track"
        dueAt={NOW + 20 * MIN}
        now={NOW}
        warnBefore={60 * MIN}
      />,
    );
    expect(screen.getByTestId('sla')).toHaveAttribute('data-state', 'at-risk');
    expect(screen.getByTestId('sla')).toHaveTextContent('20m left (At risk)');
    rerender(<SlaIndicator data-testid="sla" state="at-risk" dueAt={NOW - 40 * MIN} now={NOW} />);
    expect(screen.getByTestId('sla')).toHaveAttribute('data-state', 'breached');
    expect(screen.getByTestId('sla')).toHaveTextContent('Overdue 40m (Breached)');
  });

  it('shows paused, met and none without a countdown', () => {
    const { rerender } = render(
      <SlaIndicator data-testid="sla" state="paused" dueAt={NOW + MIN} now={NOW} />,
    );
    expect(screen.getByTestId('sla')).toHaveTextContent(/^Paused, due/);
    rerender(<SlaIndicator data-testid="sla" state="met" dueAt={NOW - MIN} now={NOW} />);
    expect(screen.getByTestId('sla')).toHaveTextContent(/^Met$/);
    rerender(<SlaIndicator data-testid="sla" state="none" />);
    expect(screen.getByTestId('sla')).toHaveTextContent('—No SLA');
    expect(screen.getByTestId('sla').querySelector('.mn-sla-indicator__icon')).toBeNull();
    rerender(<SlaIndicator data-testid="sla" state="none" showNone />);
    expect(screen.getByTestId('sla')).toHaveTextContent(/^No SLA$/);
  });

  it('says when an earlier target was breached', () => {
    render(<SlaIndicator data-testid="sla" state="met" breachedBefore />);
    expect(screen.getByTestId('sla')).toHaveTextContent('Met, an earlier target was breached');
  });

  it('ticks live, calls onStateChange and announces the change politely', async () => {
    vi.useFakeTimers({ now: NOW });
    const onStateChange = vi.fn();
    render(
      <SlaIndicator
        data-testid="sla"
        state="on-track"
        dueAt={NOW + 2 * MIN}
        live={60_000}
        target="Resolution"
        announceChanges
        onStateChange={onStateChange}
      />,
    );
    expect(screen.getByTestId('sla')).toHaveTextContent('2m left');
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
    await act(async () => {
      vi.advanceTimersByTime(60_000);
    });
    expect(screen.getByTestId('sla')).toHaveTextContent('1m left');
    await act(async () => {
      vi.advanceTimersByTime(120_000);
    });
    expect(screen.getByTestId('sla')).toHaveAttribute('data-state', 'breached');
    expect(onStateChange).toHaveBeenCalledWith('breached');
    expect(screen.getByRole('status')).toHaveTextContent('Resolution: Breached');
  });

  it('accepts translated messages', () => {
    render(
      <SlaIndicator
        data-testid="sla"
        state="on-track"
        dueAt={NOW + 30 * MIN}
        now={NOW}
        messages={{
          remaining: (d) => `noch ${d}`,
          states: { 'on-track': 'Im Plan' } as never,
        }}
      />,
    );
    expect(screen.getByTestId('sla')).toHaveTextContent('noch 30m (Im Plan)');
  });

  it('renders on the server with an injected clock', () => {
    const html = renderToString(
      <SlaIndicator state="on-track" dueAt={NOW + 45 * MIN} now={NOW} variant="text" />,
    );
    expect(html).toContain('mn-sla-indicator');
    expect(html).toContain('45m left');
    expect(html).toContain('data-variant="text"');
  });
});

describe('SlaTargetList', () => {
  const targets = [
    { id: 'first', label: 'First response', state: 'met' as const, completedAt: NOW - 30 * MIN },
    { id: 'next', label: 'Next response', state: 'on-track' as const, dueAt: NOW + 15 * MIN },
    { id: 'resolution', label: 'Resolution', state: 'paused' as const, dueAt: NOW + 600 * MIN },
  ];

  it('lists every target with its state', async () => {
    render(
      <SlaTargetList aria-label="SLA targets" targets={targets} now={NOW} warnBefore={30 * MIN} />,
    );
    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('First response');
    expect(items[0]).toHaveTextContent('Met');
    expect(items[0]!.querySelector('time')).toBeInTheDocument();
    expect(items[1]).toHaveTextContent('15m left (At risk)');
    expect(items[2]).toHaveTextContent('Paused');
    await expectNoA11yViolations();
  });

  it('shows an empty message', () => {
    render(<SlaTargetList targets={[]} emptyMessage="Nothing applies." />);
    expect(screen.getByText('Nothing applies.')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<SlaTargetList targets={targets} now={NOW} />)).toContain(
      'mn-sla-target-list',
    );
  });
});
