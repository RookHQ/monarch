import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  StackedProportionBar,
  formatShare,
  type ProportionSegment,
  type StackedProportionBarProps,
} from './index.js';

const pipeline: ProportionSegment[] = [
  { key: 'new', label: 'New', value: 120 },
  { key: 'enrolled', label: 'In a campaign', value: 60 },
  { key: 'replied', label: 'Replied', value: 18 },
  { key: 'bounced', label: 'Bounced', value: 0 },
  { key: 'converted', label: 'Converted', value: 2 },
];

function segments(container: HTMLElement) {
  return Array.from(container.querySelectorAll<HTMLElement>('.mn-stacked-proportion-bar__segment'));
}

describe('StackedProportionBar', () => {
  it('lists every non-zero part with value and share; the bar is decorative', async () => {
    const { container } = render(
      <main>
        <StackedProportionBar label="Lead pipeline" segments={pipeline} locale="en-US" />
      </main>,
    );
    const list = screen.getByRole('list', { name: 'Lead pipeline' });
    expect(
      within(list)
        .getAllByRole('listitem')
        .map((li) => li.textContent),
    ).toEqual(['New 120 · 60%', 'In a campaign 60 · 30%', 'Replied 18 · 9%', 'Converted 2 · 1%']);
    expect(container.querySelector('.mn-stacked-proportion-bar__track')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    expect(segments(container).map((s) => s.style.flexGrow)).toEqual(['120', '60', '18', '2']);
    await expectNoA11yViolations();
  });

  it('assigns palette colours by position, or the pinned slot', () => {
    const { container } = render(
      <StackedProportionBar
        label="Mix"
        segments={[
          { key: 'a', label: 'A', value: 1 },
          { key: 'b', label: 'B', value: 1, color: 4 },
        ]}
      />,
    );
    const [a, b] = segments(container);
    expect(a!.style.getPropertyValue('--_c')).toBe('var(--mn-color-data-1)');
    expect(b!.style.getPropertyValue('--_c')).toBe('var(--mn-color-data-4)');
  });

  it('keeps zero segments when hideZero is false', () => {
    render(<StackedProportionBar label="Pipeline" segments={pipeline} hideZero={false} />);
    expect(screen.getAllByRole('listitem')).toHaveLength(5);
    expect(screen.getByText(/^Bounced/)).toHaveTextContent('Bounced 0 · 0%');
  });

  it('draws a capacity with an empty remainder and names it', () => {
    const { container } = render(
      <StackedProportionBar
        label="Storage"
        total={100}
        remainderLabel="Free"
        segments={[
          { key: 'files', label: 'Files', value: 42 },
          { key: 'media', label: 'Media', value: 18 },
        ]}
        formatValue={(v) => `${v} GB`}
      />,
    );
    expect(container.querySelector('.mn-stacked-proportion-bar__remainder')).toHaveStyle({
      flexGrow: '40',
    });
    expect(screen.getAllByRole('listitem').at(-1)).toHaveTextContent('Free 40 GB · 40%');
    expect(container.firstElementChild).toHaveAttribute('data-remainder');
  });

  it('switches legend content and can hide the legend visually', () => {
    const { rerender } = render(
      <StackedProportionBar label="P" segments={pipeline} legendContent="percent" />,
    );
    expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('New 60%');
    rerender(<StackedProportionBar label="P" segments={pipeline} legendContent="value" />);
    expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('New 120');
    rerender(<StackedProportionBar label="P" segments={pipeline} legendContent="none" />);
    expect(screen.getAllByRole('listitem')[0]).toHaveTextContent(/^New$/);
    rerender(<StackedProportionBar label="P" segments={pipeline} legend={false} />);
    expect(screen.getByRole('list', { name: 'P' })).toHaveClass('mn-visually-hidden');
  });

  it('writes tiny shares as <1% so rounding never hides a part', () => {
    render(
      <StackedProportionBar
        label="States"
        segments={[
          { key: 'ok', label: 'Operational', value: 997 },
          { key: 'down', label: 'Down', value: 3 },
        ]}
        locale="en-US"
      />,
    );
    expect(screen.getByText(/^Down/)).toHaveTextContent('Down 3 · <1%');
    expect(screen.getByText(/^Operational/)).toHaveTextContent('Operational 997 · >99%');
  });

  it('applies the minimum segment width and size', () => {
    const { container } = render(
      <StackedProportionBar label="P" segments={pipeline} minSegmentWidth="6px" size="lg" />,
    );
    const root = container.firstElementChild as HTMLElement;
    expect(root.style.getPropertyValue('--mn-proportion-min')).toBe('6px');
    expect(root).toHaveAttribute('data-size', 'lg');
  });

  it('shows the empty label when everything is zero', async () => {
    const { container } = render(
      <StackedProportionBar label="Pipeline" segments={[{ key: 'a', label: 'A', value: 0 }]} />,
    );
    expect(container.firstElementChild).toHaveAttribute('data-empty');
    expect(screen.getByText('No data yet')).toBeInTheDocument();
    expect(segments(container)).toHaveLength(0);
    await expectNoA11yViolations();
  });

  it('ignores negative and non-finite values', () => {
    const { container } = render(
      <StackedProportionBar
        label="P"
        segments={[
          { key: 'a', label: 'A', value: -5 },
          { key: 'b', label: 'B', value: Number.NaN },
          { key: 'c', label: 'C', value: 2 },
        ]}
      />,
    );
    expect(segments(container)).toHaveLength(1);
  });

  it('forwards ref and attributes', () => {
    let node: HTMLDivElement | null = null;
    render(
      <StackedProportionBar
        ref={(el) => {
          node = el;
        }}
        id="bar"
        label="P"
        segments={pipeline}
      />,
    );
    expect(node).toHaveAttribute('id', 'bar');
    expect(node).toHaveClass('mn-stacked-proportion-bar');
  });

  it('formatShare handles edges', () => {
    expect(formatShare(1, 3, { locale: 'en-US' })).toBe('33%');
    expect(formatShare(1, 3, { locale: 'en-US', maximumFractionDigits: 1 })).toBe('33.3%');
    expect(formatShare(0, 10, { locale: 'en-US' })).toBe('0%');
    expect(formatShare(5, 0, { locale: 'en-US' })).toBe('0%');
    expect(formatShare(10, 10, { locale: 'en-US' })).toBe('100%');
    expect(formatShare(1, 1000, { locale: 'en-US', maximumFractionDigits: 1 })).toBe('0.1%');
    expect(formatShare(1, 10000, { locale: 'en-US', maximumFractionDigits: 1 })).toBe('<0.1%');
  });

  it('colours status segments from intent tokens in status mode, with a text legend', async () => {
    const { container } = render(
      <StackedProportionBar
        label="Monitors"
        legendContent="value"
        segments={[
          { key: 'up', label: 'Operational', value: 12, tone: 'success' },
          { key: 'slow', label: 'Degraded', value: 2, tone: 'warning' },
          { key: 'down', label: 'Down', value: 1, tone: 'danger' },
          { key: 'paused', label: 'Paused', value: 1, tone: 'neutral' },
        ]}
      />,
    );
    expect(container.firstElementChild).toHaveAttribute('data-mode', 'status');
    const [up, , down, paused] = segments(container);
    expect(up!.style.getPropertyValue('--_c')).toBe('var(--mn-color-success-solid)');
    expect(up).toHaveAttribute('data-tone', 'success');
    expect(down!.style.getPropertyValue('--_c')).toBe('var(--mn-color-danger-solid)');
    expect(paused!.style.getPropertyValue('--_c')).toBe('var(--mn-color-text-subtle)');
    const list = screen.getByRole('list', { name: 'Monitors' });
    expect(
      within(list)
        .getAllByRole('listitem')
        .map((li) => li.textContent),
    ).toEqual(['Operational 12', 'Degraded 2', 'Down 1', 'Paused 1']);
    expect(list.querySelector('.mn-chart__swatch')?.getAttribute('style')).toContain(
      '--mn-color-success-solid',
    );
    await expectNoA11yViolations();
  });

  it('warns and falls back to the palette when tones and palette segments are mixed', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    const mixed = [
      { key: 'a', label: 'A', value: 1, tone: 'success' },
      { key: 'b', label: 'B', value: 1 },
    ] as unknown as ProportionSegment[];
    // The types reject a mixed literal outright:
    // @ts-expect-error palette and status segments cannot share one bar
    const typed: StackedProportionBarProps['segments'] = [
      { key: 'a', label: 'A', value: 1, tone: 'success' },
      { key: 'b', label: 'B', value: 1, color: 2 },
    ];
    void typed;
    const { container } = render(<StackedProportionBar label="Mixed" segments={mixed} />);
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('mix of status tones'));
    expect(container.firstElementChild).not.toHaveAttribute('data-mode');
    expect(segments(container)[0]!.style.getPropertyValue('--_c')).toBe('var(--mn-color-data-1)');
    warn.mockRestore();
  });

  it('renders on the server', () => {
    const html = renderToString(<StackedProportionBar label="Pipeline" segments={pipeline} />);
    expect(html).toContain('mn-stacked-proportion-bar');
    expect(html).toContain('Replied');
  });
});
