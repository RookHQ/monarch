import { afterEach, describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DescriptionItem, DescriptionList } from './index.js';

afterEach(() => vi.restoreAllMocks());

describe('DescriptionList', () => {
  it('renders semantically', async () => {
    render(
      <main>
        <DescriptionList columns={2}>
          <DescriptionItem term="Owner">Ada</DescriptionItem>
          <DescriptionItem term="Stage" wide>
            Proposal
          </DescriptionItem>
        </DescriptionList>
      </main>,
    );
    expect(screen.getByText('Owner').tagName).toBe('DT');
    expect(screen.getByText('Ada').tagName).toBe('DD');
    await expectNoA11yViolations();
  });

  it('exposes columns and layout, and forwards refs', () => {
    const list = createRef<HTMLDListElement>();
    const item = createRef<HTMLDivElement>();
    render(
      <DescriptionList ref={list} columns="auto" layout="inline" data-testid="dl">
        <DescriptionItem ref={item} term="Plan">
          Team
        </DescriptionItem>
      </DescriptionList>,
    );
    const dl = screen.getByTestId('dl');
    expect(list.current).toBe(dl);
    expect(dl).toHaveAttribute('data-columns', 'auto');
    expect(dl).toHaveAttribute('data-layout', 'inline');
    expect(item.current).toHaveClass('mn-dl__item');
  });

  it('shows a placeholder for empty values, read as "Not set"', () => {
    render(
      <DescriptionList>
        <DescriptionItem term="Phone">{null}</DescriptionItem>
        <DescriptionItem term="Fax" emptyLabel="None">
          {''}
        </DescriptionItem>
        <DescriptionItem term="Notes" placeholder="No notes yet">
          {undefined}
        </DescriptionItem>
        <DescriptionItem term="Count">{0}</DescriptionItem>
      </DescriptionList>,
    );
    const phone = screen.getByText('Phone').nextElementSibling as HTMLElement;
    expect(phone).toHaveTextContent('—Not set');
    expect(phone.querySelector('[aria-hidden="true"]')).toHaveTextContent('—');
    expect(phone.closest('.mn-dl__item')).toHaveAttribute('data-empty');
    expect(screen.getByText('None')).toHaveClass('mn-visually-hidden');
    expect(screen.getByText('No notes yet')).toBeInTheDocument();
    // 0 is a value, not empty.
    expect(screen.getByText('0').closest('.mn-dl__item')).not.toHaveAttribute('data-empty');
  });

  it('copies a value with the keyboard', async () => {
    const user = userEvent.setup();
    const write = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();
    render(
      <DescriptionList>
        <DescriptionItem term="Workspace ID" copyable>
          ws_8f2a91
        </DescriptionItem>
        <DescriptionItem term="Key" copyable="sk_live_full_value" copyLabel="Copy full key">
          sk_live_…
        </DescriptionItem>
        <DescriptionItem term="Empty" copyable>
          {null}
        </DescriptionItem>
      </DescriptionList>,
    );
    const copy = screen.getByRole('button', { name: 'Copy Workspace ID' });
    await user.tab();
    expect(copy).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(write).toHaveBeenLastCalledWith('ws_8f2a91');
    await user.click(screen.getByRole('button', { name: 'Copy full key' }));
    expect(write).toHaveBeenLastCalledWith('sk_live_full_value');
    // No copy button for an empty value.
    expect(screen.getAllByRole('button')).toHaveLength(2);
    await expectNoA11yViolations();
  });

  it('clamps long values and expands them when they overflow', async () => {
    let overflow = true;
    vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockImplementation(function (
      this: HTMLElement,
    ) {
      return this.classList.contains('mn-dl__clamp') && overflow ? 80 : 20;
    });
    vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(20);
    const user = userEvent.setup();
    const { rerender } = render(
      <DescriptionList>
        <DescriptionItem term="Description" truncate={2}>
          A long description that runs past two lines in the layout.
        </DescriptionItem>
      </DescriptionList>,
    );
    const toggle = await screen.findByRole('button', { name: 'Show more' });
    const value = document.querySelector('.mn-dl__clamp') as HTMLElement;
    expect(value.style.getPropertyValue('--_lines')).toBe('2');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveAttribute('aria-controls', value.id);
    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(toggle).toHaveTextContent('Show less');
    expect(value).toHaveAttribute('data-expanded');
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');

    overflow = false;
    await act(async () => {
      rerender(
        <DescriptionList>
          <DescriptionItem term="Description" truncate>
            Short
          </DescriptionItem>
        </DescriptionList>,
      );
    });
    expect(screen.queryByRole('button')).toBeNull();
    expect(document.querySelector('.mn-dl__clamp')).toHaveAttribute('data-single');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <DescriptionList columns={4}>
        <DescriptionItem term="Owner">Ada</DescriptionItem>
        <DescriptionItem term="Id" copyable>
          a1
        </DescriptionItem>
        <DescriptionItem term="Note" truncate>
          Text
        </DescriptionItem>
        <DescriptionItem term="Empty" />
      </DescriptionList>,
    );
    expect(html).toContain('mn-dl');
    expect(html).toContain('Not set');
    expect(html).toContain('mn-dl__clamp');
  });
});
