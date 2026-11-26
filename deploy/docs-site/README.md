# Deploying monarchui.dev

Static site, no Docker, no Node process in production.

```
push to main (docs paths)
  -> GitHub-hosted runner: npm ci --ignore-scripts, build, budget, link and CSP checks
  -> "production" environment (main branch only) holds the deploy key
  -> rsync over SSH as <DEPLOY_USER> (non-root) to <DEPLOY_HOST>  ->  <WEB_ROOT>
  -> Caddy file_server on monarchui.dev (Cloudflare in front)
  -> www.monarchui.dev and monarch.rookhq.com redirect (301) to https://monarchui.dev
```

## Required variables

This repository is public, so nothing that identifies the server is kept in it: not its host
name or address, not the deploy user, not the web root, not its host key. The files here use
placeholders, and the real values are set in the places below.

| Name                 | Where it is set                                                                             | What it is                                                                                                    |
| -------------------- | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `DEPLOY_HOST`        | GitHub Actions secret on the `production` environment                                       | Host name of the server the files are uploaded to. Shown as `<DEPLOY_HOST>` in these files.                   |
| `DEPLOY_USER`        | GitHub Actions secret on the `production` environment; shell variable for `server-setup.sh` | The non-root system user that receives the upload. Shown as `<DEPLOY_USER>`.                                  |
| `DEPLOY_SSH_KEY`     | GitHub Actions secret on the `production` environment                                       | Private half of the ed25519 deploy key. The public half is given to `server-setup.sh`.                        |
| `DEPLOY_KNOWN_HOSTS` | GitHub Actions secret on the `production` environment                                       | One `known_hosts` line that pins the server's SSH host key. [`known_hosts`](known_hosts) says how to make it. |
| `WEB_ROOT`           | Shell variable for `server-setup.sh`, on the server only                                    | Absolute path of the folder Caddy serves. Replaces `<WEB_ROOT>` in `Caddyfile.snippet`.                       |
| `DEPLOY_ENABLED`     | GitHub Actions repository variable                                                          | `true` turns the deploy job on. Anything else builds and checks only.                                         |
| `DOCS_GTM_ID`        | GitHub Actions repository variable (optional)                                               | Google Tag Manager container for the docs site.                                                               |
| `DOCS_CLARITY_ID`    | GitHub Actions repository variable (optional)                                               | Microsoft Clarity project for the docs site.                                                                  |

The workflow does not need `WEB_ROOT`: the deploy key is locked on the server to that one
folder, so the upload target is always `./`.

## Setting up a server

1. Make an ed25519 key pair on your own machine. Keep the private half out of the repository.
2. Copy this folder and the `.pub` file to the server, then run as root:

   ```sh
   DEPLOY_USER=<DEPLOY_USER> WEB_ROOT=<WEB_ROOT> bash server-setup.sh /path/to/deploy_key.pub
   ```

   The script stops with a usage message if either variable is missing.

3. Set the secrets and variables in the table above (`gh secret set NAME --env production`,
   `gh variable set NAME`).
4. Set `DEPLOY_ENABLED` to `true`.

## Why it is safe

- The build runs on a throwaway runner and installs dependencies with `--ignore-scripts`. The server only ever receives finished files.
- `<DEPLOY_USER>` is a dedicated system user: no password, no TTY, no forwarding. Its only key is locked to `rrsync -wo <WEB_ROOT>` (write-only, inside that directory). It cannot read files, run a shell or touch anything else.
- The private key is the environment secret `DEPLOY_SSH_KEY` on the `production` environment, which only `main` can deploy to. Pull requests and forks never see it. The server's host key is pinned in the environment secret `DEPLOY_KNOWN_HOSTS` and checked strictly; it never comes from the network during a deploy.
- Caddy serves the folder read-only with HSTS, a strict Content-Security-Policy (inline scripts allowed by hash only), `frame-ancestors 'none'` and a locked-down Permissions-Policy. `npm run docs:csp` fails the build if the pages need a script hash the policy does not list.
- Actions are pinned to commit SHAs; the workflow has `contents: read` only.

## If `docs:csp` fails

A dependency upgrade changed an inline script. Run `node scripts/docs/check-csp.mjs --print`, update the hashes in `Caddyfile.snippet`, merge, then re-run `server-setup.sh` on the server with the same variables (it replaces the sites and needs one Caddy restart) before the deploy.

## Rotating the deploy key

Generate a new ed25519 key on your own machine, set it with `gh secret set DEPLOY_SSH_KEY --env production < key`, copy only the `.pub` to the server, run `DEPLOY_USER=<DEPLOY_USER> WEB_ROOT=<WEB_ROOT> bash server-setup.sh <pub>` again (it replaces the old key), then delete the private file.
