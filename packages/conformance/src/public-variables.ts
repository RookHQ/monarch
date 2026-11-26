/**
 * `--mn-*` variables that @rookcom/monarch-react documents as a public styling
 * API: a host app may set them in its own stylesheet or inline style, so
 * `overrides/monarch-internals` does not report them. Every other `--mn-*`
 * variable stays internal. Keep in sync with the component docs that
 * document each one (test/public-variables.test.ts checks the stylesheets).
 */
export const PUBLIC_MONARCH_VARIABLES: ReadonlySet<string> = new Set([
  // Toast: move the region clear of a docked panel (toast.docs.tsx).
  'mn-toast-offset-block-start',
  'mn-toast-offset-block-end',
  'mn-toast-offset-inline-end',
]);

/** True for a documented public variable, given without the leading `--`. */
export function isPublicMonarchVariable(name: string): boolean {
  return PUBLIC_MONARCH_VARIABLES.has(name);
}
