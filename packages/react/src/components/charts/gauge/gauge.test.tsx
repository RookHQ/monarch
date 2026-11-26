import { describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { hydrateRoot } from 'react-dom/client';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Gauge, type GaugeBand } from './index.js';

const BANDS: GaugeBand[] = [
  { from: 0, to: 70, tone: 'success', label: 'Healthy' },
  { from: 70, to: 90, tone: 'warning', label: 'High' },
  { from: 90, to: 100, tone: 'danger', label: 'Critical' },
];

describe('Gauge', () => {
  it('is a meter named by its label with the value as text', async () => {
    const { container } = render(
      <Gauge value={42} label="CPU" description="Last 5 minutes" locale="en-US" />,
    );
    const meter = screen.getByRole('meter', { name: 'CPU' });
    expect(meter).toHaveClass('mn-gauge');
    expect(meter).toHaveAttribute('aria-valuenow', '42');
    expect(meter).toHaveAttribute('aria-valuemin', '0');
    expect(meter).toHaveAttribute('aria-valuemax', '100');
    expect(meter).toHaveAttribute('aria-valuetext', '42 (from 0 to 100)');
    expect(meter).toHaveAccessibleDescription('Last 5 minutes');
    expect(meter).toHaveAttribute('data-tone', 'accent');
    expect(meter).toHaveAttribute('data-indicator', 'arc');
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    expect(container.querySelector('.mn-gauge__text')).toHaveTextContent('42');
    expect(container.querySelector('.mn-gauge__value')).not.toBeNull();
    await expectNoA11yViolations();
  });

  it('takes the tone and name of the band the value falls in', async () => {
    const { rerender } = render(<Gauge value={75} bands={BANDS} label="Memory" />);
    const meter = screen.getByRole('meter', { name: 'Memory' });
    expect(meter).toHaveAttribute('data-tone', 'warning');
    expect(meter).toHaveAttribute('data-band', 'High');
    expect(meter).toHaveAttribute('aria-valuetext', '75, High');
    expect(screen.getByText('High', { selector: '.mn-gauge__band-label' })).toBeInTheDocument();
    expect(document.querySelectorAll('.mn-gauge__band')).toHaveLength(3);
    // The top of the scale belongs to the last band.
    rerender(<Gauge value={100} bands={BANDS} label="Memory" />);
    expect(meter).toHaveAttribute('data-tone', 'danger');
    await expectNoA11yViolations();
  });

  it('draws a needle, a target and a fixed tone', () => {
    const { container } = render(
      <Gauge
        value={30}
        indicator="needle"
        tone="info"
        target={80}
        bands={BANDS}
        aria-label="Error budget"
      />,
    );
    const meter = screen.getByRole('meter', { name: 'Error budget' });
    expect(meter).toHaveAttribute('data-tone', 'info');
    expect(meter).toHaveAccessibleDescription('Target 80');
    expect(container.querySelector('.mn-gauge__needle')).not.toBeNull();
    expect(container.querySelector('.mn-gauge__value')).toBeNull();
    expect(container.querySelector('.mn-gauge__target')).not.toBeNull();
  });

  it('clamps the drawing but shows the real value, with Intl formatting', () => {
    render(
      <Gauge
        value={1.25}
        max={1}
        valueFormat={{ style: 'percent' }}
        locale="en-US"
        label="Quota"
      />,
    );
    const meter = screen.getByRole('meter', { name: 'Quota' });
    expect(meter).toHaveAttribute('aria-valuenow', '1');
    expect(meter).toHaveAttribute('aria-valuetext', '125% (from 0% to 100%)');
    expect(screen.getByText('0%')).toBeInTheDocument();
    expect(screen.getByText('100%')).toBeInTheDocument();
  });

  it('shows a dash and no value when empty, and translates its text', async () => {
    const { container } = render(
      <Gauge
        value={null}
        showRange={false}
        messages={{ name: 'Auslastung', noValue: 'Keine Daten' }}
      />,
    );
    const img = screen.getByRole('img', { name: 'Auslastung' });
    expect(img).toHaveAttribute('data-empty');
    // The dash is drawn in a hidden SVG: the words reach assistive technology as the description.
    expect(img).toHaveAccessibleDescription('Keine Daten');
    expect(img).toHaveAttribute('data-tone', 'neutral');
    expect(container.querySelector('.mn-gauge__text')).toHaveTextContent('–');
    expect(container.querySelector('.mn-gauge__range')).toBeNull();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Gauge value={64} label="Disk" bands={BANDS} />);
    expect(html).toContain('mn-gauge');
    expect(html).toContain('role="meter"');
  });

  it('formats with a fixed locale until hydrated, then the viewer locale, without a mismatch', async () => {
    let machineLocale = 'en-IN';
    const original = Intl.DateTimeFormat.prototype.resolvedOptions;
    const spy = vi
      .spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions')
      .mockImplementation(function (this: Intl.DateTimeFormat) {
        return { ...original.call(this), locale: machineLocale };
      });
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const ui = <Gauge value={1234567} min={0} max={2000000} label="Rows" />;
    const container = document.createElement('div');
    try {
      const html = renderToString(ui);
      expect(html).toContain('1,234,567');
      machineLocale = 'de-DE';
      container.innerHTML = html;
      document.body.append(container);
      let root: ReturnType<typeof hydrateRoot> | undefined;
      await act(async () => {
        root = hydrateRoot(container, ui);
      });
      expect(error).not.toHaveBeenCalled();
      expect(container.textContent).toContain('1.234.567');
      act(() => root?.unmount());
    } finally {
      container.remove();
      spy.mockRestore();
      error.mockRestore();
    }
  });
});
