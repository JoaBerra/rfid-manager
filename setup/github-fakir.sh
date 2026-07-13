#!/usr/bin/env bash
# GitHub access on fakir — git (SSH) + gh CLI
set -euo pipefail

echo "=== GitHub access — fakir ==="
echo

echo "--- Git remotes (rfid-manager) ---"
git -C ~/Projects/rfid-manager remote -v 2>/dev/null || true
echo

echo "--- SSH till GitHub ---"
# ssh -T exits 1 even on success; avoid pipefail false negative
SSH_GH_MSG=$(ssh -o BatchMode=yes -o ConnectTimeout=10 -T git@github.com 2>&1 || true)
if echo "$SSH_GH_MSG" | grep -q 'successfully authenticated'; then
  echo "OK: git push/pull via SSH (nyckel ~/.ssh/id_ed25519)"
else
  echo "FEL: SSH till git@github.com fungerar inte."
  echo "Kontrollera ~/.ssh/config och att publik nyckel finns på GitHub:"
  echo "  cat ~/.ssh/id_ed25519.pub"
  echo "  https://github.com/settings/keys"
  exit 1
fi
echo

echo "--- GitHub CLI (gh) ---"
if gh auth status -h github.com 2>/dev/null; then
  echo "OK: gh är inloggad."
  gh api user --jq '.login' 2>/dev/null | xargs -I{} echo "Användare: {}"
  echo "git_protocol: $(gh config get git_protocol -h github.com 2>/dev/null || echo okänd)"
  exit 0
fi

echo "gh är INTE inloggad — krävs för releases, PR, m.m."
echo
echo "Kör EN av följande i din terminal (interaktivt):"
echo
echo "  A) Webbläsare (rekommenderat):"
echo "     gh auth login -h github.com -p ssh -s repo,read:org,gist,workflow --skip-ssh-key -w"
echo
echo "  B) Personal Access Token (classic, KeePass):"
echo "     Skapa: https://github.com/settings/tokens (scopes: repo, read:org, gist, workflow)"
echo "     read -s GH_TOKEN && echo \"\$GH_TOKEN\" | gh auth login --with-token && unset GH_TOKEN"
echo "     gh config set git_protocol ssh -h github.com"
echo
echo "Verifiera efteråt:"
echo "  gh auth status"
echo "  gh api user --jq .login"
echo "  gh config get git_protocol -h github.com   # ska visa ssh"
echo

exit 2