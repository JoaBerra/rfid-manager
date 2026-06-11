---
title: Android-NFC-API
tags: [api, nfc, android, kotlin, kod]
created: 2026-05-26
---

# Android NFC API

## Kärnkomponenter

### NfcAdapter
- Singleton som ger åtkomst till enhetens NFC-hårdvara.
- Metoder för att:
  - Kontrollera om NFC är aktiverat.
  - Registrera `PendingIntent` för tag-discover (foreground dispatch).
  - Aktivera reader mode för bättre prestanda i vissa fall.

### Tag
- Representerar en upptäckt NFC-tagg.
- Ger åtkomst till vilka teknologier taggar stödjer (`getTechList()`).
- Används för att skapa specifika teknikobjekt (`MifareClassic.get(tag)` etc.).

### Teknikklasser

**MifareClassic**
- Mest relevant för många eskortminnen.
- Metoder: `authenticateSectorWithKeyA()`, `readBlock()`, `writeBlock()`, `increment()`, `decrement()` etc.
- Kräver korrekt nyckel för varje sektor.

**NfcA**
- Lågnivåkommunikation (raw transceive).
- Används när högre protokoll inte räcker.

**IsoDep**
- Stöd för ISO 14443-4 och kontaktlösa smart cards.
- `transceive()` för APDU-kommandon.

## Typisk Flöde för Eskortminne

1. Aktivera foreground dispatch eller reader mode.
2. Ta emot `Tag` via Intent.
3. Kontrollera vilka teknologier som finns.
4. Skapa rätt teknikobjekt.
5. Autentisera sektor(er).
6. Läs/skriv block.
7. Stäng anslutning.

## Viktiga Permissions och Manifest

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

## Bästa Praxis (initiala rekommendationer)

- Använd `enableReaderMode()` för moderna implementationer (bättre batteri och prestanda).
- Hantera `TagLostException` elegant.
- Undvik att hålla lås på UI-tråden under I/O.
- Logga alltid `Tag` ID + tech list för debug.

Se [[Kotlin-Android-NFC]] för konkreta kodexempel (kommer att byggas upp).

Relaterat: [[Eskortminne]], [[RFID-och-NFC]].