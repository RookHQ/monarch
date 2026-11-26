import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiTraceView,
  aiTraceDefaultExpandedIds,
  aiTraceToRunEvents,
  aiTraceToSubagentTree,
  aiTraceTotals,
  formatAiTraceCost,
  formatAiTraceDuration,
  otelSpansToAiTrace,
  type AiTraceSpan,
  type OtlpTraceData,
} from './index.js';

const SPANS: AiTraceSpan[] = [
  { id: 'run', kind: 'agent', name: 'Answer question', status: 'error', startMs: 0, endMs: 6000 },
  {
    id: 'plan',
    parentId: 'run',
    kind: 'model',
    name: 'Plan',
    status: 'ok',
    startMs: 0,
    endMs: 1500,
    inputTokens: 1800,
    outputTokens: 200,
    costMicros: 3100,
    model: 'large-model',
    input: '<img src=x onerror="alert(1)"> plan the answer',
    output: 'Search, then summarise.',
  },
  {
    id: 'search',
    parentId: 'run',
    kind: 'retrieval',
    name: 'Search records',
    status: 'ok',
    startMs: 1500,
    endMs: 2000,
    attributes: { index: 'records', hits: 14, cached: false },
  },
  {
    id: 'sub',
    parentId: 'run',
    kind: 'agent',
    name: 'Analysis agent',
    status: 'ok',
    startMs: 2000,
    endMs: 5000,
  },
  {
    id: 'sub-model',
    parentId: 'sub',
    kind: 'model',
    name: 'Analyse',
    status: 'ok',
    startMs: 2000,
    endMs: 4500,
    inputTokens: 4000,
    outputTokens: 900,
    costMicros: 9800,
  },
  {
    id: 'fetch',
    parentId: 'run',
    kind: 'tool',
    name: 'fetch_report',
    status: 'error',
    startMs: 5000,
    endMs: 6000,
    error: 'Timed out after 1 second.',
  },
];

const rowNames = () =>
  screen
    .getAllByRole('row')
    .slice(1)
    .map((r) => r.querySelector('.mn-ai-trace-view__name')?.textContent);
const row = (name: string | RegExp) => screen.getByRole('row', { name });

describe('aiTraceTotals', () => {
  it('adds duration, tokens, cost and errors', () => {
    const totals = aiTraceTotals(SPANS);
    expect(totals).toMatchObject({
      startMs: 0,
      endMs: 6000,
      durationMs: 6000,
      inputTokens: 5800,
      outputTokens: 1100,
      totalTokens: 6900,
      errorCount: 2,
      runningCount: 0,
      spanCount: 6,
    });
    expect(totals.costs).toEqual([{ currency: 'USD', micros: 12900 }]);
  });

  it('keeps currencies apart, ends running spans at nowMs and handles an empty trace', () => {
    const totals = aiTraceTotals(
      [
        {
          id: 'a',
          kind: 'model',
          name: 'a',
          status: 'ok',
          startMs: 100,
          endMs: 300,
          costMicros: 5,
        },
        {
          id: 'b',
          kind: 'model',
          name: 'b',
          status: 'running',
          startMs: 300,
          costMicros: 7,
          currency: 'EUR',
        },
      ],
      { nowMs: 900, currency: 'GBP' },
    );
    expect(totals.costs).toEqual([
      { currency: 'GBP', micros: 5 },
      { currency: 'EUR', micros: 7 },
    ]);
    expect(totals).toMatchObject({ startMs: 100, endMs: 900, durationMs: 800, runningCount: 1 });
    expect(aiTraceTotals([])).toMatchObject({ durationMs: 0, totalTokens: 0, costs: [] });
  });
});

describe('formatAiTraceDuration and formatAiTraceCost', () => {
  it('writes milliseconds, seconds and minutes', () => {
    const fmt = (ms: number) => formatAiTraceDuration(ms, { locale: 'en-US' });
    expect(fmt(0)).toBe('0 ms');
    expect(fmt(0.4)).toBe('0.4 ms');
    expect(fmt(850)).toBe('850 ms');
    expect(fmt(999.7)).toBe('1 s');
    expect(fmt(1420)).toBe('1.42 s');
    expect(fmt(12_540)).toBe('12.5 s');
    expect(fmt(59_990)).toBe('1 min');
    expect(fmt(125_000)).toBe('2 min 5 s');
    expect(fmt(-5)).toBe('0 ms');
  });

  it('follows the locale and custom unit labels', () => {
    expect(formatAiTraceDuration(1420, { locale: 'de-DE', units: { s: 'Sek.' } })).toBe(
      '1,42 Sek.',
    );
  });

  it('formats money from micros, with more decimals under one unit', () => {
    expect(formatAiTraceCost(1_250_000, 'USD', 'en-US')).toBe('$1.25');
    expect(formatAiTraceCost(3100, 'USD', 'en-US')).toBe('$0.0031');
  });
});

