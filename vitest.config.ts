import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    coverage: {
      provider: 'v8',
      include: ['packages/*/src/**/*.{ts,tsx}'],
      exclude: ['**/*.test.{ts,tsx}', '**/*.docs.{ts,tsx}', '**/generated/**'],
      // Floors one step below the first full CI coverage run (statements 92.86,
      // branches 85.9, functions 94.98, lines 95.7). Raise them as coverage
      // rises; never lower one to make a change pass.
      thresholds: { statements: 92, branches: 85, functions: 94, lines: 95 },
    },
    projects: [
      {
        test: {
          name: 'tokens',
          root: './packages/tokens',
          environment: 'node',
          include: ['test/**/*.test.ts'],
        },
      },
      {
        test: {
          name: 'conformance',
          root: './packages/conformance',
          environment: 'node',
          include: ['test/**/*.test.ts'],
        },
      },
      {
        // Build scripts of the React package (manifest helpers).
        test: {
          name: 'react-scripts',
          root: './packages/react',
          environment: 'node',
          include: ['scripts/**/*.test.ts'],
        },
      },
      {
        // The docs site's build plugins: example extraction and the site index.
        esbuild: { jsx: 'automatic' },
        test: {
          name: 'docs',
          root: './apps/docs',
          environment: 'jsdom',
          include: ['plugins/**/*.test.ts', 'scripts/**/*.test.ts'],
        },
      },
      {
        esbuild: { jsx: 'automatic' },
        // Tests resolve tokens from source so they never depend on a prior build.
        resolve: {
          alias: {
            '@rookcom/monarch-tokens': fileURLToPath(
              new URL('./packages/tokens/src/index.ts', import.meta.url),
            ),
          },
        },
        test: {
          name: 'react',
          root: './packages/react',
          environment: 'jsdom',
          // Component tests live next to the component (src/**/<id>.test.tsx);
          // package-wide tests (public API snapshot) stay in test/.
          include: ['src/**/*.test.{ts,tsx}', 'test/**/*.test.{ts,tsx}'],
          setupFiles: ['./test/setup.ts'],
        },
      },
    ],
  },
});
