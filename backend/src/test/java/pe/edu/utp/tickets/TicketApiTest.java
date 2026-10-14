package pe.edu.utp.tickets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String email) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("email", email, "password", "Demo2026!"));
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn();
        JsonNode response = json.readTree(result.getResponse().getContentAsString());
        assertThat(response.get("user").has("passwordHash")).isFalse();
        return response.get("token").asText();
    }

    @Test void anonymousCannotReadTickets() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }
    @Test void incorrectCredentialsReturnGenericError() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"cliente@demo.local\",\"password\":\"incorrecta\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
    }
    @Test void clientCreatesTicketForOwnCompany() throws Exception {
        String token = login("cliente@demo.local");
        mvc.perform(post("/api/tickets").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"  Error de contabilización  \",\"description\":\"El asiento no se registra en SAP.\",\"severity\":\"ALTA\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Error de contabilización"))
            .andExpect(jsonPath("$.companyId").value(1)).andExpect(jsonPath("$.status").value("ABIERTO"))
            .andExpect(jsonPath("$.code").isNotEmpty());
    }
    @Test void clientCannotChooseAnotherCompany() throws Exception {
        mvc.perform(post("/api/tickets").header("Authorization", "Bearer " + login("cliente@demo.local"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Incidencia válida\",\"description\":\"Descripción válida\",\"severity\":\"MEDIA\",\"companyId\":2}"))
            .andExpect(status().isForbidden());
    }
    @Test void clientCannotReadOtherCompanyTicket() throws Exception {
        String otherToken = login("otrocliente@demo.local");
        var created = mvc.perform(post("/api/tickets").header("Authorization", "Bearer " + otherToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Ticket privado\",\"description\":\"Información de otra empresa\",\"severity\":\"BAJA\"}"))
            .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        String token = login("cliente@demo.local");
        mvc.perform(get("/api/tickets/" + id).header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
        var list = mvc.perform(get("/api/tickets").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn();
        for (JsonNode ticket : json.readTree(list.getResponse().getContentAsString()).get("content")) {
            assertThat(ticket.get("companyId").asLong()).isEqualTo(1);
        }
    }
    @Test void blankTitleIsRejected() throws Exception {
        mvc.perform(post("/api/tickets").header("Authorization", "Bearer " + login("cliente@demo.local"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"   \",\"description\":\"Descripción válida\",\"severity\":\"MEDIA\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void internalUserCanSelectCompany() throws Exception {
        String token = login("equipo@demo.local");
        mvc.perform(post("/api/tickets").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Incidencia interna\",\"description\":\"Creada por consultor\",\"severity\":\"CRITICA\",\"companyId\":2}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.companyId").value(2));
    }
    @Test void clientCannotRequestCompanyCatalog() throws Exception {
        mvc.perform(get("/api/companies").header("Authorization", "Bearer " + login("cliente@demo.local")))
            .andExpect(status().isForbidden());
    }
    @Test void tamperedTokenIsRejected() throws Exception {
        String token = login("cliente@demo.local");
        String[] parts = token.split("\\.");
        parts[1] = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1\",\"role\":\"INTERNO\"}".getBytes());
        mvc.perform(get("/api/tickets").header("Authorization", "Bearer " + String.join(".", parts)))
            .andExpect(status().isUnauthorized());
    }
}
