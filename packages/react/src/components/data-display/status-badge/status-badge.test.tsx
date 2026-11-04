import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { contrast, fromHex, themes } from '@rookcom/monarch-tokens';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  StatusBadge,
  StatusRegistryProvider,
  builtInStatuses,
  checkStatusColor,
  defineStatuses,
  formatStatusLabel,
  resolveStatus,
} from './index.js';

const badge = () => document.querySelector('.mn-status-badge') as HTMLElement;

describe('StatusBadge', () => {
  it('renders a built-in status as text, tone and a decorative icon', async () => {
    render(<StatusBadge status="partial" />);
    expect(screen.getByText('Partial outage')).toBeInTheDocument();
    expect(badge()).toHaveAttribute('data-tone', 'warning');
    expect(badge()).toHaveAttribute('data-status', 'partial');
    expect(badge().querySelector('.mn-status-badge__icon')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('covers the service-health and generic sets, each with a label and an icon', () => {
    const keys = Object.keys(builtInStatuses);
    expect(keys).toEqual([
      'operational',
      'degraded',
      'partial',
      'down',
      'maintenance',
      'unknown',
      'success',
      'warning',
      'danger',
      'info',
      'neutral',
    ]);
    for (const def of Object.values(builtInStatuses)) {
      expect(def.label).not.toBe('');
      expect(def.icon).toBeTruthy();
    }
  });

  it('never shows a raw enum: unknown values get a readable label and the neutral look', () => {
    render(<StatusBadge status="pending_delete" />);
    expect(screen.getByText('Pending delete')).toBeInTheDocument();
    expect(badge()).toHaveAttribute('data-tone', 'neutral');
    expect(badge()).toHaveAttribute('data-known', 'false');
  });

  it('treats null as unknown', () => {
    render(<StatusBadge status={null} />);
    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('dot variant shows a dot and the text, without the icon', () => {
    render(<StatusBadge status="down" variant="dot" />);
    expect(badge().querySelector('.mn-status-badge__dot')).toHaveAttribute('aria-hidden', 'true');
    expect(badge().querySelector('.mn-status-badge__icon')).toBeNull();
    expect(screen.getByText('Down')).toBeInTheDocument();
  });

  it('icon slot: replaces or hides the icon', () => {
    const { rerender } = render(<StatusBadge status="success" icon={<svg data-testid="own" />} />);
    expect(screen.getByTestId('own')).toBeInTheDocument();
    rerender(<StatusBadge status="success" icon={false} />);
    expect(badge().querySelector('.mn-status-badge__icon')).toBeNull();
    expect(screen.getByText('Success')).toBeInTheDocument();
  });

  it('label overrides the registry label (translations)', () => {
    render(<StatusBadge status="operational" label="Opérationnel" />);
    expect(screen.getByText('Opérationnel')).toBeInTheDocument();
  });

  it('products extend the registry through a provider and per badge', () => {
    const tickets = defineStatuses({
      open: { label: 'Open', tone: 'info' },
      waiting: { label: 'Waiting on customer', tone: 'warning' },
    });
    render(
      <StatusRegistryProvider statuses={tickets}>
        <StatusBadge status="waiting" />
        <StatusBadge status="operational" />
        <StatusBadge status="open" statuses={{ open: { label: 'New', tone: 'accent' } }} />
      </StatusRegistryProvider>,
    );
    expect(screen.getByText('Waiting on customer')).toBeInTheDocument();
    expect(screen.getByText('Operational')).toBeInTheDocument();
    expect(screen.getByText('New').closest('.mn-status-badge')).toHaveAttribute(
      'data-tone',
      'accent',
    );
  });

  it('defineStatuses can extend the built-in set and rejects malformed colours', () => {
    const all = defineStatuses({ queued: { label: 'Queued', tone: 'neutral' } }, { extend: true });
    expect(all.queued.label).toBe('Queued');
    expect(all.down.label).toBe('Down');
    expect(() => defineStatuses({ bad: { label: 'Bad', tone: 'neutral', color: 'red' } })).toThrow(
      /#rrggbb/,
    );
  });

  it('resolveStatus checks local, then provided, then built-in', () => {
    expect(resolveStatus('down', { down: { label: 'Offline', tone: 'danger' } }).label).toBe(
      'Offline',
    );
    expect(resolveStatus('down').label).toBe('Down');
    expect(resolveStatus('toString').known).toBe(false);
    expect(formatStatusLabel('NOT_CONFIGURED')).toBe('Not configured');
  });
});

describe('contrast guard', () => {
  const surface = (id: keyof typeof themes) => themes[id].neutrals['bg-surface'];

  it('keeps a colour that already passes and reports no adjustment', () => {
    const check = checkStatusColor('#2563eb');
    expect(check).not.toBeNull();
    expect(check!.light).toBe('#2563eb');
  });

  it('adjusts a colour that fails 3:1 until it passes in every theme', () => {
    // A pale yellow: unreadable as a mark on white.
    const check = checkStatusColor('#fde047')!;
    expect(check.ratios.light).toBeLessThan(3);
    expect(check.adjusted).toBe(true);
    expect(contrast(fromHex(check.light), surface('light'))).toBeGreaterThanOrEqual(3);
    expect(contrast(fromHex(check.light), surface('hc-light'))).toBeGreaterThanOrEqual(
      themes['hc-light'].targets.ui,
    );
    expect(contrast(fromHex(check.dark), surface('dark'))).toBeGreaterThanOrEqual(3);
    expect(contrast(fromHex(check.dark), surface('hc-dark'))).toBeGreaterThanOrEqual(
      themes['hc-dark'].targets.ui,
    );
  });

  it('solid text on a custom colour is always 4.5:1 or more', () => {
    for (const hex of ['#fde047', '#6b7280', '#0f172a', '#22c55e', '#ef4444']) {
      const check = checkStatusColor(hex)!;
      expect(contrast(fromHex(check.onLight), fromHex(check.light))).toBeGreaterThanOrEqual(4.5);
      expect(contrast(fromHex(check.onDark), fromHex(check.dark))).toBeGreaterThanOrEqual(4.5);
    }
  });

  it('returns null for anything that is not #rrggbb', () => {
    expect(checkStatusColor('tomato')).toBeNull();
    expect(checkStatusColor('#fff')).toBeNull();
  });

  it('a custom colour is applied through guarded CSS variables, text stays a token', async () => {
    render(<StatusBadge status="custom" label="In review" color="#fde047" variant="dot" />);
    const el = badge();
    expect(el).toHaveAttribute('data-custom-color');
    expect(el.style.getPropertyValue('--mn-status-light')).not.toBe('#fde047');
    expect(el.style.getPropertyValue('--mn-status-dark')).toMatch(/^#[0-9a-f]{6}$/);
    await expectNoA11yViolations();
  });

  it('a custom colour without an icon gets a dot', () => {
    render(
      <StatusBadge
        status="shipped"
        statuses={{ shipped: { label: 'Shipped', tone: 'neutral', color: '#0d9488' } }}
      />,
    );
    expect(badge().querySelector('.mn-status-badge__dot')).not.toBeNull();
  });
});

describe('StatusBadge SSR', () => {
  it('renders on the server', () => {
    const html = renderToString(<StatusBadge status="maintenance" variant="outline" />);
    expect(html).toContain('mn-status-badge');
    expect(html).toContain('Maintenance');
  });
});
