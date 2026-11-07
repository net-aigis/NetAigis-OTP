# AGENTS.md — NetAigis OTP Android

## Zweck dieser Datei

Diese Datei fasst die im Chat getroffenen Projektentscheidungen zusammen. Sie dient als Arbeitsgrundlage für Codex/AI-Agenten, die im Repository weiterarbeiten.

Wichtig: Änderungen sollen klein, nachvollziehbar und fachlich konsistent sein. Keine großen Umbauten ohne expliziten Grund. Bestehende Entscheidungen aus dieser Datei haben Vorrang vor generischen Android-App-Vorschlägen.

---

## Projekt

- App: **NetAigis OTP**
- Englischer App-Name: **NetAigis OTP**
- Herkunft: Fork/Übernahme von FreeOTP+
- Package/Application-ID Free: `de.wittmer.app.otp.free`
- Package/Application-ID Pro: `de.wittmer.app.otp` (Default-Flavor)
- Namespace: `de.wittmer.app.otp`
- Repo: `NetAigis-OTP`
- Beobachtete lokale Pfade:
  - `/srv/devel/myGIT/git.wittmer.software/WITTMER/otp-android`
  - `/home/admin/myGIT/git.wittmer.software/WITTMER/otp-android`
- Zielplattform:
  - Android
  - /e/OS
- Module:
  - `:app`
  - `:token-data`
  - `:token-images`
  - `:text-drawable`
- Entwicklungsansatz:
  - Native Android App
  - Kotlin
  - View Binding / XML-Layouts (kein Jetpack Compose im aktuellen Stand)
  - Room (`token-data`)
  - Hilt
  - CameraX (QR)
  - Biometric

Aktuelle Entwicklungsversion: `4.0.0` (versionCode `27`; master/FreeOTP+ stand bei `3.4` / `26`).

---

## Arbeitsregeln

- Keine Annahmen ergänzen.
- Vor Änderungen zuerst relevante vorhandene Dateien lesen.
- Keine Architektur umbauen, wenn nur ein Bugfix oder kleiner Schritt verlangt ist.
- Kleine, prüfbare Änderungen bevorzugen.
- Keine Builds oder Tests ausführen, bevor erklärt wurde, warum das nötig ist.
- Bei unklarer Grundlage stoppen und Rückfrage stellen.
- Bei Fehlern im vorigen Ergebnis als Reklamation behandeln: Ursache nennen, minimalen Fix vorschlagen, nicht neu anfangen.
- Bei Änderungen an `.gitignore` die Datei anschließend mit `sort -f` sortieren. Neue Einträge nicht einfach ans Ende hängen lassen.

---

## Internationalisierung / Texte

- Keine hartcodierten sichtbaren Texte im Kotlin- oder XML-Layout-Code.
- Alle UI-Texte, Labels, Buttontexte, Fehlermeldungen und Statusmeldungen müssen über Android String-Ressourcen laufen.
- Aktueller Stand:
  - Default/`values/strings.xml` ist Englisch
  - Deutsch liegt in `values-de-rDE/strings.xml`
  - weitere Locales über Crowdin vorhanden
- Neue sichtbare Texte immer mindestens in Default (`values`) und Deutsch (`values-de-rDE`) ergänzen.
- ViewModels und Repository-Ergebnisse dürfen keine fertigen Anzeigetexte liefern; fachliche Codes oder `@StringRes`-IDs verwenden.
- Technische Konstanten sind davon ausgenommen (interne Codes, MIME-Types, Dateiformate).
- Vor Abschluss einer UI-Änderung prüfen, ob neue harte Texte entstanden sind.

---

## Projektentscheidungen

- Versionsnummern ohne führendes v schreiben, z. B. `4.0.0`.
- Version nur im Titel/Header nennen, nicht mehrfach in Abschnittsüberschriften.
- Product Flavors `free` / `pro` sind eingerichtet.
- Welche Features Pro werden, ist noch offen und wird später definiert.
- Bis zur Definition keine Feature-Sperren „auf Verdacht“ einbauen.
- Application-ID-Wechsel erzeugt eine neue App-Installation; keine automatische Datenübernahme von FreeOTP+.
- **Hauptmerkmal gegenüber FreeOTP+:** Token-**Aliase**. Issuer/Label bleiben die technischen Werte; der Alias steuert Icon-Matching (Vorrang vor Issuer) und ist in der Suche enthalten. Alias kann in der Key-URI als Query-Parameter `alias` mitgeführt werden. Die Tokenliste zeigt weiterhin den Issuer (nicht den Alias als Listentitel).

