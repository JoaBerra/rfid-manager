# Felrapportering och dokumentation av buggar

Detta dokument beskriver hur vi hanterar felrapporter (bug reports) i projektet så att de blir konsekventa, sökbara och lätta att dela (t.ex. för second opinion hos andra AI:er som Gemini).

## Struktur

- Alla formella felrapporter placeras i mappen `wiki/bugs/`.
- Varje rapport får ett eget filnamn med datum + kort beskrivning, t.ex.:
  - `2026-06-07-mqtt-socket-epem-samsung-note10.md`
- Filen är alltid **Markdown** (källan till sanningen).
- En PDF-version kan genereras vid behov för delning (t.ex. till externa parter eller andra AI:er).

## Mall / Struktur för en felrapport

Varje rapport **börjar** med YAML-frontmatter:

```yaml
---
title: "Kort, beskrivande titel på engelska"
date: YYYY-MM-DD
status: Open | In Progress | Resolved | Wontfix
severity: Low | Medium | High | Critical
reporter: "Namn / via Grok-assisted UAT"
id: "YYYY-MM-DD-KORT-BESKRIVNING"
affected-component: "vilken fil/komponent som berörs"
---
```

Därefter följer en **svensk sammanfattning** (för lättläslighet internt) och sedan den fulla engelska rapporten (för delning till Gemini/Google m.fl.).

Se exempel i `2026-06-07-mqtt-socket-epem-samsung-note10.md`.

## Arbetsflöde

1. **Identifiera problemet** under testning eller utveckling.
2. **Skapa rapport** i `wiki/bugs/` med ovan YAML-frontmatter + svensk sammanfattning.
3. **Dokumentera**:
   - Miljö (enhet, OS, build, Android Studio-version)
   - Exakta reproduktionssteg
   - Förväntat vs. faktiskt beteende
   - Loggar (Logcat, terminal, etc.)
   - Vad som testats (inställningar, workaround, etc.)
   - Hypoteser
   - Referenser till kod, wiki-sidor, commits
4. **Uppdatera huvudloggen**: Lägg en kort notis i `wiki/log.md` med länk till rapporten.
5. **Generera PDF** (vid behov för delning):
   ```bash
   md-to-pdf wiki/bugs/2026-06-07-xxx.md
   ```
   (Se `~/.local/bin/md-to-pdf` och `wiki/Verktyg-och-Setup.md` för installation och användning.)
6. **Uppdatera status**: När buggen är löst – ändra `status: Resolved` i YAML-frontmatter, lägg till upplösningssektion med bevis (nya loggar, fix commits, etc.).
7. **Städa**: Ta bort onödiga debug-loggningar i kod när problemet är löst. Uppdatera eventuella relaterade wiki-sidor.

## Verktyg

- **md-to-pdf** wrapper (installerad i `~/.local/bin/`)
- Pandoc + WeasyPrint (se `wiki/Verktyg-och-Setup.md` för installation)
- Alltid Markdown som källa – PDF är en genererad artefakt

## Exempel på bra rapporter

- `wiki/bugs/2026-06-07-mqtt-socket-epem-samsung-note10.md` (EPERM på MQTT-socket på Samsung)

## Tips

- Håll rapporten faktabaserad och teknisk – den ska kunna läsas av en annan AI eller utvecklare utan att behöva ställa följdfrågor.
- Använd konsekvent terminologi från vår [[Nomenclature-Figma-Android]].
- Länka alltid till relevant kod (fil:rad) och wiki-sidor.
- När du skickar till Gemini eller annan AI: bifoga gärna både MD och genererad PDF.

Detta arbetssätt säkerställer att vi har spårbarhet, återanvändbar kunskap och kan återkomma till gamla problem effektivt.
