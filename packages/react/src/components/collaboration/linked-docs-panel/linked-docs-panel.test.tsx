import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { LinkedDocsPanel, parseLinkUrl, type LinkedDocItem } from './index.js';

const items: LinkedDocItem[] = [
  {
    id: 'l1',
    kind: 'doc',
    docId: 'd1',
    title: 'Release runbook',
    href: 'https://docs.example.com/d/d1',
    meta: 'Updated 2 hours ago',
    status: 'Published',
    space: { id: 's-eng', name: 'Engineering' },
  },
  {
    id: 'l2',
    kind: 'doc',
    docId: 'd2',
    title: 'Launch brief',
    href: 'https://docs.example.com/d/d2',
    status: 'Draft',
    space: { id: 's-mkt', name: 'Marketing' },
  },
  { id: 'l3', kind: 'link', title: 'Design file', href: 'https://www.figma.example.com/f/123' },
];

const docs = [
  { value: 'd1', label: 'Release runbook' },
  { value: 'd3', label: 'Incident review' },
  { value: 'd4', label: 'Pricing notes' },
];

describe('LinkedDocsPanel', () => {
  it('lists docs and links in a labelled section with new-tab links', async () => {
    render(<LinkedDocsPanel items={items} />);
    const section = screen.getByRole('region', { name: /Docs/ });
    const list = within(section).getByRole('list');
    expect(within(list).getAllByRole('listitem')).toHaveLength(3);
    const runbook = within(list).getByRole('link', { name: /Release runbook/ });
    expect(runbook).toHaveAttribute('target', '_blank');
    expect(runbook).toHaveAttribute('rel', 'noopener noreferrer');
    expect(runbook).toHaveAccessibleName(
      'Release runbook Updated 2 hours ago (opens in a new tab)',
    );
    expect(within(list).getByRole('link', { name: /Design file/ })).toHaveTextContent(
      'figma.example.com',
    );
    expect(within(section).getByText('Draft')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('filters by Space with pressed chips, and offers Show all', async () => {
    const user = userEvent.setup();
    const onFilterChange = vi.fn();
    render(<LinkedDocsPanel items={items} onFilterChange={onFilterChange} />);
    const group = screen.getByRole('group', { name: 'Filter by Space' });
    const all = within(group).getByRole('button', { name: 'All 3' });
    expect(all).toHaveAttribute('aria-pressed', 'true');
    await user.click(within(group).getByRole('button', { name: 'Marketing 1' }));
    expect(onFilterChange).toHaveBeenLastCalledWith('s-mkt');
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByText('2 more in other Spaces.')).toBeInTheDocument();
    await user.click(within(group).getByRole('button', { name: 'No Space 1' }));
    expect(onFilterChange).toHaveBeenLastCalledWith(null);
    expect(screen.getByRole('link', { name: /Design file/ })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Show all' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    await expectNoA11yViolations();
  });

  it('a controlled filter for a Space with no rows shows everything; no chips for one Space', () => {
    const { rerender } = render(<LinkedDocsPanel items={items} filter="s-gone" />);
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    rerender(<LinkedDocsPanel items={items.slice(0, 1)} />);
    expect(screen.queryByRole('group')).not.toBeInTheDocument();
    rerender(<LinkedDocsPanel items={items} spaceFilter={false} />);
    expect(screen.queryByRole('group')).not.toBeInTheDocument();
  });

  it('unlinks with a labelled button and moves focus to the next row', async () => {
    const user = userEvent.setup();
    function Harness() {
      const [list, setList] = useState(items);
      return (
        <LinkedDocsPanel
          items={list}
          spaceFilter={false}
          onUnlink={(it) =>
            Promise.resolve().then(() => setList((l) => l.filter((x) => x.id !== it.id)))
          }
        />
      );
    }
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Unlink Release runbook' }));
    await waitFor(() => expect(screen.getAllByRole('listitem')).toHaveLength(2));
    await waitFor(() => expect(screen.getByRole('link', { name: /Launch brief/ })).toHaveFocus());
  });

  it('shows an alert when unlinking fails', async () => {
    const user = userEvent.setup();
    render(<LinkedDocsPanel items={items} onUnlink={() => Promise.reject(new Error('x'))} />);
    await user.click(screen.getByRole('button', { name: 'Unlink Launch brief' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('didn’t unlink');
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
  });

  it('links a doc from the picker, leaving linked docs out', async () => {
    const user = userEvent.setup();
    const onLinkDoc = vi.fn();
    const onCreateDoc = vi.fn();
    render(
      <LinkedDocsPanel
        items={items}
        docOptions={docs}
        onLinkDoc={onLinkDoc}
        onCreateDoc={onCreateDoc}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Link doc' }));
    const listbox = await screen.findByRole('listbox');
    expect(within(listbox).queryByRole('option', { name: /Release runbook/ })).toBeNull();
    await user.keyboard('pric');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onLinkDoc).toHaveBeenCalledWith('d4');

    await user.click(screen.getByRole('button', { name: 'Link doc' }));
    await user.click(await screen.findByRole('button', { name: 'New doc' }));
    expect(onCreateDoc).toHaveBeenCalled();
  });

  it('searches docs through onSearchDocs and drops linked ones', async () => {
    const user = userEvent.setup();
    const onSearchDocs = vi.fn(async () => docs);
    render(<LinkedDocsPanel items={items} onSearchDocs={onSearchDocs} onLinkDoc={() => {}} />);
    await user.click(screen.getByRole('button', { name: 'Link doc' }));
    await user.keyboard('re');
    const listbox = await screen.findByRole('listbox');
    await waitFor(() => expect(onSearchDocs).toHaveBeenCalled());
    await waitFor(() =>
      expect(within(listbox).getByRole('option', { name: /Incident review/ })).toBeInTheDocument(),
    );
    expect(within(listbox).queryByRole('option', { name: /Release runbook/ })).toBeNull();
  });

  it('adds a link with validation; Escape cancels and returns focus', async () => {
    const user = userEvent.setup();
    const onAddLink = vi.fn(() => Promise.resolve());
    render(<LinkedDocsPanel items={[]} onAddLink={onAddLink} />);
    const toggle = screen.getByRole('button', { name: 'Add link' });
    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const url = screen.getByRole('textbox', { name: 'Link' });
    expect(url).toHaveFocus();
    await user.type(url, 'javascript:alert(1)');
    await user.click(screen.getByRole('button', { name: 'Add' }));
    expect(url).toHaveAttribute('aria-invalid', 'true');
    expect(url).toHaveAccessibleDescription(/starts with https/);
    expect(onAddLink).not.toHaveBeenCalled();

    await user.clear(url);
    await user.type(url, 'https://www.example.com/spec{Enter}');
    expect(onAddLink).toHaveBeenCalledWith({
      url: 'https://www.example.com/spec',
      title: 'example.com',
    });
    await waitFor(() => expect(screen.queryByRole('textbox', { name: 'Link' })).toBeNull());
    await waitFor(() => expect(toggle).toHaveFocus());

    await user.click(toggle);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('textbox', { name: 'Link' })).toBeNull();
    await expectNoA11yViolations();
  });

  it('loading, empty, read-only, notice and error states', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    const { rerender } = render(<LinkedDocsPanel items={null} onAddLink={() => {}} />);
    expect(screen.getByRole('region')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('Loading linked docs…');

    rerender(<LinkedDocsPanel items={[]} onAddLink={() => {}} />);
    expect(screen.getByText(/Nothing linked yet. Link a spec/)).toBeInTheDocument();

    rerender(<LinkedDocsPanel items={items} readOnly onUnlink={() => {}} onAddLink={() => {}} />);
    expect(screen.queryByRole('button', { name: /Unlink/ })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Add link' })).toBeNull();

    rerender(<LinkedDocsPanel items={items} notice="You don’t have access to docs here." />);
    expect(screen.queryByRole('list')).toBeNull();
    expect(screen.getByText('You don’t have access to docs here.')).toBeInTheDocument();

    rerender(<LinkedDocsPanel items={items} error="Couldn’t load docs." onRetry={onRetry} />);
    expect(screen.getByRole('alert')).toHaveTextContent('Couldn’t load docs.');
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalled();
  });

  it('calls onOpen when a row is opened', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn((_, e: { preventDefault: () => void }) => e.preventDefault());
    render(<LinkedDocsPanel items={items} onOpen={onOpen} />);
    await user.click(screen.getByRole('link', { name: /Launch brief/ }));
    expect(onOpen).toHaveBeenCalledWith(items[1], expect.anything());
  });

  it('parses only full http(s) URLs', () => {
    expect(parseLinkUrl(' https://example.com/a ')).toBe('https://example.com/a');
    expect(parseLinkUrl('example.com')).toBeNull();
    expect(parseLinkUrl('data:text/html,hi')).toBeNull();
    expect(parseLinkUrl('')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<LinkedDocsPanel items={items} />);
    expect(html).toContain('mn-linked-docs-panel');
    expect(html).toContain('Release runbook');
  });
});
