package pe.edu.utp.tickets.ticket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.tickets.api.ApiException;
import pe.edu.utp.tickets.auth.AppUser;
import pe.edu.utp.tickets.auth.CurrentUser;
import pe.edu.utp.tickets.auth.Role;
import pe.edu.utp.tickets.company.Company;
import pe.edu.utp.tickets.config.SecurityConfig;

@WebMvcTest(controllers = TicketController.class, properties = {
        "app.jwt-secret=webmvc-test-signing-key-not-for-production",
        "app.cors-origin=http://localhost:5173"
})
@Import(SecurityConfig.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TicketService tickets;

    @MockitoBean
    private CurrentUser current;

    private AppUser client;

    private final TicketDtos.View view = new TicketDtos.View(7L, "TC-2026-00007", "VPN caída", "Sin acceso",
            1L, "Acme SAC", "Cliente Acme", Severity.ALTA, TicketStatus.ABIERTO, Instant.parse("2026-11-19T15:00:00Z"));

    @BeforeEach
    void setUp() {
        var acme = new Company("20111111111", "Acme SAC");
        ReflectionTestUtils.setField(acme, "id", 1L);
        client = new AppUser("Cliente Acme", "cliente@acme.test", "hash", Role.CLIENTE, acme);
        ReflectionTestUtils.setField(client, "id", 10L);
    }

    private JwtRequestPostProcessor asClient() {
        when(current.resolve(any(Jwt.class))).thenReturn(client);
        return jwt().jwt(token -> token.subject("10")).authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    @Test
    void anonymousRequestIsRejectedWithJsonMessage() throws Exception {
        // When / Then
        mvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Inicia sesión para continuar"));
        verifyNoInteractions(tickets);
    }

    @Test
    void listPassesQueryParametersToService() throws Exception {
        // Given
        var auth = asClient();
        when(tickets.list(client, 1, 5, TicketStatus.ABIERTO, Severity.ALTA))
                .thenReturn(new TicketDtos.PageView(List.of(view), 6, 2, 1));

        // When / Then
        mvc.perform(get("/api/tickets")
                        .param("page", "1").param("size", "5")
                        .param("status", "ABIERTO").param("severity", "ALTA")
                        .with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("TC-2026-00007"))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void unknownStatusFilterReturnsBadRequest() throws Exception {
        // When / Then
        mvc.perform(get("/api/tickets").param("status", "CERRADO").with(asClient()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solicitud inválida"));
        verifyNoInteractions(tickets);
    }

    @Test
    void detailNotFoundIsTranslatedTo404() throws Exception {
        // Given
        var auth = asClient();
        when(tickets.detail(client, 99L)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));

        // When / Then
        mvc.perform(get("/api/tickets/99").with(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket no encontrado"));
    }

    @Test
    void summaryReturnsCountsByStatus() throws Exception {
        // Given
        var auth = asClient();
        when(tickets.summary(client)).thenReturn(new TicketDtos.Summary(6, 3, 2, 1));

        // When / Then
        mvc.perform(get("/api/tickets/summary").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.inProgress").value(2));
    }

    @Test
    void validCreateReturns201WithCreatedTicket() throws Exception {
        // Given
        var auth = asClient();
        var request = new TicketDtos.CreateRequest("VPN caída", "Sin acceso", Severity.ALTA, null);
        when(tickets.create(client, request)).thenReturn(view);

        // When / Then
        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"VPN caída\",\"description\":\"Sin acceso\",\"severity\":\"ALTA\"}")
                        .with(auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("ABIERTO"));
    }

    @Test
    void blankTitleReturnsFieldErrorWithoutCallingService() throws Exception {
        // When / Then
        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"description\":\"Sin acceso\",\"severity\":\"ALTA\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Revisa los datos ingresados"))
                .andExpect(jsonPath("$.fields.title").value("Ingresa un asunto"));
        verifyNoInteractions(tickets);
    }

    @Test
    void unknownJsonFieldIsRejected() throws Exception {
        // When / Then
        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"description\":\"D\",\"severity\":\"ALTA\",\"status\":\"RESUELTO\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solicitud inválida"));
        verifyNoInteractions(tickets);
    }

    @Test
    void updateReturnsEditedTicket() throws Exception {
        // Given
        var auth = asClient();
        var edited = new TicketDtos.View(7L, "TC-2026-00007", "VPN restablecida", "Listo",
                1L, "Acme SAC", "Cliente Acme", Severity.BAJA, TicketStatus.RESUELTO, view.createdAt());
        when(tickets.update(eq(client), eq(7L), any(TicketDtos.UpdateRequest.class))).thenReturn(edited);

        // When / Then
        mvc.perform(put("/api/tickets/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"VPN restablecida\",\"description\":\"Listo\","
                                + "\"severity\":\"BAJA\",\"status\":\"RESUELTO\"}")
                        .with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESUELTO"));
        verify(tickets).update(client, 7L,
                new TicketDtos.UpdateRequest("VPN restablecida", "Listo", Severity.BAJA, TicketStatus.RESUELTO, null));
    }
}
