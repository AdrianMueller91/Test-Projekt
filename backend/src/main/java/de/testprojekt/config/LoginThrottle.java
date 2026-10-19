package de.testprojekt.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bremst das Durchprobieren von Passwörtern: nach {@value #MAX_FAILURES} Fehlversuchen je IP-Adresse
 * ist der Login gesperrt, bis seit dem letzten Fehlversuch {@link #LOCK_DURATION} vergangen sind.
 *
 * Bewusst einfach gehalten (nur im Speicher, eine Instanz). Hinter Caddy liefert Tomcat dank
 * server.forward-headers-strategy=native die echte Client-IP statt der Proxy-IP.
 */
public class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    /** Schutz gegen unbegrenztes Wachstum der Tabelle bei vielen verschiedenen Adressen. */
    private static final int MAX_ENTRIES = 10_000;

    private record Attempts(int failures, Instant lastFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String ip) {
        Attempts a = attempts.get(ip);
        if (a == null) {
            return false;
        }
        if (expired(a)) {
            attempts.remove(ip, a);
            return false;
        }
        return a.failures() >= MAX_FAILURES;
    }

    public void failure(String ip) {
        if (attempts.size() > MAX_ENTRIES) {
            attempts.values().removeIf(this::expired);
        }
        Instant now = clock.instant();
        attempts.merge(ip, new Attempts(1, now),
                (old, fresh) -> expired(old) ? fresh : new Attempts(old.failures() + 1, now));
    }

    public void success(String ip) {
        attempts.remove(ip);
    }

    private boolean expired(Attempts a) {
        return a.lastFailure().plus(LOCK_DURATION).isBefore(clock.instant());
    }
}
