package de.testprojekt.config;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Zugriffsschutz.
 *
 * - Die Angular-Dateien sind öffentlich, alle Daten unter /api nur nach Anmeldung (Session-Cookie).
 * - Login per POST /api/login (Formularfelder username, password), Logout per POST /api/logout.
 *   Beide antworten mit Statuscodes statt Weiterleitungen, das passt zur Single-Page-Anwendung.
 * - CSRF-Schutz im SPA-Modus: Spring legt den Token ins Cookie XSRF-TOKEN, Angular schickt ihn
 *   automatisch im Header X-XSRF-TOKEN zurück. Gilt auch für den Login selbst.
 *   Achtung: csrf.spa() erzeugt das Cookie in Spring Security 7.1 nur, wenn jemand den Token liest.
 *   Das übernimmt der {@link CsrfCookieFilter}; ohne ihn fehlt das Cookie und jeder POST endet mit 403.
 * - Login-Bremse je IP-Adresse gegen das Durchprobieren von Passwörtern.
 * - Content-Security-Policy: nur Skripte vom eigenen Server.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String LOGIN_URL = "/api/login";
    private static final int MIN_PASSWORD_LENGTH = 12;

    // Angular hängt Komponenten-Styles als <style>-Elemente an, daher 'unsafe-inline' nur für Styles
    private static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, LoginThrottle throttle) throws Exception {
        http
                .authorizeHttpRequests(a -> a
                        .requestMatchers(LOGIN_URL, "/actuator/health", "/actuator/health/**", "/error").permitAll()
                        .requestMatchers("/api/**", "/actuator/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(f -> f
                        // Eigene Login-Seite liefert Angular; Spring erzeugt dadurch keine eigene unter /login
                        .loginPage("/login")
                        .loginProcessingUrl(LOGIN_URL)
                        .successHandler((req, res, auth) -> {
                            throttle.success(req.getRemoteAddr());
                            res.setStatus(HttpStatus.NO_CONTENT.value());
                        })
                        .failureHandler((req, res, ex) -> {
                            throttle.failure(req.getRemoteAddr());
                            res.setStatus(HttpStatus.UNAUTHORIZED.value());
                        }))
                .logout(l -> l
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .csrf(c -> c.spa())
                // Ohne Anmeldung: 401 statt Weiterleitung, die Oberfläche zeigt dann die Login-Seite
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
                .addFilterBefore(new LoginThrottleFilter(throttle), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    LoginThrottle loginThrottle(Clock clock) {
        return new LoginThrottle(clock);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Genau ein Benutzer aus der Umgebung. Das Passwort wird nur gehasht im Speicher gehalten.
     * Lokal ohne APP_PASSWORD gibt es ein Zufallspasswort im Log; im Profil "prod" ist das Passwort Pflicht.
     */
    @Bean
    UserDetailsService userDetailsService(SecurityProperties props, PasswordEncoder encoder) {
        String username = props.username() == null || props.username().isBlank() ? "admin" : props.username();
        String password = props.password();
        if (password == null || password.isBlank()) {
            if (props.requirePassword()) {
                throw new IllegalStateException("APP_PASSWORD fehlt. In der .env auf dem Server setzen.");
            }
            password = randomPassword();
            log.warn("Kein APP_PASSWORD gesetzt. Lokales Zufallspasswort für '{}': {}", username, password);
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("APP_PASSWORD ist kürzer als " + MIN_PASSWORD_LENGTH + " Zeichen.");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(encoder.encode(password))
                .roles("USER")
                .build());
    }

    private static String randomPassword() {
        byte[] bytes = new byte[15];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Liest den (sonst verzögert erzeugten) CSRF-Token, damit Spring ihn bei jeder Antwort ins Cookie schreibt. */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }

    /** Weist Login-Versuche gesperrter IP-Adressen mit 429 ab, bevor das Passwort überhaupt geprüft wird. */
    private static final class LoginThrottleFilter extends OncePerRequestFilter {

        private static final RequestMatcher LOGIN_REQUEST =
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, LOGIN_URL);

        private final LoginThrottle throttle;

        LoginThrottleFilter(LoginThrottle throttle) {
            this.throttle = throttle;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !LOGIN_REQUEST.matches(request);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (throttle.isBlocked(request.getRemoteAddr())) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
