import { afterEach, describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { KeyValue, KeyValueItem } from './index.js';

afterEach(() => {
  vi.restoreAllMocks();
});

describe('KeyValue', () => {
  it('renders label and value pairs as a description list', async () => {
    render(
      <main>
        <KeyValue data-testid="kv">
          <KeyValueItem label="Region">eu-west</KeyValueItem>
          <KeyValueItem label="Plan">Team</KeyValueItem>
        </KeyValue>
      </main>,
    );
    const list = screen.getByTestId('kv');
    expect(list.tagName).toBe('DL');
    expect(list).toHaveClass('mn-key-value');
    expect(list).toHaveAttribute('data-separator', 'dot');
    expect(list).toHaveAttribute('data-layout', 'wrap');
    expect(screen.getAllByRole('term').map((t) => t.textContent)).toEqual(['Region', 'Plan']);
    expect(screen.getAllByRole('definition').map((d) => d.textContent)).toEqual([
      'eu-west',
      'Team',
    ]);
    await expectNoA11yViolations();
  });

  it('shows a dash read as "Not set" for empty values, with no copy button', () => {
    render(
      <KeyValue>
        <KeyValueItem label="Owner" copyable data-testid="item">
          {null}
        </KeyValueItem>
        <KeyValueItem label="Team" messages={{ empty: 'Unassigned' }}>
          {''}
        </KeyValueItem>
      </KeyValue>,
    );
    expect(screen.getByTestId('item')).toHaveAttribute('data-empty');
    expect(screen.getByText('Not set')).toHaveClass('mn-visually-hidden');
    expect(screen.getByText('Unassigned')).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('copies values with the keyboard and names each copy button', async () => {
    const user = userEvent.setup();
    const write = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();
    const onCopy = vi.fn();
    render(
      <main>
        <KeyValue>
          <KeyValueItem label="Workspace ID" copyable mono onCopyValue={onCopy}>
            ws_8f2a91
          </KeyValueItem>
          <KeyValueItem
            label="Key"
            copyable="sk_test_full_value"
            messages={{ copy: () => 'Copy full key' }}
          >
            sk_test_…
          </KeyValueItem>
          <KeyValueItem label={<strong>Node</strong>} copyable>
            n-14
          </KeyValueItem>
        </KeyValue>
      </main>,
    );
    const first = screen.getByRole('button', { name: 'Copy Workspace ID' });
    await user.tab();
    expect(first).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(write).toHaveBeenLastCalledWith('ws_8f2a91');
    expect(onCopy).toHaveBeenCalledWith('ws_8f2a91');
    await user.click(screen.getByRole('button', { name: 'Copy full key' }));
    expect(write).toHaveBeenLastCalledWith('sk_test_full_value');
    expect(screen.getByRole('button', { name: 'Copy value' })).toBeInTheDocument();
    expect(first.closest('.mn-key-value__item')).toHaveAttribute('data-mono');
    await expectNoA11yViolations();
  });

  it('exposes separator, size and layout, and forwards refs', () => {
    const listRef = createRef<HTMLDListElement>();
    const itemRef = createRef<HTMLDivElement>();
    render(
      <KeyValue ref={listRef} separator="divider" size="md" layout="stack">
        <KeyValueItem ref={itemRef} label="Status">
          Active
        </KeyValueItem>
      </KeyValue>,
    );
    expect(listRef.current).toHaveAttribute('data-separator', 'divider');
    expect(listRef.current).toHaveAttribute('data-size', 'md');
    expect(listRef.current).toHaveAttribute('data-layout', 'stack');
    expect(itemRef.current).toHaveClass('mn-key-value__item');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <KeyValue>
        <KeyValueItem label="Region" copyable>
          eu-west
        </KeyValueItem>
      </KeyValue>,
    );
    expect(html).toContain('mn-key-value');
    expect(html).toContain('eu-west');
  });
});
