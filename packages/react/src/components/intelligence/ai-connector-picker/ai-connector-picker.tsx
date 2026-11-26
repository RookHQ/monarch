'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { warnOnce } from '../../../internal/dev-warn.js';
import {
  ChevronDownGlyph,
  ErrorGlyph,
  EyeGlyph,
  LockGlyph,
  OfflineGlyph,
  PencilGlyph,
  SuccessGlyph,
  WarningGlyph,
} from '../../../internal/icons.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Button } from '../../actions/button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { RadioGroup, RadioGroupItem } from '../../forms/radio-group/index.js';
import { Switch } from '../../forms/switch/index.js';
import { SearchInput } from '../../search/search-input/index.js';

/** Whether the connection behind a connector works right now. */
export type AiConnectorStatus = 'connected' | 'needs-sign-in' | 'error' | 'unavailable';

/** What a tool can do: only read, change data, or delete / do something that cannot be undone. */
export type AiConnectorToolRisk = 'read' | 'write' | 'destructive';

/** Run without asking, ask the person each time, or never run. */
export type AiConnectorToolPermission = 'allow' | 'ask' | 'never';

/** One action a connector offers to the assistant. */
export interface AiConnectorTool {
  /** Unique within its connector. Passed to `onPermissionChange`. */
  id: string;
  /** Short name, e.g. "Create event". */
  name: string;
  /** One line on what the tool does. */
  description?: string | undefined;
  /** What the tool can do to the person's data. */
  risk: AiConnectorToolRisk;
  /** What the assistant may do with the tool. */
  permission: AiConnectorToolPermission;
}

/** An outside tool or data source the assistant can be allowed to use. */
export interface AiConnector {
  /** Unique id. Passed to every callback. */
  id: string;
  /** Display name, e.g. "Calendar". Search filters on it. */
  name: string;
  /** One line on what the connector gives the assistant. */
  description?: string | undefined;
  /** Decorative icon or logo. Default: the first letter of the name. */
  icon?: ReactNode;
  /** Whether the connection works. Only a `connected` connector can be turned on. */
  status: AiConnectorStatus;
  /** Who it is connected as, e.g. an email address. */
  account?: string | undefined;
  /** Whether the assistant may use it in this conversation. */
  enabled: boolean;
  /** The tools it offers. With none, the row has no expandable section. */
  tools?: readonly AiConnectorTool[] | undefined;
}

