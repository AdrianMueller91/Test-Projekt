package de.testprojekt.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

/**
 * Spielt den Ablauf so durch, wie ihn Angular im Browser erlebt: CSRF-Cookie abholen, Token als Header
 * zurückschicken, anmelden, Notiz anlegen.
 *
 * Eigene Testklasse mit eigenem Spring-Kontext (Property app.test-context), denn der Test-Helfer csrf()
 * aus NoteApiTest tauscht im geteilten Kontext den Cookie-Speicher dauerhaft gegen einen Session-Speicher.
 * Hier wird csrf() deshalb nicht verwendet.
 */
@SpringBootTest(properties = {
        "app.security.username=tester",
        "app.security.password=test-passwort-123",
        "app.test-context=spa-login-flow"
})
@AutoConfigureMockMvc
class SpaLoginFlowTest {

    @Autowired
    MockMvc mvc;

    @Test
    void loginAndCreateNoteLikeTheBrowser() throws Exception {
        // 1. Erster Aufruf ohne Anmeldung: 401, aber das CSRF-Cookie ist gesetzt
        Cookie xsrf = mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");

        // 2. Login mit Cookie und gleichlautendem Header, wie es Angular automatisch macht
        MvcResult login = mvc.perform(post("/api/login")
                        .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .param("username", "tester").param("password", "test-passwort-123"))
                .andExpect(status().isNoContent())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // 3. Nach dem Login verwirft Spring den alten Token; der nächste Aufruf liefert einen neuen
        Cookie freshXsrf = mvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(freshXsrf).isNotNull();

        // 4. Schreibender Aufruf mit dem neuen Token
        mvc.perform(post("/api/notes").session(session)
                        .cookie(freshXsrf).header("X-XSRF-TOKEN", freshXsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"aus dem Browser\"}"))
                .andExpect(status().isCreated());

        // 5. Header passt nicht zum Cookie (so sähe ein Angriff von einer fremden Seite aus): abgelehnt
        mvc.perform(post("/api/notes").session(session)
                        .cookie(freshXsrf).header("X-XSRF-TOKEN", "geraten")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"alt\"}"))
                .andExpect(status().isForbidden());
    }
}
