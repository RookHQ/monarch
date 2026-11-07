import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { WORLD_MAP_CENTROIDS, WORLD_MAP_SHAPES, WorldMap, type WorldMapProps } from './index.js';

if (typeof window.PointerEvent === 'undefined') {
  class PointerEventStub extends MouseEvent {
    pointerType: string;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerType = init.pointerType ?? 'mouse';
    }
  }
  window.PointerEvent = PointerEventStub as unknown as typeof PointerEvent;
}

const DATA = [
  { country: 'us', value: 1200, details: [{ label: 'Users', value: 340 }] },
  { country: 'DE', value: 300, details: [{ label: 'Users', value: 90 }] },
  { country: 'IN', value: 150 },
  { country: 'SG', value: 40 },
  { country: 'XX', value: 5 },
];

function map(props: Partial<WorldMapProps> = {}) {
  return <WorldMap title="Workspaces by country" data={DATA} locale="en-US" {...props} />;
}

const plot = () => screen.getByRole('group', { name: /5 countries/ });
const live = () => document.querySelector('[role="status"]')!;

describe('world map data', () => {
  it('has outlines and centroids keyed by ISO alpha-2 codes', () => {
    expect(Object.keys(WORLD_MAP_SHAPES).length).toBeGreaterThan(170);
    expect(WORLD_MAP_SHAPES.FR).toMatch(/^M\d/);
    expect(WORLD_MAP_SHAPES.SG).toBeUndefined();
    expect(WORLD_MAP_CENTROIDS.SG).toHaveLength(2);
    expect(Object.keys(WORLD_MAP_SHAPES).every((k) => /^[A-Z]{2}$/.test(k))).toBe(true);
  });
});

describe('WorldMap', () => {
  it('fills countries by level, dots small ones and summarises the top values', async () => {
    const { container } = render(map());
    expect(plot()).toHaveAccessibleName(
      '5 countries. Highest: United States 1,200, Germany 300, India 150.',
    );
    expect(container.querySelector('[data-code="US"]')).toHaveAttribute('data-level', '4');
    expect(container.querySelector('[data-code="DE"]')).toHaveAttribute('data-level', '2');
    expect(container.querySelector('[data-code="FR"]')).not.toHaveAttribute('data-level');
    expect(container.querySelector('.mn-world-map__dot[data-country="SG"]')).toBeInTheDocument();
    expect(screen.getByText('Not on the map: XX (5)')).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Legend' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('lists every country, its details and the extras in the table', () => {
    render(map({ extras: [{ key: 'global', label: 'Global', value: 75 }] }));
    const table = screen.getByRole('table');
    expect(within(table).getByRole('columnheader', { name: 'Users' })).toBeInTheDocument();
    const us = within(table).getByRole('rowheader', { name: 'United States' }).closest('tr')!;
    expect(us).toHaveTextContent('1,200');
    expect(us).toHaveTextContent('340');
    expect(within(table).getByRole('rowheader', { name: 'Global' })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Other values' })).toHaveTextContent('Global75');
  });

  it('steps through countries by value with the keyboard and selects one', async () => {
    const onCountrySelect = vi.fn();
    render(map({ onCountrySelect }));
    const user = userEvent.setup();
    for (let i = 0; i < 6 && document.activeElement !== plot(); i++) await user.tab();
    expect(plot()).toHaveFocus();
    expect(live()).toHaveTextContent('United States: 1,200, Users 340');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('Germany: 300');
    await user.keyboard('{End}');
    expect(live()).toHaveTextContent('Singapore: 40');
    await user.keyboard('{ArrowRight}');
    expect(live()).toHaveTextContent('Singapore');
    await user.keyboard('{Home}{ArrowDown}{Enter}');
    expect(onCountrySelect).toHaveBeenCalledWith({ ...DATA[1], country: 'DE' });
    expect(document.querySelector('.mn-world-map__outline')).not.toBeNull();
    await user.keyboard('{Escape}');
    expect(live()).toHaveTextContent('');
    await expectNoA11yViolations();
  });

  it('shows a tooltip with details on hover and selects on click', () => {
    const onCountrySelect = vi.fn();
    const { container } = render(map({ onCountrySelect }));
    const us = container.querySelector('[data-code="US"]')!;
    fireEvent.pointerMove(us, { pointerType: 'mouse' });
    const tip = container.querySelector('.mn-chart__tooltip')!;
    expect(tip).toHaveTextContent('United States');
    expect(tip).toHaveTextContent('Users340');
    expect(live()).toHaveTextContent('');
    fireEvent.click(us);
    expect(onCountrySelect).toHaveBeenCalledWith({ ...DATA[0], country: 'US' });
    fireEvent.pointerMove(container.querySelector('[data-code="FR"]')!, { pointerType: 'mouse' });
    expect(container.querySelector('.mn-chart__tooltip')).toBeNull();
  });

  it('draws bubbles sized by value on a neutral map', () => {
    const { container } = render(map({ variant: 'bubble' }));
    expect(container.querySelector('.mn-world-map')).toHaveAttribute('data-variant', 'bubble');
    const bubbles = [...container.querySelectorAll('.mn-world-map__bubble')];
    expect(bubbles).toHaveLength(4);
    const r = (c: string) =>
      Number(
        container.querySelector(`.mn-world-map__bubble[data-country="${c}"]`)!.getAttribute('r'),
      );
    expect(r('US')).toBeGreaterThan(r('DE'));
    expect(container.querySelector('[data-code="US"]')).not.toHaveAttribute('data-level');
    expect(screen.queryByRole('list', { name: 'Legend' })).toBeNull();
  });

  it('uses custom names, thresholds, colour and words', () => {
    const { container } = render(
      map({
        color: 2,
        thresholds: [100, 500, 1000],
        countryName: (c) => `Land ${c}`,
        labels: { unplaced: (n) => `Ailleurs : ${n}`, summary: (n, top) => `${n} pays — ${top}` },
      }),
    );
    expect(container.querySelector('[data-code="DE"]')).toHaveAttribute('data-level', '2');
    expect(container.querySelector('.mn-world-map')).toHaveStyle({
      '--_c': 'var(--mn-color-data-2)',
    });
    expect(screen.getByText('Ailleurs : Land XX')).toBeInTheDocument();
    expect(screen.getByRole('group', { name: /^5 pays — Land US 1,200/ })).toBeInTheDocument();
  });

  it('supports a controlled active country and the empty state', () => {
    const onActiveCountryChange = vi.fn();
    const { container, rerender } = render(map({ activeCountry: 'in', onActiveCountryChange }));
    expect(container.querySelector('.mn-chart__tooltip')).toHaveTextContent('India');
    fireEvent.pointerMove(container.querySelector('[data-code="US"]')!, { pointerType: 'mouse' });
    expect(onActiveCountryChange).toHaveBeenCalledWith('US');
    rerender(map({ data: [] }));
    expect(container.querySelector('figure')).toHaveAttribute('data-status', 'empty');
  });

  it('renders on the server', () => {
    const html = renderToString(map());
    expect(html).toContain('mn-world-map');
    expect(html).toContain('data-level="4"');
  });
});
