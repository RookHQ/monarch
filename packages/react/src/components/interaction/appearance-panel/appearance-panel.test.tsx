import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { ShellCookieStore } from '../../../internal/shell/cookies.js';
import { AppearancePanel } from './index.js';

function memoryStore(initial: Record<string, string> = {}) {
  const jar = new Map(Object.entries(initial));
  const set = vi.fn((name: string, value: string) => void jar.set(name, value));
  const store: ShellCookieStore = { get: (n) => jar.get(n) ?? null, set };
  return { store, set, jar };
}

const html = document.documentElement;
function reset() {
  for (const a of ['data-mn-theme', 'data-mn-contrast', 'data-mn-motion', 'data-mn-density'])
    html.removeAttribute(a);
  html.classList.remove('dark');
  html.style.colorScheme = '';
}
beforeEach(reset);
afterEach(() => {
  reset();
  vi.restoreAllMocks();
});

const group = (name: string) => screen.getByRole('group', { name });
const radio = (name: string, within: HTMLElement) =>
  Array.from(within.querySelectorAll<HTMLElement>('[role="radio"]')).find((r) =>
    r.textContent?.includes(name),
  )!;

describe('AppearancePanel', () => {
  it('shows four labelled, described rows', async () => {
    const { store } = memoryStore();
    render(<AppearancePanel cookies={store} />);
    for (const name of ['Theme', 'Contrast', 'Motion', 'Density'])
      expect(group(name)).toBeInTheDocument();
    expect(group('Contrast')).toHaveAccessibleDescription('Stronger text and border colours.');
    expect(screen.getByRole('radiogroup', { name: 'Contrast' })).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: 'Theme' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('picks up saved theme and contrast after mount', async () => {
    const { store } = memoryStore({ rook_theme: 'dark', mn_contrast: 'more' });
    render(<AppearancePanel cookies={store} />);
    await waitFor(() =>
      expect(radio('More', group('Contrast'))).toHaveAttribute('aria-checked', 'true'),
    );
    expect(radio('Dark', group('Theme'))).toHaveAttribute('aria-checked', 'true');
    expect(html).toHaveAttribute('data-mn-contrast', 'more');
  });

  it('saves contrast in mn_contrast and applies it, keeping the theme', async () => {
    const user = userEvent.setup();
    const { store, set } = memoryStore();
    const onValueChange = vi.fn();
    html.setAttribute('data-mn-theme', 'dark');
    render(<AppearancePanel cookies={store} onValueChange={onValueChange} />);
    await user.click(radio('More', group('Contrast')));
    expect(set).toHaveBeenCalledWith(
      'mn_contrast',
      'more',
      expect.objectContaining({ maxAge: expect.any(Number) }),
    );
    expect(html).toHaveAttribute('data-mn-contrast', 'more');
    expect(html).toHaveAttribute('data-mn-theme', 'dark');
    expect(onValueChange).toHaveBeenLastCalledWith({
      theme: 'light',
      contrast: 'more',
      motion: 'system',
      density: 'comfortable',
    });
  });

  it('saves and applies the theme through ThemeToggle', async () => {
    const user = userEvent.setup();
    const { store, set } = memoryStore();
    const onValueChange = vi.fn();
    render(<AppearancePanel cookies={store} onValueChange={onValueChange} />);
    await user.click(radio('Dark', group('Theme')));
    expect(set).toHaveBeenCalledWith('rook_theme', 'dark', expect.anything());
    expect(html).toHaveAttribute('data-mn-theme', 'dark');
    expect(onValueChange).toHaveBeenLastCalledWith(expect.objectContaining({ theme: 'dark' }));
  });

  it('applies motion and density as attributes and reports them', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<AppearancePanel cookies={null} onValueChange={onValueChange} />);
    await user.click(radio('Reduce', group('Motion')));
    expect(html).toHaveAttribute('data-mn-motion', 'reduce');
    await user.click(radio('Compact', group('Density')));
    expect(html).toHaveAttribute('data-mn-density', 'compact');
    expect(onValueChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ motion: 'reduce', density: 'compact' }),
    );
    await user.click(radio('System', group('Motion')));
    expect(html).not.toHaveAttribute('data-mn-motion');
  });

  it('moves between options with the arrow keys', async () => {
    const user = userEvent.setup();
    render(<AppearancePanel cookies={null} sections={['density']} />);
    await user.tab();
    expect(radio('Comfortable', group('Density'))).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(radio('Compact', group('Density'))).toHaveAttribute('aria-checked', 'true');
    expect(html).toHaveAttribute('data-mn-density', 'compact');
  });

  it('is controlled, shows only chosen sections, and can leave the document alone', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <AppearancePanel
        cookies={null}
        applyToDocument={false}
        sections={['motion', 'density']}
        value={{ theme: 'light', contrast: 'standard', motion: 'reduce', density: 'compact' }}
        onValueChange={onValueChange}
        heading="Appearance"
        messages={{ motion: 'Movimiento' }}
      />,
    );
    expect(screen.queryByRole('group', { name: 'Theme' })).not.toBeInTheDocument();
    expect(screen.getByText('Appearance')).toBeInTheDocument();
    expect(radio('Reduce', group('Movimiento'))).toHaveAttribute('aria-checked', 'true');
    await user.click(radio('System', group('Movimiento')));
    expect(onValueChange).toHaveBeenLastCalledWith(expect.objectContaining({ motion: 'system' }));
    // Controlled: stays until the parent changes value.
    expect(radio('Reduce', group('Movimiento'))).toHaveAttribute('aria-checked', 'true');
    expect(html).not.toHaveAttribute('data-mn-motion');
  });

  it('renders on the server without touching the document', () => {
    const out = renderToString(
      <AppearancePanel cookies={null} defaultValue={{ density: 'compact' }} />,
    );
    expect(out).toContain('mn-appearance-panel');
    expect(out).toContain('Density');
    expect(html).not.toHaveAttribute('data-mn-density');
  });
});
