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

## Skrivläge i appen (`feature/nfc-write-button`)

Skrivning till tagg sker i ett uttryckligt **skrivläge** som styrs av ren Kotlin i `nfc/WriteMode.kt` (tillstånd `Idle → Editing → Armed → Finished`).

- **Före (på `feature/outbox-rounds`):** en läsning lade taggen i SCAN-listan. Tryck på kortet — eller på *Spara läsning*, som också markerade taggen — fällde ut skrivformuläret (adress + data + SPARA). Markeringen kunde inte tas bort igen, så man kom inte förbi formuläret. SPARA armerade `pendingWrite` i `MainActivity`; nästa detektion av samma UID utförde skrivningen. Ingen avbrytsfunktion, ingen timeout.
- **Nu:** läsning visar/sparar bara läsningen. Knappen **Skriv till tagg** (aktiv bara för senast lästa tagg, läst för under 2 min sedan, och skrivbar enligt lock-bytes på sida 2; Classic/utan lock-info räknas som skrivbar) startar formuläret. **SKRIV** armerar skrivningen i 30 s. Avbryt, tillbaka-knappen, stoppad skanning, appen i bakgrunden, en annan lästa tagg eller timeout stänger skrivläget. Efter skrivning visas bekräftelse på kortet.
- **Status:** enhetstestat (`WriteModeTest`, 29 tester), bygger. **Ej provat på telefon** — bara att appen startar och inte kraschar är kontrollerat.

Se [[Användarmanual]] (avsnittet *Skriva till tagg*) och [[Kanban]].
