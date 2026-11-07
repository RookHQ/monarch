import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { UsageMeter, usageLevel, type UsageMeterItem } from './index.js';

const items: UsageMeterItem[] = [
  { id: 'req', label: 'Requests', used: 4200, limit: 10000 },
  { id: 'msg', label: 'Messages', used: 8500, limit: 10000 },
  { id: 'gb', label: 'Storage', used: 12, limit: 10, unit: 'GB' },
  { id: 'conv', label: 'Conversations', used: 31, description: 'Conversations started' },
];

describe('UsageMeter', () => {
  it('renders a named region with a meter per limited item', async () => {
    render(<UsageMeter title="Usage" period="This month" items={items} locale="en-US" />);
    expect(screen.getByRole('region', { name: 'Usage' })).toBeInTheDocument();
    const meters = screen.getAllByRole('meter');
    expect(meters).toHaveLength(3);
    expect(screen.getByRole('meter', { name: 'Requests' })).toHaveAttribute(
      'aria-valuetext',
      '4,200 of 10,000 used',
    );
    expect(screen.getByRole('meter', { name: 'Storage' })).toHaveAttribute(
      'aria-valuetext',
      '12 of 10 GB used',
    );
    await expectNoA11yViolations();
  });

  it('marks levels with text as well as colour', () => {
    render(<UsageMeter title="Usage" items={items} locale="en-US" />);
    const [req, msg, gb, conv] = screen.getAllByRole('listitem');
    expect(req).toHaveAttribute('data-level', 'normal');
    expect(req).toHaveTextContent('42% used');
    expect(msg).toHaveAttribute('data-level', 'warning');
    expect(msg).toHaveTextContent('Near the limit · 85% used');
    expect(gb).toHaveAttribute('data-level', 'over');
    expect(gb).toHaveTextContent('Over the limit · 120% used');
    expect(conv).toHaveAttribute('data-level', 'unlimited');
    expect(conv).toHaveTextContent('31');
    expect(conv).toHaveTextContent('Conversations started');
    expect(screen.getByRole('meter', { name: 'Storage' })).toHaveAttribute('data-tone', 'danger');
  });

  it('announces items near or over the limit in a polite live region', () => {
    render(<UsageMeter title="Usage" items={items} />);
    const live = screen.getByRole('status');
    expect(live).toHaveAttribute('aria-live', 'polite');
    expect(live).toHaveTextContent('Messages: near the limit. Storage: over the limit');
  });

  it('shows the reset date, compact numbers and custom formatters', () => {
    render(
      <UsageMeter
        title="Usage"
        period="This month"
        resetsAt="2026-12-01T00:00:00Z"
        timeZone="UTC"
        locale="en-US"
        notation="compact"
        warningAt={0.9}
        items={[
          { id: 'a', label: 'Calls', used: 1500, limit: 100000 },
          { id: 'b', label: 'Minutes', used: 90, limit: 100, format: (v) => `${v} min` },
        ]}
      />,
    );
    expect(screen.getByText('This month · Resets Dec 1, 2026')).toBeInTheDocument();
    expect(screen.getByText('1.5K')).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')[1]).toHaveAttribute('data-level', 'warning');
    expect(screen.getByRole('meter', { name: 'Minutes' })).toHaveAttribute(
      'aria-valuetext',
      '90 min of 100 min used',
    );
  });

  it('computes levels', () => {
    expect(usageLevel(5, null)).toBe('unlimited');
    expect(usageLevel(5, 10)).toBe('normal');
    expect(usageLevel(8, 10)).toBe('warning');
    expect(usageLevel(10, 10)).toBe('warning');
    expect(usageLevel(11, 10)).toBe('over');
  });

  it('shows loading and error states', async () => {
    const onRetry = vi.fn();
    const { rerender } = render(<UsageMeter aria-label="Usage" items={[]} loading />);
    expect(screen.getByRole('region', { name: 'Usage' })).toHaveAttribute('aria-busy', 'true');
    rerender(<UsageMeter aria-label="Usage" items={[]} error onRetry={onRetry} />);
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('renders on the server', () => {
    const html = renderToString(<UsageMeter title="Usage" items={items} locale="en-US" />);
    expect(html).toContain('mn-usage-meter');
    expect(html).toContain('role="meter"');
  });
});