/** The numbers behind the summary line. */
export interface AiConnectorPickerCounts {
  /** Connectors that are connected and turned on. */
  on: number;
  /** All connectors, ignoring the search. */
  total: number;
  /** Tools set to "Ask first" in connectors that are on. */
  needApproval: number;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiConnectorPickerMessages {
  /** Accessible name of the whole picker. */
  label: string;
  /** Accessible name and placeholder of the search field. */
  searchLabel: string;
  /** Accessible name of the clear button in the search field. */
  clearSearch: string;
  /** Accessible name of the connector list. */
  listLabel: string;
  /** The summary line. `format` formats a number for the locale. */
  summary: (counts: AiConnectorPickerCounts, format: (value: number) => string) => string;
  /** Announced after the search text changes. */
  results: (count: number, format: (value: number) => string) => string;
  /** Shown when the search matches nothing. */
  noResults: (query: string) => string;
  /** Shown when there are no connectors at all. */
  empty: string;
  /** Status text shown beside its icon. */
  statuses: Record<AiConnectorStatus, string>;
  /** Text before the account, e.g. "Connected as ada@example.com". */
  account: (account: string) => string;
  /** Accessible name of a connector's switch. */
  enable: (name: string) => string;
  /** Why the switch is disabled, per status that is not `connected`. */
  disabledReasons: Record<Exclude<AiConnectorStatus, 'connected'>, string>;
  /** Button shown for `needs-sign-in`. */
  signIn: string;
  /** Button shown for `error`. */
  reconnect: string;
  /** Button shown when `onManage` is set. */
  manage: string;
  /** Accessible name of a row button: the action and the connector, e.g. "Sign in: Calendar". */
  actionLabel: (action: string, name: string) => string;
  /** Text of the button that shows or hides a connector's tools, e.g. "4 tools". */
  tools: (count: number, format: (value: number) => string) => string;
  /** Accessible name of a connector's tool list. */
  toolsLabel: (name: string) => string;
  /** Accessible name of one tool's permission choice. */
  permissionLabel: (tool: string) => string;
  /** The three permission choices. */
  permissions: Record<AiConnectorToolPermission, string>;
  /** The mark on each tool saying what it can do. */
  risks: Record<AiConnectorToolRisk, string>;
  /** Why "Allow" is disabled on a destructive tool. */
  destructiveLocked: string;
}

export const AI_CONNECTOR_PICKER_MESSAGES: AiConnectorPickerMessages = {
  label: 'Connectors',
  searchLabel: 'Search connectors',
  clearSearch: 'Clear search',
  listLabel: 'Connectors',
  summary: ({ on, total, needApproval }, format) =>
    `${format(on)} of ${format(total)} ${total === 1 ? 'connector' : 'connectors'} on · ${format(
      needApproval,
    )} ${needApproval === 1 ? 'tool asks' : 'tools ask'} first`,
  results: (count, format) =>
    count === 0
      ? 'No connectors found'
      : `${format(count)} ${count === 1 ? 'connector' : 'connectors'} found`,
  noResults: (query) => `No connectors match “${query}”.`,
  empty: 'No connectors are available.',
  statuses: {
    connected: 'Connected',
    'needs-sign-in': 'Sign-in needed',
    error: 'Connection problem',
    unavailable: 'Unavailable',
  },
  account: (account) => `as ${account}`,
  enable: (name) => `Use ${name} in this conversation`,
  disabledReasons: {
    'needs-sign-in': 'Sign in before turning this on.',
    error: 'Reconnect before turning this on.',
    unavailable: 'This connector cannot be used right now.',
  },
  signIn: 'Sign in',
  reconnect: 'Reconnect',
  manage: 'Manage',
  actionLabel: (action, name) => `${action}: ${name}`,
  tools: (count, format) => `${format(count)} ${count === 1 ? 'tool' : 'tools'}`,
  toolsLabel: (name) => `${name} tools`,
  permissionLabel: (tool) => `Permission for ${tool}`,
  permissions: { allow: 'Allow', ask: 'Ask first', never: 'Never' },
  risks: { read: 'Reads data', write: 'Changes data', destructive: 'Can delete data' },
  destructiveLocked: 'Tools that can delete data cannot run without asking.',
};

/** `messages` with every level optional. */
export type AiConnectorPickerMessageOverrides = Partial<
  Omit<AiConnectorPickerMessages, 'statuses' | 'disabledReasons' | 'permissions' | 'risks'>
> & {
  statuses?: Partial<AiConnectorPickerMessages['statuses']> | undefined;
  disabledReasons?: Partial<AiConnectorPickerMessages['disabledReasons']> | undefined;
  permissions?: Partial<AiConnectorPickerMessages['permissions']> | undefined;
  risks?: Partial<AiConnectorPickerMessages['risks']> | undefined;
};

/** True when the assistant can actually use the connector: connected and turned on. */
export function isAiConnectorActive(connector: Pick<AiConnector, 'status' | 'enabled'>): boolean {
  return connector.enabled && connector.status === 'connected';
}

/**
 * The counts in the summary line: connectors that are on (connected and
 * enabled), all connectors, and tools set to "Ask first" in the ones that are on.
 */
export function aiConnectorCounts(connectors: readonly AiConnector[]): AiConnectorPickerCounts {
  let on = 0;
  let needApproval = 0;
  for (const connector of connectors) {
    if (!isAiConnectorActive(connector)) continue;
    on += 1;
    for (const tool of connector.tools ?? []) if (tool.permission === 'ask') needApproval += 1;
  }
  return { on, total: connectors.length, needApproval };
}

/** Connectors whose name contains the query, ignoring case, accents and outer spaces. */
export function filterAiConnectors<T extends Pick<AiConnector, 'name'>>(
  connectors: readonly T[],
  query: string,
  locale?: string | undefined,
): T[] {
  const fold = (text: string) =>
    text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLocaleLowerCase(locale);
  const needle = fold(query.trim());
  if (!needle) return [...connectors];
  return connectors.filter((connector) => fold(connector.name).includes(needle));
}

export interface AiConnectorPickerProps extends Omit<HTMLAttributes<HTMLElement>, 'onChange'> {
  /** The connectors, with their current state. The product owns this data; update it in the callbacks. */
  connectors: readonly AiConnector[];
  /** Called when the person flips a connector's switch. */
  onEnabledChange?: ((connectorId: string, enabled: boolean) => void) | undefined;
  /** Called when the person picks Allow, Ask first or Never for a tool. */
  onPermissionChange?:
    | ((connectorId: string, toolId: string, permission: AiConnectorToolPermission) => void)
    | undefined;
  /** Called by "Sign in" (`needs-sign-in`) and "Reconnect" (`error`). You run the sign-in. */
  onConnect?: ((connectorId: string) => void) | undefined;
  /** Shows a "Manage" button on every row, e.g. to open the connector's settings. */
  onManage?: ((connectorId: string) => void) | undefined;
  /**
   * Lets a destructive tool be set to "Allow". Default false: the option is
   * disabled with an explanation, so such a tool always asks or never runs.
   */
  allowDestructiveAutoApprove?: boolean | undefined;
  /** Shows the search field. Default true. */
  searchable?: boolean | undefined;
  /** The search text (controlled). */
  query?: string | undefined;
  /** The initial search text (uncontrolled). */
  defaultQuery?: string | undefined;
  /** Called with the new search text. */
  onQueryChange?: ((query: string) => void) | undefined;
  /** Ids of the connectors whose tools are shown (controlled). */
  expandedIds?: readonly string[] | undefined;
  /** Ids of the connectors whose tools are shown at first (uncontrolled). */
  defaultExpandedIds?: readonly string[] | undefined;
  /** Called with the new list of expanded connector ids. */
  onExpandedIdsChange?: ((ids: string[]) => void) | undefined;
  /** BCP 47 locale for numbers and for matching the search. Default: the runtime's. */
  locale?: string | undefined;
  /** Replaces any of the English strings; nested groups may be partial. */
  messages?: AiConnectorPickerMessageOverrides | undefined;
}

const STATUS_ICON: Record<AiConnectorStatus, ReactNode> = {
  connected: <SuccessGlyph />,
  'needs-sign-in': <LockGlyph />,
  error: <ErrorGlyph />,
  unavailable: <OfflineGlyph />,
};

const RISK_ICON: Record<AiConnectorToolRisk, ReactNode> = {
  read: <EyeGlyph />,
  write: <PencilGlyph />,
  destructive: <WarningGlyph />,
};

const RISK_TONE = { read: 'neutral', write: 'warning', destructive: 'danger' } as const;

const PERMISSIONS: readonly AiConnectorToolPermission[] = ['allow', 'ask', 'never'];

const NO_IDS: readonly string[] = [];

/**
 * Where a person chooses which outside tools and data sources an assistant may
 * use in this conversation: a searchable list of connectors, each with a
 * switch, its connection status, a sign-in button when needed, and its tools
 * with an Allow / Ask first / Never choice. It only reports decisions; the
 * product signs in, stores and enforces them.
 */
export const AiConnectorPicker = forwardRef<HTMLElement, AiConnectorPickerProps>(
  function AiConnectorPicker(
    {
      connectors,
      onEnabledChange,
      onPermissionChange,
      onConnect,
      onManage,
      allowDestructiveAutoApprove = false,
      searchable = true,
      query: queryProp,
      defaultQuery = '',
      onQueryChange,
      expandedIds: expandedProp,
      defaultExpandedIds = NO_IDS,
      onExpandedIdsChange,
      locale,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const { statuses, disabledReasons, permissions, risks, ...flat } = messagesProp ?? {};
    const m: AiConnectorPickerMessages = {
      ...AI_CONNECTOR_PICKER_MESSAGES,
      ...flat,
      statuses: { ...AI_CONNECTOR_PICKER_MESSAGES.statuses, ...statuses },
      disabledReasons: { ...AI_CONNECTOR_PICKER_MESSAGES.disabledReasons, ...disabledReasons },
      permissions: { ...AI_CONNECTOR_PICKER_MESSAGES.permissions, ...permissions },
      risks: { ...AI_CONNECTOR_PICKER_MESSAGES.risks, ...risks },
    };
    const uid = useId();
    const format = useMemo(() => {
      const formatter = new Intl.NumberFormat(locale);
      return (value: number) => formatter.format(value);
    }, [locale]);

    const [query, setQuery] = useControllable(queryProp, defaultQuery, onQueryChange);
    const [expanded, setExpanded] = useControllable<readonly string[]>(
      expandedProp,
      defaultExpandedIds,
      onExpandedIdsChange as ((ids: readonly string[]) => void) | undefined,
    );

    const visible = filterAiConnectors(connectors, query, locale);
    const counts = aiConnectorCounts(connectors);
    const searching = query.trim() !== '';

    // Announce the result count when the search text changes, never on first render.
    const lastQuery = useRef(query);
    const [live, setLive] = useState('');
    useEffect(() => {
      if (lastQuery.current === query) return;
      lastQuery.current = query;
      setLive(m.results(visible.length, format));
      // Only a new search is news.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [query]);

    const toggle = (id: string) =>
      setExpanded(
        expanded.includes(id) ? expanded.filter((other) => other !== id) : [...expanded, id],
      );

    const labelled = rest['aria-labelledby'] !== undefined;

    return (
      <section
        ref={ref}
        // A named group, not a region: a page may show several, and landmarks must be unique.
        role="group"
        aria-label={labelled ? undefined : m.label}
        {...rest}
        className={cx('mn-ai-connector-picker', className)}
      >
        {searchable ? (
          <SearchInput
            className="mn-ai-connector-picker__search"
            size="sm"
            aria-label={m.searchLabel}
            placeholder={m.searchLabel}
            clearLabel={m.clearSearch}
            value={query}
            onValueChange={setQuery}
          />
        ) : null}

        <p className="mn-ai-connector-picker__summary">{m.summary(counts, format)}</p>

        {visible.length === 0 ? (
          <div className="mn-ai-connector-picker__empty">
            <p className="mn-ai-connector-picker__empty-text">
              {searching ? m.noResults(query.trim()) : m.empty}
            </p>
            {searching ? (
              <Button size="sm" variant="secondary" onClick={() => setQuery('')}>
                {m.clearSearch}
              </Button>
            ) : null}
          </div>
        ) : (
          <ul className="mn-ai-connector-picker__list" aria-label={m.listLabel}>
            {visible.map((connector) => {
              const base = `${uid}-${connector.id}`;
              const nameId = `${base}-name`;
              const statusId = `${base}-status`;
              const reasonId = `${base}-reason`;
              const panelId = `${base}-tools`;
              const connected = connector.status === 'connected';
              const tools = connector.tools ?? [];
              const open = tools.length > 0 && expanded.includes(connector.id);
              const connectLabel =
                connector.status === 'needs-sign-in'
                  ? m.signIn
                  : connector.status === 'error'
                    ? m.reconnect
                    : undefined;
              return (
                <li
                  key={connector.id}
                  className="mn-ai-connector-picker__item"
                  data-status={connector.status}
                  data-enabled={isAiConnectorActive(connector) || undefined}
                  data-expanded={open || undefined}
                >
                  <div className="mn-ai-connector-picker__row">
                    <span className="mn-ai-connector-picker__icon" aria-hidden>
                      {connector.icon ?? Array.from(connector.name.trim())[0]?.toUpperCase()}
                    </span>
                    <div className="mn-ai-connector-picker__text">
                      <span id={nameId} className="mn-ai-connector-picker__name">
                        {connector.name}
                      </span>
                      {connector.description ? (
                        <span className="mn-ai-connector-picker__description">
                          {connector.description}
                        </span>
                      ) : null}
                      <span id={statusId} className="mn-ai-connector-picker__status">
                        <span className="mn-ai-connector-picker__status-icon" aria-hidden>
                          {STATUS_ICON[connector.status]}
                        </span>
                        <span>
                          {m.statuses[connector.status]}
                          {connected && connector.account ? ` ${m.account(connector.account)}` : ''}
                        </span>
                      </span>
                      {connector.status !== 'connected' ? (
                        <span id={reasonId} className="mn-ai-connector-picker__reason">
                          {m.disabledReasons[connector.status]}
                        </span>
                      ) : null}
                    </div>
                    <Switch
                      className="mn-ai-connector-picker__switch"
                      size="sm"
                      aria-label={m.enable(connector.name)}
                      aria-describedby={cx(statusId, connected ? undefined : reasonId)}
                      checked={connector.enabled}
                      disabled={!connected}
                      onCheckedChange={(next) => onEnabledChange?.(connector.id, next)}
                    />
                  </div>

                  {connectLabel || onManage || tools.length ? (
                    <div className="mn-ai-connector-picker__actions">
                      {tools.length ? (
                        <Button
                          size="sm"
                          variant="ghost"
                          className="mn-ai-connector-picker__toggle"
                          aria-expanded={open}
                          aria-controls={panelId}
                          aria-label={m.actionLabel(m.tools(tools.length, format), connector.name)}
                          trailingIcon={<ChevronDownGlyph />}
                          onClick={() => toggle(connector.id)}
                        >
                          {m.tools(tools.length, format)}
                        </Button>
                      ) : null}
                      {connectLabel ? (
                        <Button
                          size="sm"
                          variant="secondary"
                          aria-label={m.actionLabel(connectLabel, connector.name)}
                          onClick={() => onConnect?.(connector.id)}
                        >
                          {connectLabel}
                        </Button>
                      ) : null}
                      {onManage ? (
                        <Button
                          size="sm"
                          variant="ghost"
                          aria-label={m.actionLabel(m.manage, connector.name)}
                          onClick={() => onManage(connector.id)}
                        >
                          {m.manage}
                        </Button>
                      ) : null}
                    </div>
                  ) : null}

                  {tools.length ? (
                    <ul
                      id={panelId}
                      className="mn-ai-connector-picker__tools"
                      aria-label={m.toolsLabel(connector.name)}
                      hidden={!open}
                    >
                      {open
                        ? tools.map((tool) => {
                            const locked =
                              tool.risk === 'destructive' && !allowDestructiveAutoApprove;
                            const lockId = `${base}-${tool.id}-lock`;
                            if (locked && tool.permission === 'allow') {
                              warnOnce(
                                `AiConnectorPicker: destructive tool "${tool.id}" has permission "allow" but allowDestructiveAutoApprove is false. It is shown as set; store "ask" or "never" instead.`,
                              );
                            }
                            return (
                              <li
                                key={tool.id}
                                className="mn-ai-connector-picker__tool"
                                data-risk={tool.risk}
                                data-permission={tool.permission}
                              >
                                <div className="mn-ai-connector-picker__tool-text">
                                  <span className="mn-ai-connector-picker__tool-heading">
                                    <span className="mn-ai-connector-picker__tool-name">
                                      {tool.name}
                                    </span>
                                    <Badge
                                      size="sm"
                                      tone={RISK_TONE[tool.risk]}
                                      className="mn-ai-connector-picker__risk"
                                    >
                                      <span
                                        className="mn-ai-connector-picker__risk-icon"
                                        aria-hidden
                                      >
                                        {RISK_ICON[tool.risk]}
                                      </span>
                                      {m.risks[tool.risk]}
                                    </Badge>
                                  </span>
                                  {tool.description ? (
                                    <span className="mn-ai-connector-picker__tool-description">
                                      {tool.description}
                                    </span>
                                  ) : null}
                                </div>
                                <div className="mn-ai-connector-picker__permission">
                                  <RadioGroup
                                    orientation="horizontal"
                                    aria-label={m.permissionLabel(tool.name)}
                                    value={tool.permission}
                                    onValueChange={(next) => {
                                      const permission = next as AiConnectorToolPermission;
                                      if (permission === 'allow' && locked) return;
                                      onPermissionChange?.(connector.id, tool.id, permission);
                                    }}
                                  >
                                    {PERMISSIONS.map((permission) => {
                                      const off = permission === 'allow' && locked;
                                      return (
                                        <RadioGroupItem
                                          key={permission}
                                          value={permission}
                                          label={m.permissions[permission]}
                                          disabled={off}
                                          {...(off ? { 'aria-describedby': lockId } : {})}
                                        />
                                      );
                                    })}
                                  </RadioGroup>
                                  {locked ? (
                                    <span id={lockId} className="mn-ai-connector-picker__lock">
                                      <span
                                        aria-hidden
                                        className="mn-ai-connector-picker__lock-icon"
                                      >
                                        <LockGlyph />
                                      </span>
                                      {m.destructiveLocked}
                                    </span>
                                  ) : null}
                                </div>
                              </li>
                            );
                          })
                        : null}
                    </ul>
                  ) : null}
                </li>
              );
            })}
          </ul>
        )}

        <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
          {live}
        </span>
      </section>
    );
  },
);
