# ishtar — SSH-setup (Debian testlab-gäst)

**Uppdrag 003, D.0 beslut (Principal):**

| Fält | Värde |
|------|-------|
| Gästnamn (Proxmox) | ishtar |
| OS | Debian |
| IP | `192.168.50.151/24` (statisk) |
| Hypervisor | hulda Proxmox `192.168.50.100:8006` |
| Broker (efter D.2) | `192.168.50.151:1883` |

**Status 2026-07-12:** SSH från fakir (`ssh hulda`) — **klar**. Docker ej installerat än (kräver sudo på ishtar).

---

## Steg 1 — Öppna konsol på ishtar

1. Gå till `http://192.168.50.100:8006/`
2. Välj gästen **ishtar** (på)
3. Klicka **Console** (noVNC eller xterm.js)
4. Logga in lokalt (root eller befintlig användare)

---

## Steg 2 — Installera och starta SSH (på ishtar)

Kör i konsolen:

```bash
sudo apt update
sudo apt install -y openssh-server
sudo systemctl enable --now ssh
sudo systemctl status ssh --no-pager
```

Verifiera att SSH lyssnar:

```bash
ss -tlnp | grep ':22'
```

Om `ufw` är aktivt:

```bash
sudo ufw allow OpenSSH
sudo ufw status
```

---

## Steg 3 — Användare för fakir (om du inte redan har en)

Rekommendation: samma användarnamn som på fakir (`joakim`).

```bash
sudo adduser joakim
sudo usermod -aG sudo joakim
```

*(Hoppa över om du redan har en användare du vill använda.)*

---

## Steg 4 — Nyckel från fakir (välj A eller B)

### A) ssh-copy-id från fakir (enklast om lösenord fungerar)

På **fakir**:

```bash
ssh-copy-id -i ~/.ssh/id_ed25519_hulda.pub joakim@192.168.50.151
```

### B) Manuell nyckel i konsol på ishtar

På **ishtar** (som `joakim`, eller root som skapar för joakim):

```bash
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo 'ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIHYWMe5KubRM+IT/SeYk7M5I5Cf+T6raEQq5ur7jpQgH fakir-hulda' >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

---

## Steg 5 — SSH-config på fakir

På **fakir**, uppdatera `~/.ssh/config`:

```
Host hulda
  HostName 192.168.50.151
  User joakim
  IdentityFile ~/.ssh/id_ed25519_hulda
  IdentitiesOnly yes

Host ishtar
  HostName 192.168.50.151
  User joakim
  IdentityFile ~/.ssh/id_ed25519_hulda
  IdentitiesOnly yes
```

`Host hulda` behålls som alias för testlab-gästen (MQTT-kommandon i wiki).

Test:

```bash
ssh hulda hostname
ssh hulda true && echo "SSH OK"
```

Förväntat svar: `ishtar` (eller gästens hostname).

---

## Steg 6 — Docker + MQTT (på ishtar, kräver sudo)

`ssh hulda` fungerar utan lösenord. Docker kräver **sudo-lösenord** — kör interaktivt:

```bash
ssh hulda
bash ~/Projects/rfid-manager/test/fas2-mqtt/../../setup/ishtar-install-docker.sh
```

Eller manuellt på ishtar:

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker joakim
exit
ssh hulda
newgrp docker
cd ~/Projects/rfid-manager/test/fas2-mqtt
docker compose -f docker-compose.hulda.yml up -d
```

*(Repo-filer kan synkas från fakir med `scp` om `git` saknas.)*

Test från fakir:

```bash
docker run --rm eclipse-mosquitto:2 mosquitto_pub -h 192.168.50.151 -p 1883 -t test/uppdrag003 -m ok
```

---

## Felsökning

| Symptom | Åtgärd |
|---------|--------|
| Ping OK, port 22 stängd | Kör Steg 2 (`openssh-server`) |
| `Permission denied (publickey)` | Steg 4 — nyckel saknas i `authorized_keys` |
| `Connection refused` efter install | `sudo systemctl restart ssh` |
| Fel användare | Byt `User` i `~/.ssh/config` |
| Docker kräver sudo | `usermod -aG docker joakim` + logga in igen |