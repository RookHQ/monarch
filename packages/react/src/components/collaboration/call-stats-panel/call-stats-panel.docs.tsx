import { CallStatsPanel } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const usage = {
  sentBytes: 6_200_000,
  receivedBytes: 9_400_000,
  upBps: 420_000,
  downBps: 1_250_000,
};

export default {
  id: 'call-stats-panel',
  title: 'CallStatsPanel',
  summary: 'Data used by a call and each person’s round trip, packet loss and connection grade.',
  sections: {
    purpose: {
      summary:
        'Answers "why is this call choppy, and how much data is it using?" with a few numbers people can act on. It takes plain numbers; the product reads them from WebRTC `getStats()`. Needed by: Chat.',
      useWhen: [
        'A "Connection and data use" panel opened from the call’s More menu.',
        'Support or diagnostics views for a live call.',
      ],
      avoidWhen: ['A single connection indicator on a tile: use CallQualityBars inside CallTile.'],
    },
    designPrinciples: {
      items: [
        'Few numbers, in units people know: MB, kbps/Mbps, milliseconds and percent.',
        'Unknown is honest: a dash or "Not measured yet", never a made-up zero.',
        'Loss shows the worse direction, because either one hurts the call.',
        'How media is routed or encrypted is product copy, passed as `footnote`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`CallStatsPanel` forwards its ref to the root `<div>`.',
        '`formatCallBytes(bytes)` and `formatCallRate(bitsPerSecond)` are exported for use elsewhere in the call UI.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`usage` is `{ sentBytes, receivedBytes, upBps?, downBps? }`; `peers` are `{ id, name, rttMs?, inboundLossPct?, outboundLossPct?, quality? }`.',
        'Migrating from the app copy (Rook Chat `CallStatsPanel`): `active.usage` → `usage` (`recvBytes` → `receivedBytes`); `active.peers` with `metrics` and member names → `peers` (flatten `metrics.rttMs`, `inboundLossPct`, `outboundLossPct` and resolve `name` in the product); `active.lowData` → `lowData`; the relay/encryption sentence → `footnote`.',
      ],
    },
    variants: {
      items: [],
      notes: ['No variants. `lowData` adds a line; `footnote` adds a closing note.'],
    },
    states: {
      items: [
        {
          name: 'No peers',
          description: 'Alone on the call: "No one else is connected yet." instead of the table.',
        },
        {
          name: 'Not measured',
          description: 'Missing numbers show a dash; an unknown grade says "Not measured yet".',
        },
      ],
    },
    interaction: {
      items: [],
      notes: [
        'Not interactive. Re-render with new numbers as stats arrive (every one or two seconds is plenty).',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive; the table is reachable with screen reader table navigation.'],
    },
    accessibility: {
      role: 'table (with a visually hidden caption)',
      name: '`messages.tableCaption` ("Connection to each person")',
      items: [
        'Column headers use `scope="col"` and each person’s name is a row header.',
        'Connection bars are images named "Good connection" and so on.',
        'The usage heading is a level-3 heading, so the panel nests under a dialog or popover title.',
      ],
    },
    responsive: {
      items: [
        'Numbers never wrap; on narrow screens the table scrolls sideways inside the panel instead of the page.',
        'Long names truncate with an ellipsis.',
      ],
    },
    theme: {
      items: [
        'Text uses `--mn-color-text-default` and `--mn-color-text-muted`; row rules use `--mn-color-border-subtle`.',
        'Forced colours: row rules use CanvasText; bars use system colours.',
      ],
    },
    examples: {
      items: [
        {
          title: 'During a group call',
          render: () => (
            <div style={{ maxWidth: 360 }}>
              <CallStatsPanel
                usage={usage}
                peers={[
                  {
                    id: 'a',
                    name: 'Robin Park',
                    rttMs: 38,
                    inboundLossPct: 0.2,
                    outboundLossPct: 0.1,
                    quality: 'good',
                  },
                  {
                    id: 'b',
                    name: 'Alex Chen',
                    rttMs: 186,
                    inboundLossPct: 3.4,
                    outboundLossPct: 1.2,
                    quality: 'fair',
                  },
                  { id: 'c', name: 'Jordan Diaz', quality: 'unknown' },
                ]}
                footnote="Example footnote: say here how your product routes and protects call media."
              />
            </div>
          ),
          stack: true,
        },
        {
          title: 'Low data mode, alone',
          render: () => (
            <div style={{ maxWidth: 360 }}>
              <CallStatsPanel usage={{ sentBytes: 210_000, receivedBytes: 0 }} peers={[]} lowData />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show it in a Popover or Sheet from the call’s More menu.',
        'Smooth the numbers in the product (for example a short moving average) so they do not jump.',
      ],
      dont: [
        'Do not claim encryption or routing in `footnote` that the product does not do.',
        'Do not show raw `getStats()` dumps here.',
      ],
    },
    related: {
      items: [
        { id: 'call-controls', reason: 'Its `onOpenStats` opens this panel.' },
        { id: 'call-tile', reason: 'Shows the same connection bars per person.' },
        { id: 'popover', reason: 'A good container for the panel during a call.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['call-tile'] },
    testing: {
      unit: [
        'formatCallBytes and formatCallRate.',
        'Usage line, low-data line, table with rounded round trip, worse-direction loss, named bars, dashes for missing numbers and the footnote; axe passes.',
        'Empty state without peers; messages translate strings; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