describe('aiTraceDefaultExpandedIds', () => {
  it('expands top-level spans, errors and their ancestors, and survives a parent loop', () => {
    expect(aiTraceDefaultExpandedIds(SPANS).sort()).toEqual(['fetch', 'run']);
    const loop: AiTraceSpan[] = [
      { id: 'a', parentId: 'b', kind: 'tool', name: 'a', status: 'error', startMs: 0, endMs: 1 },
      { id: 'b', parentId: 'a', kind: 'tool', name: 'b', status: 'ok', startMs: 0, endMs: 1 },
    ];
    expect(aiTraceDefaultExpandedIds(loop).sort()).toEqual(['a', 'b']);
  });
});

describe('AiTraceView', () => {
  it('renders a named group with a tree grid, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiTraceView ref={ref} spans={SPANS} data-testid="root" className="x" locale="en-US" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-trace-view', 'x');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Run trace' })).toBe(root);
    const grid = screen.getByRole('treegrid', { name: 'Run trace' });
    const headers = within(grid).getAllByRole('columnheader');
    expect(headers.slice(0, 5).map((h) => h.textContent)).toEqual([
      'Span',
      'Kind',
      'Duration',
      'Tokens',
      'Cost',
    ]);
    // The scale end beside "Timeline" is decorative; the name is the column label.
    expect(headers[5]).toHaveAccessibleName('Timeline');
    await expectNoA11yViolations();
  });

  it('shows totals in the header', () => {
    render(<AiTraceView spans={SPANS} locale="en-US" />);
    const totals = document.querySelector('.mn-ai-trace-view__totals') as HTMLElement;
    expect(totals).toHaveTextContent('Total time6 s');
    expect(totals).toHaveTextContent('Tokens6,900');
    expect(totals).toHaveTextContent('Cost$0.0129');
    expect(totals).toHaveTextContent('Errors2');
  });

  it('renders only rows of expanded parents, with level, position and cell values', () => {
    render(<AiTraceView spans={SPANS} locale="en-US" />);
    // The root is expanded by default; the healthy sub-agent is not.
    expect(rowNames()).toEqual([
      'Answer question',
      'Plan',
      'Search records',
      'Analysis agent',
      'fetch_report',
    ]);
    const plan = row(/^Plan/);
    expect(plan).toHaveAttribute('aria-level', '2');
    expect(plan).toHaveAttribute('aria-posinset', '1');
    expect(plan).toHaveAttribute('aria-setsize', '4');
    expect(plan).not.toHaveAttribute('aria-expanded');
    expect(
      within(plan)
        .getAllByRole('gridcell')
        .slice(1, 5)
        .map((c) => c.textContent),
    ).toEqual(['Model', '1.5 s', '2,000', '$0.0031']);
    expect(row(/^Analysis agent/)).toHaveAttribute('aria-expanded', 'false');
    expect(row(/^Answer question/)).toHaveAttribute('aria-expanded', 'true');
    expect(row(/^fetch_report/)).toHaveTextContent('Error');
  });

  it('draws each bar on the shared scale and gives it a text alternative', () => {
    render(<AiTraceView spans={SPANS} locale="en-US" />);
    const search = row(/^Search records/);
    const bar = search.querySelector('.mn-ai-trace-view__bar') as HTMLElement;
    expect(bar.style.insetInlineStart).toBe('25%');
    expect(parseFloat(bar.style.inlineSize)).toBeCloseTo(8.33, 1);
    expect(bar.parentElement).toHaveAttribute('aria-hidden', 'true');
    expect(search).toHaveTextContent('Starts at +1.5 s, takes 500 ms');
  });

  it('runs spans without an end to nowMs and reads them as running', () => {
    render(
      <AiTraceView
        locale="en-US"
        nowMs={4000}
        spans={[
          { id: 'a', kind: 'model', name: 'Draft', status: 'running', startMs: 1000 },
          { id: 'b', kind: 'tool', name: 'Lookup', status: 'ok', startMs: 0, endMs: 1000 },
        ]}
      />,
    );
    const draft = row(/^Draft/);
    expect(draft).toHaveTextContent('Starts at +1 s, still running');
    const bar = draft.querySelector('.mn-ai-trace-view__bar') as HTMLElement;
    expect(bar.style.insetInlineStart).toBe('25%');
    expect(bar.style.inlineSize).toBe('75%');
    expect(within(draft).getAllByRole('gridcell')[2]).toHaveTextContent('Running');
  });

  it('supports the keyboard model: arrows, Home and End, expand and collapse', async () => {
    const user = userEvent.setup();
    render(<AiTraceView spans={SPANS} locale="en-US" />);
    await user.tab(); // filter
    await user.tab(); // the grid: one Tab stop
    expect(row(/^Answer question/)).toHaveFocus();

    await user.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}');
    const sub = row(/^Analysis agent/);
    expect(sub).toHaveFocus();
    expect(sub).toHaveAttribute('tabindex', '0');
    expect(row(/^Answer question/)).toHaveAttribute('tabindex', '-1');

    // Right opens a closed parent, then moves into it.
    await user.keyboard('{ArrowRight}');
    expect(sub).toHaveAttribute('aria-expanded', 'true');
    expect(sub).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(row(/^Analyse/)).toHaveFocus();
    expect(row(/^Analyse/)).toHaveAttribute('aria-level', '3');

    // Left on a leaf moves to its parent, then closes it.
    await user.keyboard('{ArrowLeft}');
    expect(sub).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(sub).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('row', { name: /^Analyse/ })).toBeNull();

    await user.keyboard('{End}');
    expect(row(/^fetch_report/)).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(row(/^fetch_report/)).toHaveFocus();
    await user.keyboard('{Home}');
    expect(row(/^Answer question/)).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(row(/^Answer question/)).toHaveFocus();
  });

  it('selects with Enter and opens the details, then closes them', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(<AiTraceView spans={SPANS} locale="en-US" onSelect={onSelect} />);
    expect(screen.queryByRole('region')).toBeNull();
    row(/^Answer question/).focus();
    await user.keyboard('{End}{Enter}');
    expect(onSelect).toHaveBeenLastCalledWith('fetch');
    expect(row(/^fetch_report/)).toHaveAttribute('aria-selected', 'true');
    const details = screen.getByRole('region', { name: 'Details: fetch_report' });
    expect(details).toHaveTextContent('StatusError');
    expect(details).toHaveTextContent('Start+5 s');
    expect(details).toHaveTextContent('End+6 s');
    expect(details).toHaveTextContent('Duration1 s');
    expect(details).toHaveTextContent('Timed out after 1 second.');
    expect(screen.getByText('Showing details for fetch_report')).toHaveAttribute('role', 'status');
    await expectNoA11yViolations();

    await user.click(within(details).getByRole('button', { name: 'Close details' }));
    expect(onSelect).toHaveBeenLastCalledWith(null);
    expect(screen.queryByRole('region')).toBeNull();
  });

  it('shows model, tokens, attributes and plain-text previews with Copy', async () => {
    const user = userEvent.setup();
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } });
    render(<AiTraceView spans={SPANS} locale="en-US" defaultSelectedId="plan" />);
    const details = screen.getByRole('region', { name: 'Details: Plan' });
    expect(details).toHaveTextContent('Modellarge-model');
    expect(details).toHaveTextContent('Input tokens1,800');
    expect(details).toHaveTextContent('Output tokens200');
    expect(details).toHaveTextContent('Cost$0.0031');
    // Markup in a preview is text, not elements.
    const input = within(details).getByRole('group', { name: 'Input' });
    expect(input).toHaveAttribute('tabindex', '0');
    expect(input.querySelector('pre')).toHaveTextContent(
      '<img src=x onerror="alert(1)"> plan the answer',
    );
    expect(details.querySelector('img')).toBeNull();
    await user.click(within(details).getByRole('button', { name: 'Copy Output' }));
    expect(writeText).toHaveBeenCalledWith('Search, then summarise.');

    await user.click(row(/^Search records/));
    const search = screen.getByRole('region', { name: 'Details: Search records' });
    expect(within(search).getByText('index').nextElementSibling).toHaveTextContent('records');
    expect(within(search).getByText('hits').nextElementSibling).toHaveTextContent('14');
    expect(within(search).getByText('cached').nextElementSibling).toHaveTextContent('false');
    expect(within(search).queryByRole('group')).toBeNull();
  });

  it('toggles with the chevron without selecting, and reports uncontrolled changes', async () => {
    const user = userEvent.setup();
    const onExpandedChange = vi.fn();
    const onSelect = vi.fn();
    render(
      <AiTraceView
        spans={SPANS}
        defaultExpandedIds={[]}
        onExpandedChange={onExpandedChange}
        onSelect={onSelect}
      />,
    );
    expect(rowNames()).toEqual(['Answer question']);
    await user.click(document.querySelector('[data-toggle]') as HTMLElement);
    expect(onExpandedChange).toHaveBeenLastCalledWith(['run']);
    expect(onSelect).not.toHaveBeenCalled();
    expect(rowNames()).toHaveLength(5);
  });

  it('is controllable: expandedIds, selectedId and filter come from props', async () => {
    const user = userEvent.setup();
    const onExpandedChange = vi.fn();
    const onSelect = vi.fn();
    const onFilterChange = vi.fn();
    const { rerender } = render(
      <AiTraceView
        spans={SPANS}
        expandedIds={['run']}
        onExpandedChange={onExpandedChange}
        selectedId={null}
        onSelect={onSelect}
        filter="all"
        onFilterChange={onFilterChange}
      />,
    );
    row(/^Analysis agent/).focus();
    await user.keyboard('{ArrowRight}{Enter}');
    expect(onExpandedChange).toHaveBeenLastCalledWith(['run', 'sub']);
    expect(onSelect).toHaveBeenLastCalledWith('sub');
    // Nothing changes until the parent passes new props.
    expect(row(/^Analysis agent/)).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('region')).toBeNull();
    await user.click(screen.getByRole('radio', { name: 'Errors only' }));
    expect(onFilterChange).toHaveBeenLastCalledWith('errors');
    expect(rowNames()).toHaveLength(5);

    rerender(
      <AiTraceView spans={SPANS} expandedIds={['run', 'sub']} selectedId="sub" filter="errors" />,
    );
    expect(screen.getByRole('region', { name: 'Details: Analysis agent' })).toBeInTheDocument();
    expect(rowNames()).toEqual(['Answer question', 'fetch_report']);
  });

  it('filters to errors with their ancestors, and sorts siblings slowest first', async () => {
    const user = userEvent.setup();
    function Host() {
      const [spans, setSpans] = useState(SPANS);
      return (
        <>
          <AiTraceView spans={spans} />
          <button onClick={() => setSpans(SPANS.filter((s) => s.status !== 'error'))}>fix</button>
        </>
      );
    }
    render(<Host />);
    await user.click(screen.getByRole('radio', { name: 'Slowest first' }));
    expect(rowNames()).toEqual([
      'Answer question',
      'Analysis agent',
      'Plan',
      'fetch_report',
      'Search records',
    ]);
    await user.click(screen.getByRole('radio', { name: 'Errors only' }));
    expect(rowNames()).toEqual(['Answer question', 'fetch_report']);
    expect(row(/^fetch_report/)).toHaveAttribute('aria-setsize', '1');
    await user.click(screen.getByRole('button', { name: 'fix' }));
    expect(screen.queryByRole('treegrid')).toBeNull();
    expect(screen.getByText('No errors in this run')).toBeInTheDocument();
  });

  it('shows an empty state and treats an unknown parent as top level', () => {
    const { rerender } = render(<AiTraceView spans={[]} />);
    expect(screen.getByText('No spans recorded')).toBeInTheDocument();
    expect(screen.queryByRole('treegrid')).toBeNull();
    rerender(
      <AiTraceView
        spans={[
          {
            id: 'x',
            parentId: 'gone',
            kind: 'other',
            name: 'Orphan',
            status: 'cancelled',
            startMs: 0,
          },
        ]}
      />,
    );
    expect(row(/^Orphan/)).toHaveAttribute('aria-level', '1');
    expect(row(/^Orphan/)).toHaveTextContent('Cancelled');
  });

  it('localises strings, numbers and money', async () => {
    render(
      <AiTraceView
        spans={SPANS}
        locale="de-DE"
        currency="EUR"
        defaultSelectedId="fetch"
        messages={{
          label: 'Ablauf',
          columns: { name: 'Schritt' },
          kinds: { model: 'Modell' },
          statuses: { error: 'Fehler' },
          filters: { errors: 'Nur Fehler' },
          units: { s: 'Sek.' },
          details: (name) => `Details zu ${name}`,
          closeDetails: 'Schließen',
        }}
      />,
    );
    expect(screen.getByRole('treegrid', { name: 'Ablauf' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Schritt' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Nur Fehler' })).toBeInTheDocument();
    const plan = row(/^Plan/);
    const cells = within(plan).getAllByRole('gridcell');
    expect(cells[1]).toHaveTextContent('Modell');
    expect(cells[2]).toHaveTextContent('1,5 Sek.');
    expect(cells[3]).toHaveTextContent('2.000');
    expect(cells[4]?.textContent).toMatch(/^0,0031\s€$/);
    const details = screen.getByRole('region', { name: 'Details zu fetch_report' });
    expect(details).toHaveTextContent('StatusFehler');
    expect(within(details).getByRole('button', { name: 'Schließen' })).toBeInTheDocument();
    // Untranslated strings keep their English default.
    expect(screen.getByRole('columnheader', { name: 'Kind' })).toBeInTheDocument();
  });

  it('stays light with 500 spans: collapsed children are not rendered', () => {
    const many: AiTraceSpan[] = [
      { id: 'root', kind: 'agent', name: 'Root', status: 'ok', startMs: 0, endMs: 5000 },
      ...Array.from({ length: 499 }, (_, i): AiTraceSpan => ({
        id: `s${i}`,
        parentId: i < 9 ? 'root' : `s${i % 9}`,
        kind: 'tool',
        name: `step ${i}`,
        status: 'ok',
        startMs: i * 10,
        endMs: i * 10 + 5,
      })),
    ];
    render(<AiTraceView spans={many} />);
    // Root and its nine children; the 490 grandchildren stay out of the DOM.
    expect(screen.getAllByRole('row')).toHaveLength(1 + 10);
    expect(aiTraceTotals(many).spanCount).toBe(500);
  });

  it('shows the members of a parent loop at the top level instead of dropping them', () => {
    render(
      <AiTraceView
        spans={[
          { id: 'a', parentId: 'b', kind: 'agent', name: 'Loop A', status: 'ok', startMs: 0 },
          { id: 'b', parentId: 'a', kind: 'agent', name: 'Loop B', status: 'ok', startMs: 5 },
          { id: 'c', parentId: 'a', kind: 'tool', name: 'Child of A', status: 'ok', startMs: 6 },
          { id: 'd', kind: 'tool', name: 'No start', status: 'ok', startMs: Number.NaN },
        ]}
        defaultExpandedIds={['a']}
      />,
    );
    expect(row(/^Loop A/)).toHaveAttribute('aria-level', '1');
    expect(row(/^Loop B/)).toHaveAttribute('aria-level', '1');
    expect(row(/^Child of A/)).toHaveAttribute('aria-level', '2');
    // A span without a usable start still gets a real bar position.
    const bar = row(/^No start/).querySelector('.mn-ai-trace-view__bar') as HTMLElement;
    expect(bar.style.insetInlineStart).toBe('0%');
  });

  it('links a span to its conversation part: data attributes and a button in the details', async () => {
    const user = userEvent.setup();
    const onOpenPart = vi.fn();
    const linked: AiTraceSpan[] = SPANS.map((s) =>
      s.id === 'fetch' ? { ...s, partId: 'call-7', messageId: 'msg-2' } : s,
    );
    const { rerender } = render(<AiTraceView spans={linked} onOpenPart={onOpenPart} />);
    expect(row(/^fetch_report/)).toHaveAttribute('data-part-id', 'call-7');
    expect(row(/^fetch_report/)).toHaveAttribute('data-message-id', 'msg-2');
    expect(row(/^Plan/)).not.toHaveAttribute('data-part-id');
    await user.click(row(/^Plan/));
    expect(screen.queryByRole('button', { name: 'Show in conversation' })).toBeNull();
    await user.click(row(/^fetch_report/));
    await user.click(screen.getByRole('button', { name: 'Show in conversation' }));
    expect(onOpenPart).toHaveBeenCalledWith(
      { partId: 'call-7', messageId: 'msg-2' },
      expect.objectContaining({ id: 'fetch' }),
    );
    // Without the callback there is nothing to press.
    rerender(<AiTraceView spans={linked} />);
    expect(screen.queryByRole('button', { name: 'Show in conversation' })).toBeNull();
  });

  describe('windowing', () => {
    const big: AiTraceSpan[] = [
      { id: 'root', kind: 'agent', name: 'Root', status: 'ok', startMs: 0, endMs: 50_000 },
      ...Array.from({ length: 4999 }, (_, i): AiTraceSpan => ({
        id: `s${i}`,
        parentId: 'root',
        kind: 'tool',
        name: `step ${i}`,
        status: 'ok',
        startMs: i * 10,
        endMs: i * 10 + 5,
      })),
    ];
    const dataRows = () =>
      screen.getAllByRole('row').filter((r) => r.hasAttribute('data-row-index'));

    it('renders a window of 5000 visible rows with their place in the grid', async () => {
      const { container } = render(<AiTraceView spans={big} />);
      const grid = screen.getByRole('treegrid');
      expect(grid).toHaveAttribute('aria-rowcount', '5001');
      const shown = dataRows();
      expect(shown.length).toBeGreaterThan(10);
      expect(shown.length).toBeLessThan(60);
      expect(shown[0]).toHaveAttribute('aria-rowindex', '2');
      expect(shown[0]).toHaveAttribute('tabindex', '0');
      // The rows that are not rendered still take their space.
      const gaps = container.querySelectorAll<HTMLElement>('.mn-ai-trace-view__gap');
      expect(gaps).toHaveLength(1);
      expect(parseFloat(gaps[0]?.style.blockSize ?? '0')).toBeGreaterThan(100_000);
      expect(gaps[0]?.parentElement).toHaveAttribute('aria-hidden', 'true');
      await expectNoA11yViolations(container);
    });

    it('moves the window when the box scrolls', () => {
      const { container } = render(<AiTraceView spans={big} />);
      const scroller = container.querySelector('.mn-ai-trace-view__scroll') as HTMLElement;
      expect(scroller).toHaveAttribute('data-windowed');
      Object.defineProperty(scroller, 'scrollTop', { configurable: true, value: 33 * 2000 });
      fireEvent.scroll(scroller);
      expect(screen.queryByRole('row', { name: /^step 5 / })).toBeNull();
      const shown = dataRows();
      expect(shown.length).toBeLessThan(60);
      expect(shown.some((r) => r.getAttribute('data-row-index') === '2000')).toBe(true);
      // No focused or selected row: the Tab stop is the first row in view.
      expect(shown.filter((r) => r.tabIndex === 0)).toHaveLength(1);
      expect(container.querySelectorAll('.mn-ai-trace-view__gap')).toHaveLength(2);
    });

    it('keeps the keyboard model: End and Home reach rows outside the window, and focus survives scrolling', async () => {
      const user = userEvent.setup();
      const { container } = render(<AiTraceView spans={big} />);
      await user.tab();
      await user.tab();
      expect(row(/^Root/)).toHaveFocus();
      await user.keyboard('{End}');
      const last = row(/^step 4998 /);
      expect(last).toHaveFocus();
      expect(last).toHaveAttribute('aria-rowindex', '5001');
      expect(last).toHaveAttribute('aria-posinset', '4999');
      await user.keyboard('{ArrowUp}');
      expect(row(/^step 4997 /)).toHaveFocus();
      // Scrolling away keeps the focused row in the document.
      const scroller = container.querySelector('.mn-ai-trace-view__scroll') as HTMLElement;
      Object.defineProperty(scroller, 'scrollTop', { configurable: true, value: 33 * 1000 });
      fireEvent.scroll(scroller);
      expect(row(/^step 4997 /)).toHaveFocus();
      await user.keyboard('{Home}');
      expect(row(/^Root/)).toHaveFocus();
      await user.keyboard('{Enter}');
      expect(screen.getByRole('region', { name: 'Details: Root' })).toBeInTheDocument();
    });

    it('renders every row under the threshold or when windowing is off', () => {
      const some = big.slice(0, 301);
      const { rerender } = render(<AiTraceView spans={some} windowThreshold={Infinity} />);
      expect(dataRows()).toHaveLength(301);
      expect(screen.getByRole('treegrid')).not.toHaveAttribute('aria-rowcount');
      rerender(<AiTraceView spans={some} windowThreshold={100} />);
      expect(dataRows().length).toBeLessThan(60);
    });

    it('renders the first window on the server', () => {
      const html = renderToString(<AiTraceView spans={big} />);
      expect(html).toContain('aria-rowcount="5001"');
      expect(html).toContain('>step 5<');
      expect(html).not.toContain('>step 400<');
    });
  });

  it('renders on the server', () => {
    const html = renderToString(<AiTraceView spans={SPANS} defaultSelectedId="fetch" />);
    expect(html).toContain('mn-ai-trace-view');
    expect(html).toContain('role="treegrid"');
    expect(html).toContain('Timed out after 1 second.');
  });
});

