import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AI_CONNECTOR_PICKER_MESSAGES,
  AiConnectorPicker,
  aiConnectorCounts,
  filterAiConnectors,
  isAiConnectorActive,
  type AiConnector,
} from './index.js';

const connectors: AiConnector[] = [
  {
    id: 'calendar',
    name: 'Calendar',
    description: 'Events and availability',
    status: 'connected',
    account: 'ada@example.com',
    enabled: true,
    tools: [
      {
        id: 'list',
        name: 'List events',
        description: 'Reads your events',
        risk: 'read',
        permission: 'allow',
      },
      {
        id: 'create',
        name: 'Create event',
        description: 'Adds an event',
        risk: 'write',
        permission: 'ask',
      },
      {
        id: 'delete',
        name: 'Delete event',
        description: 'Removes an event',
        risk: 'destructive',
        permission: 'ask',
      },
    ],
  },
  {
    id: 'files',
    name: 'Files',
    status: 'connected',
    enabled: false,
    tools: [{ id: 'search', name: 'Search files', risk: 'read', permission: 'ask' }],
  },
  { id: 'mail', name: 'Mail', status: 'needs-sign-in', enabled: false },
  { id: 'tracker', name: 'Issue tracker', status: 'error', enabled: true },
  { id: 'crm', name: 'Customer records', status: 'unavailable', enabled: false },
];

