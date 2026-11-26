import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TruncatedText } from './index.js';

const LONG =
  'Quarterly planning notes covering hiring, the roadmap for the next two releases and the migration of the reporting service.';

let overflow = true;
function mockLayout() {
  vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockImplementation(function (
    this: HTMLElement,
  ) {
    return this.classList.contains('mn-truncated-text__text') && overflow ? 80 : 20;
  });
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(20);
}

afterEach(() => {
  overflow = true;
  vi.restoreAllMocks();
});

describe('TruncatedText', () => {
  it('keeps the full text in the page and adds "Show more" only when it overflows', async () => {
    mockLayout();
    render(
      <main>
        <TruncatedText lines={2} data-testid="root">
          {LONG}
        </TruncatedText>
      </main>,
    );
    const toggle = await screen.findByRole('button', { name: 'Show more' });
    const text = document.querySelector('.mn-truncated-text__text') as HTMLElement;
    expect(text).toHaveTextContent(LONG);
    expect(text.style.getPropertyValue('--_lines')).toBe('2');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveAttribute('aria-controls', text.id);
    expect(screen.getByTestId('root')).toHaveAttribute('data-clamped');
    await expectNoA11yViolations();
  });

  it('expands and collapses with the keyboard, keeping focus on the button', async () => {
    mockLayout();
    const onExpandedChange = vi.fn();
    render(<TruncatedText onExpandedChange={onExpandedChange}>{LONG}</TruncatedText>);
    const user = userEvent.setup();
    const toggle = await screen.findByRole('button', { name: 'Show more' });
    await user.tab();
    expect(toggle).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(toggle).toHaveTextContent('Show less');
    expect(toggle).toHaveFocus();
    expect(document.querySelector('.mn-truncated-text__text')).toHaveAttribute('data-expanded');
    expect(onExpandedChange).toHaveBeenLastCalledWith(true);
    await user.keyboard(' ');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
  });

  it('shows no button for text that fits', async () => {
    overflow = false;
    mockLayout();
    await act(async () => {
      render(<TruncatedText lines={1}>Short</TruncatedText>);
    });
    expect(screen.queryByRole('button')).toBeNull();
    expect(document.querySelector('.mn-truncated-text__text')).toHaveAttribute('data-single');
  });

  it('translates its labels and can start expanded', async () => {
    mockLayout();
    render(
      <TruncatedText defaultExpanded messages={{ showLess: 'Collapse' }}>
        {LONG}
      </TruncatedText>,
    );
    expect(await screen.findByRole('button', { name: 'Collapse' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
  });

  it('offers the full text in a tooltip, reachable by keyboard, when not expandable', async () => {
    mockLayout();
    render(
      <main>
        <TruncatedText lines={1} tooltip expandable={false}>
          {LONG}
        </TruncatedText>
      </main>,
    );
    const user = userEvent.setup();
    const text = document.querySelector('.mn-truncated-text__text') as HTMLElement;
    await waitFor(() => expect(text).toHaveAttribute('tabindex', '0'));
    expect(screen.queryByRole('button')).toBeNull();
    await user.tab();
    expect(text).toHaveFocus();
    const tip = await screen.findByRole('tooltip');
    expect(tip).toHaveTextContent(LONG);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('tooltip')).toBeNull());
    await expectNoA11yViolations();
  });

  it('renders on the server with the full text', () => {
    const html = renderToString(<TruncatedText tooltip>{LONG}</TruncatedText>);
    expect(html).toContain('mn-truncated-text');
    expect(html).toContain(LONG);
  });
});
