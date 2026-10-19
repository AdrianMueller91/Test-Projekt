# Test-Projekt

Kleine Notiz-Anwendung, um das erste Deployment auf dem netcup-VPS durchzuspielen.
Läuft unter **https://demo.fitness-mueller.de**.

Stack: Angular 21, Spring Boot 4.1 (Java 21), Postgres 17, Docker Compose, Caddy, GitHub Actions.

## Architektur

```
Browser ──HTTPS──▶ caddy (TLS, Sicherheits-Header) ──▶ app (Spring Boot + Angular) ──▶ db (Postgres)
                    Ports 80/443                         nur intern                      internes Netz ohne Internet
```

Das Backend ist hexagonal aufgebaut (`backend/src/main/java/de/testprojekt`):

| Paket | Inhalt | darf kennen |
|---|---|---|
| `domain` | Fachobjekt `Note`, Fachregeln, Fehler | nichts |
| `application.port.in` | Use-Case-Interfaces (anlegen, auflisten, löschen) | domain |
| `application.port.out` | `NoteRepository`, Schnittstelle zur Speicherung | domain |
| `application.service` | `NoteService`, setzt die Use Cases um | domain, Ports |
| `adapter.in.web` | REST-Controller, Fehlerbehandlung, Auslieferung der Oberfläche | application.port.in |
| `adapter.out.persistence` | JDBC-Umsetzung von `NoteRepository` | application.port.out |
| `config` | Verdrahtung und Sicherheit | alles |

Domain und Application sind frei von Spring. `ArchitectureTest` prüft die Regeln bei jedem Build mit ArchUnit.

## Absicherung

- Login mit Session-Cookie (`HttpOnly`, `Secure`, `SameSite=Lax`), ein Benutzer aus der `.env` auf dem Server
- CSRF-Schutz für alle schreibenden Aufrufe einschließlich Login (Cookie `XSRF-TOKEN` → Header `X-XSRF-TOKEN`)
- Login-Bremse: nach 5 Fehlversuchen je IP 15 Minuten gesperrt (HTTP 429)
- Content-Security-Policy, HSTS, `X-Frame-Options`, keine Stacktraces nach außen
- Actuator nach außen nur `/actuator/health`
- Container ohne Root, Dateisystem der App schreibgeschützt, Datenbank nur im internen Docker-Netz
- Server: SSH nur mit Schlüssel, Firewall (22, 80, 443), fail2ban, automatische Sicherheitsupdates

## Lokal entwickeln

```bash
cd backend && ./mvnw spring-boot:run      # H2 im Speicher, Zufallspasswort steht im Log
cd frontend && npm ci && npx ng serve     # http://localhost:4200, /api geht an Port 8080
```

Tests:

```bash
cd backend && ./mvnw verify               # 19 Tests, H2; mit SPRING_DATASOURCE_URL gegen Postgres
cd frontend && npx ng test --watch=false
```

## Deployment

Jeder Push auf `main` startet `.github/workflows/ci-deploy.yml`:

1. Frontend- und Backend-Tests, Backend gegen echten Postgres
2. Docker-Image bauen, per SSH als Benutzer `deploy` nach `/opt/test-projekt` übertragen
3. `docker compose up -d`, Health-Check, bei Fehler automatisch zurück auf das vorige Image
4. Prüfung von außen über `https://<APP_DOMAIN>/actuator/health`

GitHub-Einstellungen:

| Art | Name | Ort |
|---|---|---|
| Variable | `APP_DOMAIN` | Repository |
| Secret | `VPS_HOST`, `VPS_SSH_KEY`, `VPS_KNOWN_HOSTS` | Umgebung `production` (nur `main` darf deployen) |

Server neu aufsetzen: `deploy/bootstrap-server.sh <domain>` als root ausführen. Die Zugangsdaten der Anwendung
stehen danach nur auf dem Server in `/opt/test-projekt/.env`.

## Hinweis zum Fitness-Dashboard

Dieses Projekt belegt auf dem Server die Ports 80 und 443. Bevor das Fitness-Dashboard mit eigenem Caddy auf
denselben Server kommt, muss das Test-Projekt gestoppt werden (`docker compose down` in `/opt/test-projekt`)
oder beide Anwendungen teilen sich einen gemeinsamen Caddy.