const NS = 1_700_000_000_000_000_000n;
const at = (ms: number) => (NS + BigInt(ms) * 1_000_000n).toString();

const OTLP: OtlpTraceData = {
  resourceSpans: [
    {
      resource: { attributes: [{ key: 'service.name', value: { stringValue: 'assistant' } }] },
      scopeSpans: [
        {
          scope: { name: 'gen-ai' },
          spans: [
            {
              traceId: 't1',
              spanId: 'agent',
              parentSpanId: '',
              name: 'invoke_agent Researcher',
              startTimeUnixNano: at(0),
              endTimeUnixNano: at(6000),
              attributes: [
                { key: 'gen_ai.operation.name', value: { stringValue: 'invoke_agent' } },
                { key: 'gen_ai.agent.name', value: { stringValue: 'Researcher' } },
                { key: 'gen_ai.agent.description', value: { stringValue: 'Finds filings' } },
              ],
              status: { code: 1 },
            },
            {
              traceId: 't1',
              spanId: 'chat',
              parentSpanId: 'agent',
              name: 'chat large-model',
              startTimeUnixNano: at(100),
              endTimeUnixNano: (NS + 1_500_250_000n).toString(),
              attributes: [
                { key: 'gen_ai.operation.name', value: { stringValue: 'chat' } },
                { key: 'gen_ai.request.model', value: { stringValue: 'large-model' } },
                { key: 'gen_ai.response.model', value: { stringValue: 'large-model-2' } },
                { key: 'gen_ai.usage.input_tokens', value: { intValue: '1800' } },
                { key: 'gen_ai.usage.output_tokens', value: { intValue: 200 } },
                { key: 'gen_ai.response.id', value: { stringValue: 'msg-1' } },
                { key: 'gen_ai.request.temperature', value: { doubleValue: 0.2 } },
                {
                  key: 'gen_ai.response.finish_reasons',
                  value: { arrayValue: { values: [{ stringValue: 'stop' }] } },
                },
                { key: 'gen_ai.input.messages', value: { stringValue: 'Which filings?' } },
                { key: 'gen_ai.output.messages', value: { stringValue: 'These three.' } },
              ],
            },
            {
              traceId: 't1',
              spanId: 'tool',
              parentSpanId: 'agent',
              name: 'execute_tool fetch_report',
              startTimeUnixNano: at(2000),
              endTimeUnixNano: at(3000),
              attributes: [
                { key: 'gen_ai.operation.name', value: { stringValue: 'execute_tool' } },
                { key: 'gen_ai.tool.name', value: { stringValue: 'fetch_report' } },
                { key: 'gen_ai.tool.call.id', value: { stringValue: 'call-7' } },
                { key: 'gen_ai.tool.call.arguments', value: { stringValue: '{"q":4}' } },
                { key: 'error.type', value: { stringValue: 'timeout' } },
              ],
              status: { code: 'STATUS_CODE_ERROR' },
              events: [
                {
                  name: 'exception',
                  attributes: [
                    { key: 'exception.message', value: { stringValue: 'Timed out after 1 s' } },
                  ],
                },
              ],
            },
            {
              traceId: 't1',
              spanId: 'orphan',
              parentSpanId: 'not-exported',
              name: 'vector search',
              startTimeUnixNano: at(3000),
              attributes: [{ key: 'db.system', value: { stringValue: 'pgvector' } }],
            },
            // The same span exported twice, and one from another trace.
            { traceId: 't1', spanId: 'tool', name: 'duplicate', startTimeUnixNano: at(1) },
            { traceId: 't2', spanId: 'other-trace', name: 'elsewhere', startTimeUnixNano: at(50) },
          ],
        },
      ],
    },
  ],
};

