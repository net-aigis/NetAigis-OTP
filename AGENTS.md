# AGENTS.md — NetAigis OTP Android

## Zweck dieser Datei

Diese Datei fasst die im Chat getroffenen Projektentscheidungen zusammen. Sie dient als Arbeitsgrundlage für Codex/AI-Agenten, die im Repository weiterarbeiten.

Wichtig: Änderungen sollen klein, nachvollziehbar und fachlich konsistent sein. Keine großen Umbauten ohne expliziten Grund. Bestehende Entscheidungen aus dieser Datei haben Vorrang vor generischen Android-App-Vorschlägen.

---

## Projekt

- App: **NetAigis OTP**
- Englischer App-Name: **NetAigis OTP**
- Herkunft: Fork/Übernahme von FreeOTP+
- Standard-Package/Application-ID Free: `de.wittmer.app.otp`
- Package/Application-ID Pro: `de.wittmer.app.otp.pro`
- Namespace (Ziel): `de.wittmer.app.otp`
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

Hinweis: Application-ID und Namespace sind Zielwerte. Der Code kann vorübergehend noch FreeOTP+-IDs (`org.liberty.android.freeotpplus` / `org.fedorahosted.freeotp`) verwenden, bis die Umbenennung umgesetzt ist.

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
- Aktueller Stand (FreeOTP+):
  - Default/`values/strings.xml` ist Englisch
  - Deutsch liegt in `values-de-rDE/strings.xml`
  - weitere Locales über Crowdin vorhanden
- Neue sichtbare Texte immer mindestens in Default (`values`) und Deutsch (`values-de-rDE`) ergänzen.
- ViewModels und Repository-Ergebnisse dürfen keine fertigen Anzeigetexte liefern; fachliche Codes oder `@StringRes`-IDs verwenden.
- Technische Konstanten sind davon ausgenommen (interne Codes, MIME-Types, Dateiformate).
- Vor Abschluss einer UI-Änderung prüfen, ob neue harte Texte entstanden sind.

---

## Projektentscheidungen

- Versionsnummern ohne führendes v schreiben, z. B. `0.1.0`.
- Version nur im Titel/Header nennen, nicht mehrfach in Abschnittsüberschriften.
- Product Flavors `free` / `pro` werden eingeführt.
- Welche Features Pro werden, ist noch offen und wird später definiert.
- Bis zur Definition keine Feature-Sperren „auf Verdacht“ einbauen.

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

Ziel-Builds nach Einführung der Flavors:

Standard-Build Free:

```bash
./gradlew :app:assembleFreeDebug
```

Free-Debug-APK:

```text
app/build/outputs/apk/free/debug/app-free-debug.apk
```

Free-Package:

```text
de.wittmer.app.otp
```

Pro-Build:

```bash
./gradlew :app:assembleProDebug
```

Pro-Debug-APK:

```text
app/build/outputs/apk/pro/debug/app-pro-debug.apk
```

Pro-Package:

```text
de.wittmer.app.otp.pro
```

Beide Debug-Varianten:

```bash
./gradlew :app:assembleFreeDebug :app:assembleProDebug
```

Beide Release-Varianten:

```bash
./gradlew :app:assembleFreeRelease :app:assembleProRelease
```

Übergang, solange noch keine Flavors existieren:

```bash
./gradlew :app:assembleDebug
```

Aktuelles Zwischen-Package (FreeOTP+):

```text
org.liberty.android.freeotpplus.debug
```

Release-Builds benötigen lokal eine nicht versionierte Datei:

```text
keystore.properties
```

`keystore.properties` und private `.jks`-Keystores dürfen nicht ins Git.

Manuelles Zurücksetzen der Appdaten (Ziel-Package Free):

```bash
adb shell pm clear de.wittmer.app.otp
```

Komplette Neuinstallation (Ziel Free-Debug):

```bash
adb uninstall de.wittmer.app.otp || true
adb install -r app/build/outputs/apk/free/debug/app-free-debug.apk
adb shell monkey -p de.wittmer.app.otp 1
```

Build/Install/Start nur nach erfolgreichem Build. Beispiel:

```bash
#!/usr/bin/bash

PACKAGE="de.wittmer.app.otp"
APK="app/build/outputs/apk/free/debug/app-free-debug.apk"

if ./gradlew :app:assembleFreeDebug; then
  adb wait-for-device
  adb uninstall "$PACKAGE" || true
  adb install -r "$APK"
  adb shell monkey -p "$PACKAGE" 1
else
  echo "Build fehlgeschlagen. Installation wird nicht ausgeführt."
  exit 1
fi
```

---

## Technische Basisentscheidungen

### Gradle / Android

- Die App verwendet Product Flavors für Editionen:
  - `free`
  - `pro`
- Flavor-Dimension:

```kotlin
flavorDimensions += "edition"
```

- `free` Application-ID:

```text
de.wittmer.app.otp
```

- `pro` Application-ID:

```text
de.wittmer.app.otp.pro
```

- Flavor-Source-Sets:

```text
app/src/free/
app/src/pro/
```

- Gemeinsame Codebasis bleibt in:

```text
app/src/main/
```

- Edition-/Feature-Schaltung zentral über eine `FeatureAccess`-Schnittstelle (noch einzurichten), analog zum Muster:
  - gemeinsame API in `main`
  - flavor-spezifische Implementierung in `free` / `pro`
- Keine verstreuten Prüfungen auf `BuildConfig.FLAVOR` einbauen.
- Pro-Feature-Liste: noch offen; hier ergänzen, sobald definiert.
- `pro` darf zusätzliche Ressourcen überschreiben, z. B. den App-Namen.

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

## Spätere Themen / bewusst offen

Noch nicht einbauen, solange nicht explizit angefordert:

1. Konkrete Pro-Feature-Liste und Feature-Flags
2. Application-ID- und Namespace-Umbenennung im Code
3. Flavor-Source-Sets und `FeatureAccess`
4. Branding (Name, Icon, Farben) auf NetAigis-OTP
5. Helper-Scripts für Rebuild/Release, falls gewünscht

---

## Nächster sinnvoller Block

Noch nicht final entschieden. Naheliegende Optionen:

1. Application-ID / Namespace auf `de.wittmer.app.otp` umstellen
2. Product Flavors `free` / `pro` einrichten
3. Pro-Features definieren und `FeatureAccess` anlegen
