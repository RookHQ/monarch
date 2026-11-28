import { useEffect, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { LogViewer, type LogLine, type LogLevel } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './log-viewer.fixture.json';

const start = Date.UTC(2026, 0, 5, 9, 30, 0);
const steps = [
  'Cloning repository',
  'Installing dependencies',
  'Resolved 412 packages',
  'Compiling 128 modules',
  'Cache miss for build step, rebuilding',
  'Running tests',
  'Test suite passed: 96 tests',
  'Uploading build output',
  'Retrying upload after a timeout',
  'Upload failed: connection reset by peer',
];
const levelFor = (i: number): LogLevel =>
  i % 10 === 9 ? 'error' : i % 10 === 4 || i % 10 === 8 ? 'warn' : i % 10 === 2 ? 'debug' : 'info';

const sampleLines: LogLine[] = Array.from({ length: 2000 }, (_, i) => ({
  id: String(i),
  level: levelFor(i),
  time: start + i * 250,
  source: i % 3 === 0 ? 'builder' : 'runner',
  message: `${steps[i % steps.length]} (step ${i + 1})`,
}));

/** The first lines of the build log, as the primary example shows them. */
const firstLines: LogLine[] = (
  fixture.data.lines as {
    id: string;
    level: LogLevel;
    time: string;
    source: string;
    message: string;
  }[]
).map((line) => ({ ...line, time: Date.parse(line.time) }));

const utcTime = (d: Date) => d.toISOString().slice(11, 23);

function Streaming() {
  const [lines, setLines] = useState<LogLine[]>(() => firstLines);
  const [running, setRunning] = useState(true);
  useEffect(() => {
    if (!running) return;
    const timer = setInterval(() => {
      setLines((prev) => {
        const next = sampleLines[prev.length];
        return next ? [...prev, next] : prev;
      });
    }, 400);
    return () => clearInterval(timer);
  }, [running]);
  return (
    <LogViewer
      lines={lines}
      formatTime={utcTime}
      actions={
        <Button size="sm" variant="ghost" onClick={() => setRunning((r) => !r)}>
          {running ? fixture.data.pause : 'Resume stream'}
        </Button>
      }
    />
  );
}

export default {
  id: 'log-viewer',
  title: 'LogViewer',
  summary:
    'Virtualized log lines with level filters, search highlighting, follow (tail) mode and a wrap toggle.',
  sections: {
    purpose: {
      summary:
        'Reads a stream or a long dump of plain log lines (build output, job runs, function logs) without stalling the page: only the lines in view are rendered, new lines are followed until the reader scrolls up, and levels and search narrow the noise. Needed by: none recorded yet. No Rook app shows line logs today; Rook Developers shows request logs as a table with LogDrawer, which this does not replace.',
      useWhen: [
        'Showing hundreds to hundreds of thousands of text lines, possibly arriving live.',
        'Readers need to filter by severity or search for an error.',
      ],
      avoidWhen: [
        'Structured records with columns (request logs, audit events): use a DataTable, AuditLogViewer or WebhookDeliveries.',
        'Chat-like messages with authors and days: use VirtualMessageList.',
        'A few lines of output: use CodeBlock.',
      ],
    },
    designPrinciples: {
      items: [
        'Follow by default, but never fight the reader: scrolling up pauses following, and reaching the bottom (or End, or the jump button) resumes it.',
        'Severity is text first ("Error"), colour second.',
        'Lines are text, never HTML; search marks are built from text slices.',
        'The viewer filters and displays; fetching, streaming and retention are the caller’s job.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Append to `lines` to stream; keep ids stable. Lines without a level count as `info`.',
        'Levels, query, follow and wrap are each controlled or uncontrolled (`levels`/`defaultLevels`, `query`/`defaultQuery`, `follow`/`defaultFollow`, `wrap`/`defaultWrap`).',
        '`formatLogTime(date)` is the default `HH:MM:SS.mmm` local formatter; `LOG_LEVELS` lists the levels lowest first.',
        'Forwards its ref and other HTML attributes to the root `div`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Rows are measured after render, so wrapped lines of any height scroll correctly; `estimatedRowHeight` only matters before measurement.',
        'The default time format uses the browser’s time zone, which can differ from the server’s; pass `formatTime` with a fixed zone when you server-render.',
        'There is no shadcn/ui equivalent in the apps, so no prop mapping is needed.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'searchMode',
          values: ['filter', 'highlight'],
          description:
            '`filter` hides lines without a match (default); `highlight` keeps them and marks matches.',
        },
        {
          prop: 'showTimestamps',
          values: ['true', 'false'],
          description: 'Time column. Default true.',
        },
        {
          prop: 'showLineNumbers',
          values: ['true', 'false'],
          description: 'Position in `lines`, not affected by filters. Default false.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Following',
          attribute: 'data-follow',
          description: 'Newest line kept in view; the Follow button is pressed.',
        },
        {
          name: 'Paused',
          description: 'Following stopped; a "Jump to latest" or "N new lines" button appears.',
        },
        {
          name: 'Wrapping',
          attribute: 'data-wrap',
          description: 'Long lines wrap instead of scrolling sideways.',
        },
        {
          name: 'Level',
          attribute: 'data-level on each line',
          description: 'debug, info, warn or error; warn and error rows are tinted.',
        },
        { name: 'Loading', attribute: 'aria-busy', description: 'First load with no lines yet.' },
        {
          name: 'Empty / no matches',
          description: '`empty` (default "No log lines yet.") or "No lines match the filters."',
        },
      ],
    },
    interaction: {
      items: [
        'Scrolling away from the bottom pauses following; scrolling back to the bottom resumes it.',
        'Level buttons toggle each level and show how many lines it has.',
        'The search filters (or highlights) as you type, matching message and source.',
        'Follow and Wrap lines are toggle buttons.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Search, level buttons, Follow, Wrap lines, any actions, then the log region.',
        },
        {
          keys: 'ArrowUp / ArrowDown / PageUp / PageDown',
          action: 'Scroll the focused log (browser default).',
        },
        { keys: 'End', action: 'In the log: jump to the newest line and resume following.' },
        { keys: 'Home', action: 'In the log: jump to the first line and pause following.' },
        { keys: 'Enter / Space', action: 'Toggle the focused filter or toggle button.' },
      ],
    },
    accessibility: {
      role: 'region (focusable) containing a list',
      name: '`messages.label` (default "Log").',
      items: [
        'The log is not a live region, so streamed lines do not flood screen readers; following paused or resumed is announced politely.',
        'Each line is a listitem with `aria-setsize` and `aria-posinset`, so virtualized lines are still counted.',
        'Toggles use `aria-pressed`; the level group is named "Levels".',
        'Times are `<time datetime>` elements; matches are `<mark>`.',
        'Windows High Contrast: pressed toggles use Highlight, warn and error lines get a start border, matches use Mark.',
      ],
    },
    responsive: {
      items: [
        'The toolbar wraps at 390px; the search field takes the first row.',
        'Below 30rem the log is 60% of the viewport tall.',
        'Unwrapped lines scroll sideways inside the log only; turn on wrap for narrow screens.',
        'Buttons follow Button’s 44px coarse-pointer target.',
      ],
    },
    theme: {
      items: [
        'Log background `--mn-color-bg-subtle`, mono text `--mn-font-mono`.',
        'Warn and error rows use `--mn-color-warning-subtle` and `--mn-color-danger-subtle`; level labels use the matching strong tokens.',
        'RTL: toolbar and columns follow logical properties; the jump button sits at the inline end.',
      ],
    },
    examples: {
      items: [
        { title: 'Streaming build output', stack: true, render: () => <Streaming /> },
        {
          title: 'Static dump with line numbers, wrapped, errors only',
          stack: true,
          render: () => (
            <LogViewer
              lines={sampleLines}
              defaultFollow={false}
              defaultWrap
              showLineNumbers
              defaultLevels={['error']}
              formatTime={utcTime}
              messages={{ label: 'Saved build log' }}
            />
          ),
        },
        {
          title: 'Highlight search without filtering',
          stack: true,
          render: () => (
            <LogViewer
              lines={sampleLines.slice(0, 200)}
              searchMode="highlight"
              defaultQuery="upload"
              defaultFollow={false}
              formatTime={utcTime}
              messages={{ label: 'Upload log' }}
            />
          ),
        },
        {
          title: 'Empty',
          stack: true,
          render: () => (
            <LogViewer
              lines={[]}
              empty="Logs appear when the job starts."
              messages={{ label: 'Job log' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Cap retained lines in the caller (for example the last 50,000) for endless streams.',
        'Batch appends (every 100–500 ms) rather than one state update per line.',
        'Pass `formatTime` with a fixed time zone when server-rendering.',
      ],
      dont: [
        'Do not put secrets in log lines; everything is selectable.',
        'Do not use it for structured events with columns; use a table.',
      ],
    },
    related: {
      items: [
        { id: 'log-drawer', reason: 'One structured log entry in a side sheet.' },
        { id: 'audit-log-viewer', reason: 'Who-did-what events with columns and export.' },
        { id: 'virtual-message-list', reason: 'Virtualized chat messages.' },
        { id: 'code-block', reason: 'A few lines of fixed output.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'search-input'],
    },
    testing: {
      unit: [
        'Named focusable region with lines, levels, times, sources and positions.',
        'Level filter toggles with aria-pressed, callback and no-matches state.',
        'Search in filter and highlight modes with <mark>.',
        'Follow pauses on scroll up, counts new lines, resumes with the jump button and End; Home pauses; announcements.',
        'Wrap toggle; windowing of 10,000 lines; empty and loading; custom messages and time format.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
