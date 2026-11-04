import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
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
