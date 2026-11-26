import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ProgressCircle } from './index.js';

describe('ProgressCircle', () => {
  it('is a determinate progressbar with value and readable value text', async () => {
    render(
      <ProgressCircle value={42} label="Uploading report" locale="en-US" showValue size="lg" />,
    );
    const bar = screen.getByRole('progressbar', { name: 'Uploading report' });
    expect(bar).toHaveAttribute('aria-valuemin', '0');
    expect(bar).toHaveAttribute('aria-valuemax', '100');
    expect(bar).toHaveAttribute('aria-valuenow', '42');
    expect(bar).toHaveAttribute('aria-valuetext', '42%');
    expect(bar).toHaveAttribute('data-state', 'loading');
    expect(bar.querySelector('.mn-progress-circle__indicator')).toHaveAttribute(
      'stroke-dasharray',
      '42 100',
    );
    expect(bar.querySelector('.mn-progress-circle__value')).toHaveTextContent('42%');
    expect(bar.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('is indeterminate without a value and named "Loading" by default', async () => {
    render(<ProgressCircle />);
    const bar = screen.getByRole('progressbar', { name: 'Loading' });
    expect(bar).toHaveAttribute('data-state', 'indeterminate');
    expect(bar).not.toHaveAttribute('aria-valuenow');
    expect(bar).not.toHaveAttribute('aria-valuetext');
    await expectNoA11yViolations();
  });

  it('clamps the value, supports a custom max, format and messages', () => {
    const { rerender } = render(
      <ProgressCircle value={12} max={8} formatValue={(v, m) => `${v} of ${m} files`} />,
    );
    const bar = screen.getByRole('progressbar', { name: 'Progress' });
    expect(bar).toHaveAttribute('aria-valuenow', '8');
    expect(bar).toHaveAttribute('aria-valuetext', '8 of 8 files');
    expect(bar).toHaveAttribute('data-state', 'complete');
    rerender(<ProgressCircle value={-5} messages={{ progress: 'Fortschritt' }} />);
    expect(screen.getByRole('progressbar', { name: 'Fortschritt' })).toHaveAttribute(
      'aria-valuenow',
      '0',
    );
    rerender(<ProgressCircle value={null} messages={{ loading: 'Laden' }} />);
    expect(screen.getByRole('progressbar', { name: 'Laden' })).toHaveAttribute(
      'data-state',
      'indeterminate',
    );
  });

  it('prefers aria-labelledby over the default name and exposes tone and size', () => {
    render(
      <>
        <span id="lbl">Sync</span>
        <ProgressCircle aria-labelledby="lbl" value={10} tone="success" size="xs" />
      </>,
    );
    const bar = screen.getByRole('progressbar', { name: 'Sync' });
    expect(bar).not.toHaveAttribute('aria-label');
    expect(bar).toHaveAttribute('data-tone', 'success');
    expect(bar).toHaveAttribute('data-size', 'xs');
  });

  it('does not throw on a locale the runtime rejects', () => {
    render(<ProgressCircle value={42} locale="not a locale" />);
    expect(screen.getByRole('progressbar').getAttribute('aria-valuetext')).toMatch(/42/);
  });

  it('renders on the server', () => {
    const html = renderToString(<ProgressCircle value={50} label="Import" />);
    expect(html).toContain('role="progressbar"');
    expect(html).toContain('aria-valuenow="50"');
  });
});
