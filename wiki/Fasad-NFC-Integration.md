---
title: Fasad NFC-integration — Gränssnittskontrakt (fasad://besiktning & WRITE_TAG)
tags: [fasad, nfc, rfid, deeplink, intent, writemode, taglocks, arkitektur]
created: 2026-10-27
---

# Fasad NFC-integration — Gränssnittskontrakt

> **Status:** Gränssnittskontrakt beslutat av Joakim 2026-10-27 i samråd mellan `rfid-manager` och @Kalle PL Fasad (`fasad.besiktning`). Implementation ligger som kort i [[Kanban]] (ej påbörjad). Termer: [[Ordlista]].

## Syfte och arkitekturprincip

Kopplingen mellan `rfid-manager` och Fasads besiktningsklient (`fasad.besiktning` samt `fasad.besiktning.labb`) bygger på **Alternativ A: ett löst kopplat Android Deep Link / Intent-gränssnitt med noll kodberoende mellan apparna**.

Designen följer Fasads arkitekturbeslut:
- **ADR 0010 & ADR 0012 (inget eget fastighetsregister i fältappen):** Fasad-klienten är en ren fältklient med ett lokalt fack (`besiktning.sqlite` i `Lager.kt`) för aktiva besiktningsuppdrag och ska inte hålla ett eget bestånds- eller taggregister. `rfid-manager` äger taggskrivning och låskontroll (`WriteMode.kt`, `TagLocks.kt`).
- **ADR 0014 (all styrning via kommandon på `Karna`):** Inkommande `fasad://besiktning`-anrop i Fasads `MainActivity` (`onCreate` / `onNewIntent`) översätts till ett enda kärnkommando `karna.oppnaFranTagg(lghNr, rum, del, utrustning)`.
- **ADR 0015 (`labb`-varianten):** Experiment och verktygsfunktioner som att initiera taggbindning från fältklienten kan ligga i `fasad.besiktning.labb` utan att tynga den sålda appen (`fasad.besiktning`).

## Begrepp (Konnotation | Denotation)

| Begrepp | Konnotation | Denotation |
|---------|-------------|------------|
| **`fasad://besiktning`** | Läs-URI (Android Deep Link) som öppnar rätt besiktningsuppdrag, rum och byggnadsdel/utrustning i Fasads besiktningsklient samt skickar med `uid` som närvarokvittens. Kan ligga som NDEF-URI direkt på en skrivbar NFC-tagg eller skickas som Intent från `rfid-manager`. | Inte ett REST/HTTP-anrop, inte ett MQTT-topic och inte en intern vy i `rfid-manager`. |
| **`fasad.rfid.WRITE_TAG`** | Explicit Android-Intent från Fasad till `rfid-manager` för att knyta en skrivbar NFC-tagg till ett angivet mål (`lgh`, `rum`, `del`, `utrustning`) via det skyddade skrivläget (`WriteMode`). | Inte en tyst bakgrundsskrivning utan användarbekräftelse och inte en läsoperation. |
| **`tagUid`** | Taggens fabriksbrända, oföränderliga serienummer i versalt hexformat (exempelvis `04A1B2C3D4E580`), läst vid fysisk kontakt med taggen. | Inte ett affärsobjekt eller lägenhetsnummer i Fasad och inte skrivbart användarminne; byts den fysiska taggen ut får den nya taggen ett nytt `tagUid`. |
| **`Taggbindning`** | Kopplingen mellan en fysisk NFC-tagg och ett mål i Fasad (`lgh`, `rum`, `del`/`utrustning`), lagrad antingen som självbeskrivande NDEF-URI (`fasad://besiktning?...`) på en skrivbar tagg (Läge 1) eller som en post i uppdragsbrevets `taggar`-lista för en skrivskyddad tagg med enbart `tagUid` (Läge 2). | Inte en permanent tabell eller ett fastighetsregister i telefonens SQLite-databas (förbjudet enligt Fasads ADR 0012). |

---

## 1. Läsflöde i fält (`fasad://besiktning`)

När en tagg skannas i fält (antingen direkt av Android via NDEF-URI eller via `rfid-manager`) skickas en `ACTION_VIEW`-Intent med URI till Fasad-klienten:

```text
fasad://besiktning?lgh=101&rum=badrum%201&del=kran&uid=04A1B2C3D4E580
```

### Parametrar

| Parameter | Obligatorisk | Källa i Fasad (`terms/`) | Beteende i `fasad.besiktning` (`karna.oppnaFranTagg`) |
|-----------|--------------|--------------------------|-------------------------------------------------------|
| `lgh` | Ja (vid lägenhetsbesiktning, `1–16` tecken) | `lgh_nr` i `behandla-besiktningsuppdrag.schema.json` (t.ex. `"101"`, `"1201"`) | Öppnar rätt ogjort uppdrag (`rad.lgh == lghNr`) och går till `Yta.Promenad`. |
| `rum` | Valfri | `namn` (`1–40` tecken, t.ex. `"badrum 1"`) eller `rumstyp` ur `terms/kodlistor/rumstyp.json` (`kok`, `kokvra`, `badrum`, `toalett`, `hall`, `kapprum`, `vardagsrum`, `sovrum`) | Stegar direkt till rätt rum via `gaTillRum(karna, index)` (autosparar pågående anteckningar först via `sparaInnanRumsbyte()`). |
| `del` | Valfri | `DEL_KODER` / `terms/kodlistor/del.json` (20 st, t.ex. `kran`, `element`, `vagguttag`, `fonsterkarm`, `golv`, `vagg`, `tak`, `dorr`) | Fäller ut byggnadsdelen via `karna.oppnaSchemaDel(delId)`. Tas ett foto medan delen är öppen märks bilden automatiskt med `rum` + `del`. |
| `utrustning` | Valfri (alternativ till `del`) | `UTRUSTNING_KODER` / `terms/kodlistor/utrustning.json` (23 st, t.ex. `spis`, `kylskap`, `tvattstall`, `wc_stol`, `dusch`, `diskmaskin`) | Fäller ut utrustningen via `karna.oppnaUtrustning(utrustningId)`. |
| `uid` | Valfri (sätts alltid vid skanning via `rfid-manager`) | `tagUid` i hex (t.ex. `04A1B2C3D4E580`) | Medföljer som närvarokvittens (bevis på fysisk avläsning på plats) och för uppslag av skrivskyddade taggar (Läge 2). |

---

## 2. Skrivflöde / Taggbindning (`fasad.rfid.WRITE_TAG`, Läge 1)

För skrivbara Ultralight/NTAG-taggar knyts taggen i fält utan att något förhandsregister behövs:

1. **Anrop från Fasad:** Användaren står inne på ett rum/del i Fasad (t.ex. i `fasad.besiktning.labb`) och väljer *Knyt NFC-tagg*. Fasad skickar en explicit Intent med `startActivityForResult` / Activity Result API:
   - **Action:** `fasad.rfid.WRITE_TAG`
   - **Extras:**
     - `lgh` (`String`, obligatorisk)
     - `rum` (`String`, valfri)
     - `del` (`String`, valfri)
     - `utrustning` (`String`, valfri)
2. **Skyddad skrivning i `rfid-manager`:**
   - `rfid-manager` öppnar sitt skyddade skrivläge (`WriteMode`: `Idle -> Editing -> Armed -> Finished` med 30 sekunders tidsfönster och avbrytsknapp).
   - Vid kontakt med taggen kontrolleras låsbitarna via `TagLocks.kt` så att taggen är skrivbar.
   - `rfid-manager` skriver NDEF-URI `fasad://besiktning?lgh=...&rum=...&del=...` (eller `utrustning=...`) på taggen.
3. **Kvittens tillbaka till Fasad:**
   - Vid lyckad skrivning returnerar `rfid-manager` `Activity.RESULT_OK` med extra `uid` (`tagUid` i hex) till Fasad.
   - Vid avbrott eller timeout returneras `Activity.RESULT_CANCELED`.

---

## 3. Skrivskyddade taggar (Läge 2 — endast `tagUid`)

När en befintlig eller skrivskyddad tagg bara har sitt fabriks-UID (`tagUid`) och ingen NDEF-URI i användarminnet:

1. `rfid-manager` läser `tagUid` och kan skicka vidare:
   ```text
   fasad://besiktning?uid=04A1B2C3D4E580
   ```
2. Fasad slår upp `uid` i det nedladdade uppdragsbrevets valfria lista `taggar` i `behandla-besiktningsuppdrag.schema.json`:
   ```json
   "taggar": [
     { "uid": "04A1B2C3D4E580", "rum": "badrum 1", "del": "kran" }
   ]
   ```
   På så sätt fungerar även skrivskyddade taggar helt utan eget beståndsregister i fältappen (i enlighet med ADR 0012).

---

## 4. Ansvarsgräns (Mål / Inte)

| Projekt | Mål (Konnotation) | Inte (Denotation) |
|---------|-------------------|-------------------|
| **`rfid-manager`** | Ta emot `fasad.rfid.WRITE_TAG`, bygga och skriva/läsa NDEF-URI `fasad://besiktning?...` på Ultralight/NTAG med `WriteMode` och `TagLocks`, samt vidarebefordra avlästa Fasad-taggar (inklusive `uid`) till `fasad.besiktning` / `fasad.besiktning.labb`. | Ingen kännedom om Fasads rumslistor (`rumstyp.json`, `del.json`, `utrustning.json`), protokollschema eller SQLite-databas (`besiktning.sqlite`). |
| **`fasad` (`android/besiktning/`)** | Ta emot `fasad://besiktning?...` i `MainActivity` (`onCreate` / `onNewIntent`) och anropa `karna.oppnaFranTagg(lghNr, rum, del, utrustning)` med enhetstester, samt (i `labb`) skicka `fasad.rfid.WRITE_TAG`. | Ingen lågnivåkod för Mifare Ultralight/NTAG-sidor, NDEF-formatering eller låsbitar (`TagLocks`) i Fasad-appen. |

---

## 5. Status

- **2026-10-27:** Gränssnittskontrakt beslutat av Joakim (`t62u`). Dokumenterat i wikin och upplagt som avgränsat kort i [[Kanban]] (ej påbörjat i kod).
