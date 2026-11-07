import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CodeBlock, CodeTabs, type CodeSample } from './index.js';

const ts = `const total = items.reduce((sum, i) => sum + i.price, 0);\n`;

const samples: CodeSample[] = [
  { label: 'cURL', language: 'bash', code: 'curl https://api.example.com/v1/items' },
  { label: 'JavaScript', language: 'js', code: "await fetch('https://api.example.com/v1/items');" },
  { label: 'Python', language: 'python', code: "requests.get('https://api.example.com/v1/items')" },
];

describe('CodeBlock', () => {
  it('shows a titled, focusable, named code region with copy and wrap', async () => {
    render(<CodeBlock code={ts} language="ts" title="cart.ts" data-testid="root" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-code-block', 'mn-ai-code-block');
    const region = screen.getByRole('region', { name: 'cart.ts' });
    expect(region).toHaveAttribute('tabindex', '0');
    expect(region.querySelector('code')).toHaveTextContent(ts.trim());
    expect(region.querySelector('[data-token="keyword"]')).toHaveTextContent('const');
    expect(screen.getByRole('button', { name: /Copy/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Wrap lines' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    // No AI actions.
    expect(screen.queryByRole('button', { name: /Apply|Insert/ })).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('defaults the name to the language and copies the exact code', async () => {
    render(<CodeBlock code={ts} language="python" />);
    expect(screen.getByRole('region', { name: 'Python code' })).toBeInTheDocument();
    // user-event installs a clipboard stub we can read back.
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Copy/ }));
    await waitFor(async () => expect(await navigator.clipboard.readText()).toBe(ts));
  });

  it('toggles wrapping with the keyboard and supports line numbers and diffs', async () => {
    const onWrapChange = vi.fn();
    const { container, rerender } = render(
      <CodeBlock code={'a\nb'} lineNumbers onWrapChange={onWrapChange} />,
    );
    const user = userEvent.setup();
    screen.getByRole('button', { name: 'Wrap lines' }).focus();
    await user.keyboard('{Enter}');
    expect(onWrapChange).toHaveBeenCalledWith(true);
    expect(container.firstChild).toHaveAttribute('data-wrap');
    expect(
      container.querySelectorAll('.mn-ai-code-block__number[aria-hidden="true"]'),
    ).toHaveLength(2);
    rerender(<CodeBlock code={'-old\n+new'} language="diff" messages={{ added: 'Neu' }} />);
    expect(container.firstChild).toHaveAttribute('data-diff');
    expect(container).toHaveTextContent('Neu:');
  });

  it('forwards the ref and renders on the server', () => {
    let node: HTMLDivElement | null = null;
    render(
      <CodeBlock
        ref={(el) => {
          node = el;
        }}
        code="x"
      />,
    );
    expect(node).toHaveClass('mn-code-block');
    expect(renderToString(<CodeBlock code={ts} language="ts" />)).toContain('mn-code-block');
  });
});

describe('CodeTabs', () => {
  it('shows one tab per sample, each with its own block', async () => {
    render(<CodeTabs samples={samples} label="Request examples" />);
    const tablist = screen.getByRole('tablist', { name: 'Request examples' });
    expect(
      within(tablist)
        .getAllByRole('tab')
        .map((t) => t.textContent),
    ).toEqual(['cURL', 'JavaScript', 'Python']);
    expect(screen.getByRole('tab', { name: 'cURL' })).toHaveAttribute('aria-selected', 'true');
    const panel = screen.getByRole('tabpanel');
    expect(within(panel).getByRole('region', { name: 'cURL' })).toHaveTextContent(
      'curl https://api.example.com/v1/items',
    );
    await expectNoA11yViolations();
  });

  it('arrow keys switch samples and report the choice', async () => {
    const onValueChange = vi.fn();
    render(<CodeTabs samples={samples} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('tab', { name: 'cURL' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(onValueChange).toHaveBeenCalledWith('JavaScript');
    expect(screen.getByRole('tabpanel')).toHaveTextContent(
      "await fetch('https://api.example.com/v1/items');",
    );
    // From the tab, Tab goes into the panel's controls, not to the panel itself.
    await user.tab();
    expect(screen.getByRole('tabpanel').contains(document.activeElement)).toBe(true);
    expect(document.activeElement).not.toBe(screen.getByRole('tabpanel'));
  });

  it('can be controlled', () => {
    const { rerender } = render(<CodeTabs samples={samples} value="Python" />);
    expect(screen.getByRole('tab', { name: 'Python' })).toHaveAttribute('aria-selected', 'true');
    rerender(<CodeTabs samples={samples} value="cURL" />);
    expect(screen.getByRole('tab', { name: 'cURL' })).toHaveAttribute('aria-selected', 'true');
  });

  it('renders on the server', () => {
    expect(renderToString(<CodeTabs samples={samples} />)).toContain('mn-code-tabs');
  });
});
