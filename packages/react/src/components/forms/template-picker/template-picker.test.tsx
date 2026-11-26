import { afterEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TemplatePicker, templateMatches, type TemplatePickerTemplate } from './index.js';

const TEMPLATES: TemplatePickerTemplate[] = [
  { id: 'classic', name: 'Classic', description: 'Header, intro and a list', category: 'pages' },
  {
    id: 'minimal',
    name: 'Minimal',
    description: 'Text only',
    category: 'pages',
    badge: 'Built-in',
  },
  {
    id: 'split',
    name: 'Split',
    description: 'Image beside text',
    category: 'pages',
    disabled: true,
  },
  { id: 'bug', name: 'Bug report', category: 'issues', keywords: ['defect'] },
  { id: 'feature', name: 'Feature request', category: 'issues' },
];
const CATEGORIES = [
  { id: 'pages', label: 'Pages' },
  { id: 'issues', label: 'Issues' },
];

const radio = (name: string | RegExp) => screen.getByRole('radio', { name });

/** jsdom has no layout: give cards a grid position so row navigation can be tested. */
function fakeColumns(cols: number) {
  return vi.spyOn(HTMLElement.prototype, 'offsetTop', 'get').mockImplementation(function (
    this: HTMLElement,
  ) {
    if (!this.classList.contains('mn-template-picker__card')) return 0;
    const cards = Array.from(this.parentElement?.children ?? []);
    return Math.floor(cards.indexOf(this) / cols) * 100;
  });
}

afterEach(() => vi.restoreAllMocks());

describe('TemplatePicker', () => {
  it('is a named radio group of cards with a blank option first', async () => {
    render(<TemplatePicker aria-label="Page template" templates={TEMPLATES} blank />);
    const group = screen.getByRole('radiogroup', { name: 'Page template' });
    const radios = within(group).getAllByRole('radio');
    expect(radios.map((r) => r.getAttribute('aria-labelledby') && r.textContent)).toHaveLength(6);
    expect(radios[0]).toHaveAccessibleName('Blank');
    expect(radio('Minimal')).toHaveAccessibleDescription('Built-in Text only');
    expect(radio('Split')).toBeDisabled();
    // One Tab stop: the first card while nothing is selected.
    expect(radios.filter((r) => r.tabIndex === 0)).toEqual([radios[0]]);
    await expectNoA11yViolations();
  });

  it('selects on click (uncontrolled) and reports the template', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<TemplatePicker templates={TEMPLATES} blank onValueChange={onValueChange} />);
    await user.click(radio('Classic'));
    expect(radio('Classic')).toHaveAttribute('aria-checked', 'true');
    expect(radio('Classic').tabIndex).toBe(0);
    expect(onValueChange).toHaveBeenCalledWith('classic', TEMPLATES[0]);
    await user.click(radio('Blank'));
    expect(onValueChange).toHaveBeenLastCalledWith('blank', null);
  });

  it('moves and selects with the arrow keys, skipping disabled cards and wrapping', async () => {
    const user = userEvent.setup();
    render(<TemplatePicker templates={TEMPLATES} defaultValue="classic" searchable={false} />);
    await user.tab();
    expect(radio('Classic')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(radio('Minimal')).toHaveFocus();
    expect(radio('Minimal')).toBeChecked();
    // Split is disabled.
    await user.keyboard('{ArrowRight}');
    expect(radio('Bug report')).toHaveFocus();
    await user.keyboard('{End}');
    expect(radio('Feature request')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(radio('Classic')).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(radio('Feature request')).toHaveFocus();
    await user.keyboard('{Home}');
    expect(radio('Classic')).toHaveFocus();
  });

  it('moves by row with Up and Down', async () => {
    fakeColumns(2);
    const user = userEvent.setup();
    render(<TemplatePicker templates={TEMPLATES} defaultValue="classic" searchable={false} />);
    // Grid: [Classic, Minimal] [Split(disabled), Bug] [Feature]
    await user.tab();
    await user.keyboard('{ArrowDown}');
    // Split is disabled, so Down goes on to the next enabled card in that column.
    expect(radio('Feature request')).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(radio('Classic')).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(radio('Classic')).toHaveFocus();
    await user.keyboard('{ArrowRight}{ArrowDown}');
    expect(radio('Bug report')).toHaveFocus();
  });

  it('filters by category and search, and announces the count', async () => {
    const user = userEvent.setup();
    render(<TemplatePicker templates={TEMPLATES} categories={CATEGORIES} blank />);
    const filters = screen.getByRole('group', { name: 'Categories' });
    expect(within(filters).getByRole('button', { name: 'All' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    await user.click(within(filters).getByRole('button', { name: 'Issues' }));
    expect(screen.getAllByRole('radio').map((r) => r.textContent)).toEqual([
      'Blank',
      'Bug report',
      'Feature request',
    ]);
    expect(screen.getByRole('status')).toHaveTextContent('2 templates');
    await user.type(screen.getByRole('searchbox', { name: 'Search templates' }), 'defect');
    expect(screen.getAllByRole('radio')).toHaveLength(2);
    expect(screen.getByRole('status')).toHaveTextContent('1 template');
    await user.clear(screen.getByRole('searchbox'));
    await user.type(screen.getByRole('searchbox'), 'zzz');
    expect(screen.getByText('No templates match “zzz”.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('follows a controlled value and shows the preview pane', async () => {
    const user = userEvent.setup();
    function Demo() {
      const [value, setValue] = useState<string | null>('minimal');
      return (
        <TemplatePicker
          templates={TEMPLATES}
          blank={{ name: 'Start from scratch', description: 'An empty page' }}
          value={value}
          onValueChange={(id) => setValue(id)}
          renderPreview={(t) => <p>Previewing {t ? t.name : 'blank'}</p>}
        />
      );
    }
    render(<Demo />);
    const preview = screen.getByRole('region', { name: 'Preview' });
    expect(preview).toHaveTextContent('Previewing Minimal');
    await user.click(radio('Start from scratch'));
    expect(preview).toHaveTextContent('Previewing blank');
    expect(radio('Start from scratch')).toHaveAccessibleDescription('An empty page');
  });

  it('matches names, descriptions and keywords', () => {
    expect(templateMatches(TEMPLATES[0] as TemplatePickerTemplate, 'header LIST')).toBe(true);
    expect(templateMatches(TEMPLATES[3] as TemplatePickerTemplate, 'defect')).toBe(true);
    expect(templateMatches(TEMPLATES[3] as TemplatePickerTemplate, 'image')).toBe(false);
    expect(templateMatches(TEMPLATES[3] as TemplatePickerTemplate, '  ')).toBe(true);
  });

  it('translates messages', () => {
    render(
      <TemplatePicker
        templates={TEMPLATES}
        categories={CATEGORIES}
        blank
        messages={{ blank: 'Leer', allCategories: 'Alle', searchLabel: 'Vorlagen suchen' }}
      />,
    );
    expect(radio('Leer')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Alle' })).toBeInTheDocument();
    expect(screen.getByRole('searchbox', { name: 'Vorlagen suchen' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<TemplatePicker templates={TEMPLATES} defaultValue="classic" />);
    expect(html).toContain('mn-template-picker');
    expect(html).toContain('role="radiogroup"');
  });
});
