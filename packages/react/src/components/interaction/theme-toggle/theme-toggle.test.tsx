import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { hydrateRoot } from 'react-dom/client';
import { act } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { ShellCookieStore } from '../../../internal/shell/cookies.js';
import { ThemeToggle, applyThemePreference } from './index.js';

function memoryStore(initial: Record<string, string> = {}) {
  const jar = new Map(Object.entries(initial));
  const set = vi.fn((name: string, value: string) => void jar.set(name, value));
  const store: ShellCookieStore = { get: (n) => jar.get(n) ?? null, set };
  return { store, set, jar };
}

const html = document.documentElement;
beforeEach(() => {
  html.removeAttribute('data-mn-theme');
  html.removeAttribute('data-mn-contrast');
  html.removeAttribute('data-rook-theme-routes');
  html.classList.remove('dark');
  html.style.colorScheme = '';
});
afterEach(() => vi.restoreAllMocks());

describe('ThemeToggle', () => {
  it('shows three choices and checks the saved one', async () => {
    const { store } = memoryStore({ rook_theme: 'dark' });
    render(<ThemeToggle cookies={store} />);
    const group = screen.getByRole('radiogroup', { name: 'Theme' });
    expect(group).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Dark' })).toHaveAttribute('aria-checked', 'true');
    await expectNoA11yViolations();
  });

  it('saves the choice in the rook_theme cookie on the Rook domain and applies it', async () => {
    const { store, set } = memoryStore();
    const onValueChange = vi.fn();
    render(<ThemeToggle cookies={store} onValueChange={onValueChange} />);
    await userEvent.setup().click(screen.getByRole('radio', { name: 'Dark' }));
    expect(set).toHaveBeenCalledWith(
      'rook_theme',
      'dark',
      expect.objectContaining({ maxAge: 31536000 }),
    );
    expect(onValueChange).toHaveBeenCalledWith('dark');
    expect(html).toHaveAttribute('data-mn-theme', 'dark');
    expect(html).toHaveClass('dark');
    expect(html.style.colorScheme).toBe('dark');
    expect(screen.getByRole('radio', { name: 'Dark' })).toHaveAttribute('aria-checked', 'true');
  });

  it('uses a configurable cookie name and domain', async () => {
    const { store, set } = memoryStore();
    render(<ThemeToggle cookies={store} cookieName="acme_theme" cookieDomain=".example.com" />);
    await userEvent.setup().click(screen.getByRole('radio', { name: 'System' }));
    expect(set).toHaveBeenCalledWith('acme_theme', 'system', {
      maxAge: 31536000,
      domain: '.example.com',
    });
  });

  it('moves between choices with the arrow keys', async () => {
    const { store } = memoryStore();
    render(<ThemeToggle cookies={store} />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Light' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: 'Dark' })).toHaveFocus();
    expect(screen.getByRole('radio', { name: 'Dark' })).toHaveAttribute('aria-checked', 'true');
  });

  it('is controllable', async () => {
    const { store } = memoryStore();
    const onValueChange = vi.fn();
    const { rerender } = render(
      <ThemeToggle
        cookies={store}
        value="light"
        onValueChange={onValueChange}
        applyToDocument={false}
      />,
    );
    await userEvent.setup().click(screen.getByRole('radio', { name: 'Dark' }));
    expect(onValueChange).toHaveBeenCalledWith('dark');
    // The parent has not accepted the change yet.
    expect(screen.getByRole('radio', { name: 'Light' })).toHaveAttribute('aria-checked', 'true');
    rerender(<ThemeToggle cookies={store} value="dark" applyToDocument={false} />);
    expect(screen.getByRole('radio', { name: 'Dark' })).toHaveAttribute('aria-checked', 'true');
    expect(html).not.toHaveAttribute('data-mn-theme');
  });

  it('menu variant: one button that opens a Light / Dark / System menu', async () => {
    const { store } = memoryStore({ rook_theme: 'system' });
    render(<ThemeToggle variant="menu" cookies={store} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Theme: System' });
    await user.click(trigger);
    expect(screen.getByRole('menuitemradio', { name: 'System' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    await expectNoA11yViolations();
    await user.keyboard('{ArrowDown}');
    await user.click(screen.getByRole('menuitemradio', { name: 'Light' }));
    expect(screen.getByRole('button', { name: 'Theme: Light' })).toBeInTheDocument();
    expect(html).toHaveAttribute('data-mn-theme', 'light');
  });

  it('keeps the menu text short with showLabel=false and translates', () => {
    const { store } = memoryStore();
    render(
      <ThemeToggle
        variant="menu"
        showLabel={false}
        cookies={store}
        messages={{ label: 'Thème', light: 'Clair' }}
      />,
    );
    const trigger = screen.getByRole('button', { name: 'Thème: Clair' });
    expect(trigger).not.toHaveTextContent('Clair');
  });

  it('ignores an unknown cookie value', () => {
    const { store } = memoryStore({ rook_theme: 'sepia' });
    render(<ThemeToggle cookies={store} defaultValue="system" />);
    expect(screen.getByRole('radio', { name: 'System' })).toHaveAttribute('aria-checked', 'true');
  });

  it('keeps choices in memory with cookies={null}', async () => {
    render(<ThemeToggle cookies={null} applyToDocument={false} />);
    await userEvent.setup().click(screen.getByRole('radio', { name: 'Dark' }));
    expect(screen.getByRole('radio', { name: 'Dark' })).toHaveAttribute('aria-checked', 'true');
  });

  it('follows the OS for system, and keeps public routes light', () => {
    const mq = vi.spyOn(window, 'matchMedia').mockImplementation(
      (q: string) =>
        ({
          matches: q.includes('dark'),
          media: q,
          addEventListener() {},
          removeEventListener() {},
        }) as unknown as MediaQueryList,
    );
    expect(applyThemePreference('system')).toBe('dark');
    html.setAttribute('data-rook-theme-routes', '^/w/');
    expect(applyThemePreference('dark')).toBe('light');
    expect(html).toHaveAttribute('data-mn-theme', 'light');
    mq.mockRestore();
  });

  it('keeps the contrast preference', () => {
    html.setAttribute('data-mn-contrast', 'more');
    applyThemePreference('dark');
    expect(html).toHaveAttribute('data-mn-contrast', 'more');
  });

  it('renders the default on the server and hydrates to the saved choice without a mismatch', async () => {
    const { store } = memoryStore({ rook_theme: 'dark' });
    const markup = renderToString(<ThemeToggle cookies={store} />);
    expect(markup).toContain('data-value="light"');
    const container = document.createElement('div');
    container.innerHTML = markup;
    document.body.appendChild(container);
    (globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {});
    await act(async () => {
      hydrateRoot(container, <ThemeToggle cookies={store} />);
    });
    expect(errors).not.toHaveBeenCalled();
    expect(container.querySelector('.mn-theme-toggle')).toHaveAttribute('data-value', 'dark');
    container.remove();
  });
});
