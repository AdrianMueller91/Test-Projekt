package de.testprojekt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zugangsdaten aus der Umgebung (APP_USERNAME, APP_PASSWORD).
 *
 * @param requirePassword im Profil "prod" true: ohne Passwort startet die Anwendung nicht
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(String username, String password, boolean requirePassword) {
}
