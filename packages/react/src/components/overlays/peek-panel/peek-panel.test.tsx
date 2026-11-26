import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PeekPanel, type PeekPanelProps } from './index.js';

const RECORDS = ['Order 1001', 'Order 1002', 'Order 1003'];

function List(props: Partial<PeekPanelProps>) {
  const [index, setIndex] = useState<number | null>(null);
  return (
    <main>
      <ul>
        {RECORDS.map((r, i) => (
          <li key={r}>
            <button type="button" onClick={() => setIndex(i)}>
              {r}
            </button>
          </li>
        ))}
      </ul>
      <PeekPanel
        open={index !== null}
        onOpenChange={(open) => !open && setIndex(null)}
        title={index === null ? '' : RECORDS[index]}
        description="Placed yesterday"
        onPrevious={() => setIndex((i) => (i ?? 0) - 1)}
        onNext={() => setIndex((i) => (i ?? 0) + 1)}
        hasPrevious={index !== null && index > 0}
        hasNext={index !== null && index < RECORDS.length - 1}
        position={index === null ? undefined : { index: index + 1, total: RECORDS.length }}
        href={index === null ? undefined : `/orders/${index}`}
        {...props}
      >
        <label>
          Note <input />
        </label>
      </PeekPanel>
    </main>
  );
}

describe('PeekPanel', () => {
  it('opens as a named, non-modal panel with focus inside; the list stays usable', async () => {
    render(<List />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Order 1002' }));
    const panel = screen.getByRole('dialog', { name: 'Order 1002' });
    expect(panel).toHaveAttribute('aria-modal', 'false');
    expect(panel).toHaveAccessibleDescription('Placed yesterday');
    expect(panel).toHaveFocus();
    expect(panel).toHaveAttribute('data-placement', 'overlay');
    expect(screen.getByText('2 of 3')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open full record' })).toHaveAttribute(
      'href',
      '/orders/1',
    );
    // No inert background: another row can be chosen while it is open.
    await user.click(screen.getByRole('button', { name: 'Order 1003' }));
    expect(screen.getByRole('dialog', { name: 'Order 1003' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('steps through records with the buttons and keeps them focusable at the ends', async () => {
    render(<List />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Order 1002' }));
    const next = screen.getByRole('button', { name: 'Next' });
    await user.click(next);
    expect(screen.getByRole('heading', { name: 'Order 1003' })).toBeInTheDocument();
    expect(next).toHaveAttribute('aria-disabled', 'true');
    expect(next).toHaveFocus();
    await user.click(next);
    expect(screen.getByRole('heading', { name: 'Order 1003' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Previous' }));
    expect(screen.getByRole('heading', { name: 'Order 1002' })).toBeInTheDocument();
  });

  it('supports J/K and Alt+Arrow shortcuts, but not J/K while typing', async () => {
    render(<List />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Order 1001' }));
    await user.keyboard('j');
    expect(screen.getByRole('heading', { name: 'Order 1002' })).toBeInTheDocument();
    await user.keyboard('k');
    expect(screen.getByRole('heading', { name: 'Order 1001' })).toBeInTheDocument();
    const input = screen.getByRole('textbox', { name: 'Note' });
    await user.click(input);
    await user.keyboard('j');
    expect(input).toHaveValue('j');
    expect(screen.getByRole('heading', { name: 'Order 1001' })).toBeInTheDocument();
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    expect(screen.getByRole('heading', { name: 'Order 1002' })).toBeInTheDocument();
  });

  it('leaves Alt+Arrow to a select, a combobox and a menu button inside the panel', async () => {
    render(
      <List
        actions={
          <select aria-label="Status">
            <option>Open</option>
            <option>Closed</option>
          </select>
        }
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Order 1001' }));
    screen.getByRole('combobox', { name: 'Status' }).focus();
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    expect(screen.getByRole('dialog', { name: 'Order 1001' })).toBeInTheDocument();
    screen.getByRole('textbox', { name: 'Note' }).focus();
    await user.keyboard('{Alt>}{ArrowDown}{/Alt}');
    expect(screen.getByRole('dialog', { name: 'Order 1002' })).toBeInTheDocument();
  });

  it('closes with Escape and the close button, returning focus to the row', async () => {
    render(<List />);
    const user = userEvent.setup();
    const row = screen.getByRole('button', { name: 'Order 1001' });
    await user.click(row);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(row).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Order 1003' }));
    await user.click(screen.getByRole('button', { name: 'Close preview' }));
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(screen.getByRole('button', { name: 'Order 1003' })).toHaveFocus();
  });

  it('can leave focus on the list and translate its labels', async () => {
    render(
      <List
        focusOnOpen={false}
        shortcuts={false}
        messages={{ close: 'Fermer', position: (i, t) => `${i} sur ${t}` }}
      />,
    );
    const user = userEvent.setup();
    const row = screen.getByRole('button', { name: 'Order 1001' });
    await user.click(row);
    expect(row).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Fermer' })).toBeInTheDocument();
    expect(screen.getByText('1 sur 3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next' })).not.toHaveAttribute('aria-keyshortcuts');
  });

  it('does not link unsafe URLs and renders nothing when closed', () => {
    const onOpenChange = vi.fn();
    const { rerender, container } = render(
      <PeekPanel open title="Record" onOpenChange={onOpenChange} href="javascript:alert(1)" />,
    );
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Next' })).toBeNull();
    rerender(<PeekPanel open={false} title="Record" onOpenChange={onOpenChange} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <PeekPanel open title="Order 1001" onOpenChange={() => {}} placement="inline">
        Details
      </PeekPanel>,
    );
    expect(html).toContain('mn-peek-panel');
    expect(html).toContain('Order 1001');
  });
});
