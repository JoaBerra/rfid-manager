---
title: Android Studio Installation
tags: [android-studio, aur, arch-linux, omarchy, hyprland, installation]
created: 2026-05-26
updated: 2026-05-26
---

# Android Studio Installation från AUR

> **Mål**: Installera den officiella stabila Android Studio på Arch Linux + Omarchy (Hyprland) via AUR, helt kostnadsfritt.

## Rekommenderat Paket

Använd AUR-paketet **`android-studio`** (stable).

- Rekommenderas av Arch Wiki.
- Installerar officiell Google/JetBrains-tarball till `/opt/android-studio`.
- Ger korrekt `.desktop`-fil och menyintegration.
- Aktuell version (maj 2026): ~2025.3.4.7

**Relaterade paket**:
- `android-studio-beta`
- `android-studio-canary` (används sällan för produktion)

## Steg-för-steg Installation

### 1. Systemuppdatering och Förberedelser

```bash
sudo pacman -Syu
```

Se till att `[multilib]` är aktiverat i `/etc/pacman.conf` (nödvändigt för emulatorns 32-bitarsbibliotek).

Installera grundverktyg:

```bash
sudo pacman -S --needed base-devel git multilib-devel android-tools
```

> **Notera**: `android-tools` ger system-`adb`/`fastboot`. Vissa föredrar att skippa det för att undvika konflikter med Studio:s inbyggda version. Du kan testa båda.

### 2. Installera AUR Helper (om du inte har en)

De flesta Omarchy-användare kör antingen **paru** eller **yay**.

**Rekommenderat: paru** (rent, Rust-baserat):

```bash
git clone https://aur.archlinux.org/paru.git
cd paru
makepkg -si
```

Eller **yay**:

```bash
git clone https://aur.archlinux.org/yay.git
cd yay
makepkg -si
```

### 3. Installera Android Studio

```bash
paru -S android-studio
# eller
yay -S android-studio
```

Detta är en **stor nedladdning** (~1 GB+). Ha tålamod.

### 4. Omarchy / Hyprland-specifika Förberedelser (Mycket Viktigt!)

Detta är den vanligaste källan till problem på Omarchy.

#### 4.1 Miljövariabel för Wayland (AWT)

Lägg till i din Hyprland-konfig (ofta `~/.config/hypr/hyprland.conf` eller en inkluderad fil i Omarchy):

```conf
env = _JAVA_AWT_WM_NONREPARENTING,1
```

#### 4.2 JetBrains Window Rules (fixar fokus, popups, consent-dialog)

Många Omarchy-installationer har en `jetbrains.conf`. Redigera den eller lägg till i din huvudkonfig:

```conf
# JetBrains / Android Studio
windowrulev2 = float,class:^(jetbrains-.*)$
windowrulev2 = center,class:^(jetbrains-.*)$
windowrulev2 = stayfocused,class:^(jetbrains-.*)$

# Fix för tooltips, "win" fönster, consent dialog etc.
windowrulev2 = noinitialfocus,class:^(.*jetbrains.*)$,title:^(win.*)$
windowrulev2 = nofocus,class:^(.*jetbrains.*)$,title:^(win.*)$
windowrulev2 = noinitialfocus,class:^(.*jetbrains.*)$,title:^\\s$
windowrulev2 = nofocus,class:^(.*jetbrains.*)$,title:^\\s$
```

#### 4.3 Consent Dialog Fix (vanligaste problemet)

På första uppstart fastnar ofta "Help improve Android Studio"-dialogen.

Skapa filen i förväg:

```bash
mkdir -p ~/.local/share/Google/consentOptions
echo 'rsch.send.usage.stat:1.0:0:0' > ~/.local/share/Google/consentOptions/accepted
```

(Alternativt med timestamp: `echo "rsch.send.usage.stat:1.0:0:$(date +%s)000"`)

### 5. Första Uppstart

Starta från meny eller terminal:

```bash
android-studio
```

Gå igenom Setup Wizard. Den laddar ner Android SDK till `~/Android/Sdk` (kan ändras).

### 6. Native Wayland-stöd (Rekommenderas starkt)

1. I Android Studio: **Help → Edit Custom VM Options**
2. Lägg till:

```
-Dawt.toolkit.name=WLToolkit
```

Spara och starta om. Ger skarpare rendering på HiDPI + fractional scaling i Hyprland.

### 7. SDK & Miljövariabler

Lägg till i `~/.bashrc`, `~/.zshrc` eller motsvarande:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
```

Kör `sdkmanager --licenses` om du får licensproblem.

### 8. Emulator Hardware Acceleration (KVM)

```bash
sudo usermod -aG kvm $USER
```

Logga ut och in igen (eller reboot). Installera system images via SDK Manager i Android Studio.

För emulator på Hyprland (vid svart skärm):

```bash
QT_QPA_PLATFORM=xcb emulator -avd Pixel_8_API_35
```

## Alternativ Installation (Rekommenderas av många)

Många erfarna utvecklare på Arch/Omarchy föredrar **JetBrains Toolbox** (`jetbrains-toolbox` från AUR) istället för det stora AUR-paketet. Det ger:

- Enklare uppdateringar
- Hantering av flera versioner (Canary + Stable)
- Mindre AUR-rebuilds

Överväg detta om du uppdaterar ofta.

## Diskutera / Nästa Steg

Efter installation:

- Verifiera att `android-studio` startar korrekt
- Skapa en AVD och testa emulatorn
- Börja titta på NFC-relaterade inställningar och plugin

Se även:
- [[Utvecklingsmiljö]]
- [[Verktyg-och-Setup]]
- [[Projektplan]]

## Källor & Referenser (interna)

Denna guide baseras på Arch Wiki, AUR-sida för `android-studio`, Omarchy GitHub-issues och Hyprland-gemenskapens dokumenterade lösningar (maj 2026).

## Live Installation Notes (2026-05-27)

Agenten körde installationen direkt.

**Resultat av `yay -S --noconfirm android-studio`:**

- Nedladdning av 1.37 GB tarball: **lyckades** (~47 sekunder)
- Bygge av paketet: **lyckades** fullt ut
- Slutligt `sudo pacman -U`: **misslyckades** (krävde interaktivt lösenord)

**Status just nu (2026-05-27):**

- Paket installerat via `sudo pacman -U`
- Verifierat: `android-studio 2025.3.4.7-1`
- Miljövariabler klara och verifierade:
  - `ANDROID_HOME=/home/joakim/Android/Sdk`
  - PATH innehåller emulator, platform-tools och cmdline-tools
- kvm-grupp: Verifierat att användaren är medlem

Full historik finns i [[log]].

---

## Nästa steg

3. Första uppstart av Android Studio (med Omarchy/Hyprland-fixar)
4. SDK-nedladdning + skapa test-AVD

Se [[Verktyg-och-Setup]] för fortsatta konfigurationer.