import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Progress } from './index.js';

describe('Progress', () => {
  it('is a labelled progressbar; indeterminate has no value', () => {
    const { rerender } = render(<Progress label="Upload" value={40} />);
    const bar = screen.getByRole('progressbar', { name: 'Upload' });
    expect(bar).toHaveAttribute('aria-valuenow', '40');
    rerender(<Progress label="Upload" />);
    expect(screen.getByRole('progressbar')).not.toHaveAttribute('aria-valuenow');
  });

  it('has no axe violations', async () => {
    render(<Progress label="Upload" value={40} />);
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Progress label="Sync" value={10} />);
    expect(html).toContain('mn-progress');
  });

  it('keeps the 0.3 markup by default', () => {
    const { container } = render(<Progress label="Upload" value={40} />);
    const root = container.firstElementChild!;
    expect(root).toHaveAttribute('data-tone', 'accent');
    expect(root).toHaveAttribute('data-size', 'md');
    expect(root).not.toHaveAttribute('data-semantics');
    expect(root.querySelector('.mn-progress__bar')).toHaveStyle({ width: '40%' });
  });

  it('valueText sets aria-valuetext on the default bar', () => {
    render(<Progress label="Import" value={3} max={10} valueText="3 of 10 files" />);
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuetext', '3 of 10 files');
  });

  it('meter semantics: role=meter with value text and threshold tones', () => {
    const { rerender } = render(
      <Progress
        semantics="meter"
        label="Storage"
        value={50}
        thresholds={{ warning: 80, danger: 95 }}
      />,
    );
    const meter = screen.getByRole('meter', { name: 'Storage' });
    expect(meter).toHaveAttribute('aria-valuenow', '50');
    expect(meter).toHaveAttribute('aria-valuemin', '0');
    expect(meter).toHaveAttribute('aria-valuemax', '100');
    expect(meter).toHaveAttribute('aria-valuetext', '50 of 100');
    expect(meter).toHaveAttribute('data-tone', 'neutral');
    rerender(
      <Progress
        semantics="meter"
        label="Storage"
        value={85}
        thresholds={{ warning: 80, danger: 95 }}
      />,
    );
    expect(screen.getByRole('meter')).toHaveAttribute('data-tone', 'warning');
    rerender(
      <Progress
        semantics="meter"
        label="Storage"
        value={96}
        thresholds={{ warning: 80, danger: 95 }}
      />,
    );
    expect(screen.getByRole('meter')).toHaveAttribute('data-tone', 'danger');
  });

  it('meter over the limit: danger tone, clamped value, over-limit text', () => {
    render(<Progress semantics="meter" label="Seats" value={12} max={10} />);
    const meter = screen.getByRole('meter', { name: 'Seats' });
    expect(meter).toHaveAttribute('data-over');
    expect(meter).toHaveAttribute('data-tone', 'danger');
    expect(meter).toHaveAttribute('aria-valuenow', '10');
    expect(meter).toHaveAttribute('aria-valuetext', '12 of 10, over the limit');
    expect(meter.querySelector('.mn-progress__bar')).toHaveStyle({ width: '100%' });
  });

  it('an explicit tone wins over thresholds', () => {
    render(
      <Progress
        semantics="meter"
        label="Quota"
        value={99}
        tone="success"
        thresholds={{ danger: 90 }}
      />,
    );
    expect(screen.getByRole('meter')).toHaveAttribute('data-tone', 'success');
  });

  it('segments stack in order and describe themselves', async () => {
    const { container } = render(
      <main>
        <Progress
          label="Sprint progress"
          max={20}
          segments={[
            { label: 'Done', value: 8, tone: 'success' },
            { label: 'In review', value: 4 },
            { label: 'In progress', value: 3 },
          ]}
        />
      </main>,
    );
    const bar = screen.getByRole('progressbar', { name: 'Sprint progress' });
    expect(bar).toHaveAttribute('aria-valuenow', '15');
    expect(bar).toHaveAttribute('aria-valuetext', 'Done 8, In review 4, In progress 3 (15 of 20)');
    const segs = container.querySelectorAll<HTMLElement>('.mn-progress__segment');
    expect(segs).toHaveLength(3);
    expect(segs[0]).toHaveAttribute('data-tone', 'success');
    expect(segs[1]).toHaveAttribute('data-tone', 'data-2');
    expect(segs[1]!.style.insetInlineStart).toBe('40%');
    expect(segs[2]!.style.width).toBe('15%');
    await expectNoA11yViolations();
  });

  it('segments never overflow the track', () => {
    const { container } = render(
      <Progress
        label="Capacity"
        segments={[
          { label: 'A', value: 80 },
          { label: 'B', value: 50 },
        ]}
      />,
    );
    const segs = container.querySelectorAll<HTMLElement>('.mn-progress__segment');
    expect(segs[1]!.style.width).toBe('20%');
  });

  it('meter and segments have no axe violations and render on the server', async () => {
    render(
      <main>
        <Progress semantics="meter" label="Storage" value={7.2} max={10} valueText="7.2 of 10 GB" />
        <Progress
          semantics="meter"
          label="Hours"
          segments={[{ label: 'Billable', value: 30 }]}
          max={40}
        />
      </main>,
    );
    await expectNoA11yViolations();
    expect(renderToString(<Progress semantics="meter" label="S" value={1} />)).toContain(
      'role="meter"',
    );
  });
});
