# How to push Fas 4 release to GitHub

The release is prepared in this directory. Push from your local terminal.

```bash
cd /home/joakim/projects/rfid/rfid-manager/releases/2026-06-Fas4

# Initialize git
git init
git checkout -b master
git add .
git commit -m "feat(release): Fas 4 sign-off — i18n, dark mode, MQTT, export, haptik, sök, paginering"

# Add remote (or use existing)
git remote add origin git@github.com:JoaBerra/rfid-manager.git

# Tag
git tag -a fas-4-sign-off-2026-06-11 -m "Fas 4 UAT godkänd av Kund 2026-06-11 — alla 8 punkter klara"

# Push
git push -u origin master --force
git push --tags
```

## GitHub Release

Skapa en release från taggen `fas-4-sign-off-2026-06-11`:
https://github.com/JoaBerra/rfid-manager/releases/new

Lägg till release notes (se README.md för vad som ingår).

## Tarball (alternativ)

```bash
cd /home/joakim/projects/rfid/rfid-manager/releases
tar -czf ~/RFIDManager-Fas4-Sign-off-2026-06-11.tar.gz 2026-06-Fas4/
```

Ladda upp tarballen som asset på GitHub Release.

---

## Historik i GitHub-repot

- `fas-4-sign-off-2026-06-11` ← denna release
- `fas-3-sign-off-2026-06-10`
- `fas-2-uat-godkand-2026-06-07`
- Föregående releaser
