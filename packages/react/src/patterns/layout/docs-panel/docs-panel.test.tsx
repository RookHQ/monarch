import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DocsPanel } from './index.js';
import type { LinkedDocItem } from '../../../components/collaboration/linked-docs-panel/index.js';

const items: LinkedDocItem[] = [
  { id: 'l1', kind: 'doc', docId: 'd1', title: 'Release runbook', href: '#runbook' },
  { id: 'l2', kind: 'link', title: 'Design file', href: 'https://design.example.com/f/1' },
];

describe('DocsPanel', () => {
  it('frames the linked docs panel in a card by default', async () => {
    const { container } = render(<DocsPanel items={items} readOnly />);
    const root = container.firstElementChild!;
    expect(root).toHaveClass('mn-docs-panel');
    expect(root).toHaveAttribute('data-frame', 'card');
    expect(screen.getByRole('region', { name: /Docs/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Release runbook/ })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('passes callbacks and messages through to the panel', async () => {
    const user = userEvent.setup();
    const onUnlink = vi.fn();
    render(
      <DocsPanel
        items={items}
        onUnlink={onUnlink}
        frame="divider"
        messages={{ title: 'Documents' }}
      />,
    );
    expect(screen.getByRole('region', { name: /Documents/ })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Unlink Release runbook' }));
    expect(onUnlink).toHaveBeenCalledWith(items[0]);
  });

  it('collapsible: mounts the panel only when opened, with a count on the toggle', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<DocsPanel items={items} collapsible count={2} onOpenChange={onOpenChange} readOnly />);
    const toggle = screen.getByRole('button', { name: 'Docs , 2 linked' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('region')).not.toBeInTheDocument();
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(onOpenChange).toHaveBeenCalledWith(true);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    const region = screen.getByRole('region', { name: /Docs/ });
    expect(toggle).toHaveAttribute('aria-controls', region.id);
    await expectNoA11yViolations();
    await user.keyboard(' ');
    expect(screen.queryByRole('region')).not.toBeInTheDocument();
  });

  it('collapsible and controlled', () => {
    const { rerender } = render(<DocsPanel items={null} collapsible open={false} />);
    expect(screen.queryByRole('region')).not.toBeInTheDocument();
    rerender(<DocsPanel items={null} collapsible open />);
    expect(screen.getByRole('region')).toHaveAttribute('aria-busy', 'true');
  });

  it('translates the toggle', () => {
    render(
      <DocsPanel
        items={items}
        collapsible
        count={1}
        messages={{ toggle: 'Documentos', toggleCount: (n) => `${n} vinculado` }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Documentos , 1 vinculado' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<DocsPanel items={items} />);
    expect(html).toContain('mn-docs-panel');
    expect(html).toContain('Release runbook');
  });
});
