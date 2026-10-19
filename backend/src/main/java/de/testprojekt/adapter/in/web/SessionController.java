package de.testprojekt.adapter.in.web;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liefert der Oberfläche den angemeldeten Benutzer. Ohne Anmeldung antwortet Spring Security mit 401. */
@RestController
class SessionController {

    @GetMapping("/api/me")
    MeResponse me(Principal principal) {
        return new MeResponse(principal.getName());
    }

    record MeResponse(String username) {
    }
}
