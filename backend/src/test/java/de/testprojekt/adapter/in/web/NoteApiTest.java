package de.testprojekt.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;

/**
 * Prüft REST-Schnittstelle, Zugriffsschutz und Datenbank zusammen.
 * Standard ist H2 im Speicher; die Pipeline setzt SPRING_DATASOURCE_URL auf einen echten Postgres.
 */
@SpringBootTest(properties = {
        "app.security.username=tester",
        "app.security.password=test-passwort-123"
})
@AutoConfigureMockMvc
class NoteApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/notes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithCorrectPassword() throws Exception {
        login("test-passwort-123", "10.0.0.1")
                .andExpect(status().isNoContent());
    }

    @Test
    void loginWithWrongPassword() throws Exception {
        login("falsch", "10.0.0.2")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/login").param("username", "tester").param("password", "test-passwort-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginIsThrottledAfterRepeatedFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("falsch", "10.0.0.3")
                    .andExpect(status().isUnauthorized());
        }
        // Auch das richtige Passwort hilft während der Sperre nicht
        login("test-passwort-123", "10.0.0.3")
                .andExpect(status().isTooManyRequests());
        // Andere Adressen sind nicht betroffen
        login("test-passwort-123", "10.0.0.4")
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void createListAndDeleteNote() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"ohne Token\"}"))
                .andExpect(status().isForbidden());

        String body = mvc.perform(post("/api/notes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"  Erste Notiz  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.text").value("Erste Notiz"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));

        mvc.perform(delete("/api/notes/" + id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/notes/" + id).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/notes")).andExpect(jsonPath("$[*].id", not(hasItem(id))));
    }

    @Test
    @WithMockUser
    void rejectsInvalidNote() throws Exception {
        mvc.perform(post("/api/notes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "tester")
    void meReturnsUsername() throws Exception {
        mvc.perform(get("/api/me")).andExpect(jsonPath("$.username").value("tester"));
    }

    @Test
    void angularRoutesFallBackToIndexHtml() throws Exception {
        // index.html liegt für den Test unter src/test/resources/static
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<app-root>")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")));
        mvc.perform(get("/gibt-es-nicht.js")).andExpect(status().isNotFound());
    }

    /** Login mit eigener Absenderadresse je Test, damit sich die Login-Bremse der Tests nicht gegenseitig stört. */
    private ResultActions login(String password, String remoteAddress) throws Exception {
        RequestPostProcessor fromAddress = request -> {
            request.setRemoteAddr(remoteAddress);
            return request;
        };
        return mvc.perform(post("/api/login").with(csrf()).with(fromAddress)
                .param("username", "tester")
                .param("password", password));
    }
}
