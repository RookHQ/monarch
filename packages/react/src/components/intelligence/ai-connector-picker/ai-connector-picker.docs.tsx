import { useState } from 'react';
import {
  AiConnectorPicker,
  type AiConnector,
  type AiConnectorPickerProps,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-connector-picker.fixture.json';

const CONNECTORS: AiConnector[] = fixture.data.connectors.map((c) => ({
  id: c.id,
  name: c.name,
  description: c.description,
  status: c.status as AiConnector['status'],
  ...('account' in c ? { account: c.account } : {}),
  enabled: c.enabled,
  ...('tools' in c
    ? {
        tools: c.tools.map((t) => ({
          id: t.id,
          name: t.name,
          description: t.description,
          risk: t.risk as 'read' | 'write' | 'destructive',
          permission: t.permission as 'allow' | 'ask' | 'never',
        })),
      }
    : {}),
}));

/** The product owns the data: this demo keeps it in state and applies each decision. */
function Demo({
  initial = CONNECTORS,
  ...props
}: Partial<AiConnectorPickerProps> & { initial?: AiConnector[] }) {
  const [connectors, setConnectors] = useState(initial);
  return (
    <AiConnectorPicker
      {...props}
      connectors={connectors}
      onEnabledChange={(id, enabled) =>
        setConnectors((all) => all.map((c) => (c.id === id ? { ...c, enabled } : c)))
      }
      onPermissionChange={(id, toolId, permission) =>
        setConnectors((all) =>
          all.map((c) =>
            c.id === id
              ? { ...c, tools: c.tools?.map((t) => (t.id === toolId ? { ...t, permission } : t)) }
              : c,
          ),
        )
      }
      onConnect={(id) =>
        // A real product opens its sign-in here and updates the status when it succeeds.
        setConnectors((all) =>
          all.map((c) =>
            c.id === id ? { ...c, status: 'connected', account: 'ada@example.com' } : c,
          ),
        )
      }
    />
  );
}

export default {
  id: 'ai-connector-picker',
  title: 'AiConnectorPicker',
  summary:
    'Choose which outside tools and data an assistant may use in a conversation, and how far each may go.',
  sections: {
    purpose: {
      summary:
        'Puts the person in charge of what an assistant can reach: a searchable list of connectors, each with a switch for this conversation, its connection status, a way to sign in, and per-tool permissions (Allow, Ask first, Never).',
      useWhen: [
        'An assistant can call outside tools or read outside data, and the person should decide which ones for this conversation.',
        'Tools differ in risk and the person should set which run freely, which ask first and which never run.',
        'You want one place that shows what is connected, as whom, and what needs signing in.',
      ],
      avoidWhen: [
        'Approving one tool call as it happens: use AiToolApproval.',
        'Choosing which documents or folders ground the answers: use AiKnowledgeScope.',
        'A general list of on/off settings with no connections or tools: use SwitchSettingsList.',
      ],
    },
    designPrinciples: {
      items: [
        'Nothing is granted by the component: every switch, permission and sign-in is reported through a callback and the product decides and stores it.',
        'The safe choice is the default: a tool that can delete data cannot be set to Allow unless the product opts in with `allowDestructiveAutoApprove`.',
        'Status and risk are always in words with an icon, never colour alone.',
        'A disabled control always says why, in visible text linked to it.',
        'The summary counts what the assistant can actually use: a connector that is switched on but not connected does not count.',
        'Built from existing parts: SearchInput, Switch, Button, Badge and RadioGroup.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`; native attributes pass through. Pass `aria-labelledby` to name it from your own heading.',
        '`connectors` is controlled only: update it in `onEnabledChange` and `onPermissionChange`. `query` and `expandedIds` can be controlled or left to the component.',
        'Helpers: `aiConnectorCounts(connectors)` returns the numbers in the summary, `filterAiConnectors(connectors, query, locale)` the search result, `isAiConnectorActive(connector)` whether the assistant can use it.',
        'A destructive tool whose data already says `allow` while `allowDestructiveAutoApprove` is false is shown as it is (so the person can see and change it) with a development warning; the option cannot be chosen again.',
        'The component only records choices. Enforce them where tools run.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'allowDestructiveAutoApprove',
          values: ['false', 'true'],
          description:
            'Whether "Allow" can be chosen for a destructive tool. Default false: the option is disabled and explained.',
        },
        {
          prop: 'searchable',
          values: ['true', 'false'],
          description: 'Shows the search field. Turn it off for a short, fixed list.',
        },
      ],
      notes: ['There are no size or tone variants; it fills the width it is given.'],
    },
    states: {
      items: [
        {
          name: 'Connection status',
          attribute: 'data-status (on the connector item)',
          description:
            '`connected`, `needs-sign-in`, `error` or `unavailable`; also shown as text with an icon.',
        },
        {
          name: 'In use',
          attribute: 'data-enabled (on the connector item)',
          description: 'The connector is connected and switched on.',
        },
        {
          name: 'Expanded',
          attribute: 'data-expanded (on the item), aria-expanded (on the tools button)',
          description: 'The connector’s tools are shown.',
        },
        {
          name: 'Tool risk and permission',
          attribute: 'data-risk, data-permission (on the tool item)',
          description: '`read`, `write` or `destructive`; `allow`, `ask` or `never`.',
        },
        {
          name: 'No results',
          description:
            'The search matches nothing: a message with the search text and a Clear search button.',
        },
      ],
    },
    interaction: {
      items: [
        'The switch calls `onEnabledChange(connectorId, enabled)`. It is disabled unless the connector is connected, with the reason under the status.',
        '"Sign in" (needs sign-in) and "Reconnect" (error) call `onConnect(connectorId)`; you run the sign-in and update `status`.',
        'The "N tools" button shows or hides the connector’s tools; several can be open at once.',
        'Each tool has an Allow / Ask first / Never choice that calls `onPermissionChange(connectorId, toolId, permission)`.',
        'Typing in the search filters by name, ignoring case and accents; the number of results is announced.',
        '"Manage" appears on every row when `onManage` is given.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action:
            'Moves through the search field, then each connector’s switch, tools button, Sign in or Reconnect, Manage, and each open permission group (one stop per group).',
        },
        { keys: 'Space', action: 'Flips the focused switch.' },
        {
          keys: 'Enter / Space',
          action: 'Activates the focused button (tools, Sign in, Reconnect, Manage).',
        },
        {
          keys: 'Arrow keys',
          action:
            'Inside a permission group, move to and select the next or previous choice, skipping a disabled one.',
        },
        { keys: 'Escape', action: 'In the search field, clears the text.' },
      ],
    },
    accessibility: {
      role: 'group (a named `<section>`)',
      name: '`messages.label` ("Connectors"), or your own heading through `aria-labelledby`',
      items: [
        'Each switch is named "Use <connector> in this conversation" and described by the status and, when disabled, the reason.',
        'Row buttons carry the connector in their name ("Sign in: Mail", "3 tools: Calendar") and start with their visible text.',
        'The tools button has `aria-expanded` and `aria-controls`; the tools are a list named "<connector> tools".',
        'Each permission choice is a `radiogroup` of native radios named "Permission for <tool>".',
        'The disabled "Allow" on a destructive tool is described by the visible explanation.',
        'The result count is announced through a polite live region after the search text changes, never on first render.',
        'Risk and status use an icon and text; forced-colours mode keeps borders and text in system colours.',
      ],
    },
    responsive: {
      items: [
        'Rows wrap: at 390px, and in a narrow AI panel, a tool’s permission choice moves under its name and description.',
        'Buttons and radios grow to 44px touch targets on coarse pointers.',
        'RTL: logical properties throughout; arrow keys in the radio group follow the browser.',
      ],
    },
    theme: {
      items: [
        'Connectors in use take the `--mn-color-ai-border` edge; status text uses the success, warning and danger tokens.',
        'Risk marks are Badges: neutral for read, warning for write, danger for destructive.',
        'The chevron turn is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Connectors with tools',
          stack: true,
          render: () => <Demo defaultExpandedIds={fixture.data.expanded} />,
        },
        {
          title: 'Search with no match',
          stack: true,
          render: () => <Demo defaultQuery="spreadsheet" />,
        },
        {
          title: 'Destructive tools may be allowed (opt-in)',
          stack: true,
          render: () => (
            <Demo
              initial={CONNECTORS.slice(0, 1)}
              searchable={false}
              allowDestructiveAutoApprove
              defaultExpandedIds={['calendar']}
            />
          ),
        },
        {
          title: 'Translated, with Manage',
          stack: true,
          render: () => (
            <Demo
              initial={CONNECTORS.slice(1, 4)}
              locale="es"
              onManage={() => {}}
              messages={{
                label: 'Conectores',
                searchLabel: 'Buscar conectores',
                clearSearch: 'Borrar la búsqueda',
                listLabel: 'Conectores',
                summary: ({ on, total, needApproval }, format) =>
                  `${format(on)} de ${format(total)} activos · ${format(needApproval)} con aprobación`,
                results: (count, format) => `${format(count)} conectores`,
                noResults: (query) => `Ningún conector coincide con «${query}».`,
                statuses: {
                  connected: 'Conectado',
                  'needs-sign-in': 'Falta iniciar sesión',
                  error: 'Problema de conexión',
                },
                account: (account) => `como ${account}`,
                enable: (name) => `Usar ${name} en esta conversación`,
                disabledReasons: {
                  'needs-sign-in': 'Inicia sesión para activarlo.',
                  error: 'Vuelve a conectar para activarlo.',
                },
                signIn: 'Iniciar sesión',
                reconnect: 'Volver a conectar',
                manage: 'Gestionar',
                tools: (count, format) => `${format(count)} herramientas`,
                toolsLabel: (name) => `Herramientas de ${name}`,
                permissionLabel: (tool) => `Permiso para ${tool}`,
                permissions: { allow: 'Permitir', ask: 'Preguntar antes', never: 'Nunca' },
                risks: { read: 'Lee datos', write: 'Cambia datos' },
              }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Enforce the choices where tools run; this component only records them.',
        'Set new write and destructive tools to "Ask first" in your data.',
        'Say who a connector is connected as with `account`, so people know whose data the assistant sees.',
        'Keep `status` up to date after `onConnect`, including when sign-in fails.',
      ],
      dont: [
        'Do not turn a connector on for the person; wait for the switch.',
        'Do not set `allowDestructiveAutoApprove` unless the person or their administrator has asked for it.',
        'Do not hide connectors that need sign-in: showing them with "Sign in" is how people fix them.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-tool-approval',
          reason: 'Asks about one tool call when a tool is on "Ask first".',
        },
        { id: 'ai-permission-scope', reason: 'States what an assistant is allowed to do.' },
        { id: 'ai-knowledge-scope', reason: 'Chooses the sources answers are grounded in.' },
        { id: 'ai-tool-call', reason: 'Shows a tool call in the conversation.' },
        { id: 'switch-settings-list', reason: 'A plain list of on/off settings.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'radio-group', 'switch', 'search-input'],
    },
    testing: {
      unit: [
        'Renders a named region with search, summary and a connector list; ref, className and props forwarding.',
        'Status in words and data attributes; the summary counts only connected, switched-on connectors and their "Ask first" tools.',
        'Switch reports changes by click and Space without changing the data; disabled with a linked reason when not connected.',
        'Sign in and Reconnect call `onConnect` only where the status needs it; Manage only with `onManage`.',
        'Tools expand and collapse, uncontrolled and controlled, by click and Enter, with `aria-expanded` and `aria-controls`.',
        'Permission radios report changes, move with arrow keys, and mark write and destructive tools.',
        'A destructive tool cannot be set to Allow by default (disabled, described, skipped by arrows) and can with `allowDestructiveAutoApprove`.',
        'Search filters by name, announces the count politely, shows an empty state with Clear search; controlled query; hidden search; no connectors.',
        'Translated strings, locale number formatting, helper functions.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
