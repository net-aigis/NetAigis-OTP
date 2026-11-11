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

Aktuelle Entwicklungsversion: `4.0.3` (versionCode `30`; master/FreeOTP+ stand bei `3.4` / `26`).

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

- Versionsnummern ohne führendes v schreiben, z. B. `4.0.3`.
- Version nur im Titel/Header nennen, nicht mehrfach in Abschnittsüberschriften.
- Product Flavors `free` / `pro` sind eingerichtet.
- Welche Features Pro werden, ist noch offen und wird später definiert.
- Bis zur Definition keine Feature-Sperren „auf Verdacht“ einbauen.
- Application-ID-Wechsel erzeugt eine neue App-Installation; keine automatische Datenübernahme von FreeOTP+.
- **Hauptmerkmal gegenüber FreeOTP+:** Token-**Aliase** und optionales **Icon** (für F-Droid/Fastlane als Fork-Mehrwert hervorheben).
  - **Alias**: optionaler Anzeigename in der Tokenliste (statt Issuer). Ändert nichts an der OTP-Berechnung. Ist in der Suche enthalten. Kann in der Key-URI als Query-Parameter `alias` mitgeführt werden.
  - **Icon**: optionaler Name eines eingebauten Symbols (`iconKey`); wird genutzt, wenn kein eigenes Token-Bild gesetzt ist. Kann in der Key-URI als `icon` / `iconKey` mitgeführt werden.
  - **Thumbnail-Auflösung** (ohne eigenes Bild): **Icon** → **Alias** → **Issuer** (erster Treffer gewinnt).
  - Issuer/Label bleiben die technischen Werte vom ausstellenden Dienst; für eigene Bezeichnungen Alias bzw. Icon nutzen.
- **Kategorie** (Organisation, zusätzlich dokumentieren):
  - Optionaler freier Text am Token (z. B. Arbeit / Privat); ändert nichts an der OTP-Berechnung.
  - Filter in der Tokenliste: Tabs bzw. Menü — Alle, Keine (ohne Kategorie), plus jede vorhandene Kategorie.
  - In Add/Edit mit Autocomplete aus bestehenden Kategorien; Feldhilfe über String-Ressourcen.
  - Persistenz in Room (`otp_tokens.category`); derzeit **nicht** als Key-URI-Query-Parameter (nur Alias/`icon`).

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

Release-Artefakte bauen/hochladen (Forgejo und/oder GitHub; vorerst nur Pro):

```bash
./helper/release.sh 4.0.3 --build
FTOKEN_RELEASE=... ./helper/release.sh 4.0.3 --upload
./helper/release.sh 4.0.3 --upload-github
FTOKEN_RELEASE=... ./helper/release.sh 4.0.3 --build --upload --upload-github
```

`release.sh` baut/uploadet derzeit nur Pro (Debug+Release). Free ist pausiert.
Artefaktnamen: Pro (Default) ohne Suffix — `netaigis-otp-${VERSION}.apk` / `netaigis-otp-debug-${VERSION}.apk`; Free ggf. mit `-free-`.
Release-Beschreibung kommt aus `app/src/main/res/raw/changelog.md` (Abschnitt `## VERSION`).
GitHub-Ziel: `GITHUB_REPO` (Default `net-aigis/NetAigis-OTP`), Auth über `gh auth` / `GH_TOKEN`.

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
- `helper/release.sh` — Pro Debug+Release bauen (Free pausiert); optional Upload nach Forgejo (`--upload`) und/oder GitHub (`--upload-github`); Notes aus `changelog.md`
- Seed nutzt `testdata/sample-tokens.txt` (otpauth-URIs)
- Debug-Packages: `de.wittmer.app.otp.debug` (Pro) / `de.wittmer.app.otp.free.debug` (Free)

---

## Spätere Themen / bewusst offen

Noch nicht einbauen, solange nicht explizit angefordert:

1. Spätere Free/Pro-Unterschiede an `FeatureAccess` ergänzen (aktuell gleich)
2. Fastlane-Beschreibungen/Screenshots vollständig auf NetAigis umstellen
3. Crowdin-Texte jenseits der Markennamen feinjustieren
4. Gradle/AGP-Deprecations abbauen (aktuell AGP 9.4.1 / Gradle 9.8.2; nötig vor AGP 10):
   1. Built-in Kotlin + neues AGP-DSL: `android.builtInKotlin=false` und `android.newDsl=false` entfernen; in allen Modulen `kotlin("android")` entfernen; Root-`kotlin-gradle-plugin` bereinigen
   2. APK-Umbenennung in `app/build.gradle.kts` von `applicationVariants` auf `AndroidComponentsExtension` umstellen
   3. Weitere deprecated Opt-outs in `gradle.properties` abbauen (`usesSdkInManifest.disallowed`, `defaultTargetSdkToCompileSdkIfUnset`, `enableAppCompileTimeRClass`, `r8.optimizedResourceShrinking`, `defaults.buildfeatures.resvalues`, `nonFinalResIds`; ggf. `uniquePackageNames`, `nonTransitiveRClass`, `r8.strictFullModeForKeepRules`)
   4. Optional: `android.dependency.excludeLibraryComponentsFromConstraints=true`; `Configuration.setVisible` über Plugin-Updates; Configuration Cache nur bei Bedarf

---

## F-Droid Aufnahme (Stand 2026-11-10)

