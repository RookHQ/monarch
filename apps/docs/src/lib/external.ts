/** Spread onto links that leave the docs site: they open in a new tab, without handing it `window.opener`. */
export const NEW_TAB = { target: '_blank', rel: 'noopener noreferrer' } as const;

/** The maker's site. Plain `noopener` (no `noreferrer`), so rookhq.com can see visits coming from Monarch. */
export const MAKER_URL = 'https://rookhq.com';
export const MAKER_LINK = { target: '_blank', rel: 'noopener', href: MAKER_URL } as const;
