# Användarmanual — RFID Manager

> **Fas:** 5.1  
> **Senast uppdaterad:** 2026-06-13

---

## Skriva till tagg (Write)

När en tagg har detekterats och visas i SCAN-listan kan du skriva data till den.

### Steg-för-steg

1. **Scanna en tagg** — tryck STARTA SKANNING och håll taggen mot telefonen
2. **Välj taggen** — tryck på taggens kort i listan (det markeras med highlight)
3. **Write-formuläret visas** — under taggens kort visas nu:

   | Fält | Beskrivning |
   |---|---|
   | **Mål page/block** | Ange adress (page för Ultralight, block för Classic) |
   | **Data (hex)** | Ange hex-data (t.ex. `48656C6C6F` = "Hello") |
   | **SPARA** | Förbered skrivningen |

### Adress och låsstatus

- Adressfältets **kantfärg** visar status:
  - 🟢 **Grön** — adressen är skrivbar
  - 🔴 **Röd** — adressen är låst (skrivning kommer misslyckas)
- En **supporting-text** under fältet bekräftar: "✓ Sidan är skrivbar" eller "🔒 Sidan är låst"

### Minneskarta

Klicka på **▼ Minne** för att expandera en karta över blocken:

| Indikator | Betydelse |
|---|---|
| 🟢 Grön prick + "skrivbar" | Blockets pages går att skriva till |
| 🔴 Röd prick + "låst" | Blockets pages är skrivskyddade (lock bytes) |

Klicka på en rad i minneskartan för att fylla i adressen automatiskt.

### Genomföra skrivning

1. Fyll i **mål page/block** (t.ex. `4`)
2. Fyll i **data i hex** (t.ex. `48656C6C6F`)
3. Tryck **SPARA** — du får toast "Redo att spara – håll taggen mot telefonen"
4. **Håll taggen mot telefonen igen** — skrivningen exekveras
5. Bekräftelse: toast "Write to page X succeeded!"
6. Taggen i listan uppdateras med ny data

> **OBS:** Endast hexadecimala värden (0–9, A–F) accepteras i datafältet. Vanlig text som "test" ger toast "Invalid hex data".

### Pages 0–3 (System)

Dessa pages innehåller UID, lock bytes och OTP — de är **alltid låsta** och går inte att skriva till via användargränssnittet.
