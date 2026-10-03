# Användarmanual — RFID Manager

> **Fas:** 5.1  
> **Senast uppdaterad:** 2026-06-13

---

## Skriva till tagg (Write)

När en tagg har detekterats och visas i SCAN-listan kan du skriva data till den.

### Steg-för-steg

> **Ändrat på `feature/nfc-write-button` (2026-10-03, enhetstestat, ej provat på telefon):** en läsning visar och sparar bara läsningen. Skrivläget startar först när du trycker **Skriv till tagg**.

1. **Scanna en tagg** — tryck STARTA SKANNING och håll taggen mot telefonen. Taggen visas i listan; du kan **Spara** läsningen som vanligt.
2. **Tryck Skriv till tagg** på taggens kort. Knappen är aktiv bara för den senast lästa taggen (läst för under 2 minuter sedan) och om den går att skriva till (okänd typ eller helt låst Ultralight/NTAG ger röd text och inaktiv knapp).
3. **Skrivformuläret visas** under kortet:

   | Fält | Beskrivning |
   |---|---|
   | **Mål page/block** | Ange adress (page för Ultralight, block för Classic) |
   | **Data (hex)** | Ange hex-data (t.ex. `48656C6C6F` = "Hello") |
   | **SKRIV** | Beställ skrivningen |
   | **Avbryt** | Stäng skrivläget utan att skriva |

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

1. Tryck **Skriv till tagg** på den nyss lästa taggen
2. Fyll i **mål page/block** (t.ex. `4`)
3. Fyll i **data i hex** (t.ex. `48656C6C6F`)
4. Tryck **SKRIV** — kortet visar "⚡ Skrivläge – håll taggen mot telefonen (NN s)" med nedräkning (30 s)
5. **Håll taggen mot telefonen igen** — skrivningen utförs
6. Bekräftelse på kortet: "✓ Skrivning klar (sida/block X)" (grönt) eller "✗ Skrivningen misslyckades …" (rött); tryck OK för att stänga
7. Taggen i listan uppdateras med ny data

**Avbryta:** tryck **Avbryt**, tryck **tillbaka**, stoppa skanningen, lägg appen i bakgrunden eller läs en annan tagg — skrivläget stängs utan att något skrivs. Efter 30 sekunder utan tagg avbryts det av sig självt ("Skrivläget avbröts (tidsgräns)").

> **OBS:** Endast hexadecimala värden (0–9, A–F) accepteras i datafältet. Vanlig text som "test" ger toast "Invalid hex data".

### Pages 0–3 (System)

Dessa pages innehåller UID, lock bytes och OTP — de är **alltid låsta** och går inte att skriva till via användargränssnittet.
