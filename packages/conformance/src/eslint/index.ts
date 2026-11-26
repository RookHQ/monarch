/**
 * `@rookcom/monarch-conformance/eslint`: the Monarch rules as an ESLint 9
 * flat-config plugin, for feedback in the editor while code is written.
 * `monarch check` stays the whole-project check for CI.
 *
 *   import monarch from '@rookcom/monarch-conformance/eslint';
 *   export default [monarch.configs.recommended];
 *
 * `eslint` is an optional peer: nothing here imports it.
 */
import { VERSION } from '../version.js';
import { noArbitrarySpacing } from './rules/no-arbitrary-spacing.js';
import { noDeepImports } from './rules/no-deep-imports.js';
import { noDeprecatedApi } from './rules/no-deprecated-api.js';
import { noRawColors } from './rules/no-raw-colors.js';
import { noRookSpecific } from './rules/no-rook-specific.js';
import { preferMonarchComponent } from './rules/prefer-monarch-component.js';
import { requireAccessibleName } from './rules/require-accessible-name.js';
import { requireEntryStyles } from './rules/require-entry-styles.js';
import type { FlatConfig, MonarchPlugin, MonarchRule, RuleLevel } from './types.js';

export const rules = {
  'no-deep-imports': noDeepImports,
  'require-entry-styles': requireEntryStyles,
  'no-raw-colors': noRawColors,
  'no-arbitrary-spacing': noArbitrarySpacing,
  'prefer-monarch-component': preferMonarchComponent,
  'no-deprecated-api': noDeprecatedApi,
  'no-rook-specific': noRookSpecific,
  'require-accessible-name': requireAccessibleName,
} satisfies Record<string, MonarchRule>;

export type MonarchRuleName = keyof typeof rules;

/** Levels of the `recommended` config; they match the severities of `monarch check`. */
export const RECOMMENDED_LEVELS: Readonly<Record<MonarchRuleName, RuleLevel>> = {
  'no-deep-imports': 'error',
  'require-entry-styles': 'warn',
  'no-raw-colors': 'error',
  'no-arbitrary-spacing': 'warn',
  'prefer-monarch-component': 'warn',
  'no-deprecated-api': 'warn',
  'no-rook-specific': 'off',
  'require-accessible-name': 'error',
};

const FILES = ['**/*.{js,jsx,mjs,cjs,ts,tsx,mts,cts}'];

const prefixed = (levels: Readonly<Record<string, RuleLevel>>): Record<string, RuleLevel> =>
  Object.fromEntries(Object.entries(levels).map(([name, level]) => [`monarch/${name}`, level]));

const meta = { name: '@rookcom/monarch-conformance/eslint', version: VERSION };

/** Filled in below: a flat config names the plugin object it belongs to. */
const configs = {} as { recommended: FlatConfig; general: FlatConfig };

const plugin: MonarchPlugin = { meta, rules, configs };

/** For apps built by Rook: every rule except `no-rook-specific`. */
configs.recommended = {
  name: 'monarch/recommended',
  files: FILES,
  plugins: { monarch: plugin },
  rules: prefixed(RECOMMENDED_LEVELS),
};

/** For teams outside Rook: `recommended` plus `no-rook-specific`. */
configs.general = {
  name: 'monarch/general',
  files: FILES,
  plugins: { monarch: plugin },
  rules: prefixed({ ...RECOMMENDED_LEVELS, 'no-rook-specific': 'warn' }),
};

export { configs, meta };
export type * from './types.js';
export default plugin;
