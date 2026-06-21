# Hardware - Testenheter

## Samsung Galaxy Note 10 (SM-N970F/DS)

**Status:** Primär fysisk testenhet (juni 2026)

**Specifikationer:**
- Modell: SM-N970F/DS (internationell dual-SIM, Exynos 9825)
- One UI 4.1
- Android 12 (Google Play systemuppdatering: 1 februari 2026)
- NFC: Ja (fullt stöd)
- MIFARE Classic-kompatibilitet: Ja (listad i MifareClassicTool COMPATIBLE_DEVICES.md)

**Relevans för projektet:**
- Stödjer NFC på 13.56 MHz (HF) vilket krävs för vanliga eskortminnen (Mifare Classic 1K/4K etc.).
- Exynos-varianten använder Samsungs NFC-kontroller som stödjer MIFARE Classic (till skillnad från vissa Broadcom-baserade äldre modeller).

**Rekommenderad information att samla in:**
- Bootloader-status (låst/upplåst) - viktigt för eventuell custom ROM eller avancerad testning.
- Full build info: `adb shell getprop ro.build.fingerprint`
- Security patch level.
- Bekräfta NFC-inställning: Inställningar → Anslutningar → NFC och kontaktlösa betalningar.
- Testa med appar:
  - MIFARE Classic Tool (läsa/skriva sektorer)
  - NFC Tools (allmän NFC-test)
- USB debugging + auktoriserad PC för ADB.
- Eventuella kända problem (t.ex. NFC-antenn-flex på vissa Note 10-enheter).

**Utveckling:**
- Används för verklig tagg-läsning/skrivning (emulator har begränsat NFC-stöd).
- Kräver korrekt `NfcManager` implementation med `enableReaderMode` eller foreground dispatch.
- Måste hantera nycklar för MIFARE Classic (Key A/B) för eskortminnen.

**Länkar:**
- [[Projektöversikt]]
- [[NFC-Implementation-Plan]] (kommer)

## Ansluta via USB för utveckling (ADB)

**Viktigt innan anslutning:**

- På telefonen (Galaxy Note 10):
  - Inställningar > Om telefonen > Tryck 7 gånger på "Build number" för att låsa upp Utvecklaralternativ.
  - Inställningar > Utvecklaralternativ > Slå på **USB-felsökning**.
  - (Rekommenderat) Slå på "OEM unlocking" om du vill ha möjlighet till custom ROM senare.

- Anslut USB-A till USB-C kabeln.

- På telefonen: Dra ner notification shade, tryck på USB-notisen, välj **"File Transfer"** (MTP) eller "Transfer files".

- Telefonen visar en dialog: "Allow USB debugging?" med datorns RSA fingerprint.
  - **Viktigt:** Markera "Always allow from this computer" och tryck Allow.

**På datorn (Arch/Omarchy):**

Eftersom plugdev-grupp och udev-regler saknades vid kontroll (upptäckt 2026-06-01):

Kör det förberedda hjälpskriptet (rekommenderat):

```bash
bash ~/projects/rfid/rfid-manager/setup/fix-usb-adb.sh
```

(Det kör pacman usbutils, skapar plugdev-grupp + lägger till dig, kopierar 51-android.rules till /etc/udev/rules.d/ och gör reload. Scriptet ligger i `~/projects/rfid/rfid-manager/setup/`.)

Eller manuellt:

```bash
# Installera verktyg för USB-diagnostik
sudo pacman -S usbutils

# Skapa grupp och lägg till användare
sudo groupadd plugdev 2>/dev/null || true
sudo usermod -aG plugdev $USER

# Installera udev-regler för Samsung (och Google)
sudo mkdir -p /etc/udev/rules.d
sudo cp ~/projects/rfid/rfid-manager/setup/51-android.rules /etc/udev/rules.d/51-android.rules || sudo tee /etc/udev/rules.d/51-android.rules << 'RULES'
SUBSYSTEM=="usb", ATTR{idVendor}=="04e8", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"
RULES

# Ladda om regler
sudo udevadm control --reload-rules
sudo udevadm trigger

# Logga ut och in igen (eller reboot) för att group-medlemskap ska gälla
```

