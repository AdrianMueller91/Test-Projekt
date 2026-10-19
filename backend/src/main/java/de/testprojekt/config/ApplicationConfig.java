package de.testprojekt.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.testprojekt.application.port.out.NoteRepository;
import de.testprojekt.application.service.NoteService;

/**
 * Verdrahtung des Anwendungskerns. Domain und Application bleiben dadurch frei von Spring;
 * der Controller bekommt den Service nur über seine Use-Case-Interfaces zu sehen.
 */
@Configuration
class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    NoteService noteService(NoteRepository noteRepository, Clock clock) {
        return new NoteService(noteRepository, clock);
    }
}