---

## Branding

Quelle: [net-aigis.de](https://net-aigis.de/)

### Farben

| Name | Hex | Rolle |
|---|---|---|
| Ink | `#111d24` | Primary |
| Ink dark | `#0d171d` | Primary dark |
| Lime | `#cdfb65` | Secondary / Accent |
| Paper | `#f7f9fa` | heller Hintergrund |
| Muted | `#52616a` | Nebentext |
| Line | `#dce3e6` | Trenner |
| Focus | `#418801` | Accessibility-Fokus |

Material DayNight bleibt; FreeOTP-Blau (`#16AFF9`) ist ersetzt.

### Hintergrund-Assets

- `app/src/main/res/drawable-nodpi/logo_watermark.jpg` — **niemals löschen** (Portrait-Referenz)
- `app/src/main/res/drawable-nodpi/logo_watermark_wide.png` — breite Kulisse für feste Skalierung / Drehen
- Laufzeit/UI nutzen derzeit `_wide`; das JPG bleibt trotzdem im Repo

### Icon

- Launcher: Banner-Schild (Vorlage `arts/branding-preview/icon-shield-square-v4.png` / `shield-wide-v4.png`)
- Adaptive Icon: Hintergrund `#111d24`, Vordergrund Schild-Asset

### App-Namen

| Variante | Name |
|---|---|
| free / release | NetAigis OTP Free |
| free / debug | NetAigis OTP Free Debug |
| pro / release | NetAigis OTP |
| pro / debug | NetAigis OTP Debug |

---

## Kommunikations- und Arbeitsstil

- Sprache im Chat: Deutsch.
- Code-Bezeichner bleiben Englisch, soweit im Projekt bereits so angelegt.
- Antworten sollen direkt, konkret und kurz sein.
- Keine Spekulation.
- Keine vollständigen Datei-Ersetzungen vorschlagen, wenn ein gezielter Patch reicht.
- Vor Codeänderungen prüfen, ob die aktuelle Datei wirklich bekannt ist.
- Wenn ein Buildfehler gemeldet wird:
  - nur den konkreten Fehler behandeln
  - keine Nebenumbauten einführen
- Wenn mehrere Punkte offen sind:
  - kleine Schritte
  - erst aktuellen Block abschließen
  - dann committen

---

## Build und Test

Standard-Build Free:

```bash
./gradlew :app:assembleFreeDebug
```

Free-Debug-APK:

```text
app/build/outputs/apk/free/debug/otp-free-debug.apk
```

Free-Package:

```text
de.wittmer.app.otp.free
```

Free-Debug-Package:

```text
de.wittmer.app.otp.free.debug
```

Pro-Build (Default):

```bash
./gradlew :app:assembleProDebug
```

Pro-Debug-APK:

```text
app/build/outputs/apk/pro/debug/otp-debug.apk
```

Pro-Package:

```text
de.wittmer.app.otp
```

Pro-Debug-Package:

```text
de.wittmer.app.otp.debug
```

Beide Debug-Varianten:

```bash
./gradlew :app:assembleFreeDebug :app:assembleProDebug
```

Beide Release-Varianten:

```bash
./gradlew :app:assembleFreeRelease :app:assembleProRelease
```

Release-Builds benötigen lokal eine nicht versionierte Datei:

```text
keystore.properties
```

`keystore.properties` und private `.jks`-Keystores dürfen nicht ins Git.

Manuelles Zurücksetzen der Appdaten (Pro):

```bash
adb shell pm clear de.wittmer.app.otp
```

Komplette Neuinstallation (Pro-Debug, Default):

```bash
./helper/rebuild_and_start.sh
```

Bei mehreren Geräten/Emulatoren Zielserial angeben (oder `ANDROID_SERIAL` setzen):

```bash
./helper/rebuild_and_start.sh --edition pro --device fad4c341
```

Mit Testtokens aus `testdata/sample-tokens.txt`:

```bash
./helper/rebuild_and_start.sh --seed
```

Free-Debug:

```bash
./helper/rebuild_and_start.sh --edition free
./helper/rebuild_and_start.sh --edition free --seed
```

Release-Artefakte bauen/hochladen (Forgejo):

```bash
./helper/release.sh 4.0.0 --build
FTOKEN_RELEASE=... ./helper/release.sh 4.0.0 --upload
FTOKEN_RELEASE=... ./helper/release.sh 4.0.0 --build --upload
```

---

## Technische Basisentscheidungen

### Gradle / Android

- Product Flavors:
  - `free`
  - `pro`
- Flavor-Dimension:

```kotlin
flavorDimensions += "edition"
```

- `free` Application-ID: `de.wittmer.app.otp.free`
- `pro` Application-ID: `de.wittmer.app.otp` (`isDefault = true`)
- Flavor-Source-Sets:

```text
app/src/free/
app/src/pro/
app/src/freeDebug/
app/src/proDebug/
```

- Gemeinsame Codebasis: `app/src/main/`
- Edition-/Feature-Schaltung über `FeatureAccess`:
  - API: `app/src/main/.../feature/FeatureAccess.kt`
  - Implementierung: `FeatureAccessImpl` in `free` / `pro`
  - Hilt-Bindung: `FeatureModule`
- Keine verstreuten Prüfungen auf `BuildConfig.FLAVOR` einbauen.
- Feature-Stand: Free und Pro derzeit gleicher Funktionsumfang (`screenshotModeEnabled = true` in beiden).
- `FeatureAccess` bleibt die zentrale Schaltung für spätere Unterschiede.
- App-Namen flavor-/build-spezifisch (siehe Branding-Tabelle: Free trägt „Free“, Pro ist der Default-Name ohne Suffix).

### Package-Mapping

| Alt | Neu |
|---|---|
| `org.fedorahosted.freeotp` | `de.wittmer.app.otp` |
| `org.fedorahosted.freeotp.data` | `de.wittmer.app.otp.data` |
| `org.liberty.android.freeotp.token_images` | `de.wittmer.app.otp.token_images` |
| `com.amulyakhare.textdrawable` | `de.wittmer.app.otp.textdrawable` |
| Application-ID `org.liberty.android.freeotpplus` | `de.wittmer.app.otp` (Pro) / `de.wittmer.app.otp.free` |

`com.google.android.apps.authenticator.Base32String` bleibt unverändert (Vendor-Code).

### Room / SQLite

- Token-Datenhaltung über Room im Modul `token-data`.
- Bestehende FreeOTP+-Migrationen und Datenformate beachten; keine stillen Schema-Brüche.

### Storage

- Android Storage Access Framework verwenden.
- Kein `MANAGE_EXTERNAL_STORAGE`.
- Geplante Storage APIs wo nötig:
  - `ACTION_OPEN_DOCUMENT_TREE`
  - `ACTION_CREATE_DOCUMENT`
  - `ACTION_OPEN_DOCUMENT`

### JDK

- JDK-Prüfung unter openSUSE besser mit:

```bash
javac --version
```

nicht nur mit:

```bash
java -version
```

---

## Helper-Skripte

- `helper/rebuild_and_start.sh` — Debug bauen, installieren, starten (`--edition free|pro`, `--device SERIAL`, `--seed`, `--wipe-backup SERIAL`)
- `helper/release.sh` — Free/Pro Debug+Release bauen und optional nach Forgejo (`otp-android`) hochladen
- Seed nutzt `testdata/sample-tokens.txt` (otpauth-URIs)
- Debug-Packages: `de.wittmer.app.otp.debug` (Pro) / `de.wittmer.app.otp.free.debug` (Free)

---

## Spätere Themen / bewusst offen

Noch nicht einbauen, solange nicht explizit angefordert:

1. Spätere Free/Pro-Unterschiede an `FeatureAccess` ergänzen (aktuell gleich)
2. Fastlane-Beschreibungen/Screenshots vollständig auf NetAigis umstellen
3. Crowdin-Texte jenseits der Markennamen feinjustieren

---

## Nächster sinnvoller Block

1. Weitere Pro-Features bei Bedarf an `FeatureAccess` hängen
2. Fastlane-Metadaten inhaltlich nachziehen
