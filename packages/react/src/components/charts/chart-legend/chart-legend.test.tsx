import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ChartLegend, type ChartLegendEntry } from './index.js';

const ITEMS: ChartLegendEntry[] = [
  { key: 'web', label: 'Web' },
  { key: 'mobile', label: 'Mobile', value: '1,204' },
  { key: 'api', label: 'API', color: 'neutral', dashed: true },
];

describe('ChartLegend', () => {
  it('renders a named list with a swatch per entry in palette order', async () => {
    const { container } = render(<ChartLegend items={ITEMS} className="extra" />);
    const list = screen.getByRole('list', { name: 'Legend' });
    expect(list).toHaveClass('mn-chart-legend', 'mn-chart__legend', 'extra');
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    const swatches = container.querySelectorAll<HTMLElement>('.mn-chart__swatch');
    expect(swatches[0]?.style.getPropertyValue('--_c')).toBe('var(--mn-color-data-1)');
    expect(swatches[1]?.style.getPropertyValue('--_c')).toBe('var(--mn-color-data-2)');
    expect(swatches[2]?.style.getPropertyValue('--_c')).toBe('var(--mn-color-text-subtle)');
    expect(swatches[2]).toHaveAttribute('data-dashed');
    expect(screen.getByText('1,204')).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('toggles series as a toolbar with one tab stop', async () => {
    const onHiddenChange = vi.fn();
    render(<ChartLegend items={ITEMS} toggleable onHiddenSeriesChange={onHiddenChange} />);
    const toolbar = screen.getByRole('toolbar', { name: 'Legend' });
    expect(toolbar).toHaveAttribute('aria-orientation', 'horizontal');
    const web = screen.getByRole('button', { name: 'Web' });
    const mobile = screen.getByRole('button', { name: 'Mobile, 1,204' });
    const api = screen.getByRole('button', { name: 'API' });
    expect(web).toHaveAttribute('aria-pressed', 'true');
    expect(web).toHaveAttribute('tabindex', '0');
    expect(mobile).toHaveAttribute('tabindex', '-1');

    const user = userEvent.setup();
    await user.tab();
    expect(web).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(mobile).toHaveFocus();
    await user.keyboard('{End}');
    expect(api).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(web).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(api).toHaveFocus();
    await user.keyboard('{Home}');
    expect(web).toHaveFocus();

    await user.keyboard('{Enter}');
    expect(onHiddenChange).toHaveBeenLastCalledWith(['web']);
    expect(web).toHaveAttribute('aria-pressed', 'false');
    expect(web).toHaveAttribute('data-hidden');
    await user.keyboard(' ');
    expect(onHiddenChange).toHaveBeenLastCalledWith([]);
    expect(web).toHaveAttribute('aria-pressed', 'true');
    await expectNoA11yViolations();
  });

  it('keeps the last visible series and offers Show all', async () => {
    render(<ChartLegend items={ITEMS} toggleable defaultHiddenSeries={['web', 'mobile']} />);
    const api = screen.getByRole('button', { name: 'API' });
    expect(api).toHaveAttribute('aria-disabled', 'true');
    const user = userEvent.setup();
    await user.click(api);
    expect(api).toHaveAttribute('aria-pressed', 'true');

    const reset = screen.getByRole('button', { name: 'Show all' });
    await user.click(reset);
    expect(screen.getByRole('button', { name: 'Web' })).toHaveFocus();
    expect(screen.queryByRole('button', { name: 'Show all' })).toBeNull();
    for (const b of screen.getAllByRole('button'))
      expect(b).toHaveAttribute('aria-pressed', 'true');
  });

  it('can hide every series when keepOneVisible is off', async () => {
    render(<ChartLegend items={ITEMS.slice(0, 1)} toggleable keepOneVisible={false} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Web' }));
    expect(screen.getByRole('button', { name: 'Web' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('works controlled and keeps unknown hidden keys on Show all', async () => {
    function Controlled() {
      const [hidden, setHidden] = useState<string[]>(['mobile', 'elsewhere']);
      return (
        <>
          <ChartLegend
            items={ITEMS}
            toggleable
            hiddenSeries={hidden}
            onHiddenSeriesChange={setHidden}
          />
          <output>{hidden.join(',')}</output>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'API' }));
    expect(screen.getByRole('status')).toHaveTextContent('mobile,elsewhere,api');
    await user.click(screen.getByRole('button', { name: 'Show all' }));
    expect(screen.getByRole('status')).toHaveTextContent('elsewhere');
  });

  it('reports the active entry on hover and focus', async () => {
    const onActiveChange = vi.fn();
    render(<ChartLegend items={ITEMS} toggleable onActiveChange={onActiveChange} />);
    const user = userEvent.setup();
    await user.hover(screen.getByRole('button', { name: 'Mobile, 1,204' }));
    expect(onActiveChange).toHaveBeenLastCalledWith('mobile');
    await user.unhover(screen.getByRole('button', { name: 'Mobile, 1,204' }));
    expect(onActiveChange).toHaveBeenLastCalledWith(null);
    await user.tab();
    expect(onActiveChange).toHaveBeenLastCalledWith('web');
  });

  it('stacks vertically with arrow up and down, and translates its text', async () => {
    render(
      <ChartLegend
        items={ITEMS}
        toggleable
        orientation="vertical"
        marker="dot"
        defaultHiddenSeries={['api']}
        messages={{ label: 'Legende', showAll: 'Alle zeigen' }}
      />,
    );
    const toolbar = screen.getByRole('toolbar', { name: 'Legende' });
    expect(toolbar).toHaveAttribute('data-orientation', 'vertical');
    expect(toolbar).toHaveAttribute('data-marker', 'dot');
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}');
    expect(screen.getByRole('button', { name: 'Alle zeigen' })).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(screen.getByRole('button', { name: 'API' })).toHaveFocus();
  });

  it('renders on the server', () => {
    const html = renderToString(<ChartLegend items={ITEMS} toggleable />);
    expect(html).toContain('mn-chart-legend');
    expect(html).toContain('aria-pressed="true"');
  });
});
