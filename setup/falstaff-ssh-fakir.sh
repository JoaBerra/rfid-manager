#!/usr/bin/env bash
# SSH till falstaff från fakir (engångs-setup + diagnostik)
set -euo pipefail

echo "=== falstaff SSH — fakir ==="
echo

if ssh -o BatchMode=yes -o ConnectTimeout=10 falstaff 'hostname' 2>/dev/null; then
  echo "OK: ssh falstaff (nyckel ~/.ssh/id_ed25519)"
  exit 0
fi

echo "SSH till falstaff fungerar inte än."
echo
echo "Förutsättning på falstaff (konsol):"
echo "  sudo apt install -y openssh-server && sudo systemctl enable --now ssh"
echo
echo "Från fakir (ange falstaff-lösenord en gång):"
echo "  ssh-copy-id -i ~/.ssh/id_ed25519.pub joakim@192.168.50.107"
echo
echo "Lägg till i ~/.ssh/config:"
echo "  Host falstaff"
echo "    HostName 192.168.50.107"
echo "    User joakim"
echo "    IdentityFile ~/.ssh/id_ed25519"
echo "    IdentitiesOnly yes"
echo
exit 1