Prüfgrundlage: [F-Droid Inclusion Policy](https://f-droid.org/de/docs/Inclusion_Policy/).  
Vergleichs-App bereits im Repo: FreeOTP+ (`org.liberty.android.freeotpplus`).

### Ergebnis (Kurz)

- Harte FLOSS-Kriterien: **weitgehend erfüllt** (Apache-2.0, keine proprietären Tracker/Ads/Firebase/Play-Services, Abhängigkeiten aus Google Maven / Maven Central, öffentliche Quellen auf GitHub `net-aigis/NetAigis-OTP`, eigene Application-IDs, Rebranding).
- Aufnahme-ready: **noch nicht** — siehe ToDos unten.
- Arbeit am F-Droid-Fit erfolgt vorerst manuell in Android Studio; Agenten sollen die ToDos unten als verbindliche Checkliste lesen.

### Was bereits passt

- Lizenz: `COPYING` (Apache 2.0); Icons/Hinweise: `COPYING.icons`
- Keine eingebetteten proprietären JARs/AARs (nur übliches `gradle/wrapper/gradle-wrapper.jar`)
- Keine API-Keys im Repo; keine Auto-Downloads ausführbarer Binaries
- Fork-Anforderungen: neue IDs `de.wittmer.app.otp` / `de.wittmer.app.otp.free`, Name/Icon/Strings NetAigis
- Fastlane-Metadaten unter `fastlane/metadata/android/` vorhanden
- Spendenlinks upstream (README / About)

### Offene Risiken (kein Soft-Ignore)

1. **Release-Build ohne Keystore bricht ab** — Blocker für F-Droid-CI  
   In `app/build.gradle.kts` wirft fehlende `keystore.properties` bei Release-Tasks eine `GradleException`. F-Droid baut Release **ohne** unsere Keystore-Datei. Signing nur anwenden, wenn die Datei existiert; Build darf ohne Datei durchlaufen (F-Droid signiert selbst).
2. **Drei-Monats-Regel** — Policy verlangt i. d. R. ≥ 3 Monate aktive Weiterentwicklung der App (nicht nur Upstream-Historie). NetAigis-Rebrand/Fork erst ab ~2026-11-03. Antrag erst sinnvoll nach Wartezeit oder mit Ausnahme-Begründung (Nutzer, andere F-Droid-Apps vom Autor).
3. **Fork-Mehrwert vs. FreeOTP+** — Quality-Punkt: keine reinen Kosmetik-Forks. Unser Mehrwert ist Token-**Alias**/Icon (plus Kategorien zur Organisation); in Fastlane-Beschreibung (`full_description` / `short_description`) klar sichtbar machen → sonst Ablehnungsrisiko als „Rebrand only“.

### F-Droid ToDos

Checkboxen bewusst textlich, damit sie in Android Studio und von Agenten gleich lesbar sind.

- [x] **F-Droid-1 — Keystore-Hard-Fail entfernen**  
  Datei: `app/build.gradle.kts`  
  Erledigt: Signing nur wenn `keystore.properties` vorhanden; kein `throw` mehr. Verifiziert: `./gradlew :app:assembleProRelease` ohne die Datei läuft durch.
- [x] **F-Droid-2 — Eine Application-ID für den Antrag festlegen**  
  Festgelegt: Pro `de.wittmer.app.otp` (Default-Flavor). Free parallel nicht einreichen, solange Feature-Umfang gleich ist. In künftiger `fdroiddata`-Metadata den Gradle-Flavor explizit setzen (`pro`).
- [x] **F-Droid-3 — Fastlane-Mehrwert schärfen**  
  Erledigt für `en-US` und `de-DE`: Kurz- und Langbeschreibung nennen Alias, Icon und Kategorien explizit als Organisation/Mehrwert gegenüber FreeOTP+.
- [x] **F-Droid-4 — `INTERNET`-Permission prüfen**  
  Manifest: `app/src/main/AndroidManifest.xml`. Geprüft: About/Legal öffnen URLs per `ACTION_VIEW` (kein `INTERNET` nötig). **Behalten**, weil otpauth-`image` eine Remote-URL sein kann und Glide sie beim Scan/Anzeige lädt. Entfernen würde dieses Legacy-Verhalten still brechen.
- [ ] **F-Droid-5 — Drei-Monats-Fenster / Antragstiming**  
  Nicht vor Ablauf der Policy-Frist (oder ohne Ausnahme-Begründung) den `fdroiddata`-MR öffnen. Bis dahin: App weiter pflegen, Releases taggen, Quellen öffentlich halten. (NetAigis-Fork ~2026-11-03 → Antrag frühestens ~2026-02 oder mit Ausnahme-Begründung.)
- [ ] **F-Droid-6 — Antrag vorbereiten (wenn 1–5 erledigt)**  
  Merge Request gegen [fdroiddata](https://gitlab.com/fdroid/fdroiddata): Metadata analog FreeOTP+ (`License: Apache-2.0`, `Repo` → GitHub NetAigis-OTP, `gradle: [pro]`, Application-ID `de.wittmer.app.otp`, Fastlane übernehmen). Build muss auf F-Droid-CI reproduzierbar sein.

### Bewusst nicht als F-Droid-Blocker führen

- Token-Service-Logos (andOTP-Erbe): FreeOTP+ ohne `NonFreeAssets` im Repo; GroupOffice-Hinweis in `COPYING.icons` lizenzlich dünn, aber kein bekannter Hard-Fail beim Upstream-Vorbild.
- Free/Pro-Feature-Parität: für F-Droid irrelevant, solange nur eine ID eingereicht wird.
- AGP/Gradle-Deprecations: eigenes Thema unter „Spätere Themen“, kein F-Droid-Inclusion-Blocker.

---

## Nächster sinnvoller Block

1. F-Droid-5: Drei-Monats-Fenster abwarten bzw. Ausnahme-Begründung klären; bis dahin Releases/Quellen pflegen
2. F-Droid-6: `fdroiddata`-MR vorbereiten (Flavor `pro`, ID `de.wittmer.app.otp`), sobald Timing passt
3. Weitere Pro-Features bei Bedarf an `FeatureAccess` hängen