`MODE="0666"` gör att enheten blir läs/skrivbar för alla användare även innan grupp-medlemskapet är aktivt i sessionen.

**Verifiera anslutning:**

```bash
adb kill-server
adb start-server
adb devices
```

Du bör se enheten (t.ex. "SM-N970F device" eller "unauthorized" först - auktorisera på telefonen om så).

Om "unauthorized": Auktorisera på telefonen och kör adb devices igen.

**För NFC-utveckling:**

- På telefonen: Inställningar > Anslutningar > NFC och kontaktlösa betalningar > Slå på NFC.

- Använd `adb logcat` för att se NFC-logs under test.

- I Android Studio: Välj den fysiska enheten istället för emulatorn när du deployar.

**Nästa:**
När ansluten, deploya appen till den riktiga telefonen för verklig NFC-testning (emulator har mycket begränsat NFC-stöd).

### Aktivera USB-felsökning på svenska Samsung (One UI / Android 12)

På Galaxy Note 10 med svenskt gränssnitt:

1. Gå till **Inställningar > Om telefonen > Programvaruinformation**
2. Tryck **7 gånger** på **Byggnummer** (längst ner). Du får ett meddelande att utvecklaralternativ är upplåsta.
3. Gå tillbaka till huvudmenyn för Inställningar. Nu finns **Utvecklaralternativ** (kan vara under "System" eller sök efter det).
4. Öppna **Utvecklaralternativ**.
5. Slå på **USB-felsökning**.
6. (Valfritt men rekommenderat för framtiden) Slå på **OEM-upplåsning**.

När du kopplar in via USB:
- Dra ner notification shade.
- Tryck på USB-notisen.
- Välj **Filöverföring** (MTP).
- Telefonen visar en dialog "Tillåt USB-felsökning?" med datorns RSA-nyckel. Markera "Tillåt alltid från den här datorn" och godkänn.

**Exakt meny-status vid första anslutning (rapporterad 2026-06-01):**

Användaren hade redan rätt värde i USB-inställningar:

- **USB kontrolleras av:** Den här enheten (val: Ansluten enhet | Den här enheten)
- **Använd USB till:** Överföra filer/Android Auto  ← **Detta är korrekt och ska behållas** (alternativ: USB-Internetdelning; MIDI; Överföra bilder; Endast telefonladdning)
- Omkoda exporterad video: av (irrelevant)

"Överföra filer/Android Auto" ger MTP-filöverföring + fullt ADB-stöd. Lämna "USB kontrolleras av" på "Den här enheten" initialt; byt till "Ansluten enhet" endast om tillåt-dialogen inte dyker upp.

**Observerat beteende (samma session):**
- Ingen "Tillåt USB-felsökning?"-dialog dök upp.
- Telefonen visade USB-inställningar i notifikationsrullgardinen.
- Försök att välja "Ansluten enhet" nekades av telefonen ("den inte tillåter det valet").
- **Orsak & status:** ADB såg redan enheten som "device" (auktoriserad). Dialogen visas bara första gången RSA-nyckeln är okänd. "Ansluten enhet" nekas med flit – det skulle sätta telefonen i USB-host-läge (för OTG-periferier), vilket inte är vad vi vill vid PC-anslutning. "Den här enheten" + filöverföring är rätt för ADB/MTP.

**Verifiering via adb (direkt efter rapporten):**
- Serial: R58M82BNJGW
- Modell: SM-N970F (d1eea)
- Android 12, SDK 31, fingerprint: samsung/d1eea/d1:12/SP1A.210812.016/N970FXXS9HWHA
- USB config: mtp,conn_gadget,adb (adb aktivt)
- NFC: fullt stöd + initialiserat (nfc_hal_service running, nfc.initialized=true, firmware S.LSI 4.5.11, features: android.hardware.nfc + hce + ese + uicc m.fl.). Samsungs egen NFC-kontroller som stöder Mifare Classic.