describe('otelSpansToAiTrace', () => {
  it('maps OTLP/JSON GenAI spans: kinds, names, model, tokens, times and links', () => {
    const spans = otelSpansToAiTrace(OTLP, { traceId: 't1' });
    expect(spans.map((s) => s.id)).toEqual(['agent', 'chat', 'tool', 'orphan']);
    const [agent, chat, tool] = spans as [AiTraceSpan, AiTraceSpan, AiTraceSpan];
    expect(agent).toMatchObject({
      kind: 'agent',
      name: 'Researcher',
      status: 'ok',
      startMs: 0,
      endMs: 6000,
    });
    expect(agent.parentId).toBeUndefined();
    expect(chat).toMatchObject({
      parentId: 'agent',
      kind: 'model',
      name: 'chat large-model',
      status: 'ok',
      startMs: 100,
      endMs: 1500.25,
      inputTokens: 1800,
      outputTokens: 200,
      model: 'large-model-2',
      messageId: 'msg-1',
      input: 'Which filings?',
      output: 'These three.',
    });
    // Attributes the mapping does not know are kept; content never is.
    expect(chat.attributes).toEqual({
      'gen_ai.request.temperature': 0.2,
      'gen_ai.response.finish_reasons': '["stop"]',
    });
    expect(tool).toMatchObject({
      parentId: 'agent',
      kind: 'tool',
      name: 'fetch_report',
      status: 'error',
      error: 'Timed out after 1 s',
      partId: 'call-7',
      input: '{"q":4}',
    });
    expect(tool.attributes).toEqual({ 'error.type': 'timeout' });
  });

  it('shows orphans at the top level and reads a span without an end as running', () => {
    const spans = otelSpansToAiTrace(OTLP, { traceId: 't1' });
    const orphan = spans.find((s) => s.id === 'orphan') as AiTraceSpan;
    expect(orphan.parentId).toBeUndefined();
    expect(orphan).toMatchObject({ kind: 'retrieval', status: 'running', startMs: 3000 });
    expect(orphan.endMs).toBeUndefined();
    // Without a trace filter every span is kept, once per id.
    expect(otelSpansToAiTrace(OTLP).map((s) => s.id)).toEqual([
      'agent',
      'other-trace',
      'chat',
      'tool',
      'orphan',
    ]);
  });

  it('reads the flat exported shape: microseconds, time pairs and plain attributes', () => {
    const spans = otelSpansToAiTrace([
      {
        traceId: 't',
        id: 'root',
        parentId: undefined,
        name: 'invoke_agent',
        timestamp: 1_700_000_000_000_000,
        duration: 2_500_000,
        attributes: { 'gen_ai.operation.name': 'invoke_agent', 'gen_ai.agent.id': 'a1' },
        status: { code: 0 },
      },
      {
        traceId: 't',
        spanId: 'call',
        parentSpanContext: { spanId: 'root' },
        name: 'chat',
        startTime: [1_700_000_000, 500_000_000],
        endTime: [1_700_000_001, 250_000_000],
        attributes: {
          'gen_ai.request.model': 'small-model',
          'gen_ai.usage.prompt_tokens': 12,
          'gen_ai.usage.completion_tokens': '7',
          tags: ['a', 'b'],
          nothing: null,
        },
        status: { code: 2, message: 'rate limited' },
      },
      {
        traceId: 't',
        id: 'open',
        parentId: '0000000000000000',
        name: 'still going',
        timestamp: 1_700_000_000_100_000,
        ended: false,
        attributes: { 'error.type': 'cancelled' },
      },
      { name: 'no id at all' },
    ]);
    expect(spans.map((s) => s.id)).toEqual(['root', 'open', 'call']);
    const call = spans.find((s) => s.id === 'call') as AiTraceSpan;
    expect(spans[0]).toMatchObject({
      kind: 'agent',
      startMs: 0,
      endMs: 2500,
      name: 'invoke_agent',
    });
    expect(call).toMatchObject({
      parentId: 'root',
      kind: 'model',
      status: 'error',
      error: 'rate limited',
      startMs: 500,
      endMs: 1250,
      inputTokens: 12,
      outputTokens: 7,
      model: 'small-model',
      attributes: { tags: '["a","b"]' },
    });
    // Cancelled spans have ended, even with no end time.
    expect(spans[1]).toMatchObject({ status: 'cancelled', startMs: 100, endMs: 100 });
    expect(spans[1]?.parentId).toBeUndefined();
  });

  it('applies the options: origin, content, attributes, kind, cost and link', () => {
    const spans = otelSpansToAiTrace(OTLP, {
      traceId: 't1',
      originUnixNano: (NS - 1_000_000_000n).toString(),
      content: false,
      attributes: 'none',
      kind: ({ attributes }) => (attributes['db.system'] ? 'guardrail' : undefined),
      cost: ({ mapped }) =>
        mapped.kind === 'model' ? { micros: 3100, currency: 'EUR' } : undefined,
      link: ({ span }) => ({ partId: `part-${'spanId' in span ? span.spanId : ''}` }),
    });
    const chat = spans.find((s) => s.id === 'chat') as AiTraceSpan;
    expect(chat.startMs).toBe(1100);
    expect(chat.input).toBeUndefined();
    expect(chat.output).toBeUndefined();
    expect(chat.attributes).toBeUndefined();
    expect(chat).toMatchObject({ costMicros: 3100, currency: 'EUR', partId: 'part-chat' });
    expect(chat.messageId).toBeUndefined();
    expect(spans.find((s) => s.id === 'orphan')?.kind).toBe('guardrail');
    expect(otelSpansToAiTrace(OTLP, { attributes: 'all' })[0]?.attributes).toMatchObject({
      'gen_ai.operation.name': 'invoke_agent',
    });
    expect(otelSpansToAiTrace({})).toEqual([]);
    expect(otelSpansToAiTrace([])).toEqual([]);
  });

  it('feeds AiTraceView', () => {
    render(<AiTraceView spans={otelSpansToAiTrace(OTLP, { traceId: 't1' })} locale="en-US" />);
    expect(row(/^Researcher/)).toHaveAttribute('aria-expanded', 'true');
    expect(row(/^fetch_report/)).toHaveAttribute('data-status', 'error');
    expect(row(/^fetch_report/)).toHaveAttribute('data-part-id', 'call-7');
    expect(row(/^vector search/)).toHaveAttribute('aria-level', '1');
  });
});

