import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CallQualityBars, type CallQuality, type CallQualityMessages } from '../call-tile/index.js';

/** Bytes as `512 B`, `1.4 KB`, `12 MB`, `1.2 GB` (powers of 1024). */
export function formatCallBytes(bytes: number): string {
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let value = Math.max(0, bytes);
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  const digits = unit === 0 || value >= 10 ? 0 : 1;
  return `${value.toFixed(digits)} ${units[unit]}`;
}

/** Bits per second as `800 bps`, `64 kbps`, `1.5 Mbps` (powers of 1000). */
export function formatCallRate(bitsPerSecond: number): string {
  const v = Math.max(0, bitsPerSecond);
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(v >= 10_000_000 ? 0 : 1)} Mbps`;
  if (v >= 1000) return `${Math.round(v / 1000)} kbps`;
  return `${Math.round(v)} bps`;
}

/** Data used by the call so far. */
export interface CallUsage {
  sentBytes: number;
  receivedBytes: number;
  /** Current upload rate, bits per second. */
  upBps?: number | undefined;
  /** Current download rate, bits per second. */
  downBps?: number | undefined;
}

/** Connection numbers for one remote participant, from the product's `getStats()` summary. */
export interface CallPeerStats {
  id: string;
  name: string;
  /** Round-trip time, milliseconds. */
  rttMs?: number | null | undefined;
  /** Packet loss on what you receive from them, percent. */
  inboundLossPct?: number | null | undefined;
  /** Packet loss they report on what you send, percent. */
  outboundLossPct?: number | null | undefined;
  quality?: CallQuality | undefined;
}

/** Visible and announced strings. Override them to translate. */
export interface CallStatsPanelMessages extends CallQualityMessages {
  dataUsed: string;
  usageSummary: (total: string, up: string, down: string) => string;
  lowData: string;
  person: string;
  roundTrip: string;
  loss: string;
  link: string;
  noValue: string;
  noPeers: string;
  tableCaption: string;
  unknownQuality: string;
}

export const DEFAULT_CALL_STATS_PANEL_MESSAGES: CallStatsPanelMessages = {
  good: 'Good connection',
  fair: 'Unstable connection',
  poor: 'Poor connection',
  dataUsed: 'Data used this call',
  usageSummary: (total, up, down) => `${total} total · up ${up} · down ${down}`,
  lowData: 'Low data mode is on: audio only.',
  person: 'Person',
  roundTrip: 'Round trip',
  loss: 'Loss',
  link: 'Link',
  noValue: '–',
  noPeers: 'No one else is connected yet.',
  tableCaption: 'Connection to each person',
  unknownQuality: 'Not measured yet',
};

export interface CallStatsPanelProps extends HTMLAttributes<HTMLDivElement> {
  /** Totals and current rates for the call. */
  usage: CallUsage;
  /** One row per remote participant. */
  peers: CallPeerStats[];
  /** Notes that low data mode is on. */
  lowData?: boolean | undefined;
  /**
   * A closing note, for example how media is routed or encrypted. Product
   * copy: Monarch does not know how the product connects calls.
   */
  footnote?: ReactNode;
  /** Override visible and announced strings. */
  messages?: Partial<CallStatsPanelMessages> | undefined;
}

/**
 * Live data use and per-person connection numbers (round trip, loss and a
 * quality grade) for a call's "Connection" panel. Takes plain numbers; the
 * product reads them from WebRTC `getStats()`.
 */
export const CallStatsPanel = forwardRef<HTMLDivElement, CallStatsPanelProps>(
  function CallStatsPanel(
    { usage, peers, lowData = false, footnote, messages: messagesProp, className, ...rest },
    ref,
  ) {
    const m = { ...DEFAULT_CALL_STATS_PANEL_MESSAGES, ...messagesProp };
    const headingId = useId();
    const total = formatCallBytes(usage.sentBytes + usage.receivedBytes);
    return (
      <div ref={ref} {...rest} className={cx('mn-call-stats-panel', className)}>
        <section aria-labelledby={headingId} className="mn-call-stats-panel__usage">
          <h3 id={headingId} className="mn-call-stats-panel__heading">
            {m.dataUsed}
          </h3>
          <p className="mn-call-stats-panel__muted">
            {m.usageSummary(
              total,
              formatCallRate(usage.upBps ?? 0),
              formatCallRate(usage.downBps ?? 0),
            )}
          </p>
          {lowData && <p className="mn-call-stats-panel__muted">{m.lowData}</p>}
        </section>
        {peers.length === 0 ? (
          <p className="mn-call-stats-panel__muted">{m.noPeers}</p>
        ) : (
          <div className="mn-call-stats-panel__scroll">
            <table className="mn-call-stats-panel__table">
              <caption className="mn-visually-hidden">{m.tableCaption}</caption>
              <thead>
                <tr>
                  <th scope="col">{m.person}</th>
                  <th scope="col">{m.roundTrip}</th>
                  <th scope="col">{m.loss}</th>
                  <th scope="col">{m.link}</th>
                </tr>
              </thead>
              <tbody>
                {peers.map((p) => {
                  const losses = [p.inboundLossPct, p.outboundLossPct].filter(
                    (v): v is number => typeof v === 'number' && Number.isFinite(v),
                  );
                  const loss = losses.length ? Math.max(...losses) : null;
                  const quality = p.quality ?? 'unknown';
                  return (
                    <tr key={p.id}>
                      <th scope="row" className="mn-call-stats-panel__name">
                        {p.name}
                      </th>
                      <td>
                        {typeof p.rttMs === 'number' ? `${Math.round(p.rttMs)} ms` : m.noValue}
                      </td>
                      <td>{loss !== null ? `${loss.toFixed(1)}%` : m.noValue}</td>
                      <td>
                        {quality === 'unknown' ? (
                          <span className="mn-call-stats-panel__muted">{m.unknownQuality}</span>
                        ) : (
                          <CallQualityBars quality={quality} messages={m} />
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
        {footnote ? <p className="mn-call-stats-panel__muted">{footnote}</p> : null}
      </div>
    );
  },
);
