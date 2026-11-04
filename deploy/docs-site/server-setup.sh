#!/usr/bin/env bash
# One-time (and re-runnable) server setup for monarchui.dev. Run as root on srv2073313:
#   bash server-setup.sh /path/to/deploy_key.pub
#
# Creates a non-root, key-only deploy user that can do exactly one thing (write files into the
# web root), then installs the Caddy sites: monarchui.dev serves the docs, and www.monarchui.dev
# plus monarch.rookhq.com redirect to it. Takes the PUBLIC half of the deploy key; the private
# half lives only in the GitHub environment secret DEPLOY_SSH_KEY.
set -euo pipefail

DEPLOY_USER=monarchui-deploy
WEB_ROOT=/opt/rook/static/monarchui
AUTH_FILE=/etc/ssh/authorized_keys.monarchui-deploy
SSHD_DROPIN=/etc/ssh/sshd_config.d/10-monarchui-deploy.conf
CADDYFILE=/etc/caddy/Caddyfile
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PUBKEY_FILE="${1:-}"

[ "$(id -u)" -eq 0 ] || { echo "Run as root." >&2; exit 1; }
[ -n "$PUBKEY_FILE" ] && [ -s "$PUBKEY_FILE" ] || { echo "Usage: $0 /path/to/deploy_key.pub" >&2; exit 1; }
grep -q "PRIVATE" "$PUBKEY_FILE" && { echo "That is a PRIVATE key. Pass the .pub file." >&2; exit 1; }
[ "$(wc -l < "$PUBKEY_FILE")" -le 1 ] && ssh-keygen -l -f "$PUBKEY_FILE" 2>/dev/null | grep -q ED25519 \
  || { echo "$PUBKEY_FILE is not a single ed25519 public key." >&2; exit 1; }
PUBKEY="$(tr -d '\r\n' < "$PUBKEY_FILE")"

echo "1/4 Deploy user"
if ! id -u "$DEPLOY_USER" >/dev/null 2>&1; then
  useradd --system --user-group --no-create-home --home-dir /nonexistent --shell /bin/sh "$DEPLOY_USER"
fi
usermod --password '*' "$DEPLOY_USER"   # no password login; key login still allowed

echo "2/4 Web root"
install -d -o "$DEPLOY_USER" -g "$DEPLOY_USER" -m 755 "$WEB_ROOT"

echo "3/4 SSH: the key can only run rrsync, write-only, inside $WEB_ROOT"
printf 'restrict,command="/usr/bin/rrsync -wo %s" %s\n' "$WEB_ROOT" "$PUBKEY" > "$AUTH_FILE"
chown root:root "$AUTH_FILE"; chmod 644 "$AUTH_FILE"
cat > "$SSHD_DROPIN" <<CONF
Match User $DEPLOY_USER
    AuthorizedKeysFile $AUTH_FILE
    AuthenticationMethods publickey
    PasswordAuthentication no
    AllowTcpForwarding no
    AllowAgentForwarding no
    X11Forwarding no
    PermitTTY no
    PermitTunnel no
    GatewayPorts no
    PermitUserRC no
CONF
chmod 644 "$SSHD_DROPIN"
if sshd -t; then systemctl reload ssh; else rm -f "$SSHD_DROPIN"; echo "sshd test failed; drop-in removed." >&2; exit 1; fi

echo "4/4 Caddy sites"
BACKUP="$CADDYFILE.bak-monarchui-$(date +%Y%m%d%H%M%S)"
cp -p "$CADDYFILE" "$BACKUP"
python3 - "$CADDYFILE" "$HERE/Caddyfile.snippet" <<'PY'
import re, sys
cfg, snip = sys.argv[1], sys.argv[2]
lines = open(cfg).read().split('\n')
heads = re.compile(r'^(monarch\.rookhq\.com|monarchui\.dev|www\.monarchui\.dev, monarch\.rookhq\.com) \{$')
out, i = [], 0
while i < len(lines):
    if heads.match(lines[i]):
        while out and out[-1].startswith('# Monarch'): out.pop()   # the block's own comment
        while lines[i] != '}': i += 1
        i += 1
        continue
    out.append(lines[i]); i += 1
text = '\n'.join(out).rstrip('\n') + '\n\n' + open(snip).read()
open(cfg, 'w').write(text)
PY
if ! caddy validate --config "$CADDYFILE" --adapter caddyfile >/dev/null 2>&1; then
  cp -p "$BACKUP" "$CADDYFILE"; echo "Caddy config invalid; restored backup. Nothing changed." >&2; exit 1
fi
echo "  Caddy runs with the admin API off, so applying this needs a full restart."
echo "  Every Rook site behind Caddy drops for a few seconds."
read -r -p "  Restart Caddy now? [y/N] " ans
if [ "${ans:-n}" = y ] || [ "${ans:-n}" = Y ]; then
  systemctl restart caddy; sleep 2
  if ! systemctl is-active --quiet caddy; then
    cp -p "$BACKUP" "$CADDYFILE"; systemctl restart caddy
    echo "Caddy failed to start; restored previous config." >&2; exit 1
  fi
  echo "  Caddy restarted."
else
  echo "  Skipped. Config is saved; run 'systemctl restart caddy' when ready."
fi
echo "Done."
