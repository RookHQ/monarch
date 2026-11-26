// Type-level contract: the preset must be assignable to Tailwind 3's Config
// without casts (checked by `npm run typecheck`, not executed).
import type { Config } from 'tailwindcss';
import monarch from '../src/tailwind.js';

export const config: Config = { presets: [monarch], content: ['./src/**/*.{ts,tsx}'] };