**Udev + plugdev setup slutfört (2026-06-02):**
Scriptet `bash ~/projects/rfid/rfid-manager/setup/fix-usb-adb.sh` kördes framgångsrikt i riktig terminal.
- usbutils installerat
- Användare joakim i `plugdev`-gruppen
- Regler på plats i `/etc/udev/rules.d/51-android.rules`
- Filer flyttade till dedikerad katalog `~/projects/rfid/rfid-manager/setup/` (för att hålla home root städat)
- `lsusb` bekräftar: `ID 04e8:6860 Samsung Electronics Co., Ltd Galaxy series, misc. (MTP mode)`
- `adb devices` visar fortfarande `device` efter `kill-server`

**Första riktiga tagg-läsning (2026-06-02):**
Användaren: "Den har lyckats läsa en av mina fyra NFC taggar, så det funkar. De andra taggarna kan vara dåliga..."
Senare update: "Läsning fungerar. NFC taggen dyker upp utan ljudsignal." (reader mode + NO_PLATFORM_SOUNDS fungerar). Men skrivning gav "Inget händer".

- Alla användarens testtaggar visade sig vara **MIFARE_ULTRALIGHT** (UID t.ex. 042061B2392B80, techs: NfcA + MifareUltralight + Ndef). Detta förklarar varför systemet tidigare öppnade Chrome (NDEF URL).
- Auto-läsning av så många sektorer som möjligt (0..15 för 1K, stoppar vid upprepade auth-fel) med default keys nu aktiv vid varje detektion.
- För Mifare Ultralight / NTAG (användarens testtaggar): läser user pages (4,8,12...) med readPages och visar som "P4: ..." etc.
- Data visas i UI som preview i listan + full hex-dump (Sector/Page) i "SELECTED TAG MEMORY" när taggen markeras.
- Fullständiga hex-dumpar loggas alltid under taggen "AndroidNfcManager".
- Write-fliken är nu funktionell och typ-medveten: välj tagg i listan, gå till WRITE, skriv text.
  - Ultralight/NTAG: skriver till page 4 (4 bytes).
  - Classic: skriver till block 4 (16 bytes).
  **Håll taggen i räckvidd** under skrivningen (använder cached lastTag från senaste detektion).
- Write form now has editable "Target page / block" field so user can try different addresses.
- On Ultralight detection, page 2 (lock bytes) is read and shown in the hex dump. Lock bits often cause "Transceive failed" on writePage (IOException). User can inspect the lock bytes in UI and try higher pages.

User kunde bekräfta att läsningen nu fungerar utan system-chooser/ljud. Skrivning kräver att taggen är närvarande vid write-tillfället.

Detta ger direkt tillgång till innehållet i eskortminnet när en läsbar tagg scannas.

**Known issue fixed (2026-06-02):**
User reported that even though tags were "read" (sound + system chooser dialog to Chrome), nothing appeared in the app's READ LOG.
This was because `enableReaderMode` was not reliably active (system fell back to NDEF dispatch).
Fixed by:
- Using `LaunchedEffect` for scanning state + hoisting state to Activity level.
- Always disabling previous reader mode before enabling new one + re-enable in onResume.
- Better diagnostics in logs + dynamic "SCANNING" status.
- Added NFC intent filters + onNewIntent fallback: if system chooser appears, choosing "RFID Manager" will still process the tag via onNewIntent and populate READ LOG (with UID, type, and manager will attempt sector reads).

Reader mode (START SCAN) is still the preferred path (no chooser, direct delivery).

Manifest + tech filter updated so the app is offered in the chooser when needed.

**Rekommendation efter setup:**
- Koppla ur och in telefonen igen.
- I notifikationsrullgardinen: bekräfta "Överföra filer/Android Auto".
- Slå på NFC: Inställningar → Anslutningar → NFC och kontaktlösa betalningar.
- Öppna "RFID Manager" på telefonen (redan sideloaddad via `adb install`).
- I Android Studio: välj den fysiska enheten (SM-N970F) som run target.

Se även avsnittet ovan om udev-regler på datorn.
