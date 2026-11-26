import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import {
  LANGUAGE_TAGS,
  LanguageSelect,
  canonicalLanguageTag,
  getLanguageName,
  listLanguages,
} from './index.js';

const labelOf = (o: Element) => o.querySelector('.mn-menu__text')?.firstChild?.textContent;

describe('language helpers', () => {
  it('canonicalises tags and rejects invalid ones', () => {
    expect(canonicalLanguageTag('EN-us')).toBe('en-US');
    expect(canonicalLanguageTag('not a tag')).toBeNull();
    expect(LANGUAGE_TAGS.every((t) => canonicalLanguageTag(t) === t)).toBe(true);
  });

  it('gives endonyms, capitalised, and names in a locale', () => {
    expect(getLanguageName('de')).toBe('Deutsch');
    expect(getLanguageName('fr')).toBe('Français');
    expect(getLanguageName('de', 'en')).toBe('German');
    expect(getLanguageName('ja')).toBe('日本語');
  });

  it('does not throw on a locale the runtime does not know', () => {
    expect(listLanguages({ locale: 'not a locale', tags: ['de', 'fr'] })).toHaveLength(2);
  });

  it('lists languages sorted by name in the locale, dropping invalid tags', () => {
    const list = listLanguages({ tags: ['ja', 'de', 'bad tag!'] });
    expect(list).toEqual([
      { tag: 'de', endonym: 'Deutsch', name: 'German' },
      { tag: 'ja', endonym: '日本語', name: 'Japanese' },
    ]);
  });
});

describe('LanguageSelect', () => {
  it('shows endonyms with lang, searches either name and selects', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Language</FieldLabel>
        <LanguageSelect onValueChange={onValueChange} languages={['de', 'fr', 'ja', 'es']} />
      </Field>,
    );
    const input = screen.getByRole('combobox', { name: 'Language' });
    const user = userEvent.setup();
    await user.type(input, 'german');
    const [german] = screen.getAllByRole('option');
    expect(labelOf(german!)).toBe('Deutsch');
    expect(german!.querySelector('[lang="de"]')).toHaveTextContent('Deutsch');
    expect(german!.querySelector('[lang="en"]')).toHaveTextContent('German');
    await user.clear(input);
    await user.type(input, '日本');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('ja');
    expect(input).toHaveValue('日本語');
    await expectNoA11yViolations();
  });

  it('lists suggested languages and supports localized labels with tags', async () => {
    render(
      <LanguageSelect
        aria-label="Language"
        languages={['en', 'pt-BR', 'pt-PT']}
        priority={['PT-br']}
        labelStyle="localized"
        showTag
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    const groups = await screen.findAllByRole('group');
    expect(groups.map((g) => g.firstChild?.textContent)).toEqual(['Suggested', 'All languages']);
    const option = within(groups[0]!).getByRole('option');
    expect(labelOf(option)).toBe('Brazilian Portuguese');
    expect(option).toHaveTextContent('pt-BR');
    expect(option.querySelector('[lang="pt-BR"]')).toHaveTextContent('Português (Brasil)');
    await expectNoA11yViolations();
  });

  it('renders on the server with a canonical value', () => {
    const html = renderToString(<LanguageSelect aria-label="Language" defaultValue="ES-419" />);
    expect(html).toContain('mn-language-select');
    expect(html).toContain('Español latinoamericano');
  });
});