describe('aiTraceToRunEvents and aiTraceToSubagentTree', () => {
  it('turns spans into run events with clock times and links', () => {
    const events = aiTraceToRunEvents(
      [...SPANS, { id: 'live', kind: 'guardrail', name: 'Check', status: 'running', startMs: 10 }],
      { runStartedAt: '2026-01-01T00:00:00.000Z' },
    );
    const base = Date.parse('2026-01-01T00:00:00.000Z');
    expect(events.map((e) => e.kind)).toEqual([
      'error',
      'model',
      'tool',
      'handoff',
      'model',
      'error',
      'tool',
    ]);
    expect(events[1]).toMatchObject({
      id: 'plan',
      name: 'Plan',
      startedAt: base,
      endedAt: base + 1500,
      inputTokens: 1800,
      outputTokens: 200,
      costMicros: 3100,
    });
    expect(events[5]).toMatchObject({ detail: 'Timed out after 1 second.' });
    expect(events[6]?.endedAt).toBeUndefined();
    expect(
      aiTraceToRunEvents([{ ...(SPANS[5] as AiTraceSpan), partId: 'p', messageId: 'm' }])[0],
    ).toMatchObject({ startedAt: 5000, endedAt: 6000, partId: 'p', messageId: 'm' });
  });

  it('nests agent spans, counts their own tool calls and survives a parent loop', () => {
    const tree = aiTraceToSubagentTree(
      [
        ...SPANS,
        {
          id: 'sub-tool',
          parentId: 'sub-model',
          kind: 'tool',
          name: 'sum',
          status: 'ok',
          startMs: 2100,
          endMs: 2200,
        },
        {
          id: 'late',
          parentId: 'run',
          kind: 'agent',
          name: 'Writer',
          status: 'running',
          startMs: 5500,
          messageId: 'm9',
        },
        {
          id: 'late-step',
          parentId: 'late',
          kind: 'model',
          name: 'Drafting',
          status: 'running',
          startMs: 5600,
        },
      ],
      { runStartedAt: 0 },
    );
    expect(tree).toHaveLength(1);
    const lead = tree[0];
    expect(lead).toMatchObject({
      id: 'run',
      name: 'Answer question',
      status: 'failed',
      toolCalls: 1,
    });
    expect(lead?.startedAt).toBe('1970-01-01T00:00:00.000Z');
    expect(lead?.endedAt).toBe('1970-01-01T00:00:06.000Z');
    expect(lead?.children?.map((c) => c.id)).toEqual(['sub', 'late']);
    expect(lead?.children?.[0]).toMatchObject({ status: 'done', toolCalls: 1 });
    expect(lead?.children?.[1]).toMatchObject({
      status: 'working',
      activity: 'Drafting',
      messageId: 'm9',
    });
    // No run start: no clock times.
    expect(aiTraceToSubagentTree(SPANS)[0]?.startedAt).toBeUndefined();
    const looped = aiTraceToSubagentTree([
      { id: 'a', parentId: 'b', kind: 'agent', name: 'A', status: 'ok', startMs: 0 },
      { id: 'b', parentId: 'a', kind: 'agent', name: 'B', status: 'cancelled', startMs: 1 },
    ]);
    expect(looped.map((n) => [n.id, n.status, n.children])).toEqual([
      ['a', 'done', undefined],
      ['b', 'stopped', undefined],
    ]);
  });
});
