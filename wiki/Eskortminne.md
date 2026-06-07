---
title: Eskortminne
tags: [eskortminne, rfid, mifare, industri]
created: 2026-05-26
---

# Eskortminne

## Vad är ett eskortminne?

**Eskortminne** är en typ av RFID-tagg / smart kort som används flitigt inom industri, logistik, säkerhet och underhåll. Det fungerar som ett bärbart, passivt dataminne som kan läsas och skrivas med NFC-läsare.

Ofta används det för att:
- Identifiera personal eller utrustning.
- Lagring av konfigurationsdata, underhållshistorik eller åtkomstuppgifter.
- "Eskortera" data mellan fysiska platser utan nätverksuppkoppling.

## Varför är läsning av eskortminne en vanlig uppgift?

- Många befintliga industriella system använder just denna typ av taggar.
- Data lagras ofta i strukturerade sektorer med autentisering.
- Kräver specifik kunskap om protokoll och minneslayout för att kunna läsa/skriva korrekt och säkert.

## Vanliga Protokoll

- **Mifare Classic** (1K/4K) — Mycket vanligt. Använder sektorer och block med nyckelbaserad autentisering (Key A / Key B).
- **NfcA / ISO 14443-3A** — Lågnivååtkomst.
- **IsoDep** — För mer avancerade smart card-liknande eskortminnen.

## Utmaningar vid Implementation

- Autentisering mot rätt sektor med korrekt nyckel.
- Hantering av olika minnesstorlekar och organisation.
- Felhantering när tagg flyttas bort under läsning/skrivning.
- Säkerhet: undvika att exponera nycklar i appen (om möjligt).

## I detta Projekt

Målet är att bygga Android-appar som på ett tillförlitligt och användarvänligt sätt kan:
- Upptäcka eskortminne.
- Autentisera och läsa specifika sektorer.
- Tolka och presentera datat på ett meningsfullt sätt.
- Eventuellt skriva tillbaka uppdaterad information.

Se [[RFID-och-NFC]], [[Android-NFC-API]] och [[Kotlin-Android-NFC]].