describe('AiConnectorPicker', () => {
  it('renders a named region with search, summary and a list of connectors', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiConnectorPicker
        ref={ref}
        data-testid="root"
        className="extra"
        connectors={connectors}
        defaultExpandedIds={['calendar']}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-connector-picker', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Connectors' })).toBe(root);
    expect(screen.getByRole('searchbox', { name: 'Search connectors' })).toBeInTheDocument();
    const list = screen.getByRole('list', { name: 'Connectors' });
    expect(within(list).getAllByRole('switch')).toHaveLength(5);
    // The errored connector is enabled in the data but cannot be used, so it does not count.
    expect(root).toHaveTextContent('1 of 5 connectors on · 2 tools ask first');
    expect(root).toHaveTextContent('Connected as ada@example.com');
    await expectNoA11yViolations();
  });

  it('shows status in words and marks each row with its state', () => {
    render(<AiConnectorPicker connectors={connectors} />);
    const items = screen.getAllByRole('listitem');
    expect(items[0]).toHaveAttribute('data-status', 'connected');
    expect(items[0]).toHaveAttribute('data-enabled', 'true');
    expect(items[2]).toHaveTextContent('Sign-in needed');
    expect(items[3]).toHaveTextContent('Connection problem');
    expect(items[3]).not.toHaveAttribute('data-enabled');
    expect(items[4]).toHaveTextContent('Unavailable');
  });

  it('reports switch changes and does not change the data itself', async () => {
    const user = userEvent.setup();
    const onEnabledChange = vi.fn();
    render(<AiConnectorPicker connectors={connectors} onEnabledChange={onEnabledChange} />);
    const files = screen.getByRole('switch', { name: 'Use Files in this conversation' });
    expect(files).not.toBeChecked();
    await user.click(files);
    expect(onEnabledChange).toHaveBeenCalledWith('files', true);
    expect(files).not.toBeChecked();
    const calendar = screen.getByRole('switch', { name: 'Use Calendar in this conversation' });
    calendar.focus();
    await user.keyboard(' ');
    expect(onEnabledChange).toHaveBeenLastCalledWith('calendar', false);
  });

  it('disables the switch with a visible, linked reason when not connected', async () => {
    const user = userEvent.setup();
    const onEnabledChange = vi.fn();
    render(<AiConnectorPicker connectors={connectors} onEnabledChange={onEnabledChange} />);
    const mail = screen.getByRole('switch', { name: 'Use Mail in this conversation' });
    expect(mail).toBeDisabled();
    expect(mail).toHaveAccessibleDescription('Sign-in needed Sign in before turning this on.');
    await user.click(mail);
    expect(onEnabledChange).not.toHaveBeenCalled();
    expect(
      screen.getByRole('switch', { name: 'Use Customer records in this conversation' }),
    ).toHaveAccessibleDescription('Unavailable This connector cannot be used right now.');
  });

  it('offers Sign in and Reconnect only where the status needs it', async () => {
    const user = userEvent.setup();
    const onConnect = vi.fn();
    render(<AiConnectorPicker connectors={connectors} onConnect={onConnect} />);
    await user.click(screen.getByRole('button', { name: 'Sign in: Mail' }));
    expect(onConnect).toHaveBeenLastCalledWith('mail');
    screen.getByRole('button', { name: 'Reconnect: Issue tracker' }).focus();
    await user.keyboard('{Enter}');
    expect(onConnect).toHaveBeenLastCalledWith('tracker');
    expect(screen.getAllByRole('button', { name: /^(Sign in|Reconnect):/ })).toHaveLength(2);
  });

  it('shows Manage only when onManage is given', async () => {
    const user = userEvent.setup();
    const onManage = vi.fn();
    const { rerender } = render(<AiConnectorPicker connectors={connectors} />);
    expect(screen.queryByRole('button', { name: /^Manage:/ })).not.toBeInTheDocument();
    rerender(<AiConnectorPicker connectors={connectors} onManage={onManage} />);
    await user.click(screen.getByRole('button', { name: 'Manage: Files' }));
    expect(onManage).toHaveBeenCalledWith('files');
  });

  it('expands and collapses tools (uncontrolled), from the keyboard too', async () => {
    const user = userEvent.setup();
    const onExpandedIdsChange = vi.fn();
    render(<AiConnectorPicker connectors={connectors} onExpandedIdsChange={onExpandedIdsChange} />);
    const toggle = screen.getByRole('button', { name: '3 tools: Calendar' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('list', { name: 'Calendar tools' })).not.toBeInTheDocument();
    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(onExpandedIdsChange).toHaveBeenLastCalledWith(['calendar']);
    const tools = screen.getByRole('list', { name: 'Calendar tools' });
    expect(toggle).toHaveAttribute('aria-controls', tools.id);
    expect(within(tools).getAllByRole('radiogroup')).toHaveLength(3);
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(onExpandedIdsChange).toHaveBeenLastCalledWith([]);
    // A connector with no tools has no toggle.
    expect(screen.queryByRole('button', { name: /tools: Mail/ })).not.toBeInTheDocument();
  });

  it('follows controlled expandedIds', async () => {
    const user = userEvent.setup();
    const onExpandedIdsChange = vi.fn();
    const { rerender } = render(
      <AiConnectorPicker
        connectors={connectors}
        expandedIds={[]}
        onExpandedIdsChange={onExpandedIdsChange}
      />,
    );
    await user.click(screen.getByRole('button', { name: '1 tool: Files' }));
    expect(onExpandedIdsChange).toHaveBeenCalledWith(['files']);
    expect(screen.queryByRole('list', { name: 'Files tools' })).not.toBeInTheDocument();
    rerender(
      <AiConnectorPicker
        connectors={connectors}
        expandedIds={['files']}
        onExpandedIdsChange={onExpandedIdsChange}
      />,
    );
    expect(screen.getByRole('list', { name: 'Files tools' })).toBeInTheDocument();
  });

  it('marks what each tool can do and reports permission changes', async () => {
    const user = userEvent.setup();
    const onPermissionChange = vi.fn();
    render(
      <AiConnectorPicker
        connectors={connectors}
        defaultExpandedIds={['calendar']}
        onPermissionChange={onPermissionChange}
      />,
    );
    const tools = screen.getByRole('list', { name: 'Calendar tools' });
    const rows = within(tools).getAllByRole('listitem');
    expect(rows[0]).toHaveAttribute('data-risk', 'read');
    expect(rows[1]).toHaveTextContent('Changes data');
    expect(rows[2]).toHaveTextContent('Can delete data');
    const group = screen.getByRole('radiogroup', { name: 'Permission for Create event' });
    expect(within(group).getByRole('radio', { name: 'Ask first' })).toBeChecked();
    await user.click(within(group).getByRole('radio', { name: 'Never' }));
    expect(onPermissionChange).toHaveBeenLastCalledWith('calendar', 'create', 'never');
    // Controlled data: nothing moves until the product updates `connectors`.
    expect(within(group).getByRole('radio', { name: 'Ask first' })).toBeChecked();
  });

  it('moves between permissions with the arrow keys', async () => {
    const user = userEvent.setup();
    function Harness() {
      const [data, setData] = useState(connectors);
      return (
        <AiConnectorPicker
          connectors={data}
          defaultExpandedIds={['calendar']}
          onPermissionChange={(connectorId, toolId, permission) =>
            setData((all) =>
              all.map((c) =>
                c.id === connectorId
                  ? {
                      ...c,
                      tools: c.tools?.map((t) => (t.id === toolId ? { ...t, permission } : t)),
                    }
                  : c,
              ),
            )
          }
        />
      );
    }
    render(<Harness />);
    const group = screen.getByRole('radiogroup', { name: 'Permission for Create event' });
    within(group).getByRole('radio', { name: 'Ask first' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(within(group).getByRole('radio', { name: 'Never' })).toBeChecked();
    await user.keyboard('{ArrowLeft}{ArrowLeft}');
    expect(within(group).getByRole('radio', { name: 'Allow' })).toBeChecked();
    expect(document.body).toHaveTextContent('1 tool asks first');
  });

  it('never lets a destructive tool be set to Allow by default, and says why', async () => {
    const user = userEvent.setup();
    const onPermissionChange = vi.fn();
    render(
      <AiConnectorPicker
        connectors={connectors}
        defaultExpandedIds={['calendar']}
        onPermissionChange={onPermissionChange}
      />,
    );
    const group = screen.getByRole('radiogroup', { name: 'Permission for Delete event' });
    const allow = within(group).getByRole('radio', { name: 'Allow' });
    expect(allow).toBeDisabled();
    expect(allow).toHaveAccessibleDescription(
      'Tools that can delete data cannot run without asking.',
    );
    await user.click(allow);
    expect(onPermissionChange).not.toHaveBeenCalled();
    // Arrow keys skip the disabled option.
    within(group).getByRole('radio', { name: 'Ask first' }).focus();
    await user.keyboard('{ArrowLeft}');
    expect(onPermissionChange).not.toHaveBeenCalledWith('calendar', 'delete', 'allow');
    // Other tools keep Allow.
    expect(
      within(screen.getByRole('radiogroup', { name: 'Permission for Create event' })).getByRole(
        'radio',
        { name: 'Allow' },
      ),
    ).toBeEnabled();
  });

  it('allows it when allowDestructiveAutoApprove is set', async () => {
    const user = userEvent.setup();
    const onPermissionChange = vi.fn();
    render(
      <AiConnectorPicker
        connectors={connectors}
        defaultExpandedIds={['calendar']}
        allowDestructiveAutoApprove
        onPermissionChange={onPermissionChange}
      />,
    );
    const group = screen.getByRole('radiogroup', { name: 'Permission for Delete event' });
    await user.click(within(group).getByRole('radio', { name: 'Allow' }));
    expect(onPermissionChange).toHaveBeenCalledWith('calendar', 'delete', 'allow');
    expect(
      screen.queryByText('Tools that can delete data cannot run without asking.'),
    ).not.toBeInTheDocument();
  });

  it('filters by name, announces the count and offers a way out of an empty result', async () => {
    const user = userEvent.setup();
    const onQueryChange = vi.fn();
    render(<AiConnectorPicker connectors={connectors} onQueryChange={onQueryChange} />);
    const status = screen.getByRole('status');
    expect(status).toBeEmptyDOMElement();
    const search = screen.getByRole('searchbox', { name: 'Search connectors' });
    await user.type(search, 'CAL');
    expect(onQueryChange).toHaveBeenLastCalledWith('CAL');
    expect(screen.getAllByRole('switch')).toHaveLength(1);
    expect(status).toHaveTextContent('1 connector found');
    // The summary still counts every connector.
    expect(document.body).toHaveTextContent('1 of 5 connectors on');
    await user.type(search, 'zzz');
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
    expect(screen.getByText('No connectors match “CALzzz”.')).toBeInTheDocument();
    expect(status).toHaveTextContent('No connectors found');
    // One in the field, one in the empty state.
    const clear = screen.getAllByRole('button', { name: 'Clear search' });
    expect(clear).toHaveLength(2);
    await user.click(clear[1] as HTMLElement);
    expect(screen.getAllByRole('switch')).toHaveLength(5);
    expect(status).toHaveTextContent('5 connectors found');
  });

  it('follows a controlled query', async () => {
    const user = userEvent.setup();
    const onQueryChange = vi.fn();
    render(
      <AiConnectorPicker connectors={connectors} query="mail" onQueryChange={onQueryChange} />,
    );
    expect(screen.getAllByRole('switch')).toHaveLength(1);
    await user.type(screen.getByRole('searchbox'), 'x');
    expect(onQueryChange).toHaveBeenCalledWith('mailx');
    expect(screen.getByRole('searchbox')).toHaveValue('mail');
  });

  it('can hide the search and says when there are no connectors', () => {
    render(<AiConnectorPicker connectors={[]} searchable={false} />);
    expect(screen.queryByRole('searchbox')).not.toBeInTheDocument();
    expect(screen.getByText('No connectors are available.')).toBeInTheDocument();
  });

  it('uses translated strings and the locale for numbers', () => {
    render(
      <AiConnectorPicker
        aria-label="Conectores de la conversación"
        connectors={connectors}
        defaultExpandedIds={['calendar']}
        locale="ar-EG-u-nu-arab"
        messages={{
          searchLabel: 'Buscar conectores',
          summary: ({ on, total }, format) => `${format(on)} de ${format(total)} activos`,
          statuses: { 'needs-sign-in': 'Falta iniciar sesión' },
          permissions: { ask: 'Preguntar' },
          risks: { destructive: 'Puede borrar datos' },
          signIn: 'Iniciar sesión',
        }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Conectores de la conversación' })).toBeTruthy();
    expect(screen.getByRole('searchbox', { name: 'Buscar conectores' })).toBeInTheDocument();
    expect(document.body).toHaveTextContent('١ de ٥ activos');
    expect(document.body).toHaveTextContent('Falta iniciar sesión');
    expect(document.body).toHaveTextContent('Puede borrar datos');
    expect(screen.getAllByRole('radio', { name: 'Preguntar' })).toHaveLength(3);
    expect(screen.getByRole('button', { name: 'Iniciar sesión: Mail' })).toBeInTheDocument();
    // Untranslated keys keep the English default.
    expect(screen.getAllByRole('radio', { name: 'Never' })).toHaveLength(3);
  });

  it('has no accessibility violations with everything open', async () => {
    render(
      <AiConnectorPicker
        connectors={connectors}
        defaultExpandedIds={['calendar', 'files']}
        onManage={() => {}}
      />,
    );
    await expectNoA11yViolations();
  });

  it('exposes helpers for counts, filtering and whether a connector is in use', () => {
    expect(aiConnectorCounts(connectors)).toEqual({ on: 1, total: 5, needApproval: 2 });
    expect(filterAiConnectors(connectors, '  ISSUE ').map((c) => c.id)).toEqual(['tracker']);
    expect(filterAiConnectors([{ name: 'Café' }], 'cafe')).toHaveLength(1);
    expect(filterAiConnectors(connectors, '')).toHaveLength(5);
    expect(isAiConnectorActive({ status: 'error', enabled: true })).toBe(false);
    expect(isAiConnectorActive({ status: 'connected', enabled: true })).toBe(true);
    expect(AI_CONNECTOR_PICKER_MESSAGES.permissions.ask).toBe('Ask first');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiConnectorPicker connectors={connectors} defaultExpandedIds={['calendar']} />,
    );
    expect(html).toContain('mn-ai-connector-picker');
    expect(html).toContain('Permission for Delete event');
  });
});
