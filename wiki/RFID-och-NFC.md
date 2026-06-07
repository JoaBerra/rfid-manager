---
title: RFID-och-NFC
tags: [rfid, nfc, hf, uhf, hårdvara]
created: 2026-05-26
---

# RFID och NFC

## Frekvensband och Android-stöd

### HF RFID (13.56 MHz) — Rekommenderat för Android
- Nästan alla moderna Android-telefoner har inbyggt NFC-chip som stödjer detta band.
- Detta är den frekvens som används för de flesta "smarta kort" och **eskortminnen** inom industrin.
- Fullt stöd via Androids inbyggda NFC API.

### UHF RFID (860–960 MHz)
- Ger längre räckvidd (långdistans).
- **Stöds inte** inbyggt i Android-telefoner eller surfplattor.
- Lösning: Extern RFID-läsare ansluten via Bluetooth eller USB.
- Kräver oftast tillverkarens specifika (men oftast kostnadsfria) Android SDK.

## Android NFC API

Google tillhandahåller ett komplett, kostnadsfritt API för NFC.

Viktiga klasser och gränssnitt:

- `NfcAdapter` — Central adapter för att aktivera/avaktivera NFC och registrera listeners.
- `Tag` — Representerar en fysisk tagg som upptäckts.
- Teknikspecifika klasser:
  - `MifareClassic` — Vanligt för många eskortminnen (sektorbaserat minne).
  - `NfcA` — Lågnivååtkomst (ISO 14443-3A).
  - `IsoDep` — ISO 14443-4 / ISO 7816 (smart card-liknande kommunikation).

Dessa klasser gör det möjligt att:
- Upptäcka taggar i närheten.
- Autentisera mot specifika sektorer.
- Läsa och skriva data till exakta minnesblock.

## Viktiga Överväganden

- **Hårdvarukompatibilitet** är avgörande: Testa alltid med de exakta eskortminnen som ska användas i produktion.
- NFC kräver närhet (några centimeter).
- Bakgrundslyssning + foreground dispatch är nödvändigt för bra UX.
- Permissions: `NFC` + ofta `VIBRATE` för feedback.

Se [[Eskortminne]], [[Android-NFC-API]] och [[Hårdvarukrav-och-Enheter]].