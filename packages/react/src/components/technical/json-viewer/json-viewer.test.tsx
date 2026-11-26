import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { JSONViewer, formatJsonPath, stringifyJsonValue } from './index.js';

const sample = {
  id: 'ord_1001',
  total: 42.5,
  paid: true,
  note: null,
  items: [
    { sku: 'A-1', qty: 2 },
    { sku: 'B-7', qty: 1 },
  ],
  customer: { name: 'Sample Customer', 'first name': 'Sample' },
};

const item = (name: RegExp | string) => screen.getByRole('treeitem', { name });

describe('JSONViewer', () => {
  it('renders a named tree with the root expanded one level', async () => {
    render(<JSONViewer value={sample} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-json-viewer');
    const tree = screen.getByRole('tree', { name: 'JSON' });
    const rows = within(tree).getAllByRole('treeitem');
    expect(rows).toHaveLength(7);
    expect(rows[0]).toHaveAttribute('aria-expanded', 'true');
    expect(rows[0]).toHaveAttribute('tabindex', '0');
    expect(item(/^id: "ord_1001"/)).toHaveAttribute('aria-level', '2');
    expect(item(/^items:/)).toHaveAttribute('aria-expanded', 'false');
    expect(item(/^items:/)).toHaveTextContent('2 items');
    expect(screen.queryByRole('treeitem', { name: /sku/ })).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('follows the APG tree keyboard model', async () => {
    const user = userEvent.setup();
    render(<JSONViewer value={sample} />);
    const root = screen.getAllByRole('treeitem')[0]!;
    root.focus();
    await user.keyboard('{ArrowDown}');
    expect(item(/^id:/)).toHaveFocus();
    await user.keyboard('{End}');
    expect(item(/^customer:/)).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(item(/^customer:/)).toHaveAttribute('aria-expanded', 'true');
    await user.keyboard('{ArrowRight}');
    expect(item(/^name:/)).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(item(/^customer:/)).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(item(/^customer:/)).toHaveAttribute('aria-expanded', 'false');
    await user.keyboard('{Home}');
    expect(root).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(root).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getAllByRole('treeitem')).toHaveLength(1);
    await user.keyboard(' ');
    expect(screen.getAllByRole('treeitem')).toHaveLength(7);
  });

  it('mirrors the left and right arrows in right-to-left text', async () => {
    const user = userEvent.setup();
    render(
      <div dir="rtl">
        <JSONViewer value={sample} />
      </div>,
    );
    screen.getAllByRole('treeitem')[0]!.focus();
    await user.keyboard('{End}');
    expect(item(/^customer:/)).toHaveFocus();
    // The children are to the left: ArrowLeft opens and steps in, ArrowRight steps out and closes.
    await user.keyboard('{ArrowLeft}');
    expect(item(/^customer:/)).toHaveAttribute('aria-expanded', 'true');
    await user.keyboard('{ArrowLeft}');
    expect(item(/^name:/)).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(item(/^customer:/)).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(item(/^customer:/)).toHaveAttribute('aria-expanded', 'false');
  });

  it('expands every sibling with *', async () => {
    const user = userEvent.setup();
    render(<JSONViewer value={sample} />);
    item(/^items:/).focus();
    await user.keyboard('*');
    expect(item(/^items:/)).toHaveAttribute('aria-expanded', 'true');
    expect(item(/^customer:/)).toHaveAttribute('aria-expanded', 'true');
  });

  it('search highlights matches and opens their ancestors', async () => {
    const user = userEvent.setup();
    render(<JSONViewer value={sample} />);
    await user.type(screen.getByRole('searchbox', { name: 'Search keys and values' }), 'b-7');
    expect(screen.getByText('1 match')).toBeInTheDocument();
    const hit = item(/^sku: "B-7"/);
    expect(hit).toHaveAttribute('data-match');
    expect(hit.querySelector('mark')).toHaveTextContent('B-7');
    expect(item(/^items:/)).toHaveAttribute('aria-expanded', 'true');
  });

  it('copies the selected path and value', async () => {
    const user = userEvent.setup();
    const onCopy = vi.fn();
    render(<JSONViewer value={sample} onCopy={onCopy} defaultExpandDepth={2} />);
    await user.click(item(/^first name:/));
    expect(screen.getByText('$.customer["first name"]')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Copy path/ }));
    expect(onCopy).toHaveBeenLastCalledWith('$.customer["first name"]', 'path');
    await user.click(item(/^items:/));
    await user.click(screen.getByRole('button', { name: /Copy value/ }));
    expect(onCopy).toHaveBeenLastCalledWith(JSON.stringify(sample.items, null, 2), 'value');
  });

  it('renders large containers in batches behind Show more', async () => {
    const user = userEvent.setup();
    const big = Array.from({ length: 250 }, (_, i) => i);
    render(<JSONViewer value={big} maxChildren={100} />);
    expect(screen.getAllByRole('treeitem')).toHaveLength(102);
    const more = item(/Show 100 more \(150 hidden\)/);
    more.focus();
    await user.keyboard('{Enter}');
    expect(screen.getAllByRole('treeitem')).toHaveLength(202);
    expect(item(/^100: 100$/)).toHaveFocus();
  });

  it('a click on a row does what Enter does on it', async () => {
    const user = userEvent.setup();
    const big = Array.from({ length: 150 }, (_, i) => i);
    render(<JSONViewer value={{ list: big, meta: { ok: true } }} maxChildren={100} />);
    await user.click(screen.getByText('list'));
    expect(item(/^list:/)).toHaveAttribute('aria-expanded', 'true');
    expect(item(/^list:/)).toHaveFocus();
    await user.click(item(/Show 50 more/));
    expect(item(/^149: 149$/)).toBeInTheDocument();
    // The same from the keyboard on the other container.
    item(/^meta:/).focus();
    await user.keyboard('{Enter}');
    expect(item(/^meta:/)).toHaveAttribute('aria-expanded', 'true');
    await user.click(screen.getByText('meta'));
    expect(item(/^meta:/)).toHaveAttribute('aria-expanded', 'false');
  });

  it('shows circular references instead of looping', () => {
    const a: Record<string, unknown> = { name: 'loop' };
    a.self = a;
    render(<JSONViewer value={a} defaultExpandDepth={5} />);
    expect(item(/^self: \[Circular reference\]/)).not.toHaveAttribute('aria-expanded');
    expect(stringifyJsonValue(a)).toContain('"self": "[Circular]"');
  });

  it('formats paths', () => {
    expect(formatJsonPath(['users', 0, 'first name'])).toBe('$.users[0]["first name"]');
    expect(formatJsonPath(['users', 0, 'name'], '')).toBe('users[0].name');
  });

  it('takes custom messages', () => {
    render(
      <JSONViewer value={[1]} messages={{ label: 'Payload', items: (n) => `${n} entries` }} />,
    );
    expect(screen.getByRole('tree', { name: 'Payload' })).toBeInTheDocument();
    expect(screen.getAllByRole('treeitem')[0]).toHaveTextContent('1 entries');
  });

  it('renders on the server', () => {
    const html = renderToString(<JSONViewer value={sample} />);
    expect(html).toContain('mn-json-viewer');
    expect(html).toContain('role="tree"');
  });
});
