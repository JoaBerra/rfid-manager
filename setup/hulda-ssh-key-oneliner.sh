#!/bin/bash
# Kör DETTA på hulda (konsol), inte på fakir.
# Lägger till fakir-nyckeln så "ssh hulda" fungerar från dev-maskinen.

mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo 'ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIHYWMe5KubRM+IT/SeYk7M5I5Cf+T6raEQq5ur7jpQgH fakir-hulda' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
echo "OK — nyckel tillagd. Testa från fakir: ssh hulda hostname"