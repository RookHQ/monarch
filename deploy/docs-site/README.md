# Deploying monarchui.dev

Static site, no Docker, no Node process in production.

```
push to main (docs paths)
  -> GitHub-hosted runner: npm ci --ignore-scripts, build, budget, link and CSP checks
  -> "production" environment (main branch only) holds the deploy key
  -> rsync over SSH as monarchui-deploy (non-root)  ->  /opt/rook/static/monarchui
  -> Caddy file_server on monarchui.dev (Cloudflare in front)
  -> www.monarchui.dev and monarch.rookhq.com redirect (301) to https://monarchui.dev
```

## Why it is safe

- The build runs on a throwaway runner and installs dependencies with `--ignore-scripts`. The server only ever receives finished files.
- `monarchui-deploy` is a dedicated system user: no password, no TTY, no forwarding. Its only key is locked to `rrsync -wo /opt/rook/static/monarchui` (write-only, inside that directory). It cannot read files, run a shell or touch anything else.
- The private key is the environment secret `DEPLOY_SSH_KEY` on the `production` environment, which only `main` can deploy to. Pull requests and forks never see it. The server's host key is pinned in `known_hosts` and checked strictly.
- Caddy serves the folder read-only with HSTS, a strict Content-Security-Policy (inline scripts allowed by hash only), `frame-ancestors 'none'` and a locked-down Permissions-Policy. `npm run docs:csp` fails the build if the pages need a script hash the policy does not list.
- Actions are pinned to commit SHAs; the workflow has `contents: read` only.

## If `docs:csp` fails

A dependency upgrade changed an inline script. Run `node scripts/docs/check-csp.mjs --print`, update the hashes in `Caddyfile.snippet`, merge, then re-run `server-setup.sh` on the server (it replaces the sites and needs one Caddy restart) before the deploy.

## Rotating the deploy key

Generate a new ed25519 key on your own machine, set it with `gh secret set DEPLOY_SSH_KEY --env production < key`, copy only the `.pub` to the server, run `server-setup.sh <pub>` again (it replaces the old key), then delete the private file.
