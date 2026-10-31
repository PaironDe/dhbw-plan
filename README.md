# DHBW//PLAN

Eigener Stundenplan für die DHBW Lörrach im Neon-Night-City-Look.

- Kurs frei wählbar (z. B. `TIF24A`), Plan kommt direkt vom DHBW-Kalender
- Fächer an-/abwählen; Einmaltermine (Prüfungen, Präsentationen, Einführungen) werden immer angezeigt
- Hintergrund-Sync alle 3 Stunden, Benachrichtigung bei Änderungen an deinen Terminen
- Offline: letzter Stand bleibt gespeichert

## Bauen

Jeder Push auf `main` baut über GitHub Actions eine signierte APK und legt sie als Release an.
Benötigte Repository-Secrets (Settings → Secrets and variables → Actions):

| Secret | Inhalt |
|---|---|
| `KEYSTORE_BASE64` | Inhalt von `keystore-base64.txt` |
| `SIGNING_PASSWORD` | Passwort aus `passwort.txt` |

Den Keystore niemals verlieren: Ohne ihn lassen sich keine Updates über die installierte App installieren.

## Installieren

- Einmalig: APK aus dem neuesten Release aufs Handy laden und öffnen.
- Automatische Updates: [Obtainium](https://github.com/ImranR98/Obtainium) installieren und die Repo-URL hinzufügen.

Schriften: Orbitron, Rajdhani, JetBrains Mono (SIL Open Font License, siehe `fonts-licenses/`).
