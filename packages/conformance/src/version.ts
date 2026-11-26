import { readFileSync } from 'node:fs';

/**
 * Read from the package's own package.json at runtime, so the version in
 * reports and SARIF can never drift from the published version. Works from
 * both src/ (tests) and dist/ (published), which sit one level below it.
 */
const pkg = JSON.parse(readFileSync(new URL('../package.json', import.meta.url), 'utf8')) as {
  name: string;
  version: string;
};

export const VERSION: string = pkg.version;
export const TOOL_NAME: string = pkg.name;
