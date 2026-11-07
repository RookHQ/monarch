import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiSourcePart } from '../ai-conversation/index.js';
import { AiCitation, aiSourceHost, aiSourceIndex, aiSourceMeta } from './index.js';

const source: AiSourcePart = {
  type: 'source',
  id: 's1',
  title: 'Quarterly planning notes',
  url: 'https://www.example.com/notes/q3',
  kind: 'doc',
  product: 'Docs',
};

describe('AiCitation', () => {
  it('is a button named "Source n: title" showing [n]', async () => {
    render(
      <p>
        Revenue grew <AiCitation index={1} source={source} openDelay={0} data-testid="m" />
      </p>,
    );
    const marker = screen.getByRole('button', { name: 'Source 1: Quarterly planning notes' });
    expect(marker).toHaveClass('mn-ai-citation');
    expect(marker).toHaveTextContent('[1]');
    expect(marker).toHaveAttribute('type', 'button');
    expect(marker).toHaveAttribute('aria-expanded', 'false');
    await expectNoA11yViolations();
  });

  it('opens the card on focus without moving focus, and Escape closes it', async () => {
    render(<AiCitation index={2} source={source} quote="Revenue grew 12%." />);
    const user = userEvent.setup();
    await user.tab();
    const marker = screen.getByRole('button', { name: /Source 2/ });
    expect(marker).toHaveFocus();
    const card = await screen.findByRole('dialog', { name: 'Source 2' });
    expect(card).toHaveTextContent('Quarterly planning notes');
    expect(card).toHaveTextContent('Revenue grew 12%.');
    expect(card).toHaveTextContent('example.com · Docs · Document');
    expect(marker).toHaveAttribute('aria-expanded', 'true');
    expect(marker).toHaveFocus();
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(marker).toHaveFocus();
  });

  it('opens on hover and closes when the pointer leaves', async () => {
    render(<AiCitation index={1} source={source} openDelay={0} closeDelay={0} />);
    const user = userEvent.setup();
    const marker = screen.getByRole('button');
    await user.hover(marker);
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    await user.unhover(marker);
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('Enter pins the card and moves focus into it; Escape returns focus to the marker', async () => {
    const onOpen = vi.fn();
    render(<AiCitation index={1} source={source} onOpen={onOpen} />);
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{Enter}');
    const open = screen.getByRole('button', { name: 'Open source' });
    await waitFor(() => expect(open).toHaveFocus());
    await user.tab();
    const visit = screen.getByRole('link', { name: 'Visit page (opens in a new tab)' });
    expect(visit).toHaveFocus();
    expect(visit).toHaveAttribute('href', source.url);
    expect(visit).toHaveAttribute('target', '_blank');
    expect(visit).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(screen.getByRole('button', { name: /Source 1/ })).toHaveFocus();
  });

  it('calls onOpen with the source and closes the card', async () => {
    const onOpen = vi.fn();
    render(<AiCitation index={1} source={source} onOpen={onOpen} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Source 1/ }));
    await user.click(await screen.findByRole('button', { name: 'Open source' }));
    expect(onOpen).toHaveBeenCalledWith(source);
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('never links to unsafe URLs', async () => {
    render(
      <AiCitation
        index={1}
        source={{ ...source, url: 'javascript:alert(1)' }}
        open
        onOpenChange={() => {}}
      />,
    );
    const card = await screen.findByRole('dialog');
    expect(card.querySelector('a')).toBeNull();
    expect(card).not.toHaveTextContent('javascript');
  });

  it('uses translated messages', () => {
    render(
      <AiCitation index={3} source={source} messages={{ marker: (n, t) => `Quelle ${n}: ${t}` }} />,
    );
    expect(
      screen.getByRole('button', { name: 'Quelle 3: Quarterly planning notes' }),
    ).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiCitation index={1} source={source} />);
    expect(html).toContain('mn-ai-citation');
    expect(html).toContain('[<!-- -->1<!-- -->]');
  });
});

describe('source helpers', () => {
  it('derives the host, meta line and numbering', () => {
    expect(aiSourceHost(source)).toBe('example.com');
    expect(aiSourceHost({ url: 'javascript:void 0' })).toBeUndefined();
    expect(aiSourceHost({})).toBeUndefined();
    expect(aiSourceMeta({ kind: 'record', product: 'CRM' })).toBe('CRM · Record');
    const index = aiSourceIndex([
      { type: 'text', text: 'x', state: 'done' },
      source,
      { ...source, id: 's2' },
      source,
    ]);
    expect(index.get('s1')?.index).toBe(1);
    expect(index.get('s2')?.index).toBe(2);
    expect(index.size).toBe(2);
  });
});
