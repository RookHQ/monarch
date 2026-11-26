import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PeriodSelector, formatPeriodRange, resolvePeriod, type PeriodPreset } from './index.js';

const today = '2026-11-07';

describe('PeriodSelector', () => {
  it('renders a named group of preset segments; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<PeriodSelector ref={ref} data-testid="root" className="extra" today={today} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-period-selector', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getAllByRole('group', { name: 'Period' })[0]).toBe(root);
    expect(screen.getByRole('radio', { name: 'Today' })).toBeChecked();
    ['Last 7 days', 'Last 30 days', 'This quarter', 'Custom'].forEach((name) =>
      expect(screen.getByRole('radio', { name })).toBeInTheDocument(),
    );
    await expectNoA11yViolations();
  });

  it('reports the preset and its resolved dates', async () => {
    const onValueChange = vi.fn();
    const user = userEvent.setup();
    render(<PeriodSelector today={today} onValueChange={onValueChange} />);
    await user.click(screen.getByRole('radio', { name: 'Last 7 days' }));
    expect(onValueChange).toHaveBeenLastCalledWith(
      { preset: '7d' },
      { start: '2026-11-01', end: '2026-11-07' },
    );
    expect(screen.getByRole('radio', { name: 'Last 7 days' })).toBeChecked();
  });

  it('moves between presets with the arrow keys', async () => {
    const onValueChange = vi.fn();
    const user = userEvent.setup();
    render(
      <PeriodSelector
        today={today}
        defaultValue={{ preset: '7d' }}
        onValueChange={onValueChange}
      />,
    );
    screen.getByRole('radio', { name: 'Last 7 days' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: 'Last 30 days' })).toHaveFocus();
    expect(onValueChange).toHaveBeenLastCalledWith(
      { preset: '30d' },
      { start: '2026-10-09', end: '2026-11-07' },
    );
  });

  it('shows a date-range picker for a custom period, seeded with the previous dates', async () => {
    const onValueChange = vi.fn();
    const user = userEvent.setup();
    render(
      <PeriodSelector
        today={today}
        defaultValue={{ preset: '7d' }}
        onValueChange={onValueChange}
      />,
    );
    await user.click(screen.getByRole('radio', { name: 'Custom' }));
    expect(onValueChange).toHaveBeenLastCalledWith(
      { preset: 'custom', range: { start: '2026-11-01', end: '2026-11-07' } },
      { start: '2026-11-01', end: '2026-11-07' },
    );
    expect(screen.getByRole('group', { name: 'Custom period' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('offers only the given presets and shows the resolved range', () => {
    const { container } = render(
      <PeriodSelector
        today={today}
        presets={['30d', '90d', '180d', '365d']}
        showRange
        locale="en-GB"
      />,
    );
    expect(screen.getAllByRole('radio')).toHaveLength(4);
    expect(screen.getByRole('radio', { name: 'Last 30 days' })).toBeChecked();
    expect(container.querySelector('.mn-period-selector__range')?.textContent).toBe(
      formatPeriodRange({ start: '2026-10-09', end: '2026-11-07' }, 'en-GB'),
    );
    expect(formatPeriodRange({ start: '2026-10-09', end: '2026-10-09' }, 'en-GB')).toBe(
      '9 Oct 2026',
    );
  });

  it('renders the select variant with a named trigger', async () => {
    render(<PeriodSelector today={today} variant="select" defaultValue={{ preset: '30d' }} />);
    const trigger = screen.getByRole('combobox', { name: 'Period' });
    expect(trigger).toHaveTextContent('Last 30 days');
    await expectNoA11yViolations();
  });

  it.each<[PeriodPreset, string, string]>([
    ['today', '2026-11-07', '2026-11-07'],
    ['yesterday', '2026-11-06', '2026-11-06'],
    ['this-week', '2026-11-02', '2026-11-07'],
    ['this-month', '2026-11-01', '2026-11-07'],
    ['last-month', '2026-10-01', '2026-10-31'],
    ['this-quarter', '2026-10-01', '2026-11-07'],
    ['last-quarter', '2026-07-01', '2026-09-30'],
    ['this-year', '2026-01-01', '2026-11-07'],
    ['365d', '2025-11-08', '2026-11-07'],
  ])('resolves %s', (preset, start, end) => {
    expect(resolvePeriod({ preset }, { today, weekStartsOn: 1 })).toEqual({ start, end });
  });

  it('resolves custom to its range or null', () => {
    expect(resolvePeriod({ preset: 'custom' }, { today })).toBeNull();
    const range = { start: '2026-01-01', end: '2026-01-31' };
    expect(resolvePeriod({ preset: 'custom', range }, { today })).toEqual(range);
  });

  it('translates through messages', () => {
    render(
      <PeriodSelector today={today} messages={{ label: 'Periodo', presets: { today: 'Hoy' } }} />,
    );
    expect(screen.getAllByRole('group', { name: 'Periodo' }).length).toBeGreaterThan(0);
    expect(screen.getByRole('radio', { name: 'Hoy' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<PeriodSelector today={today} showRange locale="en" />);
    expect(html).toContain('mn-period-selector');
    expect(html).toContain('Last 30 days');
  });
